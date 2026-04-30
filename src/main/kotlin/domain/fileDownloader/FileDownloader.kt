package org.example.domain.fileDownloader

import kotlinx.coroutines.flow.Flow
import org.example.domain.model.DownloadEvent
import org.example.domain.model.DownloadRequest

interface FileDownloader {
    fun download(request: DownloadRequest): Flow<DownloadEvent>
}