Điểm thiếu lớn nhất hiện tại là chưa có domain/usecase, chưa có DI nên ViewModel tự new repository/data source trực tiếp SignInScreen.kt OnboardingScreen.kt ProfileScreen.kt. Nếu bạn muốn, tôi có thể bước tiếp theo là vẽ luôn sơ đồ kiến trúc đề xuất cho repo này và đưa ra package structure cụ thể để refactor.

Ưu tiên cao

Dependency injection và tách ViewModel khỏi việc tự khởi tạo dependency. Hiện nhiều ViewModel tự new FittyFirebaseRepository, AppPreferencesDataSource, FittyMessagingCoordinator, nên khó test và khó thay thế implementation SignInScreen.kt OnboardingScreen.kt SplashScreen.kt ProfileScreen.kt HomeScreen.kt. Nên chuyển sang Hilt + constructor injection + ViewModel thường thay vì AndroidViewModel.
FittyFirebaseRepository đang là “god repository”. Một class đang ôm auth, startup, onboarding, notification token, plan generation, user profile FittyFirebaseRepository.kt FittyFirebaseRepository.kt FittyFirebaseRepository.kt FittyFirebaseRepository.kt. Nên tách thành AuthRepository, UserRepository, OnboardingRepository, PlanRepository, NotificationRepository.
Thiếu domain/usecase, dù tài liệu đã định hướng MVVM + repository + use case + UDF 01-app-foundation-navigation.md 07-home-dashboard.md. Những logic như resolve startup destination, save onboarding, generate starter plan, sign in, logout nên ra use case riêng thay vì nhét vào ViewModel/repository.
Ưu tiên trung bình

Đang có gọi dữ liệu lặp ở Home và Profile: init { refreshUser() } rồi lại LaunchedEffect(Unit) { viewModel.refreshUser() }, tức là load 2 lần HomeScreen.kt HomeScreen.kt ProfileScreen.kt ProfileScreen.kt. Giữ một nơi trigger là đủ.
HomeScreen và vài màn khác vẫn là UI “hybrid”: state lấy từ backend nhưng nhiều section còn hardcode nội dung trong composable, nên khó bảo trì và khó test HomeScreen.kt HomeScreen.kt HomeScreen.kt. Nên đẩy toàn bộ dữ liệu hiển thị vào UiState theo từng section.
FittyInAppBannerManager là singleton state toàn cục dùng MutableStateFlow, dễ gây coupling ẩn và khó kiểm thử FittyInAppBanner.kt FittyInAppBanner.kt. Nên đổi sang event channel ở app state hoặc dùng SnackbarHostState.
saveOnboardingAnswers() đang xóa toàn bộ documents con rồi ghi lại FittyFirebaseRepository.kt FittyFirebaseRepository.kt. Cách này đơn giản nhưng không tối ưu, dễ tạo side effect và tăng số lần write. Nên chuyển sang upsert có key ổn định hoặc batched write.
Ưu tiên thấp nhưng nên làm

Design system mới ở mức đầu: SectionHeader, MetricTile, SettingsRow, QuickAction, TaskCard còn nằm rải trong feature thay vì đưa về core/designsystem HomeScreen.kt HomeScreen.kt HomeScreen.kt ProfileScreen.kt ProfileScreen.kt. Nếu muốn UI scale tốt, nên chuẩn hóa chúng thành reusable components.
Local data layer đang tồn tại nhưng gần như đứng ngoài flow chính, tạo cảm giác kiến trúc nửa vời giữa Firebase và SQLite FittyLocalRepository.kt FittyDatabaseHelper.kt. Hoặc bỏ, hoặc đưa nó thành cache/offline layer thật sự phía sau repository interface.
Chuỗi hiển thị còn hardcode nhiều trong code UI, ví dụ splash SplashScreen.kt. Nên đưa về strings.xml để hỗ trợ i18n và giảm rác UI code.
Nếu làm theo roadmap ngắn, tôi sẽ đi theo thứ tự:

Thêm Hilt và inject dependencies.
Tách FittyFirebaseRepository.
Tạo domain/usecase cho auth, startup, onboarding.
Sửa các màn đang load dữ liệu lặp.
Chuẩn hóa lại design system và dọn hardcoded UI text.

Ưu tiên cao

Dependency injection và tách ViewModel khỏi việc tự khởi tạo dependency. Hiện nhiều ViewModel tự new FittyFirebaseRepository, AppPreferencesDataSource, FittyMessagingCoordinator, nên khó test và khó thay thế implementation SignInScreen.kt OnboardingScreen.kt SplashScreen.kt ProfileScreen.kt HomeScreen.kt. Nên chuyển sang Hilt + constructor injection + ViewModel thường thay vì AndroidViewModel.
FittyFirebaseRepository đang là “god repository”. Một class đang ôm auth, startup, onboarding, notification token, plan generation, user profile FittyFirebaseRepository.kt FittyFirebaseRepository.kt FittyFirebaseRepository.kt FittyFirebaseRepository.kt. Nên tách thành AuthRepository, UserRepository, OnboardingRepository, PlanRepository, NotificationRepository.
Thiếu domain/usecase, dù tài liệu đã định hướng MVVM + repository + use case + UDF 01-app-foundation-navigation.md 07-home-dashboard.md. Những logic như resolve startup destination, save onboarding, generate starter plan, sign in, logout nên ra use case riêng thay vì nhét vào ViewModel/repository.
Ưu tiên trung bình

Đang có gọi dữ liệu lặp ở Home và Profile: init { refreshUser() } rồi lại LaunchedEffect(Unit) { viewModel.refreshUser() }, tức là load 2 lần HomeScreen.kt HomeScreen.kt ProfileScreen.kt ProfileScreen.kt. Giữ một nơi trigger là đủ.
HomeScreen và vài màn khác vẫn là UI “hybrid”: state lấy từ backend nhưng nhiều section còn hardcode nội dung trong composable, nên khó bảo trì và khó test HomeScreen.kt HomeScreen.kt HomeScreen.kt. Nên đẩy toàn bộ dữ liệu hiển thị vào UiState theo từng section.
FittyInAppBannerManager là singleton state toàn cục dùng MutableStateFlow, dễ gây coupling ẩn và khó kiểm thử FittyInAppBanner.kt FittyInAppBanner.kt. Nên đổi sang event channel ở app state hoặc dùng SnackbarHostState.
saveOnboardingAnswers() đang xóa toàn bộ documents con rồi ghi lại FittyFirebaseRepository.kt FittyFirebaseRepository.kt. Cách này đơn giản nhưng không tối ưu, dễ tạo side effect và tăng số lần write. Nên chuyển sang upsert có key ổn định hoặc batched write.
Ưu tiên thấp nhưng nên làm

Design system mới ở mức đầu: SectionHeader, MetricTile, SettingsRow, QuickAction, TaskCard còn nằm rải trong feature thay vì đưa về core/designsystem HomeScreen.kt HomeScreen.kt HomeScreen.kt ProfileScreen.kt ProfileScreen.kt. Nếu muốn UI scale tốt, nên chuẩn hóa chúng thành reusable components.
Local data layer đang tồn tại nhưng gần như đứng ngoài flow chính, tạo cảm giác kiến trúc nửa vời giữa Firebase và SQLite FittyLocalRepository.kt FittyDatabaseHelper.kt. Hoặc bỏ, hoặc đưa nó thành cache/offline layer thật sự phía sau repository interface.
Chuỗi hiển thị còn hardcode nhiều trong code UI, ví dụ splash SplashScreen.kt. Nên đưa về strings.xml để hỗ trợ i18n và giảm rác UI code.
Nếu làm theo roadmap ngắn, tôi sẽ đi theo thứ tự:

Thêm Hilt và inject dependencies.
Tách FittyFirebaseRepository.
Tạo domain/usecase cho auth, startup, onboarding.
Sửa các màn đang load dữ liệu lặp.
Chuẩn hóa lại design system và dọn hardcoded UI text.
