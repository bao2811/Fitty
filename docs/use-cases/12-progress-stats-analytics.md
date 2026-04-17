# UC-12 - Progress, stats và biểu đồ

## Mục tiêu

Tạo khu vực phân tích tiến trình để người dùng thấy xu hướng cân nặng, workout, calories, streak và achievement.

## Phạm vi

- Track -> Progress tab.
- Track -> Stats tab.
- Date range selector.
- Chart cards.
- Metric grid.

## Cài đặt cần chuẩn bị

1. Cần dữ liệu từ workout session, meal log, body assessment, streak.
2. Thêm thư viện chart hoặc tự vẽ bằng Canvas Compose.
3. Nếu muốn giảm dependency, vẽ line/bar chart đơn giản bằng Canvas.

Dependency gợi ý:

```kotlin
implementation(libs.androidx.lifecycle.viewmodel.compose)
```

Có thể chưa cần thư viện chart ngoài ở phiên bản đầu.

## Package/file gợi ý

- `feature_track/progress/ProgressScreen.kt`
- `feature_track/progress/StatsScreen.kt`
- `feature_track/progress/ProgressViewModel.kt`
- `core/designsystem/chart/SimpleLineChart.kt`
- `core/designsystem/chart/SimpleBarChart.kt`
- `domain/model/ProgressSummary.kt`
- `domain/usecase/GetProgressSummaryUseCase.kt`

## Các bước triển khai chi tiết

1. Tạo `DateRange`:
   - 7D
   - 30D
   - 90D
   - 1Y
2. Tạo model `ProgressSummary`:
   - weightTrend
   - weeklyWorkouts
   - caloriesTracked
   - streakTrend
   - totalWorkouts
   - bestStreak
   - mealsLogged
   - hoursTrained
   - achievements
3. Tạo `GetProgressSummaryUseCase`.
4. Use case đọc:
   - `WorkoutSessionRepository`
   - `MealRepository`
   - `BodyAssessmentRepository`
   - `StreakRepository`
   - `AchievementRepository`
5. Progress tab UI:
   - date range selector 7D/30D/90D/1Y
   - chart card `Weight trend`
   - chart card `Weekly workouts`
   - chart card `Calories tracked`
   - chart card `Streak trend`
6. Chart behavior:
   - nếu chưa có dữ liệu, hiện empty state
   - nếu chỉ có 1 điểm, vẫn hiện marker
   - label trục đơn giản, dễ đọc
7. Stats tab UI:
   - grid metric tiles
   - total workouts
   - best streak
   - meals logged
   - calories tracked
   - hours trained
   - achievement gallery
   - share card button placeholder
8. Achievement gallery:
   - locked/unlocked state
   - title
   - short description
9. Khi đổi date range:
   - ViewModel reload summary
   - chỉ loading chart area, không reset cả screen nếu không cần
10. Xử lý empty:
    - chưa có workout: CTA Start Workout
    - chưa có meal: CTA Log Meal
    - chưa có body data: CTA Body Scan

## Luồng kiểm thử thủ công

1. Vào Track -> Progress.
2. Khi chưa có dữ liệu, thấy empty state và CTA.
3. Hoàn tất một workout session.
4. Quay lại Progress, Weekly workouts có dữ liệu.
5. Lưu một meal.
6. Calories tracked cập nhật.
7. Đổi 7D sang 30D, chart reload.
8. Vào Stats, metric tiles đúng số liệu.
9. Achievement unlocked hiển thị khác locked.

## Tiêu chí hoàn thành

- Progress và Stats đọc từ dữ liệu thật/fake repository có cấu trúc.
- Có date range selector.
- Empty state có CTA rõ.
- Metric không bị tràn layout.
- Chart đơn giản nhưng đọc được.

