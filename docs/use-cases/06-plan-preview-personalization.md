# UC-06 - Xem trước kế hoạch cá nhân hóa

## Mục tiêu

Sau onboarding, Fitty tạo bản kế hoạch khởi đầu và cho người dùng xem trước trước khi vào Main App. Đây là bước chứng minh dữ liệu onboarding thật sự ảnh hưởng đến app.

## Phạm vi

- Generate starter plan local.
- Plan preview screen.
- Explainable personalization.
- Start plan hoặc quay lại chỉnh preferences.

## Cài đặt cần chuẩn bị

1. Cần profile từ UC-05.
2. Nếu dùng Room, thêm entity cho workout plan.
3. Nếu chưa có Room, có thể lưu plan bằng repository in-memory/DataStore tạm thời.

Dependency gợi ý:

```kotlin
implementation(libs.androidx.room.runtime)
ksp(libs.androidx.room.compiler)
```

## Package/file gợi ý

- `feature_onboarding/PlanPreviewScreen.kt`
- `feature_onboarding/PlanPreviewViewModel.kt`
- `domain/model/WorkoutPlan.kt`
- `domain/model/WorkoutDay.kt`
- `domain/usecase/GenerateStarterPlanUseCase.kt`
- `domain/usecase/CalculateTdeeUseCase.kt`
- `domain/usecase/SaveWorkoutPlanUseCase.kt`

## Các bước triển khai chi tiết

1. Tạo model `WorkoutPlan`:
   - id
   - goal
   - weeklySplit
   - days
   - caloriesTarget
   - waterTarget
   - estimatedPace
   - explanation list
2. Tạo model `WorkoutDay`:
   - dayOfWeek
   - title
   - type
   - durationMinutes
   - difficulty
3. Tạo `CalculateTdeeUseCase`:
   - nhận age, gender, height, weight, activity level
   - trả về calorie maintenance estimate
   - điều chỉnh theo goal: lose/gain/maintain
4. Tạo `GenerateStarterPlanUseCase`:
   - input là `UserFitnessProfile`
   - xác định số ngày tập từ availability
   - chọn split theo goal và level
   - chọn duration theo preference
   - tạo explanation:
     - dựa trên goal
     - dựa trên level
     - dựa trên equipment
     - dựa trên schedule
5. Rule gợi ý:
   - Beginner + lose weight: Full body + cardio + mobility.
   - Gain muscle + gym: Push/Pull/Legs hoặc Upper/Lower.
   - Home no equipment: bodyweight strength + mobility.
   - Injury note có knee: ưu tiên low-impact.
6. Tạo `PlanPreviewUiState`:
   - isLoading
   - plan
   - errorMessage
7. Tạo `PlanPreviewScreen`:
   - header `Your Fitty starter plan`
   - summary cards: Goal, Calories target, Workout days, Weekly split, Estimated pace
   - hero card `Your first week`
   - danh sách ngày tập đầu tiên
   - CTA `Start My Plan`
   - text button `Adjust preferences`
8. Khi bấm `Start My Plan`:
   - lưu workout plan
   - điều hướng Main App
9. Khi bấm `Adjust preferences`:
   - quay lại onboarding hoặc mở màn chỉnh nhanh.
10. Nếu generate fail:
    - hiển thị error card
    - cho phép retry

## Luồng kiểm thử thủ công

1. Hoàn tất onboarding với goal Lose Weight, Beginner, 4 ngày/tuần.
2. Kiểm tra Plan Preview tạo ra plan 4 ngày.
3. Kiểm tra calories target có giá trị.
4. Kiểm tra explanation có nhắc goal, level, schedule.
5. Bấm Adjust preferences, quay lại chỉnh được.
6. Bấm Start My Plan, vào Main App.
7. Mở lại app, plan vẫn còn.

## Tiêu chí hoàn thành

- Plan preview phản ánh dữ liệu onboarding.
- Có calorie target, workout days, weekly split và estimated pace.
- Có giải thích vì sao plan được tạo.
- Có thể lưu plan và đi vào Main App.

