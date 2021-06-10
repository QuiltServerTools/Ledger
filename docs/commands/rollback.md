# Rollback
`/ledger rollback`  
Alias: `rb`  
Permission: `ledger.commands.rollback`

---

### `/ledger rollback <params>`
This command will find **all** actions that match the [parameters](../parameters.md) that ***haven't*** already been rolled back.
It will then undo all possible selected actions. 
To reverse a rollback, use the [restore command](../commands/restore.md).
It is ***highly*** recommended to use the [preview command](../commands/preview.md) instead to make sure you are doing what you want.

**Supported Rollbacks**

- Block
- Item
- Entity