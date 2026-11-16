package com.example.upaos.data.canvas

import com.example.upaos.data.model.CanvasFileDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

class CanvasDocumentException(message: String) : IOException(message)

/** Signed file URLs receive no bearer token or cookies, including across CDN redirects. */
object CanvasDocuments {
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).build()
    const val MAX_BYTES = 32L * 1024 * 1024
    fun safeUrl(raw: String): HttpUrl? = raw.toHttpUrlOrNull()?.takeIf {
        it.isHttps && it.port == 443 && it.username.isEmpty() && it.password.isEmpty() &&
            (it.host == "upao.instructure.com" || it.host.endsWith(".instructure.com") ||
                it.host.endsWith(".canvas-user-content.com") || it.host.endsWith(".amazonaws.com") ||
                // Canvas file storage: https://community.instructure.com/en/kb/articles/485223-canvas-domain-email-and-server-management
                it.host.endsWith(".inscloudgate.net"))
    }
    suspend fun download(metadata: CanvasFileDetail, directory: File, validate: () -> Unit): File = withContext(Dispatchers.IO) {
        validate()
        if (metadata.lockedForUser == true || metadata.hiddenForUser == true || metadata.locked == true) throw CanvasDocumentException("Canvas restringió el acceso a este archivo.")
        if ((metadata.size ?: 0) > MAX_BYTES) throw CanvasDocumentException("El archivo supera el límite de visualización de 32 MB.")
        var url = safeUrl(metadata.url.orEmpty()) ?: throw CanvasDocumentException("El servidor de este documento no está habilitado para su visualización.")
        directory.mkdirs()
        val extension = canvasDocumentExtension(metadata)
        val target = File.createTempFile("canvas-${metadata.id}-", ".$extension", directory)
        try {
            repeat(6) {
                validate()
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (response.code in 300..399) {
                        val redirect = response.header("Location")?.let { url.resolve(it) } ?: throw IOException("Redirección inválida")
                        url = safeUrl(redirect.toString()) ?: throw CanvasDocumentException("El destino de descarga no está habilitado para su visualización.")
                    } else {
                        if (!response.isSuccessful) throw CanvasDocumentException(when (response.code) {
                            401, 403 -> "Canvas no autorizó la descarga. Actualiza el documento y comprueba tus permisos."
                            404 -> "Este documento ya no está disponible en Canvas."
                            else -> "Canvas no pudo entregar el documento (HTTP ${response.code}). Vuelve a intentar."
                        })
                        val body = checkNotNull(response.body)
                        if (body.contentLength() > MAX_BYTES) throw CanvasDocumentException("El archivo supera el límite de visualización de 32 MB.")
                        if (body.contentType()?.subtype == "html" && metadata.contentType != "text/html")
                            throw CanvasDocumentException("Canvas devolvió una página de acceso en lugar del documento. Actualiza e intenta nuevamente.")
                        body.byteStream().use { input -> target.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var total = 0L
                            while (true) {
                                validate()
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                if (total > MAX_BYTES) throw CanvasDocumentException("El archivo supera el límite de visualización de 32 MB.")
                                output.write(buffer, 0, count)
                            }
                        } }
                        validate()
                        return@withContext target
                    }
                }
            }
            throw IOException("Demasiadas redirecciones")
        } catch (e: Exception) { target.delete(); throw e }
    }
}

fun canvasDocumentOwner(user: String): String = java.security.MessageDigest.getInstance("SHA-256")
    .digest(user.trim().lowercase().toByteArray()).joinToString("") { "%02x".format(it) }

internal fun canvasDocumentExtension(metadata: CanvasFileDetail): String {
    fun extension(name: String?) = name?.substringAfterLast('.', "")?.lowercase(java.util.Locale.ROOT)
        ?.takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
    return extension(metadata.filename) ?: extension(metadata.displayName) ?: when (metadata.contentType) {
        "application/pdf" -> "pdf"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
        "image/png" -> "png"
        "image/jpeg" -> "jpg"
        else -> "bin"
    }
}
