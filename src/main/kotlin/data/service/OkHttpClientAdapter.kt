package org.example.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.example.core.model.DownloadException
import org.example.domain.model.FileInfo
import org.example.domain.service.HttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.text.toLongOrNull


class OkHttpClientAdapter(
    private val client: OkHttpClient = defaultClient()
) : HttpClient {

    override suspend fun getFileInfo(url: String): FileInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).head().build()

        execute(request).use { response ->
            if (!response.isSuccessful) {
                throw DownloadException.HeadRequestFailed(response.code)
            }

            val contentLength = response.headers[CONTENT_HEADER]?.toLongOrNull()
                ?: throw DownloadException.MissingContentLength()

            val supportsRanges = response.headers[ACCEPT_HEADER] == "bytes"
            if (!supportsRanges) {
                throw DownloadException.RangeRequestsUnsupported()
            }

            FileInfo(contentLength = contentLength, supportsRanges = true)
        }
    }

    override suspend fun downloadChunk(
        url: String,
        chunkIndex: Int,
        rangeStart: Long,
        rangeEnd: Long
    ): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=$rangeStart-$rangeEnd")
            .get()
            .build()

        execute(request).use { response ->
            if (!response.isSuccessful && response.code != HTTP_PARTIAL_CONTENT) {
                throw DownloadException.ChunkDownloadFailed(chunkIndex, response.code)
            }
            response.body?.bytes() ?: throw DownloadException.EmptyResponse(chunkIndex)
        }
    }

    private fun execute(request: Request): Response = try {
        client.newCall(request).execute()
    } catch (e: IOException) {
        throw DownloadException.NetworkError(e)
    }

    companion object {
        private const val HTTP_PARTIAL_CONTENT = 206
        private const val CONTENT_HEADER = "Content-Length"
        private const val ACCEPT_HEADER = "Accept-Ranges"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}