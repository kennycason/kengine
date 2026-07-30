package com.kengine.storage

const val DEFAULT_PORTABLE_STORAGE_RECORD_LIMIT = 64 * 1024

interface PortableStorage {
    fun load(key: String): ByteArray?
    fun save(key: String, value: ByteArray): Boolean
    fun delete(key: String): Boolean
    fun exists(key: String): Boolean = load(key) != null

    fun loadString(key: String): String? = load(key)?.decodeToString()

    fun saveString(key: String, value: String): Boolean = save(key, value.encodeToByteArray())
}

object NoOpPortableStorage : PortableStorage {
    override fun load(key: String): ByteArray? = null
    override fun save(key: String, value: ByteArray): Boolean = false
    override fun delete(key: String): Boolean = true
    override fun exists(key: String): Boolean = false
}

object PortableStorageKey {
    const val MAX_LENGTH = 64

    fun isValid(key: String): Boolean {
        if (key.isEmpty() || key.length > MAX_LENGTH) {
            return false
        }
        for (char in key) {
            val allowed = char in 'a'..'z' ||
                char in 'A'..'Z' ||
                char in '0'..'9' ||
                char == '.' ||
                char == '_' ||
                char == '-'
            if (!allowed) {
                return false
            }
        }
        return true
    }

    fun requireValid(key: String) {
        require(isValid(key)) {
            "Portable storage keys must be 1-$MAX_LENGTH characters and contain only letters, numbers, '.', '_', or '-'."
        }
    }
}

class InMemoryPortableStorage(
    private val maxRecordSize: Int = DEFAULT_PORTABLE_STORAGE_RECORD_LIMIT
) : PortableStorage {
    private val records = mutableMapOf<String, ByteArray>()

    override fun load(key: String): ByteArray? {
        PortableStorageKey.requireValid(key)
        return records[key]?.copyOf()
    }

    override fun save(key: String, value: ByteArray): Boolean {
        PortableStorageKey.requireValid(key)
        if (value.size > maxRecordSize) {
            return false
        }
        records[key] = value.copyOf()
        return true
    }

    override fun delete(key: String): Boolean {
        PortableStorageKey.requireValid(key)
        records.remove(key)
        return true
    }

    override fun exists(key: String): Boolean {
        PortableStorageKey.requireValid(key)
        return records.containsKey(key)
    }
}
