package org.example.domain.model

data class FileInfo(
    val contentLength: Long,
    val supportsRanges: Boolean
)