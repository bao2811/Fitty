# UC-10 - Chụp bữa ăn, phân tích calo và xác nhận

## Mục tiêu

Triển khai flow Scan -> Review -> Confirm cho meal tracking. Người dùng chụp ảnh bữa ăn, hệ thống nhận diện món/calo, người dùng chỉnh lại và lưu meal log.

## Phạm vi

- Meals tab trong Track.
- Meal capture screen.
- Meal result screen.
- Manual correction.
- Meal log storage.
- AI uncertainty/confidence display.

## Cài đặt cần chuẩn bị

1. Thêm CameraX cho chụp ảnh.
2. Thêm quyền camera trong `AndroidManifest.xml`.
3. Thêm Coil để hiển thị ảnh preview.
4. Thêm Room để lưu meal log.
5. Thêm ML Kit image labeling hoặc fake analyzer giai đoạn đầu.

Dependency gợi ý:

```kotlin
implementation(libs.androidx.camera.core)
implementation(libs.androidx.camera.camera2)
implementation(libs.androidx.camera.lifecycle)
implementation(libs.androidx.camera.view)
implementation(libs.coil.compose)
implementation(libs.androidx.room.ktx)
implementation(libs.mlkit.image.labeling)
```

## Package/file gợi ý

- `feature_track/meals/MealsScreen.kt`
- `feature_track/meals/MealCaptureScreen.kt`
- `feature_track/meals/MealResultScreen.kt`
- `feature_track/meals/MealViewModel.kt`
- `domain/model/MealLog.kt`
- `domain/model/FoodItem.kt`
- `domain/model/CalorieEstimation.kt`
- `domain/usecase/AnalyzeMealImageUseCase.kt`
- `domain/usecase/EstimateCaloriesUseCase.kt`
- `domain/usecase/SaveMealLogUseCase.kt`

## Các bước triển khai chi tiết

1. Tạo model `FoodItem`:
   - name
   - quantity
   - unit
   - calories
   - protein
   - carbs
   - fat
   - confidence
2. Tạo model `MealLog`:
   - id
   - mealType: breakfast/lunch/dinner/snack
   - imageUri
   - foodItems
   - totalCalories
   - createdAt
   - source: scan/manual
3. Tạo `MealAnalysisState`:
   - Idle
   - Capturing
   - Analyzing
   - Success
   - Error
4. Meals tab:
   - header title `Meals`
   - button `Scan Meal`
   - daily calorie summary card
   - meal timeline: Breakfast, Lunch, Dinner, Snacks
   - mỗi meal card có thumbnail, món, calories, edit icon, confidence badge
5. Camera permission:
   - trước khi request, hiển thị rationale sheet
   - nếu denied, hiển thị button mở settings
6. Meal Capture Screen:
   - camera preview full screen
   - capture button bottom center
   - gallery button left
   - flash button right
   - overlay `Keep the entire meal in frame`
7. Khi capture:
   - lưu ảnh local/cache
   - nén ảnh nếu quá lớn
   - điều hướng Meal Result với image uri
8. Analyze:
   - giai đoạn đầu có thể tạo `FakeMealAnalyzer`
   - sau đó thay bằng ML Kit/custom model
   - output gồm detected labels + confidence
9. Calorie estimate:
   - map label sang nutrition database local
   - ước lượng portion mặc định
   - tạo calorie range nếu confidence thấp
10. Meal Result Screen:
    - image preview
    - detected item chips
    - estimated calories/macros card
    - editable quantity rows
    - buttons Save Meal, Re-analyze, Edit Manually
11. Manual correction:
    - cho sửa tên món
    - quantity
    - calories
    - xóa/thêm food item
12. Khi Save Meal:
    - lưu Room
    - cập nhật Home meal summary
    - quay về Meals tab hoặc Home
13. Xử lý lỗi:
    - Image too dark
    - Food not fully visible
    - Could not estimate calories confidently
    - retry hoặc Edit Manually

## Luồng kiểm thử thủ công

1. Vào Track -> Meals.
2. Bấm Scan Meal.
3. Nếu chưa cấp quyền camera, thấy rationale.
4. Cấp quyền, thấy camera preview.
5. Chụp ảnh hoặc chọn gallery.
6. Màn result hiển thị analyzing rồi success.
7. Sửa quantity một món.
8. Calories total thay đổi.
9. Bấm Save Meal.
10. Quay lại Meals, meal mới xuất hiện trong timeline.
11. Home meal summary cập nhật.
12. Giả lập analyzer lỗi, thấy Retry/Edit Manually.

## Tiêu chí hoàn thành

- Flow Scan -> Review -> Confirm hoạt động.
- User luôn có quyền chỉnh kết quả AI.
- Meal log được lưu bền.
- Confidence được hiển thị rõ.
- Lỗi camera/AI có hướng xử lý.

