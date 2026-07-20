package com.kengine.three

internal class GpuResourceCache3D<K, T>(
    private val cleanup: (T) -> Unit
) : GpuResource3D {
    private val resources = linkedMapOf<K, T>()
    private var cleanedUp = false

    val size: Int
        get() = resources.size

    fun getOrPut(
        key: K,
        load: () -> T
    ): T {
        check(!cleanedUp) {
            "GpuResourceCache3D has already been cleaned up."
        }

        return resources[key] ?: load().also { resource ->
            resources[key] = resource
        }
    }

    fun containsKey(key: K): Boolean {
        return resources.containsKey(key)
    }

    fun get(key: K): T? {
        return resources[key]
    }

    override fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        var firstError: Throwable? = null
        val cachedResources = resources.values.toList().asReversed().distinct()
        resources.clear()
        cachedResources.forEach { resource ->
            try {
                cleanup(resource)
            } catch (e: Throwable) {
                if (firstError == null) {
                    firstError = e
                }
            }
        }
        firstError?.let { throw it }
    }
}
