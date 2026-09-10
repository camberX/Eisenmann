package com.github.noamm9.skijarenderer.skia

data class SkijaFont(val location: String) {
    val bytes: ByteArray by lazy { ResourceLoader.read(location) }
}
