package com.kengine.map.tiled

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object LayerDataDecoder {
    fun decode(layer: Layer): List<Int> {
        return decode(layer.data, layer.encoding)
    }
    fun decode(data: String, encoding: String): List<Int> {
        if (encoding != "base64") {
            throw IllegalArgumentException("Unsupported encoding: $encoding")
        }

        val decoded = Base64.decode(data)
        return List(decoded.size / 4) { index ->
            decoded[index * 4].toInt() and 0xFF or
                    ((decoded[index * 4 + 1].toInt() and 0xFF) shl 8) or
                    ((decoded[index * 4 + 2].toInt() and 0xFF) shl 16) or
                    ((decoded[index * 4 + 3].toInt() and 0xFF) shl 24)
        }
    }
}