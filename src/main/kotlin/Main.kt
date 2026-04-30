package org.example

import kotlinx.coroutines.runBlocking
import org.example.core.util.mb
import org.example.data.fileDownloader.FileDownloaderImpl
import org.example.data.service.FixedSizeChunkSplitter
import org.example.data.service.OkHttpClientAdapter
import org.example.data.service.ParallelChunkDownloaderImpl
import org.example.domain.model.DownloadEvent
import org.example.domain.model.DownloadRequest
import kotlin.system.exitProcess

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
suspend fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("[ERROR] No arguments found")
        System.err.println("Example: ./gradlew runDownloader -Pargs=\"http://localhost:8080/test.zip\"")
        exitProcess(1)
    }

    val request = DownloadRequest(
        url = args[0],
        destinationPath = "C:\\Users\\david\\Documents\\docker-shared\\downloaded-file.txt",
        maxParallelChunks = 10,
        chunkSizeBytes = 1.mb
    )

    val client = OkHttpClientAdapter()
    val downloader = FileDownloaderImpl(
        httpClient = client,
        chunkPlanner = FixedSizeChunkSplitter(),
        chunkDownloader = ParallelChunkDownloaderImpl(client)
    )

    downloader.download(request).collect { event ->
        when (event) {
            is DownloadEvent.Progress -> {
                print("\rProgress: ${event.percentage}% (${event.bytesDownloaded}/${event.totalBytes} bytes)")
            }

            is DownloadEvent.Completed -> {
                println("\n[SUCCESS] Downloaded ${event.result.fileSize} bytes to ${event.result.filePath} in ${event.result.durationMs}ms")
            }

            is DownloadEvent.Failed -> {
                println("\n[ERROR] ${event.result.errorMessage}")
                exitProcess(1)
            }
        }
    }
}