# UC-11 - Body scan và đánh giá tiến trình cơ thể

## Mục tiêu

Triển khai body scan an toàn và có trách nhiệm: người dùng chụp front/side/back, app kiểm tra chất lượng ảnh, tạo nhận xét trung tính về posture/progress và lưu assessment để so sánh theo thời gian.

## Phạm vi

- Body tab trong Track.
- Body scan capture.
- Body analysis result.
- Consent/privacy notice.
- Lưu lịch sử assessment.

## Cài đặt cần chuẩn bị

1. Cần CameraX.
2. Cần Room để lưu assessment metadata.
3. Cần Coil để hiển thị ảnh.
4. Có thể thêm ML Kit Pose Detection ở giai đoạn AI.

Dependency gợi ý:

```kotlin
implementation(libs.androidx.camera.core)
implementation(libs.androidx.camera.camera2)
implementation(libs.androidx.camera.lifecycle)
implementation(libs.androidx.camera.view)
implementation(libs.coil.compose)
implementation(libs.mlkit.pose.detection)
```

## Package/file gợi ý

- `feature_track/body/BodyProgressScreen.kt`
- `feature_track/body/BodyScanCaptureScreen.kt`
- `feature_track/body/BodyAnalysisResultScreen.kt`
- `feature_track/body/BodyScanViewModel.kt`
- `domain/model/BodyAssessment.kt`
- `domain/usecase/AnalyzeBodyPhotoUseCase.kt`
- `domain/usecase/SaveBodyAssessmentUseCase.kt`

## Các bước triển khai chi tiết

1. Tạo model `BodyAssessment`:
   - id
   - createdAt
   - frontImageUri
   - sideImageUri
   - backImageUri
   - postureObservation
   - compositionTrendNote
   - symmetryNote
   - focusAreaSuggestions
   - confidence
2. Tạo safe language rule:
   - không dùng từ body shaming
   - không kết luận y tế
   - dùng phrasing trung tính như `posture may improve with core work`
3. Body tab UI:
   - title `Body Progress`
   - primary card `Start Body Scan`
   - guide card front/side/back instructions
   - history carousel previous scans
   - metric summary posture/composition/symmetry/confidence
4. Consent step:
   - giải thích ảnh dùng để đánh giá tiến trình
   - không đưa kết luận y tế
   - user phải bấm đồng ý trước khi scan
5. Body Scan Capture:
   - full-screen camera
   - guided overlay silhouette
   - hint `Stand straight, full body visible, good lighting`
   - capture sequence: Front -> Side -> Back
6. Quality check:
   - ảnh đủ sáng
   - body nằm trong khung
   - không quá mờ
   - nếu fail, yêu cầu retake
7. Analyzer:
   - giai đoạn đầu fake analyzer dựa trên ảnh hợp lệ
   - giai đoạn sau ML Kit pose landmarks
   - output confidence + observations
8. Body Analysis Result:
   - before/after cards hoặc current photos
   - posture observation card
   - visible changes card
   - suggested focus area card
   - confidence note
   - buttons Save Assessment, Retake, Ask Coach
9. Khi Save:
   - lưu assessment
   - cập nhật history
   - cập nhật Progress tab
10. Ask Coach:
    - mở Coach với context assessment mới nhất

## Luồng kiểm thử thủ công

1. Vào Track -> Body.
2. Bấm Start Body Scan.
3. Thấy privacy/consent notice.
4. Đồng ý, vào camera.
5. Chụp Front, Side, Back theo hướng dẫn.
6. Nếu ảnh fail quality, app yêu cầu retake.
7. Nếu ảnh đạt, màn result hiển thị nhận xét.
8. Bấm Save Assessment.
9. Quay lại Body tab, history có scan mới.
10. Bấm Ask Coach, Coach nhận context body scan.

## Tiêu chí hoàn thành

- Có consent rõ trước body scan.
- Capture flow có front/side/back.
- Nhận xét dùng ngôn ngữ trung tính, không gây hại.
- Assessment được lưu và xem lại.
- Có đường dẫn sang Coach.

