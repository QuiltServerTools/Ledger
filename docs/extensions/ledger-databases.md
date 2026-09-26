# Ledger Databases 
Adds support for MySQL, MariaDB, H2, and PostgreSQL databases in Ledger.

# Config
These settings can be appended to the bottom of the bottom of the [config file](../config.md).

To set these as environment variables, [Konf](https://github.com/uchuhimo/konf) maps `database_extensions` to `DATABASEEXTENSIONS` (drops the underscore), e.g. set `DATABASEEXTENSIONS_PASSWORD=<your_secret>`.

### H2

Add the following to the bottom of your Ledger config file:

```toml
[database_extensions]
database = "H2"
```

### MySQL

Add the following to the bottom of your Ledger config file:

```toml
[database_extensions]
database = "MYSQL"
url = ""
username = ""
password = ""
properties = []
maxPoolSize = 10
connectionTimeout = 60000
```

`url`: Must be URL of database with `/<database_name>` appended. An example URL would be `localhost/ledger`. You can optionally add port information such as `localhost:3000/ledger`

### MariaDB

Add the following to the bottom of your Ledger config file:

```toml
[database_extensions]
database = "MARIADB"
url = ""
username = ""
password = ""
properties = []
maxPoolSize = 10
connectionTimeout = 60000
```

`url`: Must be URL of database with `/<database_name>` appended. An example URL would be `localhost/ledger`. You can optionally add port information such as `localhost:3000/ledger`

### PostgreSQL

```toml
[database_extensions]
database = "POSTGRESQL"
url = ""
username = ""
password = ""
properties = []
maxPoolSize = 10
connectionTimeout = 60000
```

### SQLite

```toml
[database_extensions]
database = "SQLITE"
```

### Connector properties

For some databases, such as MySQL, you can provide properties to the database connector. For each property, add a string entry to the `properties` array.

```toml
properties = ["useJDBCCompliantTimezoneShift=true", "useLegacyDatetimeCode=false", "serverTimezone=UTC"]
```