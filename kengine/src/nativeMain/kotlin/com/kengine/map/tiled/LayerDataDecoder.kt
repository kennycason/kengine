package com.kengine.map.tiled

import com.kengine.log.Logging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import platform.zlib.Z_DATA_ERROR
import platform.zlib.Z_MEM_ERROR
import platform.zlib.Z_NEED_DICT
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit
import platform.zlib.z_stream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object LayerDataDecoder : Logging {

    fun decode(layer: TiledMapLayer): List<Int> {
        return when (layer.compression) {
            "zlib" -> decode(decompressZlib(layer.data!!), layer.encoding.orEmpty())
            "", null -> {
                if (layer.data == null) listOf()
                else decode(layer.data, layer.encoding.orEmpty())
            }
            else -> throw IllegalArgumentException("Unsupported compression: ${layer.compression}")
        }
    }

    fun decode(data: String, encoding: String): List<Int> {
        if (encoding != "base64") {
            throw IllegalArgumentException("Unsupported encoding: $encoding")
        }

        val decoded = Base64.decode(data)
        return bytesToInts(decoded)
    }

    private fun bytesToInts(bytes: ByteArray): List<Int> {
        return List(bytes.size / 4) { index ->
            bytes[index * 4].toInt() and 0xFF or ((bytes[index * 4 + 1].toInt() and 0xFF) shl 8) or ((bytes[index * 4 + 2].toInt() and 0xFF) shl 16) or ((bytes[index * 4 + 3].toInt() and 0xFF) shl 24)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun decompressZlib(base64Data: String): String {
        val compressedData = Base64.decode(base64Data)

        memScoped {
            val stream = alloc<z_stream>()
            stream.zalloc = null
            stream.zfree = null
            stream.opaque = null

            // convert ByteArray to UByteArray for correct pointer type
            val compressedUBytes = compressedData.asUByteArray()

            stream.avail_in = compressedUBytes.size.toUInt()
            stream.next_in = compressedUBytes.refTo(0).getPointer(this)

            // initialize inflation
            val initResult = inflateInit(stream.ptr)
            if (initResult != Z_OK) {
                throw RuntimeException("Failed to initialize zlib inflation: $initResult")
            }

            try {
                val buffer = UByteArray(16384)
                val decompressed = ArrayList<UByte>()

                while (true) {
                    stream.avail_out = buffer.size.toUInt()
                    stream.next_out = buffer.refTo(0).getPointer(this)

                    val ret = inflate(stream.ptr, Z_NO_FLUSH)
                    when (ret) {
                        Z_NEED_DICT, Z_DATA_ERROR, Z_MEM_ERROR -> {
                            logger.error { "Zlib decompression failed with code: $ret" }
                            throw RuntimeException("Zlib decompression failed")
                        }
                    }

                    val have = buffer.size - stream.avail_out.toInt()
                    decompressed.addAll(buffer.take(have))

                    if (ret == Z_STREAM_END) break
                }

                // convert UByteArray back to ByteArray
                val resultBytes = decompressed.map { it.toByte() }.toByteArray()

                // return as base64 string
                return Base64.encode(resultBytes)
            } finally {
                inflateEnd(stream.ptr)
            }
        }
    }
}