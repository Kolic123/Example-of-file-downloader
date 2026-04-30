package org.example.domain.model

sealed interface DownloadEvent {

    data class Progress(
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadEvent {
        val percentage: Int
            get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
    }

    data class Completed(val result: DownloadResult.Success) : DownloadEvent

    data class Failed(val result: DownloadResult.Failure) : DownloadEvent
}