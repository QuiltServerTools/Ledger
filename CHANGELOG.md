# Changelog

## 1.3.24-reopt.1 (2026-10-04)

Reoptimization pass over upstream Ledger 1.3.24 for MC 26.3 Fabric.
Priorities: (1) zero errors on MC 26.3, (2) runtime performance, (3) database size.

### Fixed

- **MC 26.3 block-state NBT key rename** — vanilla `writeBlockState`/`readBlockState`
  renamed `Name` -> `id` and `Properties` -> `properties`. Upstream 1.3.24 still used
  the old keys, so every serialized blockState was `null` on 26.3 and stateful blocks
  (stairs, logs, doors, ...) rolled back to their default state. Both keys are now
  accepted when reading; new writes use the 26.3 format. Stored property compounds
  are unchanged between versions, so pre-26.3 rows still parse correctly.
- **Rollback keyset off-by-one** — the initial cursor could exclude the newest action
  row, causing "Rolling back N actions" to restore only N-1.

### Performance

- **Block-state dictionary encoding** — identical state strings (e.g. the same stairs
  orientation placed 10,000 times) are stored once in a new `block_states` table and
  referenced by integer id from `actions.block_state_ref` / `old_block_state_ref`.
  New rows no longer repeat state text; legacy text rows are still read transparently.
- **`/ledger compact`** (new command, `ledger.commands.purge` permission) — migrates
  legacy text block-state rows into the dictionary in batches with progress reporting,
  then runs `VACUUM` to reclaim space. Repeat-safe and crash-safe (batched commits).
- **Streaming rollback/restore** — actions are processed in keyset-paginated batches
  (<=1000 rows) under a 25 ms main-thread budget per tick; partial progress is
  committed per batch so a crash never loses completed work. Progress messages every
  5 batches. Large rollbacks no longer freeze the server or load everything in memory.
- **Composite indexes** for the common search/rollback access patterns
  (time + position + rolled_back); superseded single-column indexes are dropped
  after migration.
- **COUNT(*) result cache** (30 s TTL) to stop re-counting the whole table on every
  page of a search result.
- **SQLite tuning** — WAL journal mode, `synchronous=NORMAL`, 10 s busy timeout.
- **Faster shutdown drain** — the action queue flushes with larger batches at shutdown
  and guards each batch write with `NonCancellable`, so a server stop signal can no
  longer drop a half-written batch (upstream race).

### Verification (live MC 26.3 Fabric server)

- Boot, setblock/search/rollback/restore/status/compact all run with zero exceptions.
- Rollback -> restore round-trip preserves exact block state
  (`execute if block ... oak_stairs[facing=north,half=top]` passes after restore).
- Dictionary round-trip: new writes create refs; `compact` dedupes existing entries,
  creates new ones for unseen states, nulls all text columns; search output is
  identical before/after migration; VACUUM shrinks the file.
- Schema migration on a pre-reoptimization database: `block_states` table and ref
  columns added, indexes rebuilt, WAL active.
