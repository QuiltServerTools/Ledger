# Configuration

Ledger's configuration file is found in `config/ledger.toml`.
It is written in [TOML](https://toml.io/en/) and can be edited in any text editor.
Any changes you make to the config will be automatically updated without needing to reload or restart the server.
When checking for the config for a value, it first checks the system properties,
then the system environment, then the config file and finally it will use the default.
For more info, read about [Konf](https://github.com/uchuhimo/konf).
To regenerate the config file, simply delete it.

### Database settings

Found under `[database]`

`queueTimeoutMin` [Default: 5] is the maximum amount of time to wait for the queue to drain when the server stops in minutes

`queueCheckDelaySec` [Default: 10] is the amount of time between checking if the queue is empty when the server stops in seconds

### Search settings

Found under `[search]`

`pageSize` [Default: 8] controls the number of actions displayed per page

`purgePermissionLevel` [Default: 4] controls the permission level required to run the purge command

### Message theme

Found under `[color]`

Ledger allows for the customisation of the colors used in the messages sent in game. 
By default, Ledger uses the blue theme. More themes can be found in the [themes](themes.md) file

### Filters

These allow you to control what is and is not logged

Found under `[actions]`

All listed here are arrays and are formatted like so:
```toml
array = []
blocks = ["minecraft:air", "minecraft:dirt"]
```

`typeBlacklist` [Default: empty] controls what action types are logged. Hover over the action in a chat message to see the type

`worldBlacklist` [Default: empty] controls in which dimensions events are logged.

`objectBlacklist` [Default: empty] controls which objects are logged. These can be item types, block types or entities

`sourceBlacklist` [Default: empty] controls which sources are logged. Examples are `"lava"` and `"gravity"`

## Default Config
```toml
[database]
# The maximum amount of time to wait for the queue to drain when the server stops
queueTimeoutMin = 5
# The amount of time between checking if the queue is empty when the server stops
queueCheckDelaySec = 10

[search]
# Number of actions to show per page
pageSize = 8
# Permission level for purge command
purgePermissionLevel = 4

[color]
# Colors in hex format
primary = "#009688"
primaryVariant = "#52c7b8"
secondary = "#1e88e5"
secondaryVariant = "#6ab7ff"
light = "#c5d6f0"

[actions]
# Blacklists - blacklisted things will not be logged in the database
# Example - Prevent all actions with stone or bat as the object from being logged
# objectBlacklist = [
#   "minecraft:stone",
#   "minecraft:bat"
# ]

# Blacklists action types. Ex: "block-break", "entity-kill"
typeBlacklist = []
# Blacklists worlds/dimensions. Ex: "mincraft:the_end", "minecraft:overworld"
worldBlacklist = []
# Blacklists objects (Items, Mobs, Blocks). Ex: "minecraft:cobblestone", "minecraft:blaze"
objectBlacklist = []
# Blacklists sources. Ex: "lava", "gravity", "fire", "fall"
sourceBlacklist = []

[networking]
# This section relates to Ledger's ability to interact with client mods for ease of use
# Networking is enabled by default but you can disable it here

# Change to true to allow Ledger client mod packets
networking = true
```