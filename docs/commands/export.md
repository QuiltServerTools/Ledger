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
# Export format. "csv" only currently
type = "csv"
# The number of actions to query from the database at once when exporting
batchSize = 10000
```

**Exported CSV Example**

Named like `ledger-export-2024-08-04_22-45-44.csv`

| Time           | Source   | Action    | Object              | X    | Y    | Z    | World                | Extra                                           |
| -------------- | -------- | --------- | ------------------- | ---- | ---- | ---- | -------------------- | ----------------------------------------------- |
| 2024/8/5 13:36 | X1aoSa   | added     | 64 Cobblestone      | -19  | 62   | 121  | minecraft:overworld  | {count:64,id:"minecraft:cobblestone"}           |
| 2024/8/5 13:36 | X1aoSa   | picked up | 1 Red Shulker Box   | -15  | 65   | 119  | minecraft:overworld  | {components:{"minecraft:container":[{…          |
| 2024/8/5 13:36 | X1aoSa   | broke     | Red Shulker Box     | -15  | 65   | 120  | minecraft:overworld  | {Items:[{Slot:1b,count:64,id:"minecraft:…       |
| 2024/8/5 13:36 | X1aoSa   | removed   | 64 Cobblestone      | -15  | 65   | 120  | minecraft:overworld  | {count:64,id:"minecraft:cobblestone"}           |
| 2024/8/5 13:36 | X1aoSa   | removed   | 64 Cobblestone      | -15  | 65   | 120  | minecraft:overworld  | {count:64,id:"minecraft:cobblestone"}           |
| 2024/8/5 13:36 | X1aoSa   | placed    | Red Shulker Box     | -15  | 65   | 120  | minecraft:overworld  | {Items:[{Slot:0b,count:64,id:"minecraft:…       |
| 2024/8/5 13:36 | X1aoSa   | picked up | 1 White Shulker Box | -16  | 65   | 121  | minecraft:overworld  | {components:{"minecraft:container":[…           |
| 2024/8/5 13:36 | X1aoSa   | broke     | White Shulker Box   | -16  | 65   | 120  | minecraft:overworld  | {Items:[{Slot:0b,count:64,id:"minecraft:…       |
| 2024/8/5 13:36 | X1aoSa   | placed    | White Shulker Box   | -16  | 65   | 120  | minecraft:overworld  | {Items:[{Slot:0b,count:64,id:"minecraft:rail... |
| 2024/8/5 13:35 | X1aoSa   | removed   | 64 Smooth Stone     | -17  | 64   | 104  | minecraft:overworld  | {count:64,id:"minecraft:smooth_stone"}          |
| 2024/8/5 13:35 | X1aoSa   | removed   | 64 Smooth Stone     | -17  | 64   | 104  | minecraft:overworld  | {count:64,id:"minecraft:smooth_stone"}          |
| 2024/8/5 13:35 | X1aoSa   | removed   | 2 Smooth Stone      | -17  | 64   | 104  | minecraft:overworld  | {count:2,id:"minecraft:smooth_stone"}           |
| 2024/8/5 13:35 | X1aoSa   | removed   | 11 Smooth Stone     | -17  | 64   | 104  | minecraft:overworld  | {count:11,id:"minecraft:smooth_stone"}          |
| 2024/8/5 13:35 | X1aoSa   | added     | 64 Cobblestone      | -19  | 62   | 121  | minecraft:overworld  | {count:64,id:"minecraft:cobblestone"}           |
| 2024/8/5 13:34 | X1aoSa   | changed   | Lever               | -24  | 65   | 119  | minecraft:overworld  |                                                 |
| 2024/8/5 13:34 | X1aoSa   | changed   | Lever               | -24  | 65   | 119  | minecraft:overworld  |                                                 |
| 2024/8/5 13:34 | @lava    | killed    | Bat                 | -9   | -55  | 152  | minecraft:overworld  | {AbsorptionAmount:0.0f,Air:300s,…               |
| 2024/8/5 13:34 | @fall    | killed    | Zombified Piglin    | 36   | 188  | 98   | minecraft:the_nether | {AbsorptionAmount:0.0f,Air:300s,…               |
| 2024/8/5 13:34 | @fall    | killed    | Zombified Piglin    | 35   | 188  | 98   | minecraft:the_nether | {AbsorptionAmount:0.0f,Air:300s,…               |
| 2024/8/5 13:34 | X1aoSa   | killed    | Magma Cube          | -14  | 65   | 129  | minecraft:overworld  | {AbsorptionAmount:0.0f,Air:300s,…               |
| 2024/8/5 13:34 | X1aoSa   | broke     | Sunflower           | -14  | 65   | 128  | minecraft:overworld  |                                                 |
| 2024/8/5 13:34 | @gravity | broke     | Sunflower           | -14  | 66   | 128  | minecraft:overworld  |                                                 |
