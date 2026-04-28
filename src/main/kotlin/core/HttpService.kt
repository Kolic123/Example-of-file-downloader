package org.example.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

data class FileInfo(
    val contentLength: Long,
    val supportsRanges: Boolean
)

class HttpService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
){
    suspend fun getFileInfo(url: String): FileInfo {
        return withContext(Dispatchers.IO){
            val request = Request.Builder()
                .url(url)
                .head()
                .build()
            val response = client.newCall(request).execute()

            if(!response.isSuccessful){
                throw DownloadException("HEAD request failed: ${response.code}")
            }

            val contentLength = response.headers["Content-Length"]?.toLongOrNull()
                ?: throw DownloadException("Content-Length header missing")

            val supportsRanges = response.headers["Accept-Ranges"] == "bytes"

            if(!supportsRanges){
                throw DownloadException("Sever doesnt support byte ranges")
            }

            FileInfo(contentLength, supportsRanges)
        }
    }

    suspend fun downloadChunk(url: String, rangeStart: Long, rangeEnd: Long): ByteArray{
        return withContext(Dispatchers.IO){
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=$rangeStart-$rangeEnd")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if(!response.isSuccessful && response.code != 206){
                throw DownloadException("Faild to download chunk: ${response.code}")
            }
            response.body?.bytes() ?: throw DownloadException("Empty response")
        }
    }


}