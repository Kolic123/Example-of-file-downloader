package org.example.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

class FileMerger {

    suspend fun mergeChunks(chunkFiles: List<File>, destinationFile: File, totalSize: Long
    ) = withContext(Dispatchers.IO) {

        val sortedChunks = chunkFiles.sortedBy { file ->
            val match = Regex("chunk_(\\d+)").find(file.name)
            match?.groupValues?.get(1)?.toInt() ?: 0
        }


        RandomAccessFile(destinationFile, "rw").use { raf ->
            raf.setLength(totalSize)

            var currentPosition = 0L


            for (chunkFile in sortedChunks) {
                val chunkData = chunkFile.readBytes()
                raf.seek(currentPosition)
                raf.write(chunkData)
                currentPosition += chunkData.size

            }


        }


    }
}