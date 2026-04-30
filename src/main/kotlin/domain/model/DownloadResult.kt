package org.example.domain.model

sealed class DownloadResult {
    data class Success(
        val filePath: String,
        val fileSize: Long,
        val durationMs: Long
    ) : DownloadResult()

    data class Failure(
        val errorMessage: String,
        val cause: Throwable? = null,
        val durationMs: Long
    ) : DownloadResult()
}