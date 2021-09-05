[![discord](https://img.shields.io/discord/764543203772334100?label=discord)](https://discord.gg/UxHnDWr)

For a much better guide, visit our [wiki](https://quiltservertools.github.io/Ledger/latest/)

## Ledger

A world change logging tool for fabric

Ledger can be found on discord at https://discord.gg/GtwDTTr3pe

### Install

Put Ledger in your mods folder along with Fabric API and fabric-language-kotlin. When you launch your server, the config file will be created automatically

### Configuration

Ledger's configuration file is found in `config/ledger.toml`

#### Message theme

Found under `[color]`

Ledger allows for the customisation of the colors used in the messages sent in game. By default, Ledger uses the blue theme. More themes can be found in the [themes](./themes.md) file

#### Search settings

Found under `[search]`

`pageSize` [Default: 8] controls the number of actions displayed per page

#### Database settings

Found under `[database]`

`maxQueueSize` [Default: 50] is the number of items logged before writing to the database

`queueTimeoutSec` [Default: 5] is the maximum amount of time to wait for the queue to fill before writing

#### Filters

These allow you to control what is and is not logged

Found under `[actions]`

All listed here are arrays and are formatted like so:
```
array = []
blocks = ["minecraft:air", "minecraft:dirt"]
```

`typeBlacklist` [Default: empty] controls what action types are logged. Hover over the action in a chat message to see the type

`worldBlacklist` [Default: empty] controls in which dimensions events are logged. Provide

`objectBlacklist` [Default: empty] controls which objects are logged. These can be item types, block types or entities

`sourceBlacklist` [Default: empty] controls which sources are logged. Examples are `"lava"` and `"gravity"`

### Commands

#### Inspect

`/lg inspect` - toggles inspect mode
`/lg inspect [on|off]` - enables or disables inspect mode
`/lg inspect <pos>` - inspects the block at a given position

### Search

`/lg search <args>` - searches with the given arguments

### Rollback

`/lg rollback <args>` - rollbacks with filters specified

### Page

`/lg page <index>`

When viewing results, you can use this command to quickly skip to a certain page

### Permissions

All Ledger commands support the Luckperms API

The permission nodes are `ledger.<command>`

All ledger command have fallback on permission level 3 should you not wish to have a permissions mod installed

### Contribute

Ledger is written in kotlin, and therefore all contributions should be in kotlin where possible. The sole exception to this is mixins, which must be written in java
