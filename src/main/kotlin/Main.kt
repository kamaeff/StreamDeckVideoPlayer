package com.kamaeff.streamdeckvideo

import com.kamaeff.streamdeckvideo.devices.StreamDeckMk2
import org.hid4java.HidManager


fun main(vararg args: String) {
    HidManager.getHidServices().attachedHidDevices.find {
        StreamDeckMk2.isMatchingId(it.productId, it.vendorId)
    }?.let {
        StreamDeckMk2(it)
    }?.use {
        it.setBrightness(100)
        playVideo(args[0], it)
    }
}
