package org.example.domain.model

import org.example.core.util.mb

data class DownloadRequest(
    val url: String,
    val destinationPath: String,
    val maxParallelChunks: Int = 5,
    val chunkSizeBytes: Long = 1.mb
)