package com.example.upaos.ui.canvas

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import com.example.upaos.data.api.TrustedServers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal fun openCanvasExternal(context: Context, url: String?) {
    val parsed = url?.toHttpUrlOrNull() ?: return
    if (!TrustedServers.isCanvas(parsed)) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(parsed.toString()))) }
}

@Composable
internal fun CanvasExternalButton(url: String?, label: String = "Ver en Canvas", external: Boolean = false) {
    val context = LocalContext.current
    var preview by remember(url) { mutableStateOf(false) }
    if (preview && url != null) CanvasWebContent(url, "Canvas") { preview = false }
    OutlinedButton(enabled = url?.toHttpUrlOrNull()?.let(TrustedServers::isCanvas) == true,
        onClick = { if (external) openCanvasExternal(context, url) else preview = true }) { Text(label) }
}
