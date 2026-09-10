package com.github.noamm9.skijarenderer.skia

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

internal object ResourceLoader {
    fun read(location: String): ByteArray {
        val normalized = normalizeLocation(location)
        return if (isHttpLocation(normalized)) readURL(normalized) else readFileOrClasspath(normalized)
    }

    internal fun normalizeLocation(location: String): String = location.trim().removePrefix("/")

    private fun isHttpLocation(location: String): Boolean {
        return location.startsWith("http://", ignoreCase = true) || location.startsWith("https://", ignoreCase = true)
    }

    private fun readURL(location: String): ByteArray {
        val connection = URI(location).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("User-Agent", "NvgRenderer/1.0")

        return try {
            val code = connection.responseCode
            if (code !in 200 .. 299) throw IOException("HTTP $code")
            connection.inputStream.use { it.readBytes() }
        }
        finally {
            connection.disconnect()
        }
    }

    private fun readFileOrClasspath(location: String): ByteArray {
        val file = File(location)
        if (file.exists() && file.isFile) return file.readBytes()

        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(location)
            ?: ResourceLoader::class.java.getResourceAsStream("/$location")
            ?: throw FileNotFoundException("$location not found")

        return stream.use { it.readBytes() }
    }
}