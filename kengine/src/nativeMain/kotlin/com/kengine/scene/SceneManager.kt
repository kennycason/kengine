package com.kengine.scene

import com.kengine.log.Logging
import com.kengine.time.getClockContext

class SceneManager : Logging {
    private val stack = mutableListOf<Scene>()
    private var transition: ActiveTransition? = null

    val currentScene: Scene? get() = stack.lastOrNull()
    val depth: Int get() = stack.size
    val isTransitioning: Boolean get() = transition != null

    fun push(scene: Scene, transition: SceneTransition? = null) {
        val from = currentScene
        from?.pause()

        stack.add(scene)
        scene.enter()
        logger.info { "Scene pushed: ${scene::class.simpleName}, stack depth: ${stack.size}" }

        if (transition != null && from != null) {
            startTransition(from, scene, transition)
        }
    }

    fun pop(transition: SceneTransition? = null): Scene? {
        if (stack.isEmpty()) return null

        val removed = stack.removeLast()
        val to = currentScene

        logger.info { "Scene popped: ${removed::class.simpleName}, stack depth: ${stack.size}" }

        if (transition != null && to != null) {
            startTransition(removed, to, transition) {
                removed.exit()
                removed.cleanup()
                to.resume()
            }
        } else {
            removed.exit()
            removed.cleanup()
            to?.resume()
        }

        return removed
    }

    fun replace(scene: Scene, transition: SceneTransition? = null) {
        if (stack.isEmpty()) {
            push(scene)
            return
        }

        val removed = stack.removeLast()
        stack.add(scene)
        scene.enter()
        logger.info { "Scene replaced: ${removed::class.simpleName} -> ${scene::class.simpleName}" }

        if (transition != null) {
            startTransition(removed, scene, transition) {
                removed.exit()
                removed.cleanup()
            }
        } else {
            removed.exit()
            removed.cleanup()
        }
    }

    fun update() {
        if (transition != null) {
            updateTransition()
        } else {
            currentScene?.update()
        }
    }

    fun draw() {
        val t = transition
        if (t != null) {
            t.transition.render(t.progress, t.from, t.to)
        } else {
            currentScene?.draw()
        }
    }

    fun cleanup() {
        stack.asReversed().forEach { scene ->
            scene.exit()
            scene.cleanup()
        }
        stack.clear()
        transition = null
    }

    private fun startTransition(
        from: Scene,
        to: Scene,
        transition: SceneTransition,
        onComplete: (() -> Unit)? = null
    ) {
        this.transition = ActiveTransition(
            from = from,
            to = to,
            transition = transition,
            startTimeMs = getClockContext().totalTimeMs,
            onComplete = onComplete
        )
    }

    private fun updateTransition() {
        val t = transition ?: return
        val elapsed = getClockContext().totalTimeMs - t.startTimeMs
        t.progress = if (t.transition.durationMs <= 0) 1.0
            else (elapsed / t.transition.durationMs.toDouble()).coerceIn(0.0, 1.0)

        t.to.update()

        if (t.transition.isComplete(t.progress)) {
            t.onComplete?.invoke()
            transition = null
        }
    }

    private class ActiveTransition(
        val from: Scene,
        val to: Scene,
        val transition: SceneTransition,
        val startTimeMs: Long,
        val onComplete: (() -> Unit)?,
        var progress: Double = 0.0
    )
}
