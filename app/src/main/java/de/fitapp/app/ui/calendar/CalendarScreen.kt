@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.fitapp.app.ui.calendar

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader

/**
 * Ziele-Kalender: täglicher Ampel-Tracker (Schlaf, Kalorienziel, Goonen, Lernen, Sport).
 * Die Seite liegt unter assets/kalender.html und wird lokal in einer WebView geladen;
 * die Einträge speichert sie per localStorage auf dem Gerät.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CalendarScreen() {
    Scaffold(
        topBar = { LargeTopAppBar(title = { Text("Kalender", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        AndroidView(
            modifier = Modifier.padding(padding).fillMaxSize(),
            factory = { context ->
                val loader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                    .build()
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#07090D"))
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true // localStorage für die Einträge
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView, request: WebResourceRequest
                        ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)
                    }
                    loadUrl("https://appassets.androidplatform.net/assets/kalender.html")
                }
            }
        )
    }
}
