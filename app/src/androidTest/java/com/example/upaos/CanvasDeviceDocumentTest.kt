package com.example.upaos

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.upaos.data.api.CanvasApiClient
import com.example.upaos.data.canvas.CanvasDocuments
import com.example.upaos.data.local.TokenManager
import com.example.upaos.data.model.CanvasModuleItem
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@RunWith(AndroidJUnit4::class)
class CanvasDeviceDocumentTest {
    @Test fun installedModuleModelResolvesExternalLinks() {
        val item = Gson().fromJson("""{"id":1,"title":"Synthetic","type":"ExternalUrl","external_url":"https://drive.google.com/file/d/synthetic/view"}""", CanvasModuleItem::class.java)
        assertEquals("https://drive.google.com/file/d/synthetic/view", item.externalUrl)
    }

    /** Explicitly opt-in read-only check; never logs credentials, signed URLs or document contents. */
    @Test fun savedAccountCanReadModuleDocuments() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("canvasLiveRead") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tokens = TokenManager(context)
        val user = tokens.getSavedUser().orEmpty()
        val token = tokens.getCanvasToken(user).orEmpty()
        assertTrue("Se necesita una cuenta Canvas guardada", user.isNotBlank() && token.isNotBlank())
        val api = CanvasApiClient.pagedApiService
        val auth = "Bearer $token"
        val courses = api.getActiveCourses(auth).body().orEmpty()
        var checked = 0
        for (course in courses.take(3)) {
            val modules = api.getCourseModules(auth, course.id).body().orEmpty()
            for (item in modules.flatMap { it.items.orEmpty() }.filter { it.isFile }.take(2)) {
                val metadata = if (item.contentId != null) api.getFileByUrl(auth, "https://upao.instructure.com/api/v1/files/${item.contentId}").body()
                    else item.url?.let { api.getFileByUrl(auth, it).body() }
                assertNotNull("No se pudo consultar la ficha del archivo", metadata)
                val host = metadata!!.url?.toHttpUrlOrNull()?.host.orEmpty()
                try {
                    val file = CanvasDocuments.download(metadata, File(context.cacheDir, "canvas_device_check")) {
                        check(tokens.getSavedUser() == user) { "La cuenta cambió" }
                    }
                    try { assertTrue("Documento vacío", file.length() > 0) } finally { file.delete() }
                } catch (e: Exception) {
                    throw AssertionError("Documento: host=$host; causa=${e.javaClass.simpleName}; detalle=${e.message?.takeIf { !it.contains("://") && !it.contains("token", true) }}")
                }
                checked++
            }
        }
        assertTrue("No se encontraron documentos de módulos para comprobar", checked > 0)
    }
}
