package org.example.data.service

import org.example.domain.model.Chunk
import org.example.domain.service.ChunkSplitter


class FixedSizeChunkSplitter : ChunkSplitter {
    override fun splitIntoChunks(
        totalSize: Long,
        chunkSize: Long
    ): List<Chunk> {
        require(totalSize >= 0) { "totalSize must be non-negative, was $totalSize" }
        require(chunkSize > 0) { "chunkSize must be positive, was $chunkSize" }

        if (totalSize == 0L) return emptyList()

        return (0L until totalSize step chunkSize)
            .mapIndexed { index, start ->
                val end = minOf(start + chunkSize - 1, totalSize - 1)
                Chunk(start = start, end = end, index = index)
            }
    }
}