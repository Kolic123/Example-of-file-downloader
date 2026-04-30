package org.example.data.fileDownloader

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.example.core.model.DownloadException
import org.example.domain.model.Chunk
import org.example.domain.model.DownloadEvent
import org.example.domain.model.DownloadRequest
import org.example.domain.model.FileInfo
import org.example.domain.service.ChunkSplitter
import org.example.domain.service.HttpClient
import org.example.domain.service.ParallelChunkDownloader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileDownloaderImplTest {

    private val httpClient: HttpClient = mockk()
    private val chunkSplitter: ChunkSplitter = mockk()
    private val chunkDownloader: ParallelChunkDownloader = mockk()

    private val downloader = FileDownloaderImpl(
        httpClient = httpClient,
        chunkPlanner = chunkSplitter,
        chunkDownloader = chunkDownloader
    )

    @Test
    fun `emits Completed event when download succeeds`(@TempDir tempDir: Path) = runTest {
        val destination = tempDir.resolve("output.txt").toString()
        val fileInfo = FileInfo(contentLength = 2000, supportsRanges = true)
        val chunks = listOf(
            Chunk(start = 0, end = 999, index = 0),
            Chunk(start = 1000, end = 1999, index = 1),
        )
        val request = DownloadRequest(
            url = "http://example.com/file",
            destinationPath = destination,
            maxParallelChunks = 2,
            chunkSizeBytes = 1000
        )

        coEvery { httpClient.getFileInfo(request.url) } returns fileInfo
        coEvery { chunkSplitter.splitIntoChunks(2000, 1000) } returns chunks
        coEvery {
            chunkDownloader.execute(any(), any(), any(), any(), any())
        } returns Unit

        val events = downloader.download(request).toList()

        assertEquals(1, events.size)
        assertTrue(events.single() is DownloadEvent.Completed)

        val completed = events.single() as DownloadEvent.Completed
        assertEquals(destination, completed.result.filePath)
        assertEquals(2000L, completed.result.fileSize)
        assertTrue(completed.result.durationMs >= 0)
    }

    @Test
    fun `emits Progress events as bytes are downloaded`(@TempDir tempDir: Path) = runTest {
        val destination = tempDir.resolve("output.txt").toString()
        val fileInfo = FileInfo(contentLength = 2000, supportsRanges = true)
        val chunks = listOf(
            Chunk(start = 0, end = 999, index = 0),
            Chunk(start = 1000, end = 1999, index = 1),
        )
        val request = DownloadRequest(
            url = "http://example.com/file",
            destinationPath = destination,
            maxParallelChunks = 2,
            chunkSizeBytes = 1000
        )

        coEvery { httpClient.getFileInfo(any()) } returns fileInfo
        coEvery { chunkSplitter.splitIntoChunks(any(), any()) } returns chunks

        val callbackSlot = slot<(Long) -> Unit>()
        coEvery {
            chunkDownloader.execute(any(), any(), any(), any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured(1000L)
            callbackSlot.captured(2000L)
        }

        val events = downloader.download(request).toList()

        val progressEvents = events.filterIsInstance<DownloadEvent.Progress>()
        assertEquals(2, progressEvents.size)
        assertEquals(1000L, progressEvents[0].bytesDownloaded)
        assertEquals(2000L, progressEvents[0].totalBytes)
        assertEquals(2000L, progressEvents[1].bytesDownloaded)
        assertTrue(events.last() is DownloadEvent.Completed)
    }

    @Test
    fun `emits Failed event and cleans up file when HEAD request fails`(@TempDir tempDir: Path) = runTest {
        val destination = tempDir.resolve("output.txt").toString()
        val request = DownloadRequest(
            url = "http://example.com/file",
            destinationPath = destination,
            maxParallelChunks = 2,
            chunkSizeBytes = 1000
        )

        coEvery {
            httpClient.getFileInfo(any())
        } throws DownloadException.HeadRequestFailed(httpCode = 404)

        val events = downloader.download(request).toList()

        assertEquals(1, events.size)
        assertTrue(events.single() is DownloadEvent.Failed)

        val failed = events.single() as DownloadEvent.Failed
        assertTrue(failed.result.cause is DownloadException.HeadRequestFailed)
        assertTrue(failed.result.durationMs >= 0)

        assertTrue(!File(destination).exists())
    }

    @Test
    fun `emits Failed event and cleans up partial file when chunk download fails`(@TempDir tempDir: Path) = runTest {
        val destination = tempDir.resolve("output.txt").toString()
        val fileInfo = FileInfo(contentLength = 2000, supportsRanges = true)
        val chunks = listOf(Chunk(start = 0, end = 1999, index = 0))
        val request = DownloadRequest(
            url = "http://example.com/file",
            destinationPath = destination,
            maxParallelChunks = 1,
            chunkSizeBytes = 2000
        )

        coEvery { httpClient.getFileInfo(any()) } returns fileInfo
        coEvery { chunkSplitter.splitIntoChunks(any(), any()) } returns chunks
        coEvery {
            chunkDownloader.execute(any(), any(), any(), any(), any())
        } throws DownloadException.ChunkDownloadFailed(chunkIndex = 0, httpCode = 503)

        val events = downloader.download(request).toList()

        assertEquals(1, events.size)
        assertTrue(events.single() is DownloadEvent.Failed)

        val failed = events.single() as DownloadEvent.Failed
        assertTrue(failed.result.cause is DownloadException.ChunkDownloadFailed)

        assertTrue(!File(destination).exists())
    }

    @Test
    fun `passes correct parameters to chunk downloader`(@TempDir tempDir: Path) = runTest {
        val destination = tempDir.resolve("output.txt").toString()
        val fileInfo = FileInfo(contentLength = 5000, supportsRanges = true)
        val chunks = listOf(Chunk(start = 0, end = 4999, index = 0))
        val request = DownloadRequest(
            url = "http://example.com/file",
            destinationPath = destination,
            maxParallelChunks = 8,
            chunkSizeBytes = 5000
        )

        coEvery { httpClient.getFileInfo(any()) } returns fileInfo
        coEvery { chunkSplitter.splitIntoChunks(any(), any()) } returns chunks
        coEvery { chunkDownloader.execute(any(), any(), any(), any(), any()) } returns Unit

        downloader.download(request).toList()

        coVerify {
            httpClient.getFileInfo("http://example.com/file")
            chunkSplitter.splitIntoChunks(totalSize = 5000, chunkSize = 5000)
            chunkDownloader.execute(
                url = "http://example.com/file",
                chunks = chunks,
                destinationFile = any<RandomAccessFile>(),
                maxParallel = 8,
                onBytesDownloaded = any()
            )
        }
    }
}