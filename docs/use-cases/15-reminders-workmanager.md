# UC-15 - Nhắc nhở thông minh bằng WorkManager

## Mục tiêu

Triển khai hệ thống reminder cho workout, meal, water, sleep và streak warning. Reminder cần bền qua app restart và có logic cá nhân hóa, không chỉ notification cố định.

## Phạm vi

- Reminder preferences.
- WorkManager scheduling.
- Notification channel.
- Smart reminder rules.
- Retry khi schedule fail.

## Cài đặt cần chuẩn bị

1. Thêm WorkManager.
2. Thêm quyền notification cho Android 13+.
3. Tạo notification channel.
4. Cần DataStore để đọc reminder settings.

Dependency gợi ý:

```kotlin
implementation(libs.androidx.work.runtime.ktx)
implementation(libs.androidx.datastore.preferences)
```

Manifest cần chú ý:

- `POST_NOTIFICATIONS` cho Android 13+.
- Không request notification ngay khi mở app; request khi user bật reminder.

## Package/file gợi ý

- `feature_reminder/ReminderSettingsViewModel.kt`
- `data/reminder/ReminderScheduler.kt`
- `data/reminder/WorkoutReminderWorker.kt`
- `data/reminder/MealReminderWorker.kt`
- `data/reminder/WaterReminderWorker.kt`
- `data/reminder/SleepReminderWorker.kt`
- `data/reminder/StreakWarningWorker.kt`
- `domain/usecase/ScheduleReminderUseCase.kt`
- `domain/usecase/CancelReminderUseCase.kt`

## Các bước triển khai chi tiết

1. Tạo `ReminderPreferences`:
   - workoutEnabled
   - workoutTime
   - mealEnabled
   - breakfastTime
   - lunchTime
   - dinnerTime
   - waterEnabled
   - waterFrequencyMinutes
   - sleepEnabled
   - sleepTime
   - quietHours
2. Tạo notification channel:
   - id `fitty_reminders`
   - name `Fitty reminders`
   - importance default
3. Tạo `ReminderScheduler` interface:
   - `scheduleWorkoutReminder(preferences)`
   - `scheduleMealReminders(preferences)`
   - `scheduleWaterReminders(preferences)`
   - `scheduleSleepReminder(preferences)`
   - `scheduleStreakWarning()`
   - `cancelAll()`
4. Tạo implementation bằng WorkManager.
5. Mỗi reminder có unique work name:
   - `workout_reminder`
   - `meal_breakfast_reminder`
   - `meal_lunch_reminder`
   - `meal_dinner_reminder`
   - `water_reminder`
   - `sleep_reminder`
   - `streak_warning`
6. Worker workout:
   - kiểm tra hôm nay có workout không
   - kiểm tra đã complete chưa
   - nếu chưa, gửi notification
7. Worker meal:
   - kiểm tra meal type tương ứng đã log chưa
   - nếu chưa, gửi reminder log meal
8. Worker water:
   - kiểm tra quiet hours
   - gửi nhắc uống nước theo frequency
9. Worker streak warning:
   - gần cuối ngày kiểm tra streak đang có nguy cơ mất
   - nếu chưa workout/log habit cần thiết, gửi alert
10. Khi user đổi settings:
    - lưu DataStore
    - cancel work cũ
    - schedule work mới
11. Xử lý permission:
    - nếu user bật reminder mà chưa có notification permission, hiển thị rationale
    - nếu denied, lưu preference nhưng hiển thị warning
12. Xử lý schedule fail:
    - lưu pending state
    - retry khi app mở lại
    - hiển thị snackbar không chặn app

## Luồng kiểm thử thủ công

1. Vào reminder settings.
2. Bật workout reminder và chọn giờ gần hiện tại.
3. Nếu chưa có permission, app hỏi quyền.
4. Cấp quyền.
5. Tắt app.
6. Đến giờ, notification xuất hiện.
7. Hoàn tất workout trước giờ nhắc.
8. Worker chạy nhưng không gửi notification workout.
9. Tắt meal reminder, work meal bị cancel.
10. Đổi water frequency, lịch mới được áp dụng.

## Tiêu chí hoàn thành

- Reminder dùng WorkManager.
- Có notification channel.
- Có unique work để tránh schedule trùng.
- Reminder kiểm tra trạng thái trước khi gửi.
- Permission notification xử lý đúng thời điểm.

