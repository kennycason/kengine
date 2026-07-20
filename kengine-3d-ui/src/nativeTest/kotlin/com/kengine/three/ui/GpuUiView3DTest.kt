package com.kengine.three.ui

import com.kengine.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GpuUiView3DTest {
    @Test
    fun layoutAssignsFlexibleChildrenInRows() {
        val root = GpuUiView3D(
            id = "root",
            desiredWidth = 240.0,
            desiredHeight = 40.0,
            direction = GpuUiDirection3D.ROW,
            padding = 10.0,
            spacing = 5.0
        )
        val fixed = root.view(id = "fixed", width = 50.0, height = 20.0)
        val flexible = root.view(id = "flexible", width = 0.0, height = 20.0)

        root.performLayout()

        assertEquals(10.0, fixed.layoutX)
        assertEquals(65.0, flexible.layoutX)
        assertEquals(165.0, flexible.layoutWidth)

        root.desiredWidth = 340.0
        root.performLayout()

        assertEquals(0.0, flexible.desiredWidth)
        assertEquals(265.0, flexible.layoutWidth)
    }

    @Test
    fun topmostChildHandlesClickFirst() {
        val clicks = mutableListOf<String>()
        val bottom = GpuUiView3D(
            id = "bottom",
            desiredWidth = 100.0,
            desiredHeight = 100.0,
            onClick = { clicks += "bottom" }
        )
        val top = GpuUiView3D(
            id = "top",
            desiredWidth = 100.0,
            desiredHeight = 100.0,
            onClick = { clicks += "top" }
        )
        val ui = GpuUiContext3D()
        ui.addView(bottom)
        ui.addView(top)
        ui.performLayout()

        val handled = ui.handleMouseEvents(
            mouseX = 50.0,
            mouseY = 50.0,
            isCurrentlyPressed = true,
            wasJustPressed = true
        )

        assertTrue(handled)
        assertEquals(listOf("top"), clicks)
    }

    @Test
    fun hoverDoesNotCaptureMouseInput() {
        val view = GpuUiView3D(
            id = "hover",
            desiredWidth = 100.0,
            desiredHeight = 100.0
        )
        val ui = GpuUiContext3D()
        ui.addView(view)
        ui.performLayout()

        val handled = ui.handleMouseEvents(
            mouseX = 50.0,
            mouseY = 50.0,
            isCurrentlyPressed = false
        )

        assertFalse(handled)
        assertTrue(view.isHovered())
    }

    @Test
    fun oneFramePressDoesNotLeaveUiPressed() {
        var clickCount = 0
        var releaseCount = 0
        val button = GpuUiButton3D(
            id = "button",
            desiredWidth = 100.0,
            desiredHeight = 100.0,
            backgroundColor = Color.white,
            hoverColor = Color.white,
            pressColor = Color.white,
            text = { "BUTTON" },
            onClick = { clickCount++ }
        ).apply {
            onRelease = { releaseCount++ }
        }
        val ui = GpuUiContext3D()
        ui.addView(button)
        ui.performLayout()

        ui.handleMouseEvents(
            mouseX = 50.0,
            mouseY = 50.0,
            isCurrentlyPressed = false,
            wasJustPressed = true
        )

        val handled = ui.handleMouseEvents(
            mouseX = 150.0,
            mouseY = 150.0,
            isCurrentlyPressed = false
        )

        assertEquals(1, clickCount)
        assertEquals(1, releaseCount)
        assertFalse(handled)
    }

    @Test
    fun sliderMapsMousePositionToValue() {
        var value = 0.0
        val slider = GpuUiSlider3D(
            id = "slider",
            value = { value },
            onValueChanged = { value = it },
            desiredWidth = 200.0,
            desiredHeight = 24.0,
            min = -1.0,
            max = 1.0
        )
        val ui = GpuUiContext3D()
        ui.addView(slider)
        ui.performLayout()

        ui.handleMouseEvents(
            mouseX = 150.0,
            mouseY = 12.0,
            isCurrentlyPressed = true,
            wasJustPressed = true
        )

        assertEquals(0.5, value)
    }
}
