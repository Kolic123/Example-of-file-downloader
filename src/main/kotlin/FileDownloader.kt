package org.example



import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.example.core.ChunkSplitter
import org.example.core.FileMerger
import org.example.core.HttpService
import org.example.core.ParallelChunkDownloader
import org.example.facade.DownloadRequest
import org.example.facade.DownloadResult
import org.example.facade.FileDownloaderInterface
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class FileDownloader(
    private val httpService: HttpService = HttpService(),
    private val chunkSplitter: ChunkSplitter = ChunkSplitter(),
    private val chunkDownloader: ParallelChunkDownloader = ParallelChunkDownloader(httpService),
    private val fileMerger: FileMerger = FileMerger()
) : FileDownloaderInterface {

    private val activeDownloads = ConcurrentHashMap<String, Job>()
    private val cancellationFlags = ConcurrentHashMap<String, AtomicBoolean>()

    override suspend fun downloadFile(url: String, outputDestination: String): DownloadResult {
        return downloadFile(DownloadRequest(
            url = url,
            destinationPath = outputDestination
        ))
    }

    override suspend fun downloadFile(request: DownloadRequest): DownloadResult {

        val startTime = System.currentTimeMillis()
        val cancellationFlag = AtomicBoolean(false)
        cancellationFlags[request.url] = cancellationFlag

        return try {
            val fileInfo = httpService.getFileInfo(request.url)
            val chunks = chunkSplitter.splitIntoChunks(
                fileInfo.contentLength,
                request.chunkSizeBytes
            )
            val tempDir = File(System.getProperty("java.io.tempdir"), "download_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val deferred = CoroutineScope(Dispatchers.IO).async {
                chunkDownloader.downloadChunks(
                    url = request.url,
                    chunks = chunks,
                    tempDir = tempDir,
                    maxParallel = request.maxParallelChunks,
                    onProgress = request.onProgress,
                    cancellationFlag = cancellationFlag
                )
            }

            activeDownloads[request.url] = deferred

            val chunkFiles = deferred.await()

            if(cancellationFlag.get()){
               return DownloadResult(
                   success = false,
                   filePath = request.destinationPath,
                   fileSize = 0,
                   durationMs = System.currentTimeMillis() - startTime,
                   errorMessage = "Download cancelled"
               )
            }


            fileMerger.mergeChunks(
                chunkFiles = chunkFiles,
                destinationFile = File(request.destinationPath),
                totalSize = fileInfo.contentLength
            )

            tempDir.deleteRecursively()

            DownloadResult(
                success = true,
                filePath = request.destinationPath,
                fileSize = fileInfo.contentLength,
                durationMs = System.currentTimeMillis() - startTime
            )
        }catch (e: Exception){
            DownloadResult(
                success = false,
                filePath = request.destinationPath,
                fileSize = 0,
                durationMs = System.currentTimeMillis() - startTime,
                errorMessage = e.message
            )
        }finally {
            activeDownloads.remove(request.url)
            cancellationFlags.remove(request.url)
        }

    }

    override fun cancelAllDownloads(){
        activeDownloads.values.forEach { it.cancel() }
        cancellationFlags.values.forEach { it.set(true) }
        activeDownloads.clear()
        cancellationFlags.clear()
    }

}