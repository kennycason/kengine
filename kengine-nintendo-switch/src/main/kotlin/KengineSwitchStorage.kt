@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import com.kengine.storage.DEFAULT_PORTABLE_STORAGE_RECORD_LIMIT
import com.kengine.storage.PortableStorage
import com.kengine.storage.PortableStorageKey
import kengine.switchhost.kengine_switch_storage_delete
import kengine.switchhost.kengine_switch_storage_exists
import kengine.switchhost.kengine_switch_storage_load
import kengine.switchhost.kengine_switch_storage_save
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

private const val SWITCH_STORAGE_KEY_PREFIX_MAX_LENGTH = 32

class SwitchPortableStorage(
    namespace: String,
    private val maxRecordSize: Int = DEFAULT_PORTABLE_STORAGE_RECORD_LIMIT
) : PortableStorage {
    private val keyPrefix = sanitizeKeyPrefix(namespace)

    override fun load(key: String): ByteArray? {
        val hostKey = hostKey(key)
        val destination = ByteArray(maxRecordSize)
        val read = destination.usePinned { pinned ->
            kengine_switch_storage_load(hostKey, pinned.addressOf(0), destination.size)
        }
        if (read < 0) {
            return null
        }
        return destination.copyOf(read)
    }

    override fun save(key: String, value: ByteArray): Boolean {
        if (value.size > maxRecordSize) {
            return false
        }
        val hostKey = hostKey(key)
        val result = if (value.isEmpty()) {
            kengine_switch_storage_save(hostKey, null, 0)
        } else {
            value.usePinned { pinned ->
                kengine_switch_storage_save(hostKey, pinned.addressOf(0), value.size)
            }
        }
        return result == 1
    }

    override fun delete(key: String): Boolean {
        val hostKey = hostKey(key)
        return kengine_switch_storage_delete(hostKey) == 1
    }

    override fun exists(key: String): Boolean {
        val hostKey = hostKey(key)
        return kengine_switch_storage_exists(hostKey) == 1
    }

    private fun hostKey(key: String): String {
        PortableStorageKey.requireValid(key)
        val hostKey = "$keyPrefix.$key"
        PortableStorageKey.requireValid(hostKey)
        return hostKey
    }
}

private fun sanitizeKeyPrefix(namespace: String): String {
    val sanitized = namespace.map { char ->
        if (
            char in 'a'..'z' ||
            char in 'A'..'Z' ||
            char in '0'..'9' ||
            char == '_' ||
            char == '-'
        ) {
            char
        } else {
            '_'
        }
    }.joinToString("").trim('_', '-')
    return sanitized.ifEmpty { "kengine" }.take(SWITCH_STORAGE_KEY_PREFIX_MAX_LENGTH)
}
