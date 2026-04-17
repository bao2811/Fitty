# UC-07 - Home dashboard hằng ngày

## Mục tiêu

Tạo màn Home là dashboard hằng ngày: người dùng biết hôm nay cần làm gì, tiến độ ra sao và có quick actions để thao tác nhanh.

## Phạm vi

- Greeting theo thời gian.
- Today's target.
- Progress rings.
- Today's workout.
- Meal summary.
- AI insight placeholder.
- Streak card.
- Quick actions bottom sheet.

## Cài đặt cần chuẩn bị

1. Cần Main bottom navigation từ UC-01.
2. Cần component từ UC-02.
3. Cần plan từ UC-06 để hiển thị workout hôm nay.
4. Có thể thêm Room để đọc meal/workout/streak local.

Dependency gợi ý:

```kotlin
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.room.ktx)
```

## Package/file gợi ý

- `feature_home/HomeScreen.kt`
- `feature_home/HomeViewModel.kt`
- `feature_home/HomeUiState.kt`
- `domain/usecase/GetTodayDashboardUseCase.kt`
- `domain/model/DailyDashboard.kt`

## Các bước triển khai chi tiết

1. Tạo `DailyDashboard`:
   - userName
   - greeting
   - todayTargetText
   - readinessText
   - workoutProgress
   - calorieProgress
   - waterProgress
   - stepProgress
   - todayWorkout
   - mealSummary
   - insightText
   - currentStreak
2. Tạo `GetTodayDashboardUseCase`.
3. Use case lấy dữ liệu từ:
   - profile repository
   - workout plan repository
   - meal repository
   - streak repository
4. Nếu repository chưa có dữ liệu thật, trả về fake data có cấu trúc để UI triển khai trước.
5. Tạo `HomeUiState`:
   - loading sections riêng: workout, meals, insight
   - dashboard data
   - error từng section
   - quickActionsVisible
6. Tạo `HomeScreen` bằng `Scaffold`.
7. Top app bar:
   - greeting `Good morning, Alex`
   - profile avatar
   - notification icon
8. Hero card:
   - Today's target
   - readiness summary
   - nút `Start Today`
9. Progress ring row:
   - workouts
   - calories
   - water
   - steps
10. Section `Today's workout`:
    - dùng `WorkoutCard`
    - action Start Workout
11. Section `Meals today`:
    - calories consumed/target
    - bữa đã log
    - action Log Meal
12. Section `AI insight`:
    - text gợi ý ngắn
    - action Ask Coach
13. Section `Streak`:
    - số ngày streak
    - trạng thái hôm nay đã hoàn thành hay chưa
14. Floating Action Button:
    - mở quick actions bottom sheet
15. Quick actions:
    - Log Meal -> Meal Capture
    - Start Workout -> Session Mode hoặc Plan
    - Ask Coach -> Coach
    - Body Scan -> Body Scan
16. Xử lý partial loading:
    - nếu insight lỗi, chỉ card insight lỗi
    - không khóa toàn bộ Home

## Luồng kiểm thử thủ công

1. Vào Main App tab Home.
2. Thấy greeting, hero card và progress rings.
3. Bấm Start Today, đi tới workout/session phù hợp.
4. Bấm FAB, bottom sheet mở.
5. Bấm Log Meal, đi tới Meal Capture.
6. Bấm Ask Coach, đi tới Coach.
7. Giả lập lỗi insight, chỉ insight hiện error card.
8. Chuyển tab rồi quay lại, state Home không reset vô lý.

## Tiêu chí hoàn thành

- Home là dashboard có hành động rõ.
- Có quick actions bottom sheet.
- Dữ liệu được load qua ViewModel/use case.
- Mỗi section có loading/error riêng.
- UI dùng component dùng chung.

