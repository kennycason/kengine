package com.kengine.log

import kotlin.test.Test

class LoggerTest : Logging {
    data class Point(
        val x: Int,
        val y: Int,
        val value: Int,
    )

    private val points = listOf(
        Point(0, 0, 1),
        Point(0, 1, 2),
        Point(0, 2, 3),
        Point(1, 0, 4),
        Point(1, 1, 5),
        Point(1, 2, 6)
    )

    @Test
    fun `log example`() {
        logger.trace("Trace message")
        logger.debug("Debug message")
        logger.info("Info message")
        logger.warn("Warn message.")
        logger.error("Error message.")

        logger.trace { "Trace lambda message" }
        logger.debug { "Debug lambda message" }
        logger.info { "Info lambda message" }
        logger.warn { "Warn lambda message." }
        logger.error { "Error lambda message." }

        try {
            throw Exception("Uh, Oh")
        } catch (e: Exception) {
            logger.error(e)
        }
    }

    @Test
    fun `log stream`() {
        logger
            .infoStream()
            .writeLn { "Point count ${points.size}" }
            .writeLn { "Max point: ${points.maxBy { it.value }}" }
            .writeLn { "Min point: ${points.minBy { it.value }}" }
            .writeLn { "Points:" }
            .writeLn { "x,y,value" }
            .writeLn(points) { "${it.x},${it.y},${it.y}" }
            .flush()
    }

    @Test
    fun `log stream lambda - autoflush`() {
        logger.infoStream {
            writeLn { "Point count ${points.size}" }
            writeLn { "Max point: ${points.maxBy { it.value }}" }
            writeLn { "Min point: ${points.minBy { it.value }}" }
            writeLn { "Points:" }
            writeLn { "x,y,value" }
            writeLn(points) { "${it.x},${it.y},${it.y}" }
        }
    }

}
