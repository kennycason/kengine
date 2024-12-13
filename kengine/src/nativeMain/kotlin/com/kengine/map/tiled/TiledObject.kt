package com.kengine.map.tiled

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

@Serializable
data class TiledObject(
    val id: Int,
    val name: String,
    val type: String,
    val x: Double,
    val y: Double,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val rotation: Double = 0.0,
    val visible: Boolean = true,
    val properties: List<Property>? = null
) {
    @Serializable
    data class Property(
        val name: String,
        val type: String,
        @Serializable(with = PropertyValueSerializer::class)
        val value: Any
    )
}

object PropertyValueSerializer : KSerializer<Any> {
    override val descriptor = buildClassSerialDescriptor("PropertyValue") {
        element("value", String.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("Expected JSON decoder")
        val element = jsonDecoder.decodeJsonElement()

        return when (element) {
            is JsonPrimitive -> when {
                element.isString -> element.content
                element.intOrNull != null -> element.int
                element.longOrNull != null -> element.long
                element.floatOrNull != null -> element.float
                element.doubleOrNull != null -> element.double
                element.booleanOrNull != null -> element.boolean
                else -> throw SerializationException("Unsupported property value type")
            }
            else -> throw SerializationException("Unsupported property value type")
        }
    }

    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("Expected JSON encoder")
        val jsonElement = when (value) {
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            else -> throw SerializationException("Unsupported property value type")
        }
        jsonEncoder.encodeJsonElement(jsonElement)
    }
}