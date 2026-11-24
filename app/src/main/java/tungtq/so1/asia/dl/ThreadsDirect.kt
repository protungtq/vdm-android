package tungtq.so1.asia.dl

/**
 * Nhận diện link bài Threads và hằng số cho đường tải qua WebView.
 *
 * yt-dlp (bộ quét của app, bản 2026.11.23) KHÔNG có extractor cho Threads
 * (không có trong _extractors.py lẫn supportedsites.md), nên mọi link
 * Threads đưa qua yt-dlp đều lỗi "Unsupported URL", và cập nhật bộ quét
 * cũng không sửa được. Trang bài Threads cũng không nhúng sẵn link video
 * trong HTML (trang là vỏ JS, video tải sau qua GraphQL), nên không lấy
 * được link bằng cách tải HTML. Đường duy nhất trong app: mở trang trong
 * WebView rồi bắt request .mp4 như cách đã làm với Douyin
 * (xem ThreadsWebViewScreen).
 */
object ThreadsDirect {

    /** Referer khi tải video Threads bắt được. */
    const val REFERER = "https://www.threads.com/"

    private val THREADS_HOSTS = setOf(
        "threads.net",
        "www.threads.net",
        "threads.com",
        "www.threads.com",
    )

    /**
     * true khi url là link bài Threads: dạng /@user/post/<id>, hoặc link chia
     * sẻ dạng /share/... (trình duyệt tự chuyển về trang bài khi mở).
     * Link chia sẻ phải nhận ở đây vì Downloader.resolveUrl chỉ theo redirect
     * cho link rút gọn TikTok/Douyin, link Threads share tới đây vẫn còn
     * nguyên dạng /share/... — không nhận thì rơi xuống yt-dlp và lỗi
     * "Unsupported URL" như bản 0.2.18.
     */
    fun isThreadsPost(url: String): Boolean {
        return try {
            val parsed = java.net.URL(url)
            val host = parsed.host.lowercase()
            if (host !in THREADS_HOSTS) return false
            "/post/" in parsed.path || parsed.path.startsWith("/share/")
        } catch (_: Exception) {
            false
        }
    }

    /** Tên người đăng từ link bài, vd "@abcvip.truyendoaudio", null khi không đọc được. */
    fun extractUsername(url: String): String? {
        return try {
            val path = java.net.URL(url).path
            val match = Regex("/@([^/]+)/post/").find(path)
            match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { "@$it" }
        } catch (_: Exception) {
            null
        }
    }
}
