package org.example.core.model



sealed class DownloadException (
    message: String,
    cause: Throwable? = null
) : Exception(message, cause){

    data class HeadRequestFailed(val httpCode: Int) :
        DownloadException("HEAD request failed: $httpCode")

    class MissingContentLength :
        DownloadException("Content-Length header missing")

    class RangeRequestsUnsupported :
        DownloadException("Server doesn't support byte ranges")

    data class ChunkDownloadFailed(
        val chunkIndex: Int,
        val httpCode: Int
    ):DownloadException("Chunk $chunkIndex failed: HTTP $httpCode")

    data class EmptyResponse(val chunkIndex: Int) :
            DownloadException("Empty response chunk $chunkIndex")

    data class NetworkError(override val cause: Throwable) :
        DownloadException("Network error: ${cause.message}", cause)

    data class ChunkSizeMismatch(
        val chunkIndex: Int,
        val expected: Long,
        val actual: Long
    ) : DownloadException("Chunk $chunkIndex size mismatch: expected $expected, got $actual")
}