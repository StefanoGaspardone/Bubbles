package com.backend.utils

import com.backend.models.enums.FileFormat

object FileTypeDetector {
    const val HEADER_SIZE = 12

    private val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val RIFF = "RIFF".toByteArray(Charsets.US_ASCII)
    private val WEBP = "WEBP".toByteArray(Charsets.US_ASCII)
    private val PDF = "%PDF-".toByteArray(Charsets.US_ASCII)

    fun detect(header: ByteArray): FileFormat? = when {
        header.startsWith(JPEG) -> FileFormat.JPEG
        header.startsWith(PNG) -> FileFormat.PNG
        header.startsWith(RIFF) && header.startsWith(WEBP, offset = 8) -> FileFormat.WEBP
        header.startsWith(PDF) -> FileFormat.PDF
        else -> null
    }

    private fun ByteArray.startsWith(prefix: ByteArray, offset: Int = 0): Boolean {
        return size >= offset + prefix.size && prefix.indices.all { this[offset + it] == prefix[it] }
    }
}