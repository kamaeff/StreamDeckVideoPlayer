package com.kamaeff.streamdeckvideo.utils

import kotlin.time.Duration
import kotlin.time.TimeSource


data class FrameRate(
    val frameCount: Long,
    val playbackDuration: Duration,
    val currentFrameDuration: Duration,
    val currentFrameRate: Double = calculateFrameRate(1, currentFrameDuration),
    val averageFrameRate: Double = calculateFrameRate(frameCount - 1, playbackDuration)
) {
    companion object {
        private fun calculateFrameRate(frameCount: Long, duration: Duration): Double {
            val microseconds = duration.inWholeMicroseconds
            return if (microseconds == 0L){
                Double.POSITIVE_INFINITY
            } else {
                frameCount.toDouble() / duration.inWholeMicroseconds * MICROSECONDS_IN_SECOND
            }
        }
    }
}

class FrameRateCounter {
    private var frameCount = 0L
    private var startTimeMark: TimeSource.Monotonic.ValueTimeMark? = null
    private var previousTimeMark: TimeSource.Monotonic.ValueTimeMark? = null

    fun tick(): FrameRate {
        val now = TimeSource.Monotonic.markNow()
        if (startTimeMark == null) startTimeMark = now
        if (previousTimeMark == null) previousTimeMark = startTimeMark

        frameCount++
        val playbackDuration = (now - startTimeMark!!)
        val currentFrameDuration = (now - previousTimeMark!!)
        return FrameRate(
            frameCount = frameCount,
            playbackDuration = playbackDuration,
            currentFrameDuration = currentFrameDuration
        ).also { previousTimeMark = now }
    }
}