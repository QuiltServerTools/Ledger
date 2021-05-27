package us.potatoboy.ledger.network.packet

import net.minecraft.util.Identifier
import us.potatoboy.ledger.Ledger

enum class PacketTypes(val id: Identifier) {
    ACTION(Identifier(Ledger.MOD_ID, "action")),
    INSPECT(Identifier(Ledger.MOD_ID, "inspect")),
    SEARCH(Identifier(Ledger.MOD_ID, "search"))
}
