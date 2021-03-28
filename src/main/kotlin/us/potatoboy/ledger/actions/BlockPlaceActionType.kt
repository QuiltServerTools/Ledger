package us.potatoboy.ledger.actions

class BlockPlaceActionType() : AbstractActionType() {
    override val identifier: String = "block-place"
    override fun getTranslationType(): String = "block"
}