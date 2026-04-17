# Fitty - Danh sách use case triển khai

Tài liệu này phân rã yêu cầu từ `description.md` và `ui.md` thành các use case nhỏ, có thể triển khai độc lập theo từng vòng lặp. Mỗi use case nằm trong một file riêng và có cùng cấu trúc:

- Mục tiêu
- Phạm vi
- Cài đặt cần chuẩn bị
- Package/file gợi ý
- Các bước triển khai chi tiết
- Luồng kiểm thử thủ công
- Tiêu chí hoàn thành

## Thứ tự triển khai đề xuất

1. [UC-01 - Khởi tạo kiến trúc app và điều hướng](./01-app-foundation-navigation.md)
2. [UC-02 - Design system và component dùng chung](./02-design-system-components.md)
3. [UC-03 - Splash, Welcome và xác định luồng vào app](./03-splash-welcome-entry.md)
4. [UC-04 - Đăng nhập, đăng ký và guest mode](./04-authentication.md)
5. [UC-05 - Onboarding hồ sơ và mục tiêu](./05-onboarding-profile-goals.md)
6. [UC-06 - Xem trước kế hoạch cá nhân hóa](./06-plan-preview-personalization.md)
7. [UC-07 - Home dashboard hằng ngày](./07-home-dashboard.md)
8. [UC-08 - Kế hoạch tập luyện và thư viện bài tập](./08-workout-plan-library.md)
9. [UC-09 - Workout session mode](./09-workout-session-mode.md)
10. [UC-10 - Chụp bữa ăn, phân tích calo và xác nhận](./10-meal-scan-calorie-tracking.md)
11. [UC-11 - Body scan và đánh giá tiến trình cơ thể](./11-body-scan-assessment.md)
12. [UC-12 - Progress, stats và biểu đồ](./12-progress-stats-analytics.md)
13. [UC-13 - Fitty Coach chatbot và action cards](./13-coach-chatbot.md)
14. [UC-14 - Profile, cài đặt và Health Connect](./14-profile-settings-health-connect.md)
15. [UC-15 - Nhắc nhở thông minh bằng WorkManager](./15-reminders-workmanager.md)
16. [UC-16 - Streak, achievements và habit tracking](./16-streak-achievements-habits.md)
17. [UC-17 - Xử lý lỗi, hiệu năng và accessibility](./17-error-performance-accessibility.md)

## Dependency nền tảng nên thêm sớm

Project hiện đã có Compose cơ bản. Khi bắt đầu triển khai các use case, nên chuẩn hóa dependency trong `gradle/libs.versions.toml` trước:

- Navigation Compose cho điều hướng nhiều màn hình.
- Lifecycle ViewModel Compose cho state holder.
- Kotlin Coroutines/Flow cho xử lý bất đồng bộ.
- Room cho dữ liệu local có cấu trúc.
- DataStore cho preferences nhẹ.
- Hilt cho dependency injection.
- WorkManager cho reminder và sync nền.
- CameraX cho chụp ảnh.
- Coil cho tải ảnh/thumbnail.
- Health Connect khi làm tích hợp dữ liệu sức khỏe.
- ML Kit hoặc model riêng khi làm meal/body AI.

Không cần cài tất cả từ đầu. Mỗi use case bên dưới chỉ rõ nhóm cần thêm ở thời điểm triển khai.

