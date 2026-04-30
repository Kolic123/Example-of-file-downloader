package org.example.domain.service

import org.example.domain.model.Chunk

interface ChunkSplitter {
    fun splitIntoChunks(totalSize: Long, chunkSize: Long): List<Chunk>
}