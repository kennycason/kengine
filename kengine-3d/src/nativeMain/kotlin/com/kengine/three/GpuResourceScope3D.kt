package com.kengine.three

interface GpuResource3D {
    fun cleanup()
}

class GpuResourceScope3D : GpuResource3D {
    private val cleanupTasks = mutableListOf<() -> Unit>()
    private var cleanedUp = false

    fun <T : GpuResource3D> track(resource: T): T {
        return track(resource) { it.cleanup() }
    }

    fun <T> track(
        resource: T,
        cleanup: (T) -> Unit
    ): T {
        check(!cleanedUp) {
            "GpuResourceScope3D has already been cleaned up."
        }
        cleanupTasks += { cleanup(resource) }
        return resource
    }

    override fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        var firstError: Throwable? = null
        cleanupTasks.asReversed().forEach { cleanup ->
            try {
                cleanup()
            } catch (e: Throwable) {
                if (firstError == null) {
                    firstError = e
                }
            }
        }
        cleanupTasks.clear()
        firstError?.let { throw it }
    }
}
