package com.kamaeff.streamdeckvideo.devices

import org.bytedeco.javacv.FFmpegFrameFilter
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.Java2DFrameConverter
import com.kamaeff.streamdeckvideo.utils.byteArrayOf
import org.hid4java.HidDevice
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.Closeable
import javax.imageio.ImageIO
import kotlin.collections.iterator
import kotlin.math.min

class StreamDeckMk2(private val hidDevice: HidDevice) : Closeable {
    init {
        hidDevice.open()
    }

    val fps = 30
    val screenWidth = 480
    val screenHeight = 272

    private val fullReportSize = 1023
    private val imageHeaderSize = 7
    private val imagePageSize = fullReportSize - imageHeaderSize

    private val imageOutputReportId: Byte = 0x02
    private val brightnessFeatureReportId: Byte = 0x03

    private val imageFormat = "jpeg"
    private val imageFlipX = true
    private val imageFlipY = true


    fun setBrightness(level: Int) {
        hidDevice.sendFeatureReport(byteArrayOf(0x08, level), brightnessFeatureReportId)
    }

    private val imageConverter = object : Closeable {
        private val filter = run {
            val filterString = listOfNotNull(
                "vflip".takeIf { imageFlipY },
                "hflip".takeIf { imageFlipX },
            ).joinToString(",")
            FFmpegFrameFilter(filterString, screenWidth, screenHeight)
        }.apply { start() }
        private val imageOutputStream = ByteArrayOutputStream(1024)
        private val frameConverter  = Java2DFrameConverter()

        fun convert(frame: Frame): ByteArray {
            imageOutputStream.reset()
            filter.push(frame)
            val filteredImage = frameConverter.convert(filter.pullImage())

            ImageIO.write(filteredImage, imageFormat, imageOutputStream)
            return imageOutputStream.toByteArray()
        }

        fun convert(image: BufferedImage): ByteArray {
            return convert(frameConverter.convert(image))
        }

        override fun close() {
            filter.close()
            frameConverter.close()
        }
    }

    private fun imageHidPageIterator(imageBytes: ByteArray) = object : Iterator<ByteArray> {
        var pageNumber = 0
        var start = 0
        val end get() = min(start + imagePageSize, imageBytes.size)
        val pageSize get() = end - start
        val isLastPage get() = end >= imageBytes.size

        private fun encodeInt(int: Int) = intArrayOf(int and 0xFF, int shr 8)

        private fun createPage() = byteArrayOf(
            0x08, // Set full screen image command
            0x00, // Unused
            if (isLastPage) 1 else 0,
            *encodeInt(pageSize),
            *encodeInt(pageNumber)
        ).copyOf(fullReportSize)

        override fun next() = imageBytes.copyInto(
            createPage(),
            imageHeaderSize,
            start,
            end
        ).also {
            start = end
            pageNumber++
        }

        override fun hasNext() = start < imageBytes.size
    }

    fun drawImage(image: BufferedImage) = drawImage(imageConverter.convert(image))
    fun drawImage(frame: Frame) = drawImage(imageConverter.convert(frame))
    fun drawImage(imageBytes: ByteArray) {
        for (page in imageHidPageIterator(imageBytes)) {
            hidDevice.write(
                page,
                fullReportSize,
                imageOutputReportId
            )
        }
    }

    override fun close() {
        hidDevice.close()
        imageConverter.close()
    }

    companion object {
        val vendorId = 0x0fd9
        val productId = 0x0080

        fun isMatchingId(product: Int, vendor: Int = vendorId) =
            product == productId && vendor == vendorId
    }
}