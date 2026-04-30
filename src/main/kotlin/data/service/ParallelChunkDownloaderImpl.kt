package org.example.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.example.core.model.DownloadException
import org.example.domain.model.Chunk
import org.example.domain.service.HttpClient
import org.example.domain.service.ParallelChunkDownloader
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong


class ParallelChunkDownloaderImpl(
    private val httpClient: HttpClient
) : ParallelChunkDownloader {

    override suspend fun execute(
        url: String,
        chunks: List<Chunk>,
        destinationFile: RandomAccessFile,
        maxParallel: Int,
        onBytesDownloaded: (Long) -> Unit
    ) {
        require(maxParallel > 0) { "maxParallel must be positive, was $maxParallel" }
        if (chunks.isEmpty()) return

        val semaphore = Semaphore(maxParallel)
        val totalDownloaded = AtomicLong(0)
        val writeMutex = Mutex()

        coroutineScope {
            chunks.map { chunk ->
                async {
                    semaphore.withPermit {
                        downloadAndWrite(url, chunk, destinationFile, writeMutex)
                        val cumulative = totalDownloaded.addAndGet(chunk.size)
                        onBytesDownloaded(cumulative)
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun downloadAndWrite(
        url: String,
        chunk: Chunk,
        raf: RandomAccessFile,
        mutex: Mutex
    ) {
        val data = httpClient.downloadChunk(url, chunk.index, chunk.start, chunk.end)

        if (data.size.toLong() != chunk.size) {
            throw DownloadException.ChunkSizeMismatch(
                chunkIndex = chunk.index,
                expected = chunk.size,
                actual = data.size.toLong()
            )
        }

        withContext(Dispatchers.IO) {
            mutex.withLock {
                raf.seek(chunk.start)
                raf.write(data)
            }
        }
    }
}