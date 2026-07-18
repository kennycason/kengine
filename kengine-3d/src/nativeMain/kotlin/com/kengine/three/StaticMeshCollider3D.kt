package com.kengine.three

import com.kengine.math.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

data class StaticMeshContact3D(
    val point: Vec3,
    val normal: Vec3,
    val depth: Double
)

data class StaticMeshCapsuleResolveResult3D(
    val position: Vec3,
    val contacts: List<StaticMeshContact3D>,
    val collided: Boolean
)

class StaticMeshCollider3D private constructor(
    private val triangles: List<StaticMeshTriangle3D>
) {
    fun collideCapsule(
        capsule: CapsuleCollider3D,
        ignoredFloorNormalY: Double = 0.55,
        skinWidth: Double = 0.02,
        movement: Vec3? = null
    ): List<StaticMeshContact3D> {
        val inflatedRadius = capsule.radius + skinWidth
        val capsuleBounds = capsule.bounds(inflatedRadius)
        val contacts = mutableListOf<StaticMeshContact3D>()

        triangles.forEach { triangle ->
            if (!triangle.bounds.overlaps(capsuleBounds)) {
                return@forEach
            }

            val closest = closestSegmentTriangle(capsule.start, capsule.end, triangle)
            if (closest.distanceSquared > inflatedRadius * inflatedRadius) {
                return@forEach
            }

            val normal = contactNormal(
                capsulePoint = closest.segmentPoint,
                trianglePoint = closest.trianglePoint,
                triangle = triangle,
                movement = movement
            )
            if (normal.y > ignoredFloorNormalY) {
                return@forEach
            }

            val distance = sqrt(closest.distanceSquared)
            contacts += StaticMeshContact3D(
                point = closest.trianglePoint,
                normal = normal,
                depth = inflatedRadius - distance
            )
        }

        return contacts.sortedByDescending { it.depth }
    }

    fun resolveCapsule(
        position: Vec3,
        halfHeight: Double,
        radius: Double,
        ignoredFloorNormalY: Double = 0.55,
        skinWidth: Double = 0.02,
        iterations: Int = 4,
        movement: Vec3? = null
    ): StaticMeshCapsuleResolveResult3D {
        var resolvedPosition = position
        val allContacts = mutableListOf<StaticMeshContact3D>()
        var collided = false

        repeat(iterations.coerceAtLeast(1)) {
            val capsule = capsuleFromActorPosition(resolvedPosition, halfHeight, radius)
            val contacts = collideCapsule(
                capsule = capsule,
                ignoredFloorNormalY = ignoredFloorNormalY,
                skinWidth = skinWidth,
                movement = movement
            )
            val deepest = contacts.firstOrNull { it.depth > CONTACT_EPSILON } ?: return@repeat

            collided = true
            allContacts += deepest
            resolvedPosition = add(
                resolvedPosition,
                scale(deepest.normal, deepest.depth + PUSH_OUT_SLOP)
            )
        }

        return StaticMeshCapsuleResolveResult3D(
            position = resolvedPosition,
            contacts = allContacts,
            collided = collided
        )
    }

    companion object {
        fun fromLitVertices(vertices: List<LitVertex3D>): StaticMeshCollider3D {
            return fromPositions(vertices.map { it.position })
        }

        fun fromTexturedLitVertices(vertices: List<TexturedLitVertex3D>): StaticMeshCollider3D {
            return fromPositions(vertices.map { it.position })
        }

        fun fromPositions(vertices: List<Vec3>): StaticMeshCollider3D {
            val triangles = mutableListOf<StaticMeshTriangle3D>()
            for (index in 0 until vertices.size - 2 step 3) {
                StaticMeshTriangle3D.from(
                    a = vertices[index],
                    b = vertices[index + 1],
                    c = vertices[index + 2]
                )?.let { triangles += it }
            }
            return StaticMeshCollider3D(triangles)
        }
    }
}

private data class StaticMeshTriangle3D(
    val a: Vec3,
    val b: Vec3,
    val c: Vec3,
    val normal: Vec3,
    val centroid: Vec3,
    val bounds: StaticMeshAabb3D
) {
    companion object {
        fun from(
            a: Vec3,
            b: Vec3,
            c: Vec3
        ): StaticMeshTriangle3D? {
            val normal = normalize(cross(subtract(b, a), subtract(c, a)))
            if (lengthSquared(normal) < DEGENERATE_EPSILON) {
                return null
            }

            return StaticMeshTriangle3D(
                a = a,
                b = b,
                c = c,
                normal = normal,
                centroid = scale(add(add(a, b), c), 1.0 / 3.0),
                bounds = StaticMeshAabb3D.fromTriangle(a, b, c)
            )
        }
    }
}

private data class StaticMeshAabb3D(
    val min: Vec3,
    val max: Vec3
) {
    fun overlaps(other: StaticMeshAabb3D): Boolean {
        return min.x <= other.max.x && max.x >= other.min.x &&
            min.y <= other.max.y && max.y >= other.min.y &&
            min.z <= other.max.z && max.z >= other.min.z
    }

    companion object {
        fun fromTriangle(
            a: Vec3,
            b: Vec3,
            c: Vec3
        ): StaticMeshAabb3D {
            return StaticMeshAabb3D(
                min = Vec3(
                    minOf(a.x, b.x, c.x),
                    minOf(a.y, b.y, c.y),
                    minOf(a.z, b.z, c.z)
                ),
                max = Vec3(
                    maxOf(a.x, b.x, c.x),
                    maxOf(a.y, b.y, c.y),
                    maxOf(a.z, b.z, c.z)
                )
            )
        }
    }
}

private data class ClosestTriangleResult3D(
    val segmentPoint: Vec3,
    val trianglePoint: Vec3,
    val distanceSquared: Double
)

private fun CapsuleCollider3D.bounds(radius: Double): StaticMeshAabb3D {
    return StaticMeshAabb3D(
        min = Vec3(
            minOf(start.x, end.x) - radius,
            minOf(start.y, end.y) - radius,
            minOf(start.z, end.z) - radius
        ),
        max = Vec3(
            maxOf(start.x, end.x) + radius,
            maxOf(start.y, end.y) + radius,
            maxOf(start.z, end.z) + radius
        )
    )
}

private fun capsuleFromActorPosition(
    position: Vec3,
    halfHeight: Double,
    radius: Double
): CapsuleCollider3D {
    val segmentHalfHeight = (halfHeight - radius).coerceAtLeast(0.0)
    return CapsuleCollider3D(
        start = Vec3(position.x, position.y - segmentHalfHeight, position.z),
        end = Vec3(position.x, position.y + segmentHalfHeight, position.z),
        radius = radius
    )
}

private fun closestSegmentTriangle(
    segmentStart: Vec3,
    segmentEnd: Vec3,
    triangle: StaticMeshTriangle3D
): ClosestTriangleResult3D {
    var best = closestPointToTriangle(segmentStart, triangle)
    best = bestOf(best, closestPointToTriangle(segmentEnd, triangle))
    best = bestOf(best, closestSegmentSegment(segmentStart, segmentEnd, triangle.a, triangle.b))
    best = bestOf(best, closestSegmentSegment(segmentStart, segmentEnd, triangle.b, triangle.c))
    best = bestOf(best, closestSegmentSegment(segmentStart, segmentEnd, triangle.c, triangle.a))

    val planeHit = segmentTrianglePlaneHit(segmentStart, segmentEnd, triangle)
    if (planeHit != null) {
        best = bestOf(
            best,
            ClosestTriangleResult3D(
                segmentPoint = planeHit,
                trianglePoint = planeHit,
                distanceSquared = 0.0
            )
        )
    }

    return best
}

private fun closestPointToTriangle(
    point: Vec3,
    triangle: StaticMeshTriangle3D
): ClosestTriangleResult3D {
    val closest = closestPointOnTriangle(point, triangle.a, triangle.b, triangle.c)
    return ClosestTriangleResult3D(
        segmentPoint = point,
        trianglePoint = closest,
        distanceSquared = lengthSquared(subtract(point, closest))
    )
}

private fun closestSegmentSegment(
    p1: Vec3,
    q1: Vec3,
    p2: Vec3,
    q2: Vec3
): ClosestTriangleResult3D {
    val closest = closestPointsOnSegments(p1, q1, p2, q2)
    return ClosestTriangleResult3D(
        segmentPoint = closest.first,
        trianglePoint = closest.second,
        distanceSquared = lengthSquared(subtract(closest.first, closest.second))
    )
}

private fun bestOf(
    a: ClosestTriangleResult3D,
    b: ClosestTriangleResult3D
): ClosestTriangleResult3D {
    return if (b.distanceSquared < a.distanceSquared) b else a
}

private fun segmentTrianglePlaneHit(
    segmentStart: Vec3,
    segmentEnd: Vec3,
    triangle: StaticMeshTriangle3D
): Vec3? {
    val startDistance = dot(subtract(segmentStart, triangle.a), triangle.normal)
    val endDistance = dot(subtract(segmentEnd, triangle.a), triangle.normal)
    if (abs(startDistance) <= PLANE_EPSILON && pointInTriangle(segmentStart, triangle)) {
        return segmentStart
    }
    if (abs(endDistance) <= PLANE_EPSILON && pointInTriangle(segmentEnd, triangle)) {
        return segmentEnd
    }
    if (startDistance * endDistance > 0.0) {
        return null
    }

    val denominator = startDistance - endDistance
    if (abs(denominator) <= PLANE_EPSILON) {
        return null
    }

    val amount = (startDistance / denominator).coerceIn(0.0, 1.0)
    val point = add(segmentStart, scale(subtract(segmentEnd, segmentStart), amount))
    return if (pointInTriangle(point, triangle)) point else null
}

private fun pointInTriangle(
    point: Vec3,
    triangle: StaticMeshTriangle3D
): Boolean {
    val v0 = subtract(triangle.c, triangle.a)
    val v1 = subtract(triangle.b, triangle.a)
    val v2 = subtract(point, triangle.a)
    val dot00 = dot(v0, v0)
    val dot01 = dot(v0, v1)
    val dot02 = dot(v0, v2)
    val dot11 = dot(v1, v1)
    val dot12 = dot(v1, v2)
    val denominator = dot00 * dot11 - dot01 * dot01
    if (abs(denominator) <= DEGENERATE_EPSILON) {
        return false
    }

    val invDenominator = 1.0 / denominator
    val u = (dot11 * dot02 - dot01 * dot12) * invDenominator
    val v = (dot00 * dot12 - dot01 * dot02) * invDenominator
    return u >= -BARYCENTRIC_EPSILON &&
        v >= -BARYCENTRIC_EPSILON &&
        u + v <= 1.0 + BARYCENTRIC_EPSILON
}

private fun closestPointOnTriangle(
    point: Vec3,
    a: Vec3,
    b: Vec3,
    c: Vec3
): Vec3 {
    val ab = subtract(b, a)
    val ac = subtract(c, a)
    val ap = subtract(point, a)
    val d1 = dot(ab, ap)
    val d2 = dot(ac, ap)
    if (d1 <= 0.0 && d2 <= 0.0) {
        return a
    }

    val bp = subtract(point, b)
    val d3 = dot(ab, bp)
    val d4 = dot(ac, bp)
    if (d3 >= 0.0 && d4 <= d3) {
        return b
    }

    val vc = d1 * d4 - d3 * d2
    if (vc <= 0.0 && d1 >= 0.0 && d3 <= 0.0) {
        val v = d1 / (d1 - d3)
        return add(a, scale(ab, v))
    }

    val cp = subtract(point, c)
    val d5 = dot(ab, cp)
    val d6 = dot(ac, cp)
    if (d6 >= 0.0 && d5 <= d6) {
        return c
    }

    val vb = d5 * d2 - d1 * d6
    if (vb <= 0.0 && d2 >= 0.0 && d6 <= 0.0) {
        val w = d2 / (d2 - d6)
        return add(a, scale(ac, w))
    }

    val va = d3 * d6 - d5 * d4
    if (va <= 0.0 && d4 - d3 >= 0.0 && d5 - d6 >= 0.0) {
        val w = (d4 - d3) / ((d4 - d3) + (d5 - d6))
        return add(b, scale(subtract(c, b), w))
    }

    val denominator = 1.0 / (va + vb + vc)
    val v = vb * denominator
    val w = vc * denominator
    return add(add(a, scale(ab, v)), scale(ac, w))
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

    if (a <= DEGENERATE_EPSILON && e <= DEGENERATE_EPSILON) {
        return p1 to p2
    }

    if (a <= DEGENERATE_EPSILON) {
        s = 0.0
        t = (f / e).coerceIn(0.0, 1.0)
    } else {
        val c = dot(d1, r)
        if (e <= DEGENERATE_EPSILON) {
            t = 0.0
            s = (-c / a).coerceIn(0.0, 1.0)
        } else {
            val b = dot(d1, d2)
            val denominator = a * e - b * b
            s = if (abs(denominator) > DEGENERATE_EPSILON) {
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

private fun contactNormal(
    capsulePoint: Vec3,
    trianglePoint: Vec3,
    triangle: StaticMeshTriangle3D,
    movement: Vec3?
): Vec3 {
    val fromTriangle = subtract(capsulePoint, trianglePoint)
    if (lengthSquared(fromTriangle) > DEGENERATE_EPSILON) {
        return normalize(fromTriangle)
    }

    if (movement != null && lengthSquared(movement) > DEGENERATE_EPSILON) {
        return normalize(scale(movement, -1.0))
    }

    val segmentSide = subtract(capsulePoint, triangle.centroid)
    return if (dot(segmentSide, triangle.normal) < 0.0) {
        scale(triangle.normal, -1.0)
    } else {
        triangle.normal
    }
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

private fun cross(
    a: Vec3,
    b: Vec3
): Vec3 {
    return Vec3(
        a.y * b.z - a.z * b.y,
        a.z * b.x - a.x * b.z,
        a.x * b.y - a.y * b.x
    )
}

private fun normalize(value: Vec3): Vec3 {
    val length = sqrt(lengthSquared(value))
    if (length <= DEGENERATE_EPSILON) {
        return Vec3(0.0, 0.0, 0.0)
    }
    return Vec3(value.x / length, value.y / length, value.z / length)
}

private fun lengthSquared(value: Vec3): Double {
    return dot(value, value)
}

private const val BARYCENTRIC_EPSILON = 0.0001
private const val CONTACT_EPSILON = 0.000001
private const val DEGENERATE_EPSILON = 0.000000000001
private const val PLANE_EPSILON = 0.000001
private const val PUSH_OUT_SLOP = 0.001
