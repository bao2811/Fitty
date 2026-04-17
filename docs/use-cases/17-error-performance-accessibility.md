# UC-17 - Xử lý lỗi, hiệu năng và accessibility

## Mục tiêu

Chuẩn hóa cách Fitty xử lý lỗi, loading, empty state, hiệu năng Compose và accessibility trên toàn app.

## Phạm vi

- Result-state pattern.
- Error mapping.
- Retry action.
- Partial loading.
- Compose performance rules.
- Accessibility checklist.

## Cài đặt cần chuẩn bị

Use case này chủ yếu là quy ước code. Không bắt buộc thêm dependency mới. Có thể thêm test dependency nếu muốn kiểm tra ViewModel/use case.

Dependency gợi ý:

```kotlin
testImplementation(libs.junit)
androidTestImplementation(libs.androidx.compose.ui.test.junit4)
```

## Package/file gợi ý

- `core/model/AppResult.kt`
- `core/model/AppError.kt`
- `core/ui/UiState.kt`
- `core/ui/ErrorStateCard.kt`
- `core/ui/EmptyStateView.kt`
- `core/ui/LoadingStateView.kt`
- `core/performance/StableListKeys.md`
- `core/accessibility/AccessibilityChecklist.md`

## Các bước triển khai chi tiết

1. Tạo sealed class `AppResult<T>`:
   - `Success<T>`
   - `Error`
   - `Loading` nếu cần
2. Tạo sealed class `AppError`:
   - Network
   - PermissionDenied
   - ImageTooDark
   - ImageNotFullyVisible
   - AiLowConfidence
   - Validation
   - EmptyData
   - Unknown
3. Tạo mapper:
   - exception -> AppError
   - AppError -> user message
4. Quy ước UI state:
   - screen lớn có thể loading ban đầu
   - section trong dashboard phải có loading/error riêng
   - form có field error riêng
5. Tạo error messages:
   - Meal scan: `Image too dark`, `Food not fully visible`, `Could not estimate calories confidently`
   - Body scan: `Body not fully inside frame`, `Lighting too poor for accurate analysis`
   - Chatbot: `Network timeout`, `Retry`
   - Reminder: `Could not schedule reminder`, `Retry later`
   - Permission: rationale trước khi request
6. Retry pattern:
   - mỗi error card có retry nếu hành động có thể thử lại
   - retry gọi event ViewModel, không gọi use case trực tiếp từ composable
7. Empty state pattern:
   - no workouts -> CTA Start Workout
   - no meals -> CTA Log Meal
   - no body history -> CTA Start Body Scan
   - no chat -> suggested prompts
8. Performance rules:
   - dùng `LazyColumn`/`LazyRow` cho list dài
   - dùng stable key trong list
   - tránh xử lý bitmap trên main thread
   - nén ảnh trước upload/analyze
   - immutable UiState
   - tách section composables
   - không reload toàn dashboard khi chỉ một metric đổi
   - cache result meal/body gần nhất
9. Compose state rules:
   - screen state trong ViewModel
   - local UI state nhỏ có thể dùng `rememberSaveable`
   - reusable component nhận `value` và `onValueChange`
10. Accessibility rules:
    - icon button có content description
    - tap target đủ lớn
    - text đủ contrast
    - chart có caption
    - không dựa vào màu duy nhất để biểu thị trạng thái
    - hỗ trợ dark mode
    - form error đọc được bằng text
11. Permission rules:
    - camera: giải thích trước request
    - notification: request khi user bật reminder
    - Health Connect: request khi user bấm connect
12. Test strategy:
    - unit test validation use cases
    - unit test plan generation
    - unit test streak update
    - UI test form validation quan trọng
    - manual test camera/permission trên thiết bị thật

## Luồng kiểm thử thủ công

1. Tắt mạng, gửi Coach message, thấy lỗi retry.
2. Từ chối camera permission, thấy rationale/open settings.
3. Giả lập meal image tối, thấy lỗi phù hợp.
4. Mở app chưa có data, thấy empty state có CTA.
5. Bật TalkBack, icon quan trọng đọc được.
6. Đổi dark mode, UI vẫn đọc rõ.
7. Scroll danh sách dài, không giật rõ rệt.
8. Chuyển tab nhiều lần, state không reset vô lý.

## Tiêu chí hoàn thành

- App có quy ước lỗi thống nhất.
- Không có màn hình AI nào giả định kết quả luôn đúng.
- Loading/error/empty state được xử lý rõ.
- Compose list dùng lazy và stable keys.
- Các màn chính đạt checklist accessibility cơ bản.

