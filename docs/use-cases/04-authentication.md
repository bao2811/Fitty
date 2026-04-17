# UC-04 - Đăng nhập, đăng ký và guest mode

## Mục tiêu

Triển khai flow xác thực cơ bản cho Fitty: đăng nhập, tạo tài khoản, quên mật khẩu và chế độ khách. Auth cần cung cấp dữ liệu hồ sơ ban đầu cho các tính năng calo, plan và reminder.

## Phạm vi

- Sign In screen.
- Sign Up screen.
- Forgot Password placeholder.
- Guest mode.
- Validate form.
- Repository auth có thể dùng fake local trước, sau đó thay bằng Firebase/API.

## Cài đặt cần chuẩn bị

1. Thêm ViewModel Compose nếu chưa có.
2. Thêm Coroutines.
3. Nếu dùng Firebase Auth, thêm Firebase sau. Giai đoạn đầu có thể dùng fake repository.
4. Sync Gradle.

Dependency gợi ý cho giai đoạn local:

```kotlin
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.kotlinx.coroutines.android)
```

## Package/file gợi ý

- `feature_auth/SignInScreen.kt`
- `feature_auth/SignInViewModel.kt`
- `feature_auth/SignUpScreen.kt`
- `feature_auth/SignUpViewModel.kt`
- `feature_auth/ForgotPasswordScreen.kt`
- `domain/repository/AuthRepository.kt`
- `data/repository/FakeAuthRepository.kt`
- `domain/usecase/SignInUseCase.kt`
- `domain/usecase/SignUpUseCase.kt`

## Các bước triển khai chi tiết

1. Tạo `AuthRepository` interface:
   - `signIn(email, password)`
   - `signUp(profile, password)`
   - `continueAsGuest()`
   - `signOut()`
2. Tạo `FakeAuthRepository`:
   - validate email/password ở local
   - trả về user fake khi thành công
   - delay ngắn để mô phỏng loading
3. Tạo `SignInUiState`:
   - `email`
   - `password`
   - `emailError`
   - `passwordError`
   - `isSubmitting`
   - `errorMessage`
4. Tạo `SignInEvent`:
   - `EmailChanged`
   - `PasswordChanged`
   - `SubmitClicked`
   - `ForgotPasswordClicked`
   - `CreateAccountClicked`
5. Tạo UI Sign In:
   - top app bar có back arrow
   - email text field
   - password text field có ẩn/hiện password
   - Forgot password
   - Sign In button
   - divider `or`
   - Continue with Google placeholder
   - footer chuyển sang Create Account
6. Validate Sign In:
   - email không rỗng
   - email có ký tự `@`
   - password tối thiểu 6 ký tự
   - disable button khi đang submit
7. Tạo `SignUpUiState`:
   - full name
   - email
   - password
   - confirm password
   - age hoặc DOB
   - gender
   - height
   - weight
   - accept terms
   - field errors
8. Tạo UI Sign Up bằng `LazyColumn`.
9. Validate Sign Up:
   - tên không rỗng
   - email hợp lệ
   - password đủ dài
   - confirm password trùng
   - tuổi trong khoảng hợp lý
   - chiều cao/cân nặng là số dương
   - đã đồng ý điều khoản
10. Sau auth thành công:
    - lưu trạng thái user/guest.
    - nếu chưa onboarding, đi tới onboarding.
    - nếu đã onboarding, đi main.

## Luồng kiểm thử thủ công

1. Mở Welcome.
2. Bấm Sign In.
3. Submit form rỗng, thấy lỗi từng field.
4. Nhập email sai format, thấy lỗi email.
5. Nhập password ngắn, thấy lỗi password.
6. Nhập hợp lệ, bấm Sign In, thấy loading và điều hướng.
7. Quay lại Welcome, bấm Create Account.
8. Submit form rỗng, thấy lỗi từng field.
9. Nhập confirm password khác password, thấy lỗi.
10. Nhập hợp lệ, app đi onboarding.
11. Bấm Continue as Guest, app vẫn đi onboarding nhưng profile đánh dấu guest.

## Tiêu chí hoàn thành

- Auth screen không giữ business logic trong composable.
- Form có inline validation.
- Có loading state và error state.
- Guest mode hoạt động.
- Repository có thể thay implementation sau mà UI không đổi.

