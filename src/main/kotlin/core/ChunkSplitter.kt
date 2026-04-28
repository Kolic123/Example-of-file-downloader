package org.example.core


data class Chunk(
    val start: Long,
    val end: Long,
    val index: Int,
)
class ChunkSplitter {
    fun splitIntoChunks(totalSize: Long, chunkSizeBytes: Long): List<Chunk>{
        val chunks = mutableListOf<Chunk>()
        var start = 0L
        var chunkIndex = 0

        while(start < totalSize){
            val end = minOf(start + chunkSizeBytes - 1, totalSize - 1)
            chunks.add(Chunk(start, end, chunkIndex))
            start = end + 1
            chunkIndex++
        }
        return chunks
    }
}