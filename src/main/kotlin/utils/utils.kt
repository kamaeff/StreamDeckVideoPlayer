package com.kamaeff.streamdeckvideo.utils

const val MICROSECONDS_IN_SECOND = 1_000_000L

fun byteArrayOf(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }
