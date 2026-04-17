# UC-05 - Onboarding hồ sơ và mục tiêu

## Mục tiêu

Thu thập dữ liệu cá nhân hóa ban đầu để Fitty tạo plan: mục tiêu, chỉ số cơ thể, level tập luyện, lịch rảnh, thiết bị, dinh dưỡng và nhắc nhở.

## Phạm vi

- 7 màn onboarding.
- Lưu nháp onboarding.
- Validate dữ liệu theo từng bước.
- Hoàn tất onboarding và lưu profile.

## Cài đặt cần chuẩn bị

1. Cần Navigation Compose từ UC-01.
2. Cần DataStore để lưu trạng thái nhẹ.
3. Nên thêm Room nếu muốn lưu profile có cấu trúc ngay từ đầu.
4. Nếu chưa dùng Room, có thể lưu profile nháp bằng DataStore hoặc repository in-memory trước.

Dependency gợi ý:

```kotlin
implementation(libs.androidx.datastore.preferences)
implementation(libs.androidx.room.runtime)
ksp(libs.androidx.room.compiler)
```

Nếu thêm Room, cần thêm plugin KSP trong Gradle.

## Package/file gợi ý

- `feature_onboarding/OnboardingViewModel.kt`
- `feature_onboarding/GoalSelectionScreen.kt`
- `feature_onboarding/BodyMetricsScreen.kt`
- `feature_onboarding/FitnessLevelScreen.kt`
- `feature_onboarding/WeeklyAvailabilityScreen.kt`
- `feature_onboarding/EquipmentLocationScreen.kt`
- `feature_onboarding/NutritionPreferenceScreen.kt`
- `feature_onboarding/ReminderPreferencesScreen.kt`
- `domain/model/UserFitnessProfile.kt`
- `domain/usecase/SaveOnboardingProfileUseCase.kt`

## Các bước triển khai chi tiết

1. Tạo `UserFitnessProfile`:
   - userId
   - goal
   - age
   - gender
   - heightCm
   - weightKg
   - targetWeightKg
   - bodyFatPercent optional
   - fitnessLevel
   - workoutDays
   - workoutDurationMinutes
   - preferredTime
   - equipmentAccess
   - injuryNote
   - nutritionStyle
   - dietaryRestrictions
   - reminderPreferences
2. Tạo enum cho các lựa chọn:
   - `FitnessGoal`
   - `FitnessLevel`
   - `WorkoutLocation`
   - `NutritionStyle`
   - `PreferredWorkoutTime`
3. Tạo `OnboardingUiState` chứa toàn bộ form.
4. Tạo `OnboardingEvent` cho từng thay đổi field.
5. Màn Goal Selection:
   - step indicator `Step 1 of 7`
   - grid card: Lose Weight, Gain Muscle, Maintain Fitness, Improve Endurance, Improve Flexibility, Build Healthy Habits
   - Continue chỉ bật khi đã chọn goal
6. Màn Body Metrics:
   - age
   - height
   - weight
   - target weight
   - body fat optional
   - validate số và khoảng hợp lý
7. Màn Fitness Level:
   - Beginner
   - Intermediate
   - Advanced
   - mỗi card có mô tả số buổi/tuần
8. Màn Weekly Availability:
   - chips thứ trong tuần
   - chips duration 20/30/45/60
   - chips Morning/Afternoon/Evening
   - cho phép chọn nhiều ngày
9. Màn Equipment and Location:
   - Home no equipment
   - Home basic equipment
   - Gym
   - Mix
   - optional injury note
   - optional mobility limitation
10. Màn Nutrition Preference:
    - Standard
    - High Protein
    - Vegetarian
    - Vegan
    - Low Carb
    - Flexible
    - toggles lactose-free, nut allergy, avoid seafood
11. Màn Reminder Preferences:
    - workout reminder toggle + time
    - meal reminder toggle + breakfast/lunch/dinner
    - water reminder toggle + frequency
    - sleep reminder toggle + time
12. Khi bấm Continue mỗi bước:
    - validate bước hiện tại
    - nếu lỗi, hiển thị dưới field/card
    - nếu hợp lệ, chuyển bước tiếp theo
13. Khi hoàn tất:
    - gọi `SaveOnboardingProfileUseCase`
    - lưu profile
    - lưu `onboarding_completed = true`
    - điều hướng tới Plan Preview

## Luồng kiểm thử thủ công

1. Đi từ Welcome hoặc Auth vào onboarding.
2. Ở bước Goal, chưa chọn gì thì Continue disabled.
3. Chọn một goal, Continue enabled.
4. Nhập Body Metrics sai, thấy lỗi.
5. Nhập hợp lệ, sang bước tiếp.
6. Chọn nhiều ngày tập.
7. Chọn thiết bị và nhập injury note.
8. Chọn nutrition style và restrictions.
9. Bật reminder và chọn giờ.
10. Hoàn tất onboarding, app chuyển tới Plan Preview.
11. Tắt mở lại app, không quay lại onboarding.

## Tiêu chí hoàn thành

- Onboarding có đủ 7 bước.
- State được giữ trong ViewModel.
- Mỗi bước validate độc lập.
- Dữ liệu đủ để tạo plan cá nhân hóa.
- Hoàn tất onboarding được lưu bền.

