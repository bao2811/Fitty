# UC-03 - Splash, Welcome và xác định luồng vào app

## Mục tiêu

Tạo luồng vào app: Splash kiểm tra trạng thái người dùng, sau đó điều hướng tới Welcome/Auth, Onboarding hoặc Main App.

## Phạm vi

- Splash screen.
- Welcome screen.
- Startup state resolver.
- Lưu trạng thái onboarding bằng DataStore.

## Cài đặt cần chuẩn bị

1. Thêm DataStore Preferences.
2. Thêm ViewModel Compose nếu UC-01 chưa thêm.
3. Tạo repository đọc trạng thái app.
4. Sync Gradle.

Dependency gợi ý:

```kotlin
implementation(libs.androidx.datastore.preferences)
implementation(libs.androidx.lifecycle.viewmodel.compose)
```

## Package/file gợi ý

- `feature_entry/SplashScreen.kt`
- `feature_entry/SplashViewModel.kt`
- `feature_entry/WelcomeScreen.kt`
- `data/preferences/AppPreferencesDataSource.kt`
- `domain/usecase/ResolveStartupDestinationUseCase.kt`

## Các bước triển khai chi tiết

1. Tạo model `StartupDestination`:
   - `Welcome`
   - `Onboarding`
   - `Main`
2. Tạo `AppPreferencesDataSource`.
3. Trong DataStore lưu:
   - `onboarding_completed`
   - `guest_mode_enabled`
   - `user_id` hoặc token tạm nếu có auth.
4. Tạo `ResolveStartupDestinationUseCase`.
5. Logic ban đầu:
   - Nếu chưa có user và chưa guest: đi Welcome.
   - Nếu có user/guest nhưng chưa onboarding: đi Onboarding.
   - Nếu đã onboarding: đi Main.
6. Tạo `SplashUiState`:
   - `isLoading`
   - `destination`
   - `errorMessage`
7. Tạo `SplashViewModel`:
   - gọi use case khi init
   - delay ngắn nếu cần để tránh chuyển màn quá gắt
   - phát `UiEffect.Navigate(destination)`
8. Tạo `SplashScreen`:
   - full screen
   - logo Fitty
   - subtitle `Your AI fitness partner`
   - loading indicator
   - version text
9. Tạo `WelcomeScreen`:
   - hero illustration hoặc placeholder hình thể thao
   - headline `Train smarter, eat better, stay consistent`
   - nút `Create Account`
   - nút `Sign In`
   - text button `Continue as Guest`
10. Gắn navigation:
    - Create Account -> Sign Up
    - Sign In -> Sign In
    - Continue as Guest -> Onboarding
11. Khi bấm guest:
    - lưu `guest_mode_enabled = true`
    - điều hướng tới onboarding goal.

## Luồng kiểm thử thủ công

1. Cài app mới.
2. Mở app, thấy Splash.
3. Sau Splash đi tới Welcome.
4. Bấm Continue as Guest.
5. App đi tới Onboarding.
6. Hoàn tất onboarding giả lập.
7. Tắt mở lại app.
8. App đi thẳng tới Main.
9. Xóa data app.
10. Mở lại, app quay về Welcome.

## Tiêu chí hoàn thành

- Splash không chứa logic điều hướng trực tiếp trong composable.
- Startup resolver nằm trong use case/ViewModel.
- Welcome có đủ 3 hành động chính.
- Trạng thái guest và onboarding được lưu bền bằng DataStore.

