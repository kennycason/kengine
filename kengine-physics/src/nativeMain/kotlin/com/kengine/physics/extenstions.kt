package com.kengine.physics

import chipmunk.cpBool
import chipmunk.cpVect
import com.kengine.math.Vec2
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

@OptIn(ExperimentalForeignApi::class)
internal fun cpBool.toBoolean() = this != 0.toUByte()
internal fun Boolean.toCpBool() = if (this) 1.toUByte() else 0.toUByte()
@OptIn(ExperimentalForeignApi::class)
internal fun CValue<cpVect>.toVec2() = useContents { Vec2(x = x, y = y) }
