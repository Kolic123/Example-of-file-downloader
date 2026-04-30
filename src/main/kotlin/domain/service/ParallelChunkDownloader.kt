package org.example.domain.service

import org.example.domain.model.Chunk
import java.io.RandomAccessFile


interface ParallelChunkDownloader {
    suspend fun execute(
        url: String,
        chunks: List<Chunk>,
        destinationFile: RandomAccessFile,
        maxParallel: Int,
        onBytesDownloaded: (Long) -> Unit
    )
}