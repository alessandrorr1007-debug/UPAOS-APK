package com.example.upaos.ui.canvas

import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.upaos.data.canvas.canvasNativeRoute
import com.example.upaos.data.canvas.canvasIsSubmissionUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Browsing has no access to the API bearer token, private files or a JavaScript bridge. */
@Composable
internal fun CanvasWebContent(url: String, title: String, close: () -> Unit) {
    val context = LocalContext.current
    val absolute = remember(url) { "https://upao.instructure.com/".toHttpUrlOrNull()!!.resolve(url) }
    if (canvasIsSubmissionUrl(url)) {
        LaunchedEffect(url) { openCanvasExternal(context, absolute.toString()); close() }
        return
    }
    var browser by remember { mutableStateOf<WebView?>(null) }
    var loading by remember(url) { mutableStateOf(true) }
    var error by remember(url) { mutableStateOf<String?>(null) }
    var route by remember(url) { mutableStateOf<com.example.upaos.data.canvas.CanvasNativeRoute?>(null) }
    route?.let { CanvasLinkedResource(it, canvasSavedToken()) { route = null } }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BackHandler { if (browser?.canGoBack() == true) browser?.goBack() else close() }
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2)
                    TextButton(onClick = close) { Text("Cerrar") }
                }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (absolute?.isHttps != true || absolute.username.isNotEmpty() || absolute.password.isNotEmpty()) {
                    Text("Este enlace no admite una vista segura dentro de la app.")
                } else AndroidView(modifier = Modifier.weight(1f).fillMaxWidth(), factory = { ctx ->
                    WebView(ctx).apply {
                        browser = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.setSupportMultipleWindows(false)
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val parsed = request.url.toString().toHttpUrlOrNull() ?: return true
                                if (!parsed.isHttps || parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) return true
                                if (!request.isForMainFrame) return false
                                if (canvasIsSubmissionUrl(parsed.toString())) {
                                    openCanvasExternal(context, parsed.toString()); return true
                                }
                                val native = canvasNativeRoute(parsed.toString())
                                if (native != null && native.kind != "quizzes" && native != canvasNativeRoute(absolute.toString())) { route = native; return true }
                                return false
                            }
                            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) { loading = true; error = null }
                            override fun onPageFinished(view: WebView, url: String?) { loading = false }
                            override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                                if (request.isForMainFrame) { loading = false; error = "No se pudo cargar el contenido. Revisa tu conexión." }
                            }
                        }
                        loadUrl(absolute.toString())
                    }
                }, onRelease = { browser = null; it.stopLoading(); it.destroy() })
            }
        }
    }
}
