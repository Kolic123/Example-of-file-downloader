package org.example.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.example.facade.ProgressInfo
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class ParallelChunkDownloader(
    private val httpService: HttpService
) {
    suspend fun downloadChunks(
        url: String,
        chunks: List<Chunk>,
        tempDir: File,
        maxParallel: Int,
        onProgress: ((ProgressInfo) -> Unit)?,
        cancellationFlag: AtomicBoolean
    ) : List<File>{
        val semaphore = Semaphore(maxParallel)
        val downloadedBytes = java.util.concurrent.atomic.AtomicLong(0)
        val chunkFiles = ConcurrentHashMap<Int, File>()
        coroutineScope {
            val jobs = chunks.map{ chunk ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        if(cancellationFlag.get()) return@async

                        val chunkFile = downloadSingleChunk(url, chunk, tempDir)

                        chunkFiles[chunk.index] = chunkFile
                        val newTotal = downloadedBytes.addAndGet(chunk.end - chunk.start + 1)
                        onProgress?.invoke(ProgressInfo(
                            downloadedBytes = newTotal,
                            totalBytes = chunks.sumOf { it.end - it.start + 1 },
                            completedChunks = downloadedBytes.get().toInt() / (chunk.end - chunk.start + 1).toInt(),
                            totalChunks = chunks.size,
                            percentage = (newTotal * 100 / chunks.sumOf { it.end - it.start + 1 }).toInt()
                        ))

                    }

                }

            }

            jobs.forEach {it.await()}

        }

        return chunks.map {chunk ->
            chunkFiles[chunk.index]!!
        }

    }
    private suspend fun downloadSingleChunk(
        url: String,
        chunk: Chunk,
        tempDir: File
    ): File{
        val data = httpService.downloadChunk(url, chunk.start, chunk.end)

        val chunkFile = File(tempDir, "chunk_${chunk.index}")
        chunkFile.writeBytes(data)
        val expectedSize = chunk.end - chunk.start + 1
        if (chunkFile.length() != expectedSize) {
            throw DownloadException(
                "Chunk ${chunk.index} size mismatch: expected $expectedSize, got ${chunkFile.length()}"
            )
        }

        return chunkFile
    }
}