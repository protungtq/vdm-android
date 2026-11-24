package tungtq.so1.asia

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tungtq.so1.asia.dl.DouyinDirect
import tungtq.so1.asia.dl.Downloader
import tungtq.so1.asia.dl.ThreadsDirect

/** Một link video Threads đã bắt được, kèm dung lượng đo được (nếu có). */
private data class ThreadsCapturedLink(
    val url: String,
    val sizeLabel: String? = null,
)

/**
 * Mở trang bài Threads trong trình duyệt của app và BẮT request video
 * (cùng cách extension trên trình duyệt máy tính bắt link, và cùng cách app
 * đã làm với Douyin): khi trang chạy video, mọi request mạng đi qua
 * shouldInterceptRequest, link nào trông như file video sẽ được giữ lại
 * để tải trực tiếp. Bắt được link đầu tiên thì tải luôn.
 *
 * Vì sao cần màn này: yt-dlp không có extractor cho Threads (bản 2026.11.23
 * không có trong danh sách hỗ trợ), và trang bài Threads không nhúng sẵn
 * link video trong HTML (video tải sau qua GraphQL), nên không quét được
 * bằng bộ quét chính hay bằng cách tải HTML.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadsWebViewScreen(
    pageUrl: String,
    onVideoFound: (directUrl: String, pageTitle: String?) -> Unit,
    onAutoDownload: (directUrl: String, pageTitle: String?) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var webView by remember { mutableStateOf<WebView?>(null) }
    val capturedLinks = remember { mutableStateListOf<ThreadsCapturedLink>() }
    var notified by remember { mutableStateOf(false) }
    var autoDownloaded by remember { mutableStateOf(false) }

    fun addCaptured(url: String) {
        // Gộp link trùng: cùng 1 file video nhưng chữ ký khác nhau thì chỉ giữ 1.
        val key = threadsCaptureKey(url)
        if (capturedLinks.any { threadsCaptureKey(it.url) == key }) return
        val link = ThreadsCapturedLink(url)
        capturedLinks.add(link)
        // Đo dung lượng để hiện cạnh link, giúp chọn dễ hơn.
        scope.launch(Dispatchers.IO) {
            val size = DouyinDirect.probeContentLength(url, ThreadsDirect.REFERER)
            val sizeLabel = DouyinDirect.formatBytes(size)
            if (sizeLabel != null) {
                withContext(Dispatchers.Main) {
                    val idx = capturedLinks.indexOfFirst { it.url == url }
                    if (idx >= 0) {
                        capturedLinks[idx] = capturedLinks[idx].copy(sizeLabel = sizeLabel)
                    }
                }
            }
        }
        // Bắt được link đầu tiên thì tải luôn, khỏi chọn.
        if (!autoDownloaded) {
            autoDownloaded = true
            Toast.makeText(
                context,
                "Đã bắt được link video, đang tải…",
                Toast.LENGTH_SHORT,
            ).show()
            onAutoDownload(url, webView?.title)
        }
    }

    fun notifyCaptured() {
        if (!notified) {
            notified = true
            Toast.makeText(context, "Đã bắt được link video.", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler {
        val wv = webView
        if (wv?.canGoBack() == true) {
            wv.goBack()
        } else {
            onClose()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Threads") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VdmRed,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                "App sẽ tự bắt link video rồi tải luôn. " +
                    "Nếu trang hiện yêu cầu đăng nhập, hãy đăng nhập rồi đợi video chạy.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (capturedLinks.isNotEmpty()) {
                Text(
                    "Đã bắt được ${capturedLinks.size} link video:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(capturedLinks, key = { it.url }) { captured ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val parts = listOfNotNull(captured.sizeLabel)
                            Text(
                                if (parts.isNotEmpty()) {
                                    "${parts.joinToString(" · ")} · ${threadsShortUrl(captured.url)}"
                                } else {
                                    threadsShortUrl(captured.url)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    onVideoFound(captured.url, webView?.title)
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Tải")
                            }
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        val wv = webView ?: return@OutlinedButton
                        wv.evaluateJavascript(THREADS_EXTRACT_VIDEO_SRC_JS) { result ->
                            val src = threadsParseJsStringResult(result)
                            if (src.isBlank() || src.startsWith("blob:")) {
                                Toast.makeText(
                                    context,
                                    "Chưa lấy được link. Hãy đợi video chạy rồi thử lại.",
                                    Toast.LENGTH_LONG,
                                ).show()
                            } else {
                                onVideoFound(src, wv.title)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text("Lấy link từ trình phát")
                }
            }
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        // Giả dạng Chrome trên điện thoại để Threads trả trang đầy đủ.
                        settings.userAgentString = Downloader.CHROME_UA
                        webViewClient = object : SafeWebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest,
                            ): WebResourceResponse? {
                                val url = request.url.toString()
                                if (isThreadsVideoUrl(url)) {
                                    view.post {
                                        addCaptured(url)
                                        notifyCaptured()
                                    }
                                    return null
                                }
                                // Trả null để request chạy bình thường.
                                return null
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                // Tự bấm play để video chạy và request video xuất hiện sớm.
                                view.evaluateJavascript(THREADS_AUTOPLAY_JS, null)
                            }
                        }
                        val cm = CookieManager.getInstance()
                        cm.setAcceptCookie(true)
                        cm.setAcceptThirdPartyCookies(this, true)
                        loadUrl(pageUrl)
                        webView = this
                    }
                },
                update = {},
                onRelease = { it.destroy() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Khóa gộp link trùng: cùng máy chủ + cùng đường dẫn thì là cùng 1 file,
 * kể cả khi chữ ký trong query khác nhau.
 */
private fun threadsCaptureKey(url: String): String {
    return try {
        val parsed = URL(url)
        "${parsed.host}${parsed.path}"
    } catch (_: Exception) {
        url
    }
}

/** Link nào trông như file video trực tiếp thì bắt. */
private fun isThreadsVideoUrl(url: String): Boolean {
    val lower = url.lowercase()
    // Loại ảnh và file phụ trước.
    if (lower.contains(".jpeg") || lower.contains(".jpg") ||
        lower.contains(".png") || lower.contains(".webp") ||
        lower.contains(".gif") || lower.contains(".css") ||
        lower.contains(".js")
    ) {
        return false
    }
    if (lower.contains(".mp4") || lower.contains(".mov") || lower.contains(".webm")) {
        return true
    }
    val host = try {
        URL(url).host.lowercase()
    } catch (_: Exception) {
        return false
    }
    // CDN video của Instagram/Threads: link phát đôi khi không có đuôi file.
    if (host.endsWith("cdninstagram.com") && lower.contains("video")) return true
    if (host.endsWith("fbcdn.net") && lower.contains("video")) return true
    return false
}

private fun threadsShortUrl(url: String): String {
    val host = try {
        URL(url).host
    } catch (_: Exception) {
        ""
    }
    val tail = url.takeLast(48)
    return if (host.isNotEmpty()) "$host …$tail" else tail
}

// Hai đoạn JS dưới đây giống bản trong DouyinWebViewScreen: lấy src của
// phần tử video đang chạy, và tự bấm play khi video xuất hiện.
private const val THREADS_EXTRACT_VIDEO_SRC_JS =
    "(function(){var v=document.querySelector('video');if(!v)return '';" +
        "var s=v.currentSrc||v.src||'';" +
        "if(!s){var so=v.querySelector('source');if(so)s=so.src||'';}" +
        "return s;})()"

private const val THREADS_AUTOPLAY_JS =
    "(function(){if(window.__vdmAutoplay)return;window.__vdmAutoplay=true;" +
        "var n=0;var t=setInterval(function(){n++;" +
        "var v=document.querySelector('video');" +
        "if(v){try{v.muted=true;var p=v.play();if(p&&p.catch)p.catch(function(){});}" +
        "catch(e){}clearInterval(t);}" +
        "if(n>=24)clearInterval(t);},500);})()"

/** evaluateJavascript trả về chuỗi JSON (có ngoặc kép, đã escape). */
private fun threadsParseJsStringResult(result: String?): String {
    if (result.isNullOrBlank() || result == "null") return ""
    var text = result.trim()
    if (text.length >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
        text = text.substring(1, text.length - 1)
    }
    return text
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
        .replace("\\/", "/")
}
