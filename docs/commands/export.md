# Export
`/ledger export`
Permission: `ledger.commands.export`

---

### `/ledger export <jdbc_url> [batch_size]`
This command will export the current database to another database specified via a JDBC URL,
skipping rows containing blacklisted items.

This operation leaves the current database untouched but
destroys any existing data in the relevant tables in the target database.

Attempting to export to the same target database concurrently may lead to unexpected results.