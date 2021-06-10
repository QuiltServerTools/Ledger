# Restore
`/ledger restore`  
Alias: None  
Permission: `ledger.commands.rollback`

---

### `/ledger restore <params>`
This command will find **all** actions that match the [parameters](../parameters.md) that ***have*** already been rolled back.
It will then undo all possible selected actions. 
Basically, it undoes [rollbacks](../commands/rollback.md).
It is ***highly*** recommended to use the [preview command](../commands/preview.md) instead to make sure you are doing what you want.

**Supported Restores**

- Block
- Item
- Entity