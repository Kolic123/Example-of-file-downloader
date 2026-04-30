package org.example.data.service

import org.example.domain.model.Chunk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FixedSizeChunkSplitterTest {

    private val planner = FixedSizeChunkSplitter()

    @Test
    fun `splits evenly when total size is multiple of chunk size`() {
        val chunks = planner.splitIntoChunks(totalSize = 3000, chunkSize = 1000)

        assertEquals(
            listOf(
                Chunk(start = 0, end = 999, index = 0),
                Chunk(start = 1000, end = 1999, index = 1),
                Chunk(start = 2000, end = 2999, index = 2),
            ),
            chunks
        )
    }

    @Test
    fun `last chunk is smaller when total size is not a multiple`() {
        val chunks = planner.splitIntoChunks(totalSize = 2500, chunkSize = 1000)

        assertEquals(3, chunks.size)
        assertEquals(2000L..2499L, chunks.last().start..chunks.last().end)
        assertEquals(500, chunks.last().size)
    }

    @Test
    fun `returns empty list for zero size`() {
        assertTrue(planner.splitIntoChunks(totalSize = 0, chunkSize = 1000).isEmpty())
    }

    @Test
    fun `single chunk when total size is smaller than chunk size`() {
        val chunks = planner.splitIntoChunks(totalSize = 500, chunkSize = 1000)

        assertEquals(1, chunks.size)
        assertEquals(Chunk(start = 0, end = 499, index = 0), chunks.single())
    }

    @Test
    fun `rejects negative total size`() {
        assertFailsWith<IllegalArgumentException> {
            planner.splitIntoChunks(totalSize = -1, chunkSize = 1000)
        }
    }

    @Test
    fun `rejects zero or negative chunk size`() {
        assertFailsWith<IllegalArgumentException> {
            planner.splitIntoChunks(totalSize = 1000, chunkSize = 0)
        }
    }
}