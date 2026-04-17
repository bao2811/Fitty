# UC-02 - Design system và component dùng chung

## Mục tiêu

Tạo bộ giao diện dùng chung cho Fitty để các màn hình có cùng màu sắc, spacing, button, card, chip và trạng thái loading/error/empty. Use case này giúp tránh mỗi màn hình tự style một kiểu.

## Phạm vi

- Chuẩn hóa theme Material 3.
- Tạo màu thương hiệu Fitty.
- Tạo component dùng chung.
- Tạo preview cho component chính.

## Cài đặt cần chuẩn bị

1. Project đã có Compose Material 3.
2. Nếu cần icon, thêm Material Icons Extended.
3. Nếu cần preview tốt hơn, giữ `ui-tooling-preview` và `ui-tooling`.
4. Sync Gradle.

Dependency gợi ý:

```kotlin
implementation(libs.androidx.compose.material.icons.extended)
```

## Package/file gợi ý

- `core/designsystem/theme/FittyColors.kt`
- `core/designsystem/theme/FittyTheme.kt`
- `core/designsystem/component/FittyTopBar.kt`
- `core/designsystem/component/FittyButton.kt`
- `core/designsystem/component/FittyCard.kt`
- `core/designsystem/component/FittyChip.kt`
- `core/designsystem/component/MetricTile.kt`
- `core/designsystem/component/ProgressRing.kt`
- `core/designsystem/component/StateViews.kt`

## Các bước triển khai chi tiết

1. Xác định color roles:
   - Primary: xanh năng lượng cho hành động chính.
   - Secondary: teal cho sức khỏe và tin cậy.
   - Accent: cam cho streak và achievement.
   - Error: đỏ mềm cho lỗi.
   - Background: sáng và dark mode.
2. Cập nhật theme:
   - light color scheme
   - dark color scheme
   - typography
   - shapes
3. Tạo spacing object:
   - `space4`
   - `space8`
   - `space12`
   - `space16`
   - `space24`
   - `space32`
4. Tạo `PrimaryButton`:
   - nhận text
   - enabled/loading
   - onClick
   - fullWidth option
5. Tạo `SecondaryButton`:
   - dùng cho action phụ
   - visual nhẹ hơn primary
6. Tạo `GoalCard`:
   - title
   - description
   - selected state
   - onClick
7. Tạo `WorkoutCard`:
   - title
   - duration
   - difficulty
   - target area
   - CTA
8. Tạo `MealCard`:
   - thumbnail optional
   - meal name
   - calories
   - confidence
   - edit action
9. Tạo `MetricTile`:
   - label
   - value
   - unit
   - trend optional
10. Tạo `ProgressRing`:
    - progress từ `0f` đến `1f`
    - label ở giữa
    - hỗ trợ animation nhẹ
11. Tạo `StateViews`:
    - `LoadingStateView`
    - `EmptyStateView`
    - `ErrorStateCard`
12. Tạo preview cho từng component:
    - normal
    - selected
    - disabled
    - loading
    - dark theme

## Luồng kiểm thử thủ công

1. Build app.
2. Mở Android Studio Preview.
3. Kiểm tra các component ở light mode và dark mode.
4. Kiểm tra text dài không tràn card.
5. Kiểm tra button loading không làm layout nhảy.
6. Kiểm tra card selected/unselected nhìn rõ.

## Tiêu chí hoàn thành

- Có theme Fitty thống nhất.
- Có các component cốt lõi dùng lại được.
- Các component không chứa logic nghiệp vụ.
- Preview hiển thị được.
- Các use case sau có thể dùng lại component thay vì tự tạo UI riêng.

