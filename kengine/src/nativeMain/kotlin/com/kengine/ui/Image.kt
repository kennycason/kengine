package com.kengine.ui

import com.kengine.graphics.Sprite

class Image(
    id: String,
    x: Double,
    y: Double,
    w: Double,
    h: Double,
    sprite: Sprite,
) : View(id, x, y, w, h, bgImage = sprite)
