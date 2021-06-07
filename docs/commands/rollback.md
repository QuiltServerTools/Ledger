# Preview
`/ledger preview`  
Alias: `pv`  
Permission: `ledger.commands.preview`

---

### `/ledger preview rollback <params>`
This command will find **all** actions that match the [parameters](../parameters.md) that ***haven't*** already been rolled back.
It will then undo all possible selected actions.
It is ***highly*** recommended to use the

**Supported Rollbacks**

- Block
- Item
- Entity