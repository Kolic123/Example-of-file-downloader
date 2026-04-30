package org.example.domain.model

data class ProgressInfo(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val completedChunks:Int,
    val totalChunks: Int,
    val percentage: Int
)