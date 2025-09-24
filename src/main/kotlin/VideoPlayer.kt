package com.kamaeff.streamdeckvideo

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import com.kamaeff.streamdeckvideo.devices.StreamDeckMk2
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine


fun playVideo(filename: String, device: StreamDeckMk2) {
    val grabber = FFmpegFrameGrabber(filename).apply {
        sampleMode = FrameGrabber.SampleMode.SHORT
        imageWidth = device.screenWidth
        imageHeight = device.screenHeight
        start()
    }
    val soundPlayer: SoundPlayer? = if (grabber.hasAudio()) SoundPlayer(grabber) else null
    var timestamp = 0L
    while (true) {
        val frame = grabber.grabFrame(
            /* doAudio = */ true,
            /* doVideo = */ true,
            /* doProcessing = */ true,
            /* keyFrames = */ false,
            /* doData = */ false
        ) ?: break
        frame.image?.run {
            // don't spam frames if device is incapable of such a frame rate
            if (frame.timestamp < timestamp) return@run

            device.drawImage(frame)
            timestamp += 1_000_000 / device.fps
        }
        if (frame.samples != null && soundPlayer != null) {
            soundPlayer.playSoundFrame(frame)
        }
    }

    soundPlayer?.close()
    grabber.close()
}

class SoundPlayer(private val grabber: FrameGrabber) : Closeable {
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
            start()
        }
    }
    private lateinit var outBuffer: ByteBuffer

    fun playSoundFrame(frame: Frame) {
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