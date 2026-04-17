# UC-14 - Profile, cài đặt và Health Connect

## Mục tiêu

Triển khai Profile như trung tâm tài khoản và cài đặt: mục tiêu, body metrics, reminders, nutrition, achievements, privacy/AI, theme/units và kết nối Health Connect.

## Phạm vi

- Profile tab.
- Edit profile.
- Manage goals.
- Reminder settings link.
- Nutrition preferences.
- Theme/units.
- Health Connect permission entry.
- Logout.

## Cài đặt cần chuẩn bị

1. Cần DataStore cho settings.
2. Cần Room/profile repository.
3. Khi làm Health Connect thật, thêm Health Connect client.
4. Health Connect chỉ xin quyền khi user bấm Connect Health Data.

Dependency gợi ý:

```kotlin
implementation(libs.androidx.datastore.preferences)
implementation(libs.androidx.health.connect.client)
```

## Package/file gợi ý

- `feature_profile/ProfileScreen.kt`
- `feature_profile/ProfileViewModel.kt`
- `feature_profile/EditProfileScreen.kt`
- `feature_profile/NotificationSettingsScreen.kt`
- `feature_profile/PrivacyAiSettingsScreen.kt`
- `domain/usecase/GetProfileSettingsUseCase.kt`
- `domain/usecase/UpdateProfileUseCase.kt`
- `domain/usecase/ConnectHealthDataUseCase.kt`

## Các bước triển khai chi tiết

1. Tạo `ProfileSettings`:
   - name
   - avatarUri
   - goal
   - bodyMetrics
   - reminderSummary
   - nutritionPreferences
   - achievementsCount
   - healthConnected
   - themeMode
   - units
   - aiConsent
2. Profile header:
   - avatar
   - name
   - level badge
   - edit profile button
3. Section cards:
   - Current Goal
   - Body Metrics
   - Reminder Settings
   - Nutrition Preferences
   - Achievements
   - Linked Health Apps
   - Privacy & AI
   - Theme / Units
4. Footer:
   - Log Out button
5. Edit Profile:
   - name
   - age
   - gender
   - height
   - weight
   - injury notes
   - save button
6. Manage Goals:
   - đổi primary goal
   - nếu đổi goal, hỏi có generate lại plan không
7. Notification Settings:
   - workout reminder
   - meal reminder
   - water reminder
   - sleep reminder
   - lưu DataStore
   - gửi event cho reminder scheduler
8. Nutrition Preferences:
   - eating style
   - allergies/restrictions
   - lưu profile
9. Privacy & AI:
   - AI consent toggle
   - body scan consent reset
   - clear chat history
10. Theme / Units:
    - light/dark/system
    - kg/lb
    - cm/ft
11. Health Connect:
    - card `Connect Health Data`
    - khi bấm mới mở permission flow
    - nếu chưa cài/không hỗ trợ, hiện hướng dẫn
    - sau khi cấp quyền, hiển thị connected state
12. Logout:
    - xác nhận bằng dialog
    - clear auth token/guest
    - không xóa local logs trừ khi user chọn clear data
    - điều hướng Welcome

## Luồng kiểm thử thủ công

1. Mở Profile tab.
2. Thấy header và các section.
3. Bấm Edit Profile, chỉnh weight, save.
4. Quay lại Profile, Body Metrics cập nhật.
5. Đổi theme sang dark/system, app áp dụng.
6. Bấm Connect Health Data, permission chỉ xuất hiện sau click.
7. Từ chối quyền, app hiển thị trạng thái chưa kết nối.
8. Bấm Log Out, dialog xác nhận.
9. Confirm, app quay về Welcome.

## Tiêu chí hoàn thành

- Profile là settings hub rõ ràng.
- Settings lưu bền bằng DataStore/repository.
- Health Connect không tự xin quyền khi chưa có hành động user.
- Logout hoạt động.
- Các thay đổi profile ảnh hưởng use case liên quan.

