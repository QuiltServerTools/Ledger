package com.github.quiltservertools.ledger.config.util

import com.fasterxml.jackson.annotation.JsonCreator
import net.minecraft.util.Identifier

// TODO 1.21
// In 24w21a Mojang removed the Identifier(String) constructor, which was used by konf
// konf doesn't seem to allow to register custom deserializers, so we need this wrapper,
// which provides the necessary constructor
data class IdentifierWrapper(val identifier: Identifier) {
    @JsonCreator
    constructor(id: String) : this(Identifier.of(id))
}
