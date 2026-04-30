package org.example.domain.service

import org.example.domain.model.FileInfo

interface HttpClient {
    suspend fun getFileInfo(url: String): FileInfo
    suspend fun downloadChunk(
        url: String,
        chunkIndex: Int,
        rangeStart: Long,
        rangeEnd: Long
    ): ByteArray
}