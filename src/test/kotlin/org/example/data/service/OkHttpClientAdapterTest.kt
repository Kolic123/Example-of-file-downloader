package org.example.data.service

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.example.core.model.DownloadException
import kotlin.test.*

class OkHttpClientAdapterTest {

    private lateinit var server: MockWebServer
    private lateinit var adapter: OkHttpClientAdapter

    @BeforeTest
    fun setUp() {
        server = MockWebServer().apply { start() }
        adapter = OkHttpClientAdapter()
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getFileInfo returns content length and range support`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Length", "12345")
                .setHeader("Accept-Ranges", "bytes")
        )

        val info = adapter.getFileInfo(server.url("/file").toString())

        assertEquals(12345L, info.contentLength)
        assertEquals(true, info.supportsRanges)
    }

    @Test
    fun `getFileInfo throws RangeRequestsUnsupported when server lacks Accept-Ranges`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Length", "1000")
        )

        assertFailsWith<DownloadException.RangeRequestsUnsupported> {
            adapter.getFileInfo(server.url("/file").toString())
        }
    }

    @Test
    fun `downloadChunk returns body bytes for 206 response`() = runTest {
        val payload = ByteArray(1000) { it.toByte() }
        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setBody(okio.Buffer().write(payload))
        )

        val result = adapter.downloadChunk(
            url = server.url("/file").toString(),
            chunkIndex = 0,
            rangeStart = 0,
            rangeEnd = 999
        )

        assertEquals(payload.toList(), result.toList())
    }
}