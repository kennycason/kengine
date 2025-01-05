package com.kengine.ui

import com.kengine.graphics.Color
import com.kengine.graphics.Sprite

class Image(
    id: String,
    x: Double,
    y: Double,
    w: Double,  // Required width
    h: Double,  // Required height
    padding: Double = 0.0,
    private val sprite: Sprite,
    bgColor: Color? = null,  // Optional background color behind the sprite
    onClick: (() -> Unit)? = null,
    onHover: (() -> Unit)? = null,
    parent: View? = null
) : View(
    id = id,
    x = x,
    y = y,
    w = w,
    h = h,
    padding = padding,
    bgColor = bgColor,
    bgImage = sprite,
    onClick = onClick,
    onHover = onHover,
    parent = parent
)
