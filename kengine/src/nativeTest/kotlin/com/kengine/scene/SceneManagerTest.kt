package com.kengine.scene

import com.kengine.test.expectThat
import kotlin.test.Test

class SceneManagerTest {

    private class TestScene(val name: String) : Scene {
        val events = mutableListOf<String>()
        var updateCount = 0
        var drawCount = 0

        override fun enter() { events.add("enter") }
        override fun exit() { events.add("exit") }
        override fun pause() { events.add("pause") }
        override fun resume() { events.add("resume") }
        override fun update() { updateCount++ }
        override fun draw() { drawCount++ }
        override fun cleanup() { events.add("cleanup") }
    }

    @Test
    fun `push adds scene to stack and calls enter`() {
        val manager = SceneManager()
        val scene = TestScene("A")

        manager.push(scene)

        expectThat(manager.depth).isEqualTo(1)
        expectThat(manager.currentScene).isEqualTo(scene)
        expectThat(scene.events).isEqualTo(mutableListOf("enter"))
    }

    @Test
    fun `push pauses previous scene`() {
        val manager = SceneManager()
        val sceneA = TestScene("A")
        val sceneB = TestScene("B")

        manager.push(sceneA)
        manager.push(sceneB)

        expectThat(manager.depth).isEqualTo(2)
        expectThat(manager.currentScene).isEqualTo(sceneB)
        expectThat(sceneA.events).isEqualTo(mutableListOf("enter", "pause"))
        expectThat(sceneB.events).isEqualTo(mutableListOf("enter"))
    }

    @Test
    fun `pop removes scene and resumes previous`() {
        val manager = SceneManager()
        val sceneA = TestScene("A")
        val sceneB = TestScene("B")

        manager.push(sceneA)
        manager.push(sceneB)
        val popped = manager.pop()

        expectThat(popped).isEqualTo(sceneB)
        expectThat(manager.depth).isEqualTo(1)
        expectThat(manager.currentScene).isEqualTo(sceneA)
        expectThat(sceneB.events).isEqualTo(mutableListOf("enter", "exit", "cleanup"))
        expectThat(sceneA.events).isEqualTo(mutableListOf("enter", "pause", "resume"))
    }

    @Test
    fun `pop on empty stack returns null`() {
        val manager = SceneManager()
        val result = manager.pop()
        expectThat(result).isNull()
    }

    @Test
    fun `replace swaps top scene`() {
        val manager = SceneManager()
        val sceneA = TestScene("A")
        val sceneB = TestScene("B")

        manager.push(sceneA)
        manager.replace(sceneB)

        expectThat(manager.depth).isEqualTo(1)
        expectThat(manager.currentScene).isEqualTo(sceneB)
        expectThat(sceneA.events).isEqualTo(mutableListOf("enter", "exit", "cleanup"))
        expectThat(sceneB.events).isEqualTo(mutableListOf("enter"))
    }

    @Test
    fun `replace on empty stack pushes`() {
        val manager = SceneManager()
        val scene = TestScene("A")

        manager.replace(scene)

        expectThat(manager.depth).isEqualTo(1)
        expectThat(manager.currentScene).isEqualTo(scene)
        expectThat(scene.events).isEqualTo(mutableListOf("enter"))
    }

    @Test
    fun `update delegates to current scene`() {
        val manager = SceneManager()
        val sceneA = TestScene("A")
        val sceneB = TestScene("B")

        manager.push(sceneA)
        manager.update()
        manager.update()
        manager.push(sceneB)
        manager.update()

        expectThat(sceneA.updateCount).isEqualTo(2)
        expectThat(sceneB.updateCount).isEqualTo(1)
    }

    @Test
    fun `draw delegates to current scene`() {
        val manager = SceneManager()
        val sceneA = TestScene("A")
        val sceneB = TestScene("B")

        manager.push(sceneA)
        manager.draw()
        manager.push(sceneB)
        manager.draw()
        manager.draw()

        expectThat(sceneA.drawCount).isEqualTo(1)
        expectThat(sceneB.drawCount).isEqualTo(2)
    }

    @Test
    fun `cleanup cleans all scenes in reverse order`() {
        val manager = SceneManager()
        val sceneA = TestScene("A")
        val sceneB = TestScene("B")
        val sceneC = TestScene("C")

        manager.push(sceneA)
        manager.push(sceneB)
        manager.push(sceneC)
        manager.cleanup()

        expectThat(manager.depth).isEqualTo(0)
        expectThat(sceneC.events).containsAll("exit", "cleanup")
        expectThat(sceneB.events).containsAll("exit", "cleanup")
        expectThat(sceneA.events).containsAll("exit", "cleanup")
    }

    @Test
    fun `three-deep stack lifecycle is correct`() {
        val manager = SceneManager()
        val menu = TestScene("Menu")
        val game = TestScene("Game")
        val pause = TestScene("Pause")

        manager.push(menu)
        manager.push(game)
        manager.push(pause)

        expectThat(menu.events).isEqualTo(mutableListOf("enter", "pause"))
        expectThat(game.events).isEqualTo(mutableListOf("enter", "pause"))
        expectThat(pause.events).isEqualTo(mutableListOf("enter"))

        manager.pop() // pop pause, resume game
        expectThat(game.events).isEqualTo(mutableListOf("enter", "pause", "resume"))
        expectThat(pause.events).isEqualTo(mutableListOf("enter", "exit", "cleanup"))

        manager.pop() // pop game, resume menu
        expectThat(game.events).isEqualTo(mutableListOf("enter", "pause", "resume", "exit", "cleanup"))
        expectThat(menu.events).isEqualTo(mutableListOf("enter", "pause", "resume"))
    }
}
