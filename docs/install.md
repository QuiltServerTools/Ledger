# Installation

1. Set up a [Fabric Server](https://fabricmc.net/wiki/tutorial:installing_minecraft_fabric_server) for 1.17
2. Install [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
3. Install [Fabric Language Kotlin](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin/)
4. Install [Ledger](https://www.curseforge.com/minecraft/mc-mods/ledger)
5. Run the server
6. Adjust [config](config.md) as needed

Run into any issues? Join our [Discord](https://discord.gg/UxHnDWr) for support!

## Other Databases
Ledger supports other databases like MySQL, PostgreSQL and H2 with the help of the [Ledger Databases](https://www.curseforge.com/minecraft/mc-mods/ledger-databases) extension.

### H2
H2 is another flat-file database like the default sqlite that may yield faster results but is more experimental.

Add the following to the bottom of your Ledger config file:

```toml
[database_extensions]
database = "H2"
```

### MySQL
MySQL requires running a separate MySQL database and more setup than just plug and play SQLite, but can support much larger databases at faster speeds.
It also supports MySQL based databases like MariaDB.

Add the following to the bottom of your Ledger config file:

```toml
[database_extensions]
database = "MYSQL"
url = ""
username = ""
password = ""
properties = []
```

`url`: Must be URL of database with `/<database_name>` appended. An example URL would be `localhost/ledger`. You can optionally add port information such as `localhost:3000/ledger`

### PostgreSQL
MySQL requires running a separate PostgreSQL database and more setup than just plug and play SQLite, but can support much larger databases at faster speeds. It is more experimental the MySQL but may yield faster performance.

Add the following to the bottom of your Ledger config file:

```toml
[database_extensions]
database = "POSTGRESQL"
url = ""
username = ""
password = ""
properties = []
```

`url`: Must be URL of database with `/<database_name>` appended. An example URL would be `localhost/ledger`. You can optionally add port information such as `localhost:3000/ledger`

## Connector properties

For some databases, such as MySQL, you can provide properties to the database connector. For each property, add a string entry to the `properties` array.

```toml
properties = ["useJDBCCompliantTimezoneShift=true", "useLegacyDatetimeCode=false", "serverTimezone=UTC"]
```
