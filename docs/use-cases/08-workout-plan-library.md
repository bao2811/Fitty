# UC-08 - Kế hoạch tập luyện và thư viện bài tập

## Mục tiêu

Triển khai tab Plan để người dùng xem workout hôm nay, kế hoạch trong tuần, thư viện bài tập và chi tiết workout.

## Phạm vi

- Plan tab với 3 tab con: Today, This Week, Library.
- Workout detail screen.
- Replace workout placeholder.
- Explainable recommendation card.

## Cài đặt cần chuẩn bị

1. Cần Navigation nested trong tab Plan.
2. Cần Room cho exercise/workout nếu muốn offline tốt.
3. Cần Coil nếu dùng thumbnail ảnh/GIF.

Dependency gợi ý:

```kotlin
implementation(libs.androidx.room.ktx)
implementation(libs.coil.compose)
```

## Package/file gợi ý

- `feature_plan/PlanScreen.kt`
- `feature_plan/PlanViewModel.kt`
- `feature_plan/WorkoutDetailScreen.kt`
- `feature_plan/WorkoutLibraryScreen.kt`
- `domain/model/Workout.kt`
- `domain/model/Exercise.kt`
- `domain/usecase/GetCurrentPlanUseCase.kt`
- `domain/usecase/GetWorkoutDetailUseCase.kt`
- `domain/usecase/ReplaceWorkoutUseCase.kt`

## Các bước triển khai chi tiết

1. Tạo model `Exercise`:
   - id
   - name
   - targetMuscle
   - sets
   - reps
   - durationSeconds optional
   - instruction
   - saferVariation
   - thumbnailUrl optional
2. Tạo model `Workout`:
   - id
   - title
   - durationMinutes
   - estimatedCalories
   - difficulty
   - equipment
   - exercises
   - explanation
3. Tạo seed data local:
   - Full body beginner
   - Cardio + core
   - Strength
   - Mobility
4. Tạo `PlanUiState`:
   - selectedTab
   - dateStrip
   - todayWorkout
   - weeklyWorkouts
   - libraryFilters
   - libraryItems
   - loading/error
5. Plan Screen top:
   - top app bar title `My Plan`
   - tab row: Today, This Week, Library
6. Today tab:
   - date strip
   - today workout card
   - `Why this workout?` explanation card
   - buttons Start Workout, Replace, View Details
7. This Week tab:
   - vertical weekly cards
   - mỗi card có day, title, duration, status
8. Library tab:
   - filter chips: strength, fat loss, mobility, cardio
   - danh sách workout/exercise
9. Workout Detail Screen:
   - header image/banner
   - title
   - duration
   - calories
   - difficulty
   - exercise list cards
   - bottom sticky CTA `Start Session`
10. Replace action:
    - mở bottom sheet
    - hiển thị danh sách workout thay thế cùng duration/equipment
    - chọn một workout
    - cập nhật plan local
11. Explainable card:
    - hiển thị lý do: goal, level, equipment, schedule
12. Tối ưu list:
    - dùng `LazyColumn`
    - dùng stable key theo workout id/exercise id

## Luồng kiểm thử thủ công

1. Mở tab Plan.
2. Today tab hiển thị workout hôm nay.
3. Bấm View Details, thấy danh sách bài tập.
4. Bấm Start Session, đi tới session mode.
5. Bấm Replace, chọn bài thay thế.
6. Quay lại Today, workout đã đổi.
7. Mở This Week, thấy toàn bộ tuần.
8. Mở Library, filter theo cardio/strength hoạt động.

## Tiêu chí hoàn thành

- Plan có đủ Today, This Week, Library.
- Workout detail đủ thông tin bài tập.
- Có explanation vì sao workout được gợi ý.
- Có đường dẫn sang session mode.
- Có thể replace workout ở mức cơ bản.

