package tungtq.so1.asia.dl

import android.util.Log
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Một mục trong lịch sử tải, lưu bền vào Download/VDM/history.json. */
data class HistoryEntry(
    val url: String,
    val title: String,
    val fileName: String,
    val sizeBytes: Long,
    val downloadedAt: Long,
    val host: String,
    val formatLabel: String,
)

/**
 * Lịch sử tải lưu vào file JSON trong thư mục Download/VDM (cùng chỗ với file
 * đã tải) để còn lại khi gỡ app và người dùng copy đi đâu cũng được.
 * Đọc/ghi bằng org.json có sẵn trong Android, không thêm thư viện.
 * Mọi lỗi đọc/ghi đều nuốt lặng: lịch sử hỏng thì coi như trống, không
 * được làm hỏng lượt tải hay crash app.
 */
object DownloadHistory {

    private const val TAG = "VDM"
    const val FILE_NAME = "history.json"
    private const val TMP_NAME = "history.json.tmp"
    private const val MAX_ENTRIES = 500

    /** File lịch sử, nằm trong thư mục tải chung. */
    fun historyFile(): File = File(Downloader.downloadDir(), FILE_NAME)

    /** Nhãn host ngắn từ link gốc, vd "Threads", "Instagram", "YouTube". */
    fun hostLabel(url: String): String {
        val host = try {
            java.net.URL(url).host.lowercase()
        } catch (_: Exception) {
            ""
        }
        return when {
            "threads.net" in host || "threads.com" in host -> "Threads"
            "instagram.com" in host -> "Instagram"
            "tiktok.com" in host -> "TikTok"
            "douyin.com" in host -> "Douyin"
            "facebook.com" in host || "fb.watch" in host -> "Facebook"
            "youtube.com" in host || "youtu.be" in host -> "YouTube"
            "x.com" in host || "twitter.com" in host -> "X"
            "reddit.com" in host -> "Reddit"
            else -> host.removePrefix("www.").takeIf { it.isNotBlank() } ?: "Khác"
        }
    }

    /** Ghi thêm 1 mục vào đầu lịch sử (mới nhất trước). */
    @Synchronized
    fun record(entry: HistoryEntry) {
        try {
            val entries = loadMutable()
            entries.removeAll { it.url == entry.url && it.fileName == entry.fileName }
            entries.add(0, entry)
            while (entries.size > MAX_ENTRIES) entries.removeAt(entries.lastIndex)
            write(entries)
        } catch (e: Exception) {
            Log.w(TAG, "cannot record download history", e)
        }
    }

    /** Đọc toàn bộ lịch sử, mới nhất trước. File hỏng thì trả về rỗng. */
    fun load(): List<HistoryEntry> {
        return try {
            loadMutable()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Xóa 1 mục khỏi lịch sử (không xóa file đã tải). */
    @Synchronized
    fun remove(entry: HistoryEntry) {
        try {
            val entries = loadMutable()
            entries.removeAll {
                it.url == entry.url && it.fileName == entry.fileName &&
                    it.downloadedAt == entry.downloadedAt
            }
            write(entries)
        } catch (e: Exception) {
            Log.w(TAG, "cannot remove history entry", e)
        }
    }

    /** Xóa toàn bộ lịch sử (không xóa file đã tải). */
    @Synchronized
    fun clear() {
        try {
            historyFile().delete()
            File(Downloader.downloadDir(), TMP_NAME).delete()
        } catch (_: Exception) {
        }
    }

    private fun loadMutable(): MutableList<HistoryEntry> {
        val file = historyFile()
        if (!file.exists() || file.length() == 0L) return mutableListOf()
        val text = file.readText(Charsets.UTF_8)
        if (text.isBlank()) return mutableListOf()
        val arr = JSONArray(text)
        val out = mutableListOf<HistoryEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out += HistoryEntry(
                url = o.optString("url"),
                title = o.optString("title"),
                fileName = o.optString("fileName"),
                sizeBytes = o.optLong("sizeBytes"),
                downloadedAt = o.optLong("downloadedAt"),
                host = o.optString("host"),
                formatLabel = o.optString("formatLabel"),
            )
        }
        return out
    }

    /** Ghi qua file tạm rồi đổi tên, để không đọc phải file đang ghi dở. */
    private fun write(entries: List<HistoryEntry>) {
        val dir = Downloader.downloadDir()
        if (!dir.exists()) dir.mkdirs()
        val arr = JSONArray()
        for (e in entries) {
            arr.put(
                JSONObject().apply {
                    put("url", e.url)
                    put("title", e.title)
                    put("fileName", e.fileName)
                    put("sizeBytes", e.sizeBytes)
                    put("downloadedAt", e.downloadedAt)
                    put("host", e.host)
                    put("formatLabel", e.formatLabel)
                },
            )
        }
        val tmp = File(dir, TMP_NAME)
        tmp.writeText(arr.toString(2), Charsets.UTF_8)
        if (!tmp.renameTo(historyFile())) {
            tmp.copyTo(historyFile(), overwrite = true)
            tmp.delete()
        }
    }
}
