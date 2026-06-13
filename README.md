# Fitty

Fitty là ứng dụng Android hỗ trợ tập luyện, theo dõi sức khỏe và quản lý tiến độ cá nhân. Ứng dụng tập trung vào trải nghiệm mobile hiện đại: onboarding cá nhân hóa, danh mục bài tập theo nhóm cơ, tập nhanh có đồng hồ, tracking bữa ăn bằng ảnh, thống kê tiến độ và Fitty Coach hỗ trợ bởi AI.

## Tính năng chính

- Đăng nhập/đăng ký bằng email, Google Sign-In, chế độ khách và khôi phục mật khẩu.
- Onboarding để thu thập mục tiêu, thể trạng, lịch tập, thiết bị và thói quen dinh dưỡng.
- Sinh kế hoạch tập khởi đầu và lịch buổi tập theo hồ sơ người dùng.
- Danh mục bài tập theo nhóm cơ, chi tiết bài tập, GIF/video hướng dẫn và cơ chế offline-first.
- Tập nhanh với đồng hồ, chỉnh set/rep/weight, thay thế bài tập và lưu lịch sử buổi tập.
- Theo dõi bữa ăn bằng ảnh, AI phân tích kcal/protein/carb/fat và lưu lịch sử quét.
- Theo dõi chỉ số cơ thể, thống kê tiến độ, streak, workout, meal log và daily summary.
- Fitty Coach trả lời dựa trên dữ liệu kế hoạch, buổi tập và dinh dưỡng đã lưu.
- Thông báo cục bộ cho nhắc việc và FCM push notification.
- Hỗ trợ tiếng Việt và tiếng Anh.

## Công nghệ sử dụng

- Kotlin, Android SDK, Gradle Kotlin DSL.
- Jetpack Compose, Material 3, Navigation Compose.
- Hilt, KSP, ViewModel, Lifecycle.
- Firebase Auth, Firestore, Storage, Cloud Messaging, Analytics, App Check.
- Google Sign-In qua Google Play Services Auth.
- Room cho dữ liệu cục bộ và offline-first.
- DataStore Preferences cho session, ngôn ngữ và thiết lập local.
- WorkManager cho đồng bộ nền.
- Retrofit, OkHttp, Gson cho API bên ngoài.
- Coil và Coil GIF cho ảnh/GIF bài tập.
- Media3 ExoPlayer cho video hướng dẫn.
- Kotlin Coroutines và kotlinx-coroutines-play-services.
- JUnit, Robolectric, Espresso, Compose UI Test.
- GitHub Actions cho lint, unit test và build APK.

## Cấu trúc thư mục quan trọng

```text
app/src/main/java/com/example/fitty/
  core/              Thành phần UI/core dùng chung
  data/              Repository, Firebase, Room, API, DataStore
  di/                Hilt module
  domain/            Model, repository interface, use case
  feature_auth/      Đăng nhập, đăng ký, Google Sign-In
  feature_entry/     Splash, Welcome
  feature_home/      Trang chủ
  feature_onboarding/ Onboarding và xem trước kế hoạch
  feature_plan/      Bài tập, nhóm cơ, tập nhanh từ danh mục
  feature_exercise/  Chi tiết bài tập và video
  feature_workout/   Phiên tập, lịch sử tập, chi tiết buổi tập
  feature_track/     Tracking bữa ăn, cơ thể, tiến độ
  feature_coach/     Fitty Coach
  feature_profile/   Hồ sơ và cài đặt
  navigation/        Điều hướng Compose
  notifications/     Local notification và FCM
```

## File cấu hình chính

- `settings.gradle.kts`: khai báo project và module `:app`.
- `build.gradle.kts`: khai báo plugin cấp project.
- `app/build.gradle.kts`: cấu hình Android, Compose, Firebase, Hilt, Room, WorkManager và dependencies.
- `gradle/libs.versions.toml`: quản lý version plugin/thư viện.
- `app/src/main/AndroidManifest.xml`: permission, Activity, service FCM, receiver reminder, FileProvider.
- `app/google-services.json`: cấu hình Firebase cho package `com.UET.mobile`.
- `app/src/main/res/values/strings.xml`: ngôn ngữ mặc định tiếng Anh.
- `app/src/main/res/values-vi/strings.xml`: bản dịch tiếng Việt.
- `.github/workflows/ci.yml`: workflow CI.
- `.env`: biến môi trường dùng khi build local.

## Yêu cầu môi trường

- Android Studio mới, có hỗ trợ Kotlin và Jetpack Compose.
- JDK 17 khuyến nghị cho CI/local build.
- Android SDK Platform 36.
- Android Build Tools 36.0.0.
- Thiết bị/emulator Android API 24 trở lên.
- Firebase project đã bật Auth, Firestore, Storage, Cloud Messaging và App Check nếu cần.

## Cài đặt dự án

1. Clone repository:

```bash
git clone https://github.com/bao2811/Fitty.git
cd Fitty
```

2. Mở project bằng Android Studio hoặc dùng Gradle wrapper.

3. Tạo file `.env` ở thư mục gốc:

```env
GEMINI_API_KEY=your_gemini_api_key
WORKOUTX_BASE_URL=https://your-workout-api-base-url/
```

Nếu chưa dùng API thật, có thể để rỗng hai biến này, nhưng các tính năng AI/API sẽ không hoạt động đầy đủ.

4. Đặt file Firebase:

```text
app/google-services.json
```

File này phải thuộc Firebase Android app có package name:

```text
com.UET.mobile
```

5. Đồng bộ Gradle trong Android Studio hoặc chạy:

```bash
./gradlew :app:compileDebugKotlin
```

Trên Windows PowerShell:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

## Cấu hình Firebase

### Authentication

Bật các provider cần dùng:

- Email/Password.
- Google.

Với Google Sign-In, cần thêm SHA-1 debug/release vào Firebase Console. Có thể lấy SHA-1 bằng:

```bash
./gradlew :app:signingReport
```

Sau khi thêm SHA-1, tải lại `google-services.json` và thay vào `app/google-services.json`.

### Firestore

Ứng dụng sử dụng các collection/subcollection chính:

- `users/{uid}`: hồ sơ người dùng.
- `users/{uid}/plan_instances`: kế hoạch tập.
- `users/{uid}/workout_sessions`: lịch sử buổi tập.
- `users/{uid}/meal_logs`: bữa ăn đã lưu.
- `users/{uid}/meal_scan_history`: lịch sử quét bữa ăn.
- `users/{uid}/body_scans`: lịch sử quét cơ thể.
- `users/{uid}/body_measurements`: chỉ số cơ thể.
- `users/{uid}/daily_summaries`: tổng hợp theo ngày.
- `users/{uid}/coach_threads`: hội thoại Fitty Coach.
- `exercises`: metadata bài tập.
- `app_content`: nội dung động.
- `starter_plan_templates`: mẫu kế hoạch khởi đầu.

### Storage

Ứng dụng upload ảnh vào các đường dẫn:

```text
users/{uid}/meal_scans/
users/{uid}/body_scans/
```

Rule dev tối thiểu nên cho phép user đã đăng nhập đọc/ghi đúng thư mục của mình:

```js
match /users/{uid}/meal_scans/{fileName} {
  allow read, write: if request.auth != null && request.auth.uid == uid;
}

match /users/{uid}/body_scans/{fileName} {
  allow read, write: if request.auth != null && request.auth.uid == uid;
}
```

Nếu bật App Check enforcement cho Storage, cần thêm debug token trong Firebase Console khi chạy debug build.

## Chạy ứng dụng

Build debug APK:

```bash
./gradlew assembleDebug
```

Cài app lên thiết bị đang kết nối:

```bash
./gradlew installDebug
```

Hoặc chạy trực tiếp bằng Android Studio.

## Kiểm thử và kiểm tra chất lượng

Chạy lint:

```bash
./gradlew lintDebug
```

Chạy unit test:

```bash
./gradlew testDebugUnitTest
```

Build kiểm tra nhanh Kotlin:

```bash
./gradlew :app:compileDebugKotlin
```

CI trên GitHub Actions tự chạy:

- `lintDebug`
- `testDebugUnitTest`
- `assembleDebug`
- upload APK debug và report.

## Ghi chú khi phát triển

- Không commit `.env` chứa API key thật.
- Sau khi đổi Firebase SHA-1/OAuth/App Check, tải lại `google-services.json`.
- Nếu ảnh scan không hiển thị, kiểm tra Storage rules, App Check và trường `imageUrl` trong Firestore.
- Nếu dữ liệu bài tập không có media, kiểm tra `exercises`, sync worker và cache trong Room.
- Nếu chuỗi UI lỗi ngôn ngữ, kiểm tra đồng thời `values/strings.xml` và `values-vi/strings.xml`.

