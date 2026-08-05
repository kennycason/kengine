private var probeInvocationCount = 0

fun kengineN64Add(a: Int, b: Int): Int {
    return a + b
}

fun kengineN64MessageCode(): Int {
    return 1996
}

fun kengineN64AllocationProbe(seed: Int, size: Int): Int {
    probeInvocationCount += 1

    val boundedSize = when {
        size < 1 -> 1
        size > 64 -> 64
        else -> size
    }
    val values = IntArray(boundedSize)

    var checksum = seed * 31 + probeInvocationCount
    var index = 0
    while (index < values.size) {
        val value = (seed + index + probeInvocationCount) * (index + 3)
        values[index] = value
        checksum = checksum xor (value + (checksum shl 1))
        index += 1
    }

    val label = "probe:$probeInvocationCount:$boundedSize:$checksum"
    return checksum xor label.length
}

fun kengineN64ProbeMessage(iterations: Int, checksum: Int): String {
    val label = if (iterations == 1) "call" else "calls"
    return "Kotlin runtime OK: $iterations $label, checksum=$checksum, invocations=$probeInvocationCount"
}
