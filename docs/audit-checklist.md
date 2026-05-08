# Fitty Audit Checklist

Muc dich: checklist de doi chieu nhanh giua code hien tai va ke hoach trong `note.txt`, `description.md`, `ui.md`, va `docs/use-cases/`.

Quy uoc:
- `[x]` Done
- `[-]` Partial
- `[ ]` Missing

## P0 - Kien Truc Bat Buoc

- [x] Hilt + DI co ban da co (`di/AppModule.kt`)
- [x] Root navigation + bottom tabs da co (`navigation/FittyNavHost.kt`, `navigation/MainScaffold.kt`)
- [-] Moi man hinh theo contract `Route + Screen + UiState + UiEvent + UiEffect`
- [-] UDF dung nghia: UI chi gui event, ViewModel xu ly logic
- [ ] Result-state pattern: `Loading / Success / Empty / Error`
- [ ] `AppError` + exception mapping + retry flow
- [ ] `collectAsStateWithLifecycle` thay cho `collectAsState`
- [-] Screen-level state tap trung o ViewModel
- [ ] Khong con `onClick = {}` hoac `clickable {}` rong trong feature code

## P0 - Kiem Tra Tung Man

### Entry / Auth / Onboarding
- [x] Splash -> Welcome / Onboarding / Main routing hoat dong
- [-] Splash dung `UiEffect` thay vi route side effect truc tiep
- [-] SignIn dung `UiEvent/UiEffect` thay vi callback truc tiep
- [-] SignUp dung `UiEvent/UiEffect` thay vi callback truc tiep
- [-] Onboarding duoc chuan hoa contract day du
- [-] Plan Preview duoc chuan hoa contract day du

### Main Tabs
- [-] Home dat scope cua UC-07
- [-] Plan khong con sample/mock data
- [-] Track khong con hardcoded dashboard mock
- [-] Coach co engine/use case/action flow that
- [-] Profile co action flow that cho edit/update/delete/connect

## P1 - Home Dashboard (UC-07)

- [ ] Co `GetTodayDashboardUseCase`
- [ ] Co `DailyDashboard` model dung domain
- [ ] Co loading rieng theo tung section
- [ ] Co error rieng theo tung section
- [ ] Co empty state khi section khong co du lieu
- [ ] Co FAB + quick actions bottom sheet
- [ ] Quick actions dieu huong that sang Meal / Workout / Coach / Body Scan
- [ ] CTA `Start Today`, `View Plan`, `Log Meal`, `Ask Coach` da hoat dong
- [ ] Khong con clickable/action rong trong `HomeScreen.kt`

## P1 - Plan / Workout Library (UC-08)

- [-] Da co `PlanRoute + PlanViewModel + PlanUiState`
- [ ] Khong con `SampleExercise` / `SampleWorkout` trong feature
- [ ] Co repository/use case cho exercise library
- [ ] Co repository/use case cho program templates
- [ ] Co repository/use case cho custom plans
- [ ] Action `Start`, `Save`, `Edit`, `Generate`, `Build manually` da hoat dong
- [ ] State filter/tab/library duoc cap du lieu that

## P1 - Workout Session Mode (UC-09)

- [ ] Co feature rieng cho workout session
- [ ] Timer / rest timer nam o ViewModel
- [ ] Co confirm back khi dang session
- [ ] Co save workout session log
- [ ] Co trigger update streak/progress sau khi complete

## P1 - Track / Meal / Body / Progress (UC-10, UC-11, UC-12)

### Track
- [-] Da co `TrackRoute + TrackViewModel + TrackUiState`
- [ ] Tab data khong con hardcoded
- [ ] Strings khong con hardcoded trong feature

### Meal Scan
- [ ] Co CameraX capture flow
- [ ] Co permission rationale / denied flow
- [ ] Co review -> confirm flow
- [ ] Co manual correction truoc khi save
- [ ] Co meal repository/use case that

### Body Scan
- [ ] Co consent/privacy notice
- [ ] Co image quality check
- [ ] Co retake flow
- [ ] Co body scan repository/use case that
- [ ] Ngon ngu AI an toan, khong body shaming

### Progress / Stats
- [ ] Co date range selector `7D/30D/90D/1Y`
- [ ] Co chart that
- [ ] Co empty state + CTA
- [ ] Co progress repository/use case that

## P1 - Coach Chatbot (UC-13)

- [-] Da co `CoachRoute + CoachViewModel + CoachUiState`
- [ ] Co domain model rieng cho `CoachMessage`
- [ ] Co sealed typed `CoachSuggestion`
- [ ] Co `BuildCoachContextUseCase`
- [ ] Co `SendCoachMessageUseCase`
- [ ] Co `ApplyCoachSuggestionUseCase`
- [ ] Co `CoachEngine` + `FakeCoachEngine` hoac remote client
- [ ] Co pending assistant response state
- [ ] Co error state + retry
- [ ] Action card `Apply/Save/Adjust/Copy` da hoat dong
- [ ] Attach image / mic placeholder co xu ly ro rang
- [ ] Khong con suggestion hardcoded ben ngoai thread message

## P1 - Profile / Settings / Health Connect (UC-14)

- [-] Profile doc du lieu user that
- [ ] Edit profile flow that
- [ ] Update goal flow that
- [ ] Delete account flow that
- [ ] Reminder settings flow that
- [ ] Health Connect integration
- [ ] Permission flow cho Health Connect
- [ ] App settings actions khong con rong

## P1 - Reminder / WorkManager (UC-15)

- [ ] Them dependency WorkManager
- [ ] Co notification channel
- [ ] Co `ReminderPreferences`
- [ ] Co `ReminderScheduler`
- [ ] Co unique work names
- [ ] Co `WorkoutReminderWorker`
- [ ] Co `MealReminderWorker`
- [ ] Co `WaterReminderWorker`
- [ ] Co `SleepReminderWorker`
- [ ] Co `StreakWarningWorker`
- [ ] Co `ScheduleReminderUseCase`
- [ ] Co `CancelReminderUseCase`
- [ ] Co notification permission flow dung thoi diem

## P1 - Streak / Achievement / Habit (UC-16)

- [ ] Co `UpdateStreakUseCase`
- [ ] Co `EvaluateAchievementsUseCase`
- [ ] Co daily check-in flow
- [ ] Co achievement unlock flow
- [ ] Co habit tracking flow
- [ ] Home/Profile/Track doc du lieu streak that

## P2 - Data Layer

- [x] Repository interface cho auth/startup/onboarding/user/session da co
- [-] Firebase layer da tach nho hon truoc
- [ ] Co repository cho plan
- [ ] Co repository cho tracking
- [ ] Co repository cho coach
- [ ] Co repository cho reminder
- [ ] Co local cache/offline layer that
- [ ] Khong con sample/mock data tron trong feature layer

## P2 - Dependencies / Tech Roadmap

- [ ] Room
- [ ] WorkManager
- [ ] CameraX
- [ ] Health Connect
- [ ] ML Kit
- [ ] HTTP client cho Coach
- [x] Firebase Auth / Firestore / Messaging
- [x] DataStore Preferences
- [x] Coil

## P2 - Design System

- [-] Da co mot so reusable component trong `core/designsystem`
- [ ] Co loading view dung chung
- [ ] Co empty state view dung chung
- [ ] Co error state view dung chung
- [ ] Co retry card/state component dung chung
- [ ] Gom cac card/row/tile dang nam rai rac trong feature

## P3 - Quality Gate

- [x] `:app:compileDebugKotlin` da pass
- [-] Unit test command can chay lai trong moi truong khong bi sandbox SDK
- [ ] Co test that cho use case
- [ ] Co test that cho ViewModel
- [ ] Co Compose UI test cho auth/onboarding/navigation
- [ ] Co test cho startup routing
- [ ] Co test cho guest flow
- [ ] Co test cho logout flow
- [ ] Co test cho tab restore state
- [ ] Sua mismatch package name trong instrumented test

## Kiem Tra Nhanh Bang Tim Kiem

Chay cac lenh nay de check debt con sot:

```powershell
rg -n "onClick = \{ \}|clickable \{ \}" app/src/main/java
rg -n "UiEffect|UiEvent|AppError|collectAsStateWithLifecycle" app/src/main/java
rg -n "WorkManager|androidx.work|CameraX|androidx.camera|HealthConnect|ML Kit|androidx.room" app/src/main/java app/build.gradle.kts gradle/libs.versions.toml
```

## Thu Tu Trien Khai De Xuat

1. Chuan hoa `UiEvent + UiEffect + AppError + lifecycle collection`
2. Xoa toan bo action rong
3. Hoan tat `Home` theo UC-07
4. Hoan tat `Coach` theo UC-13
5. Hoan tat `Track` thanh cac flow that
6. Tach `Plan` khoi sample data
7. Bo sung Room / WorkManager / CameraX / Health Connect / ML Kit
8. Viet test that cho use case, ViewModel, navigation
9. Lam vong cuoi cho error/performance/accessibility
