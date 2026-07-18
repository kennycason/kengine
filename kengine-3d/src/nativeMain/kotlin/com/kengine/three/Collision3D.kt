package com.kengine.three

import com.kengine.math.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

data class CollisionContact3D(
    val normal: Vec3,
    val depth: Double
)

data class SphereCollider3D(
    val center: Vec3,
    val radius: Double
)

data class CapsuleCollider3D(
    val start: Vec3,
    val end: Vec3,
    val radius: Double
)

data class AabbCollider3D(
    val min: Vec3,
    val max: Vec3
) {
    companion object {
        fun fromCenterSize(
            center: Vec3,
            size: Vec3
        ): AabbCollider3D {
            val halfSize = scale(size, 0.5)
            return AabbCollider3D(
                min = subtract(center, halfSize),
                max = add(center, halfSize)
            )
        }
    }
}

object Collision3D {
    fun overlap(
        a: SphereCollider3D,
        b: SphereCollider3D
    ): CollisionContact3D? {
        return contactBetween(a.center, b.center, a.radius + b.radius)
    }

    fun overlap(
        a: CapsuleCollider3D,
        b: SphereCollider3D
    ): CollisionContact3D? {
        val closest = closestPointOnSegment(b.center, a.start, a.end)
        return contactBetween(closest, b.center, a.radius + b.radius)
    }

    fun overlap(
        a: SphereCollider3D,
        b: CapsuleCollider3D
    ): CollisionContact3D? {
        return overlap(b, a)?.inverted()
    }

    fun overlap(
        a: CapsuleCollider3D,
        b: CapsuleCollider3D
    ): CollisionContact3D? {
        val closest = closestPointsOnSegments(a.start, a.end, b.start, b.end)
        return contactBetween(closest.first, closest.second, a.radius + b.radius)
    }

    fun overlap(
        a: AabbCollider3D,
        b: SphereCollider3D
    ): CollisionContact3D? {
        val closest = Vec3(
            x = b.center.x.coerceIn(a.min.x, a.max.x),
            y = b.center.y.coerceIn(a.min.y, a.max.y),
            z = b.center.z.coerceIn(a.min.z, a.max.z)
        )
        return contactBetween(closest, b.center, b.radius)
    }

    fun overlap(
        a: SphereCollider3D,
        b: AabbCollider3D
    ): CollisionContact3D? {
        return overlap(b, a)?.inverted()
    }
}

private fun CollisionContact3D.inverted(): CollisionContact3D {
    return copy(normal = scale(normal, -1.0))
}

private fun contactBetween(
    a: Vec3,
    b: Vec3,
    radiusSum: Double
): CollisionContact3D? {
    val delta = subtract(b, a)
    val distanceSquared = lengthSquared(delta)
    val radiusSquared = radiusSum * radiusSum
    if (distanceSquared > radiusSquared) {
        return null
    }

    if (distanceSquared <= 0.000000000001) {
        return CollisionContact3D(
            normal = Vec3(0.0, 1.0, 0.0),
            depth = radiusSum
        )
    }

    val distance = sqrt(distanceSquared)
    return CollisionContact3D(
        normal = scale(delta, 1.0 / distance),
        depth = radiusSum - distance
    )
}

private fun closestPointOnSegment(
    point: Vec3,
    start: Vec3,
    end: Vec3
): Vec3 {
    val segment = subtract(end, start)
    val lengthSquared = lengthSquared(segment)
    if (lengthSquared <= 0.000000000001) {
        return start
    }

    val amount = (dot(subtract(point, start), segment) / lengthSquared).coerceIn(0.0, 1.0)
    return add(start, scale(segment, amount))
}

private fun closestPointsOnSegments(
    p1: Vec3,
    q1: Vec3,
    p2: Vec3,
    q2: Vec3
): Pair<Vec3, Vec3> {
    val d1 = subtract(q1, p1)
    val d2 = subtract(q2, p2)
    val r = subtract(p1, p2)
    val a = lengthSquared(d1)
    val e = lengthSquared(d2)
    val f = dot(d2, r)
    var s: Double
    var t: Double

    if (a <= 0.000000000001 && e <= 0.000000000001) {
        return p1 to p2
    }

    if (a <= 0.000000000001) {
        s = 0.0
        t = (f / e).coerceIn(0.0, 1.0)
    } else {
        val c = dot(d1, r)
        if (e <= 0.000000000001) {
            t = 0.0
            s = (-c / a).coerceIn(0.0, 1.0)
        } else {
            val b = dot(d1, d2)
            val denominator = a * e - b * b
            s = if (abs(denominator) > 0.000000000001) {
                ((b * f - c * e) / denominator).coerceIn(0.0, 1.0)
            } else {
                0.0
            }
            val tNominal = b * s + f
            if (tNominal < 0.0) {
                t = 0.0
                s = (-c / a).coerceIn(0.0, 1.0)
            } else if (tNominal > e) {
                t = 1.0
                s = ((b - c) / a).coerceIn(0.0, 1.0)
            } else {
                t = tNominal / e
            }
        }
    }

    return add(p1, scale(d1, s)) to add(p2, scale(d2, t))
}

private fun add(
    a: Vec3,
    b: Vec3
): Vec3 {
    return Vec3(a.x + b.x, a.y + b.y, a.z + b.z)
}

private fun subtract(
    a: Vec3,
    b: Vec3
): Vec3 {
    return Vec3(a.x - b.x, a.y - b.y, a.z - b.z)
}

private fun scale(
    value: Vec3,
    scale: Double
): Vec3 {
    return Vec3(value.x * scale, value.y * scale, value.z * scale)
}

private fun dot(
    a: Vec3,
    b: Vec3
): Double {
    return a.x * b.x + a.y * b.y + a.z * b.z
}

private fun lengthSquared(value: Vec3): Double {
    return dot(value, value)
}
