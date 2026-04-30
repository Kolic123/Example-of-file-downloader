package org.example.data.service

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.example.core.model.DownloadException
import org.example.domain.model.Chunk
import org.example.domain.service.HttpClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.RandomAccessFile
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParallelChunkDownloaderImplTest {

    private val httpClient: HttpClient = mockk()
    private val downloader = ParallelChunkDownloaderImpl(httpClient)

    @Test
    fun `downloads all chunks and reports cumulative progress`(@TempDir tempDir: Path) = runTest {
        val chunks = listOf(
            Chunk(start = 0, end = 999, index = 0),
            Chunk(start = 1000, end = 1999, index = 1),
        )
        coEvery { httpClient.downloadChunk(any(), 0, 0, 999) } returns ByteArray(1000)
        coEvery { httpClient.downloadChunk(any(), 1, 1000, 1999) } returns ByteArray(1000)

        val progressUpdates = mutableListOf<Long>()

        RandomAccessFile(tempDir.resolve("test.bin").toFile(), "rw").use { raf ->
            raf.setLength(2000)

            downloader.execute(
                url = "http://example.com/file",
                chunks = chunks,
                destinationFile = raf,
                maxParallel = 2,
                onBytesDownloaded = { progressUpdates += it }
            )
        }

        assertEquals(2, progressUpdates.size)
        assertEquals(2000L, progressUpdates.last())

        coVerify(exactly = 2) { httpClient.downloadChunk(any(), any(), any(), any()) }
    }

    @Test
    fun `throws ChunkSizeMismatch when downloaded bytes don't match expected size`(@TempDir tempDir: Path) = runTest {
        val chunks = listOf(Chunk(start = 0, end = 999, index = 0))
        coEvery { httpClient.downloadChunk(any(), any(), any(), any()) } returns ByteArray(500)

        RandomAccessFile(tempDir.resolve("test.bin").toFile(), "rw").use { raf ->
            raf.setLength(1000)

            assertFailsWith<DownloadException.ChunkSizeMismatch> {
                downloader.execute(
                    url = "http://example.com/file",
                    chunks = chunks,
                    destinationFile = raf,
                    maxParallel = 1,
                    onBytesDownloaded = { }
                )
            }
        }
    }

    @Test
    fun `does nothing when chunks list is empty`(@TempDir tempDir: Path) = runTest {
        RandomAccessFile(tempDir.resolve("test.bin").toFile(), "rw").use { raf ->
            downloader.execute(
                url = "http://example.com/file",
                chunks = emptyList(),
                destinationFile = raf,
                maxParallel = 4,
                onBytesDownloaded = { error("should not emit") }
            )
        }

        coVerify(exactly = 0) { httpClient.downloadChunk(any(), any(), any(), any()) }
    }
}