package com.kamaeff.streamdeckvideo

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameGrabber
import com.kamaeff.streamdeckvideo.devices.StreamDeckMk2
import com.kamaeff.streamdeckvideo.utils.FrameRateCounter
import com.kamaeff.streamdeckvideo.utils.MICROSECONDS_IN_SECOND
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.time.TimeSource


private fun FFmpegFrameGrabber.grabAVFrame() = grabFrame(
    /* doAudio = */ true,
    /* doVideo = */ true,
    /* doProcessing = */ true,
    /* keyFrames = */ false,
    /* doData = */ false
)

fun playVideo(filename: String, device: StreamDeckMk2) {
    val grabber = FFmpegFrameGrabber(filename).apply {
        sampleMode = FrameGrabber.SampleMode.SHORT
        imageWidth = device.screenWidth
        imageHeight = device.screenHeight
        start()
    }
    val soundPlayer: SoundPlayer = if (grabber.hasAudio()) ActualSoundPlayer(grabber) else StubSoundPlayer()
    val microsecondsPerFrame = MICROSECONDS_IN_SECOND / device.fps
    var nextFrameTimestamp: Long? = null
    val frameRateCounter = FrameRateCounter()
    while (true) {
        val frame = grabber.grabAVFrame() ?: break
        if (frame.samples != null || soundPlayer is StubSoundPlayer) {
            soundPlayer.playSoundFrame(frame)
        }
        frame.image?.run {
            // don't spam frames if device is incapable of such a frame rate
            if (nextFrameTimestamp == null) nextFrameTimestamp = frame.timestamp
            if (frame.timestamp < nextFrameTimestamp) {
                System.err.println("skipping frame to keep moderate framerate")
                return@run
            }
            nextFrameTimestamp += microsecondsPerFrame

            // synchronize image frames with sound
            val soundTimestamp = soundPlayer.timestampMicroseconds
            if (frame.timestamp > soundTimestamp) {
                System.err.println("Frame is waiting for its time")
                Thread.sleep((frame.timestamp - soundTimestamp) / 1000)
            }
            device.drawImage(frame)
            System.err.println(frameRateCounter.tick())
        }
        frame.close()
    }

    soundPlayer.close()
    grabber.close()
}


interface PlaybackTimer {
    val timestampMicroseconds: Long
}

interface SoundPlayer : Closeable, PlaybackTimer {
    fun playSoundFrame(frame: Frame)
}

class ActualSoundPlayer(private val grabber: FrameGrabber) : SoundPlayer {
    private val soundLine: SourceDataLine = run {
        val audioFormat = AudioFormat(
            /* sampleRate = */ grabber.sampleRate.toFloat(),
            /* sampleSizeInBits = */ 16,
            /* channels = */ grabber.audioChannels,
            /* signed = */ true,
            /* bigEndian = */ true
        )
        val dataLineInfo = DataLine.Info(SourceDataLine::class.java, audioFormat)
        (AudioSystem.getLine(dataLineInfo) as SourceDataLine).apply {
            open(audioFormat)
        }
    }
    override val timestampMicroseconds get() = soundLine.microsecondPosition
    private lateinit var outBuffer: ByteBuffer

    override fun playSoundFrame(frame: Frame) {
        if (!soundLine.isRunning) soundLine.start()
        val channelSamplesShortBuffer = (frame.samples[0] as ShortBuffer).apply { rewind() }
        if (!::outBuffer.isInitialized) {
            outBuffer = ByteBuffer.allocate(channelSamplesShortBuffer.capacity() * 2)
        }

        for (i in 0..<channelSamplesShortBuffer.capacity()) {
            outBuffer.putShort(channelSamplesShortBuffer.get(i))
        }
        soundLine.write(outBuffer.array(), 0, outBuffer.capacity())
        outBuffer.clear()
    }

    override fun close() {
        soundLine.close()
    }
}

class StubSoundPlayer : SoundPlayer {
    private var startTimeMark: TimeSource.Monotonic.ValueTimeMark? = null

    override val timestampMicroseconds: Long
        get() {
            return if (startTimeMark == null) {
                0L
            } else {
                (TimeSource.Monotonic.markNow() - startTimeMark!!).inWholeMicroseconds
            }
        }

    override fun playSoundFrame(frame: Frame) {
        if (startTimeMark == null) {
            startTimeMark = TimeSource.Monotonic.markNow()
        }
    }

    override fun close() {}
}