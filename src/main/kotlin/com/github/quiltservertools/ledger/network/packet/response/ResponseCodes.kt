package com.github.quiltservertools.ledger.network.packet.response

enum class ResponseCodes(val code: Int) {
    NO_PERMISSION(0),
    EXECUTING(1),
    COMPLETED(2),
    ERROR(3),
    BUSY(4)
}
