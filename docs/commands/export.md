# Export
`/ledger export`    
Permission: `ledger.commands.export`

---

### `/ledger export <params>`
The export command allows you to save the results of the search command to a txt file for easy sharing.
For more information, see [search](../commands/search.md)

**Configuration**

```toml
[export]
# Files export location, default at "./<world-name>/data/"
location = "..."
# Export format. "csv" only currently.
type = "csv"
```

**Exported CSV Example**

Named like `ledger-export-2024-08-04_22-45-44.csv`

| Time                | Source     | Action    | Object        | X    | Y    | Z    | World               |
| ------------------- | ---------- | --------- | ------------- | ---- | ---- | ---- | ------------------- |
| 2024-08-04 11:38:13 | yqs1123582 | picked up | 1 Black Wool  | -5   | 68   | -13  | minecraft:overworld |
| 2024-08-04 11:38:13 | yqs1123582 | picked up | 1 Raw Mutton  | -6   | 68   | -12  | minecraft:overworld |
| 2024-08-04 11:38:12 | @fire      | broke     | Oak Planks    | 65   | -39  | -13  | minecraft:overworld |
| 2024-08-04 11:38:12 | yqs1123582 | killed    | Sheep         | -5   | 68   | -13  | minecraft:overworld |
| 2024-08-04 11:38:11 | @gravity   | broke     | Fire          | -37  | -31  | -6   | minecraft:overworld |
| 2024-08-04 11:38:06 | @gravity   | broke     | Fire          | 66   | -38  | -13  | minecraft:overworld |
| 2024-08-04 11:38:06 | @fire      | broke     | Oak Planks    | 66   | -39  | -13  | minecraft:overworld |
| 2024-08-04 11:38:05 | @fire      | broke     | Oak Planks    | 66   | -35  | -12  | minecraft:overworld |
| 2024-08-04 11:38:02 | yqs1123582 | picked up | 1 Wheat Seeds | -6   | 68   | -4   | minecraft:overworld |
| 2024-08-04 11:38:01 | @gravity   | broke     | Fire          | 65   | -36  | -10  | minecraft:overworld |
| 2024-08-04 11:38:01 | @gravity   | broke     | Fire          | 66   | -36  | -9   | minecraft:overworld |
| 2024-08-04 11:38:01 | @fire      | broke     | Oak Planks    | 65   | -36  | -9   | minecraft:overworld |
| 2024-08-04 11:38:00 | yqs1123582 | broke     | Grass Block   | -6   | 68   | -3   | minecraft:overworld |
| 2024-08-04 11:38:00 | @gravity   | broke     | Short Grass   | -6   | 69   | -3   | minecraft:overworld |
| 2024-08-04 11:37:58 | @fire      | broke     | Oak Planks    | 66   | -36  | -9   | minecraft:overworld |
| 2024-08-04 11:37:58 | @gravity   | broke     | Fire          | 66   | -38  | -10  | minecraft:overworld |
| 2024-08-04 11:37:58 | @fire      | broke     | Oak Fence     | 66   | -38  | -9   | minecraft:overworld |
| 2024-08-04 11:37:55 | @fluid     | placed    | Obsidian      | 76   | -55  | 12   | minecraft:overworld |
| 2024-08-04 11:37:55 | @fluid     | placed    | Obsidian      | 77   | -55  | 12   | minecraft:overworld |
