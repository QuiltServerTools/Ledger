# Changelog

## 1.3.24-reopt.2 (2026-10-05)

Reoptimization pass over upstream Ledger 1.3.24 for MC 26.3 Fabric.
This entry describes the current version. Changes introduced since reopt.1 are
listed first, followed by a cumulative summary of everything this build carries
relative to upstream.

### Changed since reopt.1

- **Integer time index (main storage win).** `actions.time` stores TEXT
  (`'2026-10-05 13:22:21.702'`, ~23 bytes). An index over that key cost ~35 bytes
  per row - measured at 19.6% of the entire database, the largest non-table object.
  A new `time_ms` column holds the same instant as epoch milliseconds and carries
  the index instead:
  - time index: **35.5 -> 16.4 B/row (-54%)**
  - whole database, like-for-like (both fresh, neither vacuumed):
    **181.44 -> 167.2 B/row (-7.9%)**
  - an independent 500,000-row synthetic measurement reproduces -53% index size and
    -11.5% file size
  Note on the two sizes: running `/ledger compact` after the migration reclaims the
  pages the dropped index leaves on the freelist, which takes an existing database
  down by roughly 12%. Part of that comes from VACUUM itself - which any index-drop
  would also produce - so the honest attribution for this change is the -7.9%
  like-for-like figure above.
  The TEXT column is kept and written alongside, so display strings and any external
  tooling reading `time` are unaffected.
- **Automatic, resumable migration.** On startup the column is added, indexed and
  backfilled in id-ordered batches; once no sentinel values remain the superseded
  TEXT index is dropped. Every value is verified to be an exact millisecond match for
  its TEXT source, distinct counts are preserved, and no rows are lost
  (`bench/test_time_migration.py`, 8/8 checks). If a backfill cannot finish, queries
  transparently fall back to the TEXT column, so a partially migrated database still
  returns correct results. With `updateSchema = false` the migration does not run at
  all and the plugin stays on the original schema.
- **SQLite connection tuning.** `temp_store=MEMORY`, 16 MiB page cache, 256 MiB
  `mmap_size`, 64 MiB `journal_size_limit`, applied through `SQLiteConfig` so they
  reach every pooled connection. Verified applied via JDBC, not assumed.
- **Adaptive rollback/restore tick budget.** The fixed 25 ms budget is replaced by one
  derived from the server's smoothed tick time (35 ms idle, 5 ms when ticks are already
  at capacity). Scope note: on an idle benchmark server this changes nothing
  measurable - a rollback was measured completing with zero yields, so the budget was
  never its bottleneck. The benefit is on a loaded server, which a benchmark cannot
  show. A previously recorded claim that a large share of rollback wall time went into
  `delay(1)` was wrong and has been removed from the source comments.

### Evaluated and rejected

- **Composite `(time_ms, id)` and `(time_ms, rolled_back, id)` indexes.** Once the time
  key became an 8-byte integer these became affordable in principle, so they were
  measured at 500,000 rows. Both still trigger `USE TEMP B-TREE FOR ORDER BY` for
  Ledger's `ORDER BY id DESC LIMIT n` queries, giving no query improvement (the
  differences seen were within noise) while adding 3.0% / 3.7% to the file. Not
  adopted.

### Cumulative summary vs upstream 1.3.24

- **Fixed - MC 26.3 block-state NBT key rename.** Vanilla `writeBlockState` /
  `readBlockState` renamed `Name` -> `id` and `Properties` -> `properties`; upstream
  still read the old keys, so every serialized block state was null on 26.3 and
  stateful blocks (stairs, logs, doors, ...) rolled back to their default state. Both
  key sets are now accepted on read; new writes use the 26.3 format.
- **Fixed - rollback keyset off-by-one** that could exclude the newest action row.
- **Block-state dictionary encoding** - identical state strings are stored once in
  `block_states` and referenced by integer id. Measured on a stairs workload, it saves
  31.3% (55.8 bytes per row) versus storing the literal state text.
- **`/ledger compact`** - batched migration of legacy text states into the dictionary
  plus `VACUUM`. Also the way to reclaim the space freed by the index change above.
- **Streaming rollback/restore** - keyset pagination with per-batch progress commits.
- **COUNT(*) result cache** (30 s TTL).
- **SQLite tuning** - WAL journal mode, `synchronous=NORMAL`, 10 s busy timeout.
- **Faster shutdown drain** with `NonCancellable` batch writes.
- **Reverted composite indexes** that an intermediate revision of this branch added;
  they inflated the index footprint by 127% for no query benefit, and a startup
  migration now repairs databases that carry them.

### Verification (live MC 26.3 Fabric server)

- Boot, setblock/search/rollback/restore/status/compact all run with zero exceptions.
- Rollback -> restore round-trip preserves exact block state
  (`execute if block ... oak_stairs[facing=north,half=top]` passes after restore).
- Dictionary round-trip and `compact` behaviour verified against a live database.
- Time migration: 8/8 checks on a real server boot.
- Index-repair migration: 4/4 checks on a database carrying the intermediate layout.
- Benchmarks A (13,500 uniform placements) and B (stateful stairs) both report **zero
  errors and exactly 13,500 rows** for every run.

### Benchmarked against CoreProtect 24.1 (MC 26.3)

See `ledger-vs-coreprotect-性能实测报告.html` at the project root. Medians of three
runs, 13,500 block placements:

| | Ledger reopt.2 | Ledger reopt.1 | CoreProtect (DuckDB) |
|---|---|---|---|
| bytes / row | 167.2 | 181.4 | 107.7 |
| ingest (rows/s) | 1,917 | 1,957 | 2,156 |
| lookup | 185 ms | 159 ms | 153 ms |
| rollback | 3.47 s | 3.95 s | 0.61 s |

**Only the bytes-per-row row is a real, reproducible result** - it comes from the
schema itself and is independent of machine load. Every other row moved by more
between campaigns than between the two builds: in an earlier campaign reopt.1
ingested at 1,959 rows/s and rolled back in 2.63-2.69 s, while in the final one it
did 1,957 rows/s and 3.95 s. Ingest, lookup and rollback are therefore reported as
*no measurable change*, not as improvements.
