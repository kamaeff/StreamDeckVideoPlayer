package com.kamaeff.streamdeckvideo.utils

fun byteArrayOf(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }
