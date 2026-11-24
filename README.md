# Widget Studio (HyperOS)
Widget đồng hồ + thời tiết (Open-Meteo) + lịch âm. 5 mẫu: Tối giản, Chi tiết, Lịch âm nổi bật, Dọc 2x4, Dải mini 4x1.

## Lấy file APK bằng GitHub (không cần Android Studio)
1. Tạo repo mới trên github.com, rồi "uploading an existing file" và kéo thả TOÀN BỘ nội dung đã giải nén (nhớ có thư mục .github).
   Nếu thư mục .github không lên: Add file > Create new file, đặt tên `.github/workflows/build.yml`, dán nội dung file đó.
2. Vào tab Actions > "Build APK" > Run workflow (hoặc tự chạy sau khi commit).
3. Chờ 3-6 phút, mở lần chạy xong, tải "HyperWidget-apk" ở mục Artifacts, giải nén ra app-debug.apk.
4. Chép vào điện thoại, cài (cho phép cài từ nguồn không rõ).
