package com.github.quiltservertools.ledger.config.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import net.minecraft.util.Identifier

object IdentifierSerializer : JsonSerializer<Identifier>() {
    override fun serialize(value: Identifier, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.toString())
    }
}

object IdentifierDeserializer : JsonDeserializer<Identifier>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Identifier = Identifier.of(p.valueAsString)
}

@JsonSerialize(using = IdentifierSerializer::class)
@JsonDeserialize(using = IdentifierDeserializer::class)
abstract class IdentifierMixin
