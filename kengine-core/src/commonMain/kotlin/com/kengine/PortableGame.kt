package com.kengine

import com.kengine.input.InputState
import com.kengine.render.RenderContext

interface PortableGame {
    fun update(input: InputState)
    fun draw(render: RenderContext)
    fun cleanup()
}
