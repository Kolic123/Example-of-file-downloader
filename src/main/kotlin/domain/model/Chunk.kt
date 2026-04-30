package org.example.domain.model

data class Chunk(
    val start: Long,
    val end: Long,
    val index: Int,
) {
    val size: Long get() = end - start + 1
}
