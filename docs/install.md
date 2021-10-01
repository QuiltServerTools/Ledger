# Installation

1. Set up a [Fabric Server](https://fabricmc.net/wiki/tutorial:installing_minecraft_fabric_server) for 1.17
2. Install [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
3. Install [Fabric Language Kotlin](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin/)
4. Install [Ledger](https://www.curseforge.com/minecraft/mc-mods/ledger)
5. Run the server
6. Adjust [config](config.md) as needed

Run into any issues? Join our [Discord](https://discord.gg/UxHnDWr) for support!

## Other Databases
Ledger supports other databases like MySQL and H2 with the help of the [Ledger Databases](https://www.curseforge.com/minecraft/mc-mods/ledger-databases) extension.

### MySQL
MySQL requires running a separate MySQL database and more setup than just plug and play sqlite, but can support much larger databases at faster speeds.
It also supports MySQL based databases like MariaDB.
MySQL support can be enabled in Ledger with the [Ledger Databases](https://www.curseforge.com/minecraft/mc-mods/ledger-databases) extension.
Once installed, enable it by adding
```toml
[database_extensions]
database = "MYSQL"
url = ""
username = ""
password = ""
```
to your `ledger.toml`.
The `url` is usually something like `localhost/databasename` unless you are using an external database server.

### H2
H2 is another flat-file database like the default sqlite that may yield faster results but is more experimental.
H2 support can be enabled in Ledger with the [Ledger Databases](https://www.curseforge.com/minecraft/mc-mods/ledger-databases) extension.
Once installed, enable it by adding
```toml
[database_extensions]
database = "H2"
```
to your `ledger.toml`.