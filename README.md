# VDM cho Android

Bản mobile của VN Download Manager. Đang lướt Facebook, YouTube, TikTok,
Douyin hay bất kỳ trang nào, bấm **Chia sẻ** rồi chọn **VDM** là app tự quét
link và tải video về máy. Cũng có thể mở app và dán link vào, hoặc duyệt web
ngay trong trình duyệt tích hợp của app.

- Mã gói (applicationId): `tungtq.so1.asia`
- Tên hiển thị: VDM
- Trang chủ: https://vdm.vuivui.org
- Bản desktop (Windows/macOS): https://github.com/protungtq/vdm

## Tải về

Tải APK mới nhất ở mục [Releases](https://github.com/protungtq/vdm-android/releases)
(bản `arm64` cho hầu hết điện thoại Android hiện nay). Yêu cầu Android 8.0+.

APK là bản dựng debug từ GitHub Actions. Khi cài đặt, cần cho phép cài đặt
từ nguồn không xác định. Nếu cài đè lên bản cũ mà máy báo lỗi chữ ký, gỡ
bản cũ rồi cài lại (file trong thư mục `Download/VDM/` không bị mất).

## Tính năng (bản 0.2.15)

- Nhận link qua nút Chia sẻ của Android (ACTION_SEND, text/plain).
- Quét link bằng yt-dlp, hiện tiêu đề, ảnh đại diện, thời lượng và danh sách
  chất lượng (1080p, 720p…), kèm lựa chọn chỉ lấy âm thanh.
- Tải ở chế độ foreground service nên vẫn chạy khi tắt màn hình hay chuyển
  app, có thông báo tiến trình.
- Tải đa kết nối bằng aria2c, gộp video + âm thanh bằng FFmpeg.
- File lưu trong thư mục `Download/VDM/`, đặt tên theo dạng
  `VDM_ngày_tháng_năm_số-thứ-tự` (ví dụ `VDM_16_11_2026_001.mp4`), đếm lại
  từ 001 mỗi ngày.
- Trình duyệt tích hợp: chặn quảng cáo 4 bộ lọc, tự bắt link video/âm thanh
  trên trang đang mở, bắt được link Douyin không logo.
- Cập nhật bộ quét yt-dlp trong app (không cần chờ bản app mới).
- Tự thử lại không gửi cookie khi gặp lỗi đọc trang Facebook.
- Xem trước video gộp tiếng từ luồng video rời.
- Báo cáo sự cố tích hợp (CrashReporter).

## Bộ máy

- [youtubedl-android](https://github.com/yausername/youtubedl-android) 0.18.1
  (nhúng Python + yt-dlp, FFmpeg, aria2c).
- Kotlin + Jetpack Compose (Material 3), minSdk 26 (Android 8.0+).

## Build

```sh
gradle assembleDebug
```

APK ra ở `app/build/outputs/apk/debug/` (chia theo kiến trúc CPU + 1 bản
universal). GitHub Actions tự build mỗi lần có thay đổi trên nhánh `main`
và đính kèm APK vào mục Artifacts của lần chạy đó.

Yêu cầu: JDK 17, Android SDK với platform API 35.

## Lịch sử

Repo này tách ra từ thư mục `android/` của
[protungtq/vdm](https://github.com/protungtq/vdm) ngày 17/11/2026. Các bản
phát hành 0.2.14–0.2.15 trước đó vẫn nằm ở repo cũ; từ bản 0.2.15 trở đi,
bản phát hành mới nằm ở repo này.

## Giấy phép

Dự án phát hành theo **GPL-3.0** (xem file `LICENSE`), do dùng thư viện
youtubedl-android (GPL-3.0).

## Ghi công

- yt-dlp và youtubedl-android (yausername) — bộ máy quét và tải video.
- FFmpeg — gộp video/âm thanh và tách MP3.
- aria2c — tải đa kết nối.
