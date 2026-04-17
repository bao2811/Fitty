# UC-09 - Workout session mode

## Mục tiêu

Biến workout plan thành trải nghiệm tập luyện thực tế: người dùng theo từng bài, timer, set counter, rest timer và hoàn tất session để cập nhật streak/progress.

## Phạm vi

- Full-screen session player.
- Current exercise panel.
- Timer và rest timer.
- Previous/Pause/Next/Complete Exercise.
- Complete session.
- Ghi workout session log.

## Cài đặt cần chuẩn bị

1. Cần workout detail từ UC-08.
2. Cần Coroutines/Flow để chạy timer.
3. Cần Room để lưu session log.

Dependency gợi ý:

```kotlin
implementation(libs.kotlinx.coroutines.android)
implementation(libs.androidx.room.ktx)
```

## Package/file gợi ý

- `feature_plan/session/WorkoutSessionScreen.kt`
- `feature_plan/session/WorkoutSessionViewModel.kt`
- `feature_plan/session/SessionTimer.kt`
- `domain/model/WorkoutSession.kt`
- `domain/usecase/StartWorkoutSessionUseCase.kt`
- `domain/usecase/CompleteExerciseUseCase.kt`
- `domain/usecase/CompleteWorkoutSessionUseCase.kt`

## Các bước triển khai chi tiết

1. Tạo model `WorkoutSession`:
   - id
   - workoutId
   - startedAt
   - completedAt optional
   - completedExercises
   - totalDurationSeconds
   - caloriesEstimate
2. Tạo model `ExerciseSessionState`:
   - exerciseId
   - currentSet
   - completedSets
   - isCompleted
3. Tạo `WorkoutSessionUiState`:
   - workoutTitle
   - currentExercise
   - exerciseIndex
   - totalExercises
   - timerSeconds
   - restSeconds
   - isPaused
   - isResting
   - instructionExpanded
   - completionDialogVisible
4. Tạo timer:
   - ViewModel sở hữu timer state
   - mỗi giây tăng workout timer nếu không pause
   - rest timer giảm về 0 nếu đang nghỉ
5. UI chính:
   - top progress `Exercise 2 of 8`
   - panel bài hiện tại
   - tên bài
   - sets/reps/time
   - timer lớn
   - set counter
   - rest timer nếu có
6. Buttons:
   - Previous
   - Pause/Resume
   - Next
   - Complete Exercise
7. Collapsible info:
   - instruction
   - target muscle
   - safer variation
8. Khi bấm Complete Exercise:
   - đánh dấu bài hiện tại hoàn thành
   - nếu còn bài tiếp theo, chuyển sang rest hoặc bài tiếp
   - nếu bài cuối, mở completion dialog
9. Completion dialog:
   - tổng thời gian
   - số bài hoàn thành
   - calories estimate
   - CTA `Finish Session`
10. Khi finish:
    - lưu workout session log
    - cập nhật streak
    - cập nhật progress stats
    - điều hướng về Home hoặc Plan
11. Xử lý back:
    - nếu session đang chạy, hỏi xác nhận thoát
    - nếu thoát, lưu draft hoặc bỏ session tùy chọn

## Luồng kiểm thử thủ công

1. Từ Workout Detail bấm Start Session.
2. Timer bắt đầu chạy.
3. Bấm Pause, timer dừng.
4. Bấm Resume, timer chạy tiếp.
5. Bấm Complete Exercise, chuyển bài/rest.
6. Bấm Previous, quay lại bài trước.
7. Hoàn tất bài cuối, thấy completion dialog.
8. Finish Session, app lưu log và cập nhật streak.
9. Mở Progress, thấy workout count tăng.

## Tiêu chí hoàn thành

- Session mode dùng được như trình tập luyện thật.
- Timer không nằm trong composable local state dễ mất.
- Hoàn tất session có log.
- Streak/progress có thể nhận event hoàn tất.
- Có xác nhận khi thoát giữa chừng.

