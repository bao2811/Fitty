# UC-16 - Streak, achievements và habit tracking

## Mục tiêu

Tạo vòng lặp động lực cho người dùng bằng streak, achievement và habit tracking. Tính năng này kết nối với workout session, meal log, water tracker và daily check-in.

## Phạm vi

- Streak record.
- Achievement unlock.
- Habit tracker hằng ngày.
- Daily check-in.
- Share card placeholder.

## Cài đặt cần chuẩn bị

1. Cần Room để lưu daily records.
2. Cần Home dashboard để hiển thị streak.
3. Cần Progress/Stats để hiển thị achievements.

Dependency gợi ý:

```kotlin
implementation(libs.androidx.room.ktx)
```

## Package/file gợi ý

- `domain/model/StreakRecord.kt`
- `domain/model/Achievement.kt`
- `domain/model/DailyHabit.kt`
- `domain/model/DailyCheckIn.kt`
- `domain/usecase/UpdateStreakUseCase.kt`
- `domain/usecase/EvaluateAchievementsUseCase.kt`
- `feature_home/HabitTrackerCard.kt`
- `feature_home/DailyCheckInSheet.kt`

## Các bước triển khai chi tiết

1. Tạo `DailyHabit`:
   - id
   - date
   - type: workout, water, meal, sleep, steps
   - target
   - currentValue
   - completed
2. Tạo `StreakRecord`:
   - currentStreak
   - bestStreak
   - lastCompletedDate
   - streakType
3. Tạo `Achievement`:
   - id
   - title
   - description
   - unlockedAt optional
   - progress
   - target
4. Tạo `DailyCheckIn`:
   - date
   - energyLevel
   - sleepQuality
   - soreness
   - motivation
   - note optional
5. Tạo `UpdateStreakUseCase`:
   - gọi khi complete workout/session
   - gọi khi complete habit quan trọng
   - nếu ngày kế tiếp hợp lệ, tăng streak
   - nếu bỏ quá ngày, reset hoặc giữ theo rule app
6. Tạo `EvaluateAchievementsUseCase`:
   - First Workout
   - 3-Day Streak
   - 7-Day Streak
   - 10 Meals Logged
   - 5 Body Scans
   - 10 Hours Trained
7. Tạo Habit Tracker Card trên Home:
   - slept before 11pm
   - drank enough water
   - no sugary drinks
   - 8k steps
   - trained today
8. Habit interaction:
   - tap để toggle manual habit
   - các habit tự động như workout/meal cập nhật từ log
9. Daily Check-In Sheet:
   - energy today
   - sleep quality
   - soreness
   - motivation
   - Save
10. Dùng check-in để ảnh hưởng plan:
    - energy thấp -> Coach/Plan gợi ý workout nhẹ hơn
    - soreness cao -> recovery advice
11. Achievement unlock:
    - sau mỗi event quan trọng, chạy evaluate
    - nếu có achievement mới, hiển thị snackbar/dialog nhẹ
12. Share card placeholder:
    - tạo màn/card achievement
    - nút share có thể để TODO nếu chưa tích hợp Android Sharesheet

## Luồng kiểm thử thủ công

1. Vào Home, thấy streak card và habit tracker.
2. Toggle một habit manual.
3. Hoàn tất workout session.
4. Streak tăng.
5. Achievement First Workout unlock.
6. Log đủ số meal fake/test.
7. Achievement Meals Logged unlock.
8. Mở Daily Check-In, nhập energy/soreness.
9. Coach hoặc Plan nhận được context check-in.

## Tiêu chí hoàn thành

- Streak cập nhật từ hành động thật.
- Achievement có progress và unlock state.
- Habit tracker có cả manual và automatic habit.
- Daily check-in lưu được.
- Home/Stats hiển thị dữ liệu động lực.

