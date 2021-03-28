package us.potatoboy.ledger.actions

class BlockBreakActionType() : AbstractActionType() {
    override val identifier: String = "block-break"
    override fun getTranslationType(): String = "block"
}