package org.example

import kotlinx.coroutines.runBlocking
import org.example.facade.DownloadRequest

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main(args: Array<String>) = runBlocking {

    if (args.isEmpty()) {
        println("[ERROR] No arguments found")
        println("Example: ./gradlew runDowloader -Pargs=\"http://localhost:8080/test.zip\"")
    }
    val fileDownloader = FileDownloader()
    val url = args[0]
    val destination = "C:\\Users\\david\\Documents\\docker-shared\\downloaded-file.txt"
    val request  = DownloadRequest(
        url = url,
        destinationPath = destination,
        maxParallelChunks = 10,
        chunkSizeBytes = 1024 * 1024,
        onProgress = { progress ->
            print("\rProgress: ${progress.percentage}% (${progress.downloadedBytes}/${progress.totalBytes} bytes)")
        }
    )

    val result = fileDownloader.downloadFile(request)
    if(result.success){
        println("\n[SUCCESS] Successfully downloaded")
    }else{
        println("\n[ERROR] An error occurred, ${result.errorMessage}")
    }
}