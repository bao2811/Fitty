# UC-13 - Fitty Coach chatbot và action cards

## Mục tiêu

Triển khai Fitty Coach như một chatbot có ngữ cảnh, không chỉ trả lời text. Coach có thể sinh workout suggestion, meal suggestion, grocery list hoặc recovery advice dưới dạng action cards.

## Phạm vi

- Coach tab.
- Chat thread.
- Suggested prompt chips.
- Text composer.
- Image attach placeholder.
- Structured assistant action cards.
- Apply/save action.

## Cài đặt cần chuẩn bị

1. Cần ViewModel + Coroutines.
2. Cần Room nếu lưu chat history offline.
3. Cần remote API client nếu dùng backend chatbot.
4. Giai đoạn đầu có thể dùng fake coach engine để hoàn thiện UI và action flow.

Dependency gợi ý:

```kotlin
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.kotlinx.coroutines.android)
implementation(libs.androidx.room.ktx)
implementation(libs.ktor.client.android)
```

Có thể dùng Retrofit thay Ktor nếu project chọn Retrofit.

## Package/file gợi ý

- `feature_coach/CoachScreen.kt`
- `feature_coach/CoachViewModel.kt`
- `feature_coach/CoachMessageBubble.kt`
- `feature_coach/CoachActionCard.kt`
- `domain/model/CoachMessage.kt`
- `domain/model/CoachSuggestion.kt`
- `domain/usecase/BuildCoachContextUseCase.kt`
- `domain/usecase/SendCoachMessageUseCase.kt`
- `domain/usecase/ApplyCoachSuggestionUseCase.kt`

## Các bước triển khai chi tiết

1. Tạo model `CoachMessage`:
   - id
   - role: user/assistant
   - text
   - createdAt
   - attachments
   - suggestions
2. Tạo sealed class `CoachSuggestion`:
   - `WorkoutSuggestion`
   - `MealSuggestion`
   - `GroceryList`
   - `RecoveryAdvice`
   - `PlanAdjustment`
3. Tạo `BuildCoachContextUseCase`:
   - goal
   - recent workouts
   - calorie trend
   - meal history
   - reminder status
   - body assessment summary
4. Tạo `CoachEngine` interface:
   - `sendMessage(context, message, attachments)`
5. Tạo `FakeCoachEngine`:
   - nếu message chứa `eat`, trả meal suggestion
   - nếu chứa `missed`, trả plan adjustment
   - nếu chứa `workout`, trả workout suggestion
6. Tạo `CoachUiState`:
   - messages
   - inputText
   - isSending
   - errorMessage
   - suggestedPrompts
7. Coach Screen top:
   - title `Fitty Coach`
   - settings icon
8. Suggested prompt chips:
   - What should I eat after training?
   - Adjust my workout today
   - I missed yesterday's session
   - Plan my week
9. Chat area:
   - `LazyColumn`
   - user bubbles
   - assistant bubbles
   - action cards trong assistant message
10. Composer:
    - text field
    - image attach button
    - microphone placeholder
    - send button
11. Khi gửi message:
    - append user message ngay
    - hiển thị pending assistant response
    - gọi coach engine
    - append assistant message
12. Action card behavior:
    - Workout suggestion: button `Apply to Plan`
    - Meal suggestion: button `Save Meal Suggestion`
    - Grocery list: button `Copy List` hoặc `Save`
    - Recovery advice: button `Adjust Today`
13. Không parse raw string trong UI:
    - UI nhận `CoachSuggestion` typed object
    - ViewModel xử lý event apply/save
14. Xử lý lỗi:
    - timeout/network -> assistant error bubble có Retry
    - input rỗng -> disable send

## Luồng kiểm thử thủ công

1. Mở Coach tab.
2. Bấm prompt `What should I eat after training?`.
3. User message xuất hiện.
4. Assistant trả lời kèm Meal Suggestion card.
5. Bấm Save Meal Suggestion, meal được lưu hoặc hiện confirmation.
6. Gửi `I missed yesterday's session`.
7. Assistant trả Plan Adjustment card.
8. Bấm Apply to Plan, plan được cập nhật.
9. Giả lập network error, thấy Retry trong chat.

## Tiêu chí hoàn thành

- Coach có chat thread dùng được.
- Có prompt chips.
- Có action cards typed object.
- Có thể apply/save suggestion.
- Chat không chỉ là text thuần.

