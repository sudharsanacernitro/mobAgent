package com.rk.terminal


import android.os.Build
import com.rk.libcommons.localDir
import com.rk.libcommons.child
import com.rk.terminal.ui.screens.terminal.Rootfs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

private data class DownloadFile(val url: String, val outputFile: File)

private val abiMap = mapOf(
    "x86_64" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/x86_64/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/x86_64/proot",
        alpine = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/x86_64/alpine-minirootfs-3.21.0-x86_64.tar.gz"
    ),
    "arm64-v8a" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/aarch64/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/aarch64/proot",
        alpine = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/aarch64/alpine-minirootfs-3.21.0-aarch64.tar.gz"
    ),
    "armeabi-v7a" to AbiUrls(
        talloc = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/arm/libtalloc.so.2",
        proot = "https://raw.githubusercontent.com/Xed-Editor/Karbon-PackagesX/main/arm/proot",
        alpine = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/armhf/alpine-minirootfs-3.21.0-armhf.tar.gz"
    )
)

private data class AbiUrls(val talloc: String, val proot: String, val alpine: String)


suspend fun setupAlpine(
    onProgress: (Float) -> Unit = {},
    onComplete: () -> Unit,
    onError: (Exception) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val abi = Build.SUPPORTED_ABIS.firstOrNull { it in abiMap }
                ?: throw RuntimeException("Unsupported CPU")

            val filesToDownload = listOf(
                "libtalloc.so.2" to abiMap[abi]!!.talloc,
                "proot" to abiMap[abi]!!.proot,
                "alpine.tar.gz" to abiMap[abi]!!.alpine
            ).map { (name, url) -> DownloadFile(url, Rootfs.reTerminal.child(name)) }

            var completedFiles = 0
            val totalFiles = filesToDownload.size

            filesToDownload.forEach { file ->
                val outputFile = file.outputFile.apply { parentFile?.mkdirs() }
                if (!outputFile.exists()) {
                    downloadFile(file.url, outputFile) { downloaded, total ->
                        val progress = ((completedFiles + downloaded.toFloat() / total) / totalFiles).coerceIn(0f, 1f)
                        onProgress(progress)
                    }
                }
                completedFiles++
                outputFile.setExecutable(true, false)
            }

            onComplete()
        } catch (e: Exception) {
            localDir().deleteRecursively()
            onError(e)
        }
    }
}

private suspend fun downloadFile(url: String, outputFile: File, onProgress: (Long, Long) -> Unit) {
    withContext(Dispatchers.IO) {
        OkHttpClient().newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to download: ${response.code}")

            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            outputFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        onProgress(downloadedBytes, totalBytes)
                    }
                }
            }
        }
    }
}

