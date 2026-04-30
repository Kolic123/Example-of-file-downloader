package org.example.data.fileDownloader

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import org.example.core.model.DownloadException
import org.example.domain.fileDownloader.FileDownloader
import org.example.domain.model.DownloadEvent
import org.example.domain.model.DownloadRequest
import org.example.domain.model.DownloadResult
import org.example.domain.service.ChunkSplitter
import org.example.domain.service.HttpClient
import org.example.domain.service.ParallelChunkDownloader
import java.io.File
import java.io.RandomAccessFile


class FileDownloaderImpl(
    private val httpClient: HttpClient,
    private val chunkPlanner: ChunkSplitter,
    private val chunkDownloader: ParallelChunkDownloader,
) : FileDownloader {

    override fun download(request: DownloadRequest): Flow<DownloadEvent> = channelFlow {
        val startTime = System.currentTimeMillis()

        try {
            val fileInfo = httpClient.getFileInfo(request.url)
            val chunks = chunkPlanner.splitIntoChunks(fileInfo.contentLength, request.chunkSizeBytes)

            ensureParentDirExists(request.destinationPath)

            RandomAccessFile(request.destinationPath, "rw").use { raf ->
                raf.setLength(fileInfo.contentLength)

                chunkDownloader.execute(
                    url = request.url,
                    chunks = chunks,
                    destinationFile = raf,
                    maxParallel = request.maxParallelChunks,
                    onBytesDownloaded = { downloaded ->
                        trySend(
                            DownloadEvent.Progress(
                                bytesDownloaded = downloaded,
                                totalBytes = fileInfo.contentLength
                            )
                        )
                    }
                )
            }

            send(
                DownloadEvent.Completed(
                    DownloadResult.Success(
                        filePath = request.destinationPath,
                        fileSize = fileInfo.contentLength,
                        durationMs = System.currentTimeMillis() - startTime
                    )
                )
            )
        } catch (e: CancellationException) {
            cleanupPartialFile(request.destinationPath)
            throw e
        } catch (e: DownloadException) {
            cleanupPartialFile(request.destinationPath)
            send(
                DownloadEvent.Failed(
                    DownloadResult.Failure(
                        errorMessage = e.message ?: "Unknown error",
                        cause = e,
                        durationMs = System.currentTimeMillis() - startTime
                    )
                )
            )
        }
    }
        .flowOn(Dispatchers.IO)

    private fun ensureParentDirExists(path: String) {
        File(path).parentFile?.mkdirs()
    }

    private fun cleanupPartialFile(path: String) {
        runCatching { File(path).delete() }
    }
}