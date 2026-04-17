# UC-01 - Khởi tạo kiến trúc app và điều hướng

## Mục tiêu

Thiết lập nền móng Android cho Fitty theo mô hình Compose + MVVM + repository + use case + unidirectional data flow. Sau use case này, app có khung điều hướng đầy đủ để gắn các màn hình sau: Auth, Onboarding và Main App với 5 tab.

## Phạm vi

- Tạo cấu trúc package chính.
- Thêm Navigation Compose.
- Tạo route graph gốc.
- Tạo bottom navigation cho 5 tab: Home, Plan, Track, Coach, Profile.
- Tạo màn hình placeholder để các use case sau thay thế dần.

## Cài đặt cần chuẩn bị

1. Mở `gradle/libs.versions.toml`.
2. Thêm version và library cho Navigation Compose, Lifecycle ViewModel Compose, Coroutines nếu chưa có.
3. Mở `app/build.gradle.kts`.
4. Thêm dependencies tương ứng.
5. Sync Gradle.
6. Chạy `./gradlew.bat :app:assembleDebug` để kiểm tra project còn build được.

Dependency gợi ý:

```kotlin
implementation(libs.androidx.navigation.compose)
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.kotlinx.coroutines.android)
```

## Package/file gợi ý

- `app/src/main/java/com/example/fitty/navigation/FittyRoutes.kt`
- `app/src/main/java/com/example/fitty/navigation/FittyNavHost.kt`
- `app/src/main/java/com/example/fitty/navigation/MainTab.kt`
- `app/src/main/java/com/example/fitty/navigation/MainScaffold.kt`
- `app/src/main/java/com/example/fitty/core/ui/PlaceholderScreen.kt`
- `app/src/main/java/com/example/fitty/MainActivity.kt`

## Các bước triển khai chi tiết

1. Tạo sealed class hoặc object `FittyRoutes` chứa route:
   - `splash`
   - `welcome`
   - `sign_in`
   - `sign_up`
   - `onboarding_goal`
   - `onboarding_body_metrics`
   - `onboarding_fitness_level`
   - `onboarding_schedule`
   - `onboarding_equipment`
   - `onboarding_nutrition`
   - `onboarding_reminders`
   - `plan_preview`
   - `main`
2. Tạo enum hoặc data class `MainTab` gồm:
   - Home
   - Plan
   - Track
   - Coach
   - Profile
3. Với mỗi tab, khai báo:
   - route
   - label
   - icon tạm thời dùng Material Icons hoặc Text fallback
4. Tạo `FittyNavHost`.
5. Trong `FittyNavHost`, tạo root `NavHost` với start destination là `splash`.
6. Thêm các composable placeholder cho Auth và Onboarding.
7. Tạo `MainScaffold` chứa:
   - `Scaffold`
   - bottom navigation bar
   - nested `NavHost` cho 5 tab chính
8. Cập nhật `MainActivity`:
   - bỏ `Greeting`
   - gọi `FittyTheme`
   - bên trong gọi `FittyNavHost`
9. Tạo quy ước navigation:
   - Auth thành công đi tới onboarding nếu chưa hoàn tất.
   - Onboarding hoàn tất đi tới plan preview.
   - Plan preview hoàn tất đi tới main app.
   - Bottom tab không làm mất trạng thái khi chuyển tab.
10. Thêm helper navigation tránh duplicate destination:
    - `launchSingleTop = true`
    - `restoreState = true`
    - `popUpTo(...){ saveState = true }`

## Luồng kiểm thử thủ công

1. Mở app.
2. App hiển thị Splash placeholder.
3. Bấm nút debug hoặc tạm thời auto-navigate sang Welcome.
4. Từ Welcome đi được sang Sign In, Sign Up.
5. Từ Auth đi được sang các bước Onboarding.
6. Từ Onboarding đi tới Plan Preview.
7. Từ Plan Preview đi tới Main App.
8. Chuyển qua lại 5 tab bottom navigation.
9. Nhấn Back:
   - Nếu đang ở màn hình con, quay lại màn trước.
   - Nếu đang ở tab root, không tạo vòng lặp navigation.

## Tiêu chí hoàn thành

- App build thành công.
- `MainActivity` chỉ còn vai trò host theme và navigation.
- Có đủ route cho toàn bộ flow chính trong tài liệu.
- Có bottom navigation 5 tab.
- Mỗi tab có placeholder rõ ràng để use case sau triển khai tiếp.

