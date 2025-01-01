package com.kengine.particle

import com.kengine.graphics.Color

object Effects {
    val explosion = { p: Particle ->
        val fade = ((1.0 - (p.age / p.lifetime)).coerceIn(0.0, 1.0)).toFloat()
        p.color = p.color.copy(a = (fade * 255).toInt().toUByte())
    }
    val smoke = { p: Particle ->
        p.size += 1.0
        p.color = p.color.copy(a = ((1.0 - p.age / p.lifetime) * 255).toInt().toUByte())
    }
    val rainbow = { p: Particle ->
        // Calculate hue based on age (loops back using modulo)
        val hue = ((p.age / p.lifetime) * 360).toFloat() % 360f
        p.color = Color.fromHSV(hue, saturation = 1.0f, value = 1.0f, alpha = (1.0 - p.age / p.lifetime).toFloat())
    }
}
