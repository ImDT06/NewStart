# Dự án NewStart - Báo cáo Tiến độ & Kỹ thuật

Tệp này ghi lại các tính năng đã hoàn thiện, công nghệ sử dụng và lộ trình phát triển của ứng dụng **NewStart**.

---

## 🛠 Công nghệ sử dụng (Tech Stack)

### Core
- **Ngôn ngữ:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Kiến trúc:** MVVM + Clean Architecture (Data, Domain, UI layers)
- **Dependency Injection:** Hilt (Dagger)
- **Navigation:** Compose Navigation + Hilt Navigation

### Data & Storage
- **Local Database:** Room Database (Offline-first)
- **Remote Database:** Firebase Firestore
- **Authentication:** Firebase Auth (Email/Password & Google Sign-in)
- **Image Hosting:** Cloudinary (dùng OkHttp để upload)
- **Background Tasks:** WorkManager (Đồng bộ dữ liệu ngầm khi có mạng)

### Features & AI
- **AI Integration:** Google Generative AI (Gemini) - Tự động tạo thói quen từ câu lệnh.
- **Widgets:** Jetpack Glance (Tiện ích màn hình chính cho thói quen).
- **Media:** Coil (Load ảnh), CameraX (Sắp tích hợp).
- **Localization:** Hỗ trợ đa ngôn ngữ (Tiếng Việt & Tiếng Anh).

---

## ✨ Tính năng đã hoàn thiện

### 1. Xác thực người dùng (Auth)
- [x] Đăng nhập / Đăng ký bằng Email.
- [x] Đăng nhập bằng Google.
- [x] Đặt lại mật khẩu (Forgot Password).
- [x] Chuyển đổi ngôn ngữ ngay tại màn hình Auth.
- [x] Giao diện Modern Card, tối giản, hỗ trợ tràn viền.

### 2. Quản lý Thói quen (Habits)
- [x] Thêm, sửa, xóa thói quen.
- [x] Đánh dấu hoàn thành thói quen.
- [x] Theo dõi chuỗi ngày (Streak).
- [x] Đặt giờ nhắc nhở (Reminders).
- [x] **AI Assistant:** Nhập lệnh giọng nói/văn bản để AI tự đề xuất thói quen.
- [x] **Widget:** Xem và tích thói quen từ màn hình chính.

### 3. Nhật ký (Journal)
- [x] Viết nhật ký kèm biểu tượng cảm xúc (Emoji).
- [x] Đính kèm hình ảnh (nén ảnh và upload lên Cloudinary).
- [x] Tìm kiếm và lọc nhật ký theo thời gian (Tuần, Tháng, Năm).
- [x] Xem ảnh phóng to.

### 4. Hệ thống & Cài đặt
- [x] **Offline Mode:** Sử dụng Room để lưu dữ liệu khi không có mạng.
- [x] **Auto Sync:** Dùng WorkManager tự động đẩy dữ liệu lên Firebase khi có mạng lại.
- [x] Cài đặt giao diện (Sáng/Tối/Hệ thống).
- [x] Cập nhật thông tin cá nhân và ảnh đại diện.

---

## 🚀 Lộ trình sắp tới (Roadmap)
- [ ] **Thống kê (Analytics):** Biểu đồ trực quan hóa tiến độ thói quen theo tuần/tháng.
- [ ] **AI Insight:** Gemini phân tích tâm trạng qua nhật ký và đưa ra lời khuyên.
- [ ] **Gamification:** Hệ thống XP, Level và Badge để khích lệ người dùng.
- [ ] **Bảo mật:** Khóa ứng dụng bằng vân tay/khuôn mặt (Biometrics).
- [ ] **Social:** Chia sẻ thành quả thói quen lên mạng xã hội.

---
*Cập nhật lần cuối: 18/05/2026*
