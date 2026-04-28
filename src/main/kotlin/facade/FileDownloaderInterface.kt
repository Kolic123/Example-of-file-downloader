package org.example.facade

import java.io.File

interface FileDownloaderInterface {

    suspend fun downloadFile(url: String, outputDestination: String): DownloadResult

    suspend fun downloadFile(request: DownloadRequest): DownloadResult

    fun cancelAllDownloads()
}

data class DownloadResult(
    val success: Boolean,
    val filePath: String,
    val fileSize: Long,
    val durationMs: Long,
    val errorMessage: String? = null
)

data class DownloadRequest(
    val url: String,
    val destinationPath: String,
    val maxParallelChunks: Int = 5,
    val chunkSizeBytes: Long = 1024 * 1024,
    val onProgress: ((ProgressInfo) -> Unit)? = null
)

data class ProgressInfo(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val completedChunks:Int,
    val totalChunks: Int,
    val percentage: Int
)