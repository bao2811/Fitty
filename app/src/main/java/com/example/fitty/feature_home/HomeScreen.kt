package com.example.fitty.feature_home

import android.content.Context
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.fitty.R
import com.example.fitty.core.designsystem.component.FittySectionHeader
import com.example.fitty.core.ui.ContentDebugSource
import com.example.fitty.core.ui.ContentSourceState
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.domain.model.AppNotificationType
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.HomeBehaviorConfig
import com.example.fitty.domain.model.HomeContentConfig
import com.example.fitty.domain.model.HomeTaskCategory
import com.example.fitty.domain.model.HomeTaskDraft
import com.example.fitty.domain.model.HomeTaskStatus
import com.example.fitty.domain.model.ScheduledWorkout
import com.example.fitty.domain.model.preferredDisplayName
import com.example.fitty.data.content.LocalContentFallbacks
import com.example.fitty.domain.repository.AppNotificationRepository
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.HomeTaskRepository
import com.example.fitty.domain.usecase.user.GetCurrentUserUseCase
import com.example.fitty.ui.theme.FittyGradientEnd
import com.example.fitty.ui.theme.FittyGradientStart
import com.example.fitty.ui.theme.FittyPink
import com.example.fitty.ui.theme.FittyPinkLight
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class HomeFocusMetricUi(
    val label: String,
    val value: String
)

data class HomeTaskUi(
    val id: Long,
    val category: HomeTaskCategory,
    val title: String,
    val description: String,
    val time: String,
    val status: HomeTaskStatus,
    val reminderEnabled: Boolean,
    val isPlanBacked: Boolean = false
)

data class HomeTaskPresetUi(
    val title: String,
    val description: String,
    val timeMinutes: Int,
    val category: HomeTaskCategory,
    val reminderEnabled: Boolean
)

data class HomeMacroProgressUi(
    val label: String,
    val progress: Float
)

data class HomeMealUi(
    val label: String,
    val calories: String
)

data class HomeInsightUi(
    val title: String,
    val message: String,
    val actions: List<String>
)

data class HomeAchievementUi(
    val title: String,
    val subtitle: String,
    val actionLabel: String
)

data class HomeWorkoutUi(
    val sectionTitle: String,
    val name: String,
    val meta: String,
    val exerciseSummaries: List<String> = emptyList(),
    val equipmentLabel: String,
    val primaryActionLabel: String,
    val secondaryActionLabel: String,
    val planId: String = "",
    val scheduledWorkoutId: String = ""
)

data class HomeNutritionUi(
    val sectionTitle: String,
    val summary: String,
    val macros: List<HomeMacroProgressUi>,
    val meals: List<HomeMealUi>,
    val primaryActionLabel: String,
    val secondaryActionLabel: String
)

data class HomeStreakUi(
    val sectionTitle: String,
    val currentLabel: String,
    val subtitle: String,
    val bestLabel: String,
    val dayLabels: List<String>,
    val activeDayFlags: List<Boolean>,   // which days are active
    val currentDayIndex: Int
)

data class HomeAchievementPreviewUi(
    val sectionTitle: String,
    val itemLabel: String,
    val subtitle: String,
    val actionLabel: String
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val avatarInitial: String = "F",
    val avatarUrl: String? = null,
    val greetingTitle: String = "",
    val greetingSubtitle: String = "",
    val focusTitle: String = "",
    val focusDescription: String = "",
    val focusMetrics: List<HomeFocusMetricUi> = emptyList(),
    val focusPrimaryActionLabel: String = "",
    val focusSecondaryActionLabel: String = "",
    val tasksSectionTitle: String = "",
    val tasksSectionActionLabel: String = "",
    val tasks: List<HomeTaskUi> = emptyList(),
    val streak: HomeStreakUi = HomeStreakUi("", "", "", "", emptyList(), emptyList(), 0),
    val workout: HomeWorkoutUi = HomeWorkoutUi("", "", "", emptyList(), "", "", ""),
    val nutrition: HomeNutritionUi = HomeNutritionUi("", "", emptyList(), emptyList(), "", ""),
    val insight: HomeInsightUi = HomeInsightUi("", "", emptyList()),
    val achievement: HomeAchievementPreviewUi = HomeAchievementPreviewUi("", "", "", ""),
    val showNotifications: Boolean = false,
    val notifications: List<HomeNotificationUi> = emptyList(),
    val unreadNotificationCount: Int = 0,
    val presetTasks: List<HomeTaskPresetUi> = emptyList(),
    val customTasks: List<HomeTaskUi> = emptyList(),
    val planWorkoutTask: HomeTaskUi? = null,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val targetWeightKg: Int? = null,
    val bmi: Float? = null,
    val showEditBodyDialog: Boolean = false,
    val contentSources: List<ContentDebugSource> = emptyList()
)

data class HomeNotificationUi(
    val id: Long,
    val title: String,
    val message: String,
    val time: String,
    val icon: ImageVector,
    val isRead: Boolean
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getHomeDashboardUseCase: com.example.fitty.domain.usecase.home.GetHomeDashboardUseCase,
    private val updateStreakUseCase: com.example.fitty.domain.usecase.user.UpdateStreakUseCase,
    private val homeTaskRepository: HomeTaskRepository,
    private val appNotificationRepository: AppNotificationRepository,
    private val localContentFallbacks: LocalContentFallbacks,
    private val contentRepository: ContentRepository,
    private val userRepository: com.example.fitty.domain.repository.UserRepository,
    private val sessionRepository: com.example.fitty.domain.repository.SessionRepository,
    private val trackingRepository: com.example.fitty.domain.repository.TrackingRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private var homeContentConfig: HomeContentConfig = localContentFallbacks.home(Locale.getDefault().language)
    private var homeBehaviorConfig: HomeBehaviorConfig = localContentFallbacks.homeBehaviorConfig()
    private val _uiState = MutableStateFlow(
        initialHomeUiState(context, homeContentConfig).copy(
            contentSources = listOf(
                ContentDebugSource("Home content", ContentSourceState.Fallback, "Using local fallback until remote load completes"),
                ContentDebugSource("Home behavior", ContentSourceState.Fallback, "Using local fallback until remote load completes")
            )
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState
    private val todayDateKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    init {
        observeTasks()
        observeNotifications()
        loadContent()
        refreshData()
    }

    private fun loadContent() {
        viewModelScope.launch {
            val language = sessionRepository.getAppLanguage().orEmpty().ifBlank { Locale.getDefault().language }
            homeContentConfig = contentRepository.getHomeContent(language)
            homeBehaviorConfig = contentRepository.getHomeBehaviorConfig()
            val sources = listOf(
                ContentDebugSource(
                    "Home content",
                    if (contentRepository.usedFallbackFor("home_content")) ContentSourceState.Fallback else ContentSourceState.Remote,
                    contentRepository.fallbackDetailFor("home_content")
                        ?: if (contentRepository.usedFallbackFor("home_content")) "Using local fallback" else "Loaded language=$language from Firebase"
                ),
                ContentDebugSource(
                    "Home behavior",
                    if (contentRepository.usedFallbackFor("home_behavior")) ContentSourceState.Fallback else ContentSourceState.Remote,
                    contentRepository.fallbackDetailFor("home_behavior")
                        ?: if (contentRepository.usedFallbackFor("home_behavior")) "Using local fallback" else "Loaded from Firebase"
                )
            )
            _uiState.update { state ->
                state.applyContent(context, homeContentConfig).copy(contentSources = sources)
            }
        }
    }

    /**
     * Reload all dashboard data. Called on init and when returning to Home tab.
     */
    fun refreshData() {
        refreshUser()
        loadTodaySummary()
    }

    fun refreshUser() {
        viewModelScope.launch {
            runCatching { updateStreakUseCase("login") }

            // Load user for profile display
            runCatching { getCurrentUserUseCase() }
                .onSuccess { user ->
                    _uiState.update { current ->
                        if (user == null) current.copy(
                            isLoading = false,
                            greetingTitle = context.getString(
                                R.string.home_greeting_default,
                                context.getString(R.string.home_display_name_default)
                            )
                        ).applyContent(context, homeContentConfig) else {
                            val withBody = user.toHomeUiState(context, homeContentConfig).preserveDynamicState(current)
                            val h = user.profile.heightCm
                            val w = user.profile.weightKg
                            val bmi = if (h != null && h > 0 && w != null && w > 0) {
                                w.toFloat() / ((h.toFloat() / 100f) * (h.toFloat() / 100f))
                            } else null
                            withBody.copy(
                                heightCm = h,
                                weightKg = w,
                                targetWeightKg = user.profile.targetWeightKg,
                                bmi = bmi
                            ).applyContent(context, homeContentConfig)
                        }
                    }
                }
                .onFailure {
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            greetingTitle = context.getString(
                                R.string.home_greeting_default,
                                context.getString(R.string.home_display_name_default)
                            )
                        ).applyContent(context, homeContentConfig)
                    }
                }

            // Load dashboard data (daily summary, today workout, active plan)
            runCatching { getHomeDashboardUseCase() }
                .onSuccess { dashboard ->
                    if (dashboard == null) return@onSuccess
                    _uiState.update { current ->
                        val summary = dashboard.dailySummary
                        val workout = dashboard.todayWorkout
                        val mealTarget = dashboard.user.settings.mealTargetPerDay ?: 0
                        val planWorkoutTask = buildPlanWorkoutTask(
                            user = dashboard.user,
                            workout = workout,
                            workoutsCompleted = summary?.progress?.workoutsCompleted ?: 0
                        )
                        current.copy(
                            focusMetrics = listOf(
                                HomeFocusMetricUi(
                                    context.getString(R.string.home_metric_workout),
                                    "${summary?.progress?.workoutsCompleted ?: 0}/${summary?.targets?.workouts ?: 1}"
                                ),
                                HomeFocusMetricUi(
                                    context.getString(R.string.home_metric_meals_logged),
                                    if (mealTarget > 0) "${summary?.mealsLoggedCount ?: 0}/$mealTarget"
                                    else "${summary?.mealsLoggedCount ?: 0}"
                                ),
                                HomeFocusMetricUi(
                                    context.getString(R.string.home_metric_water),
                                    context.getString(
                                        R.string.home_water_progress,
                                        (summary?.progress?.waterMl ?: 0) / 1000f,
                                        (summary?.targets?.waterMl ?: homeBehaviorConfig.waterTargetMl) / 1000f
                                    )
                                )
                            ),
                            workout = if (workout != null) HomeWorkoutUi(
                                sectionTitle = context.getString(R.string.home_workout_section_title),
                                name = workout.title.ifBlank { dashboard.activePlanName ?: "" },
                                meta = context.getString(
                                    R.string.home_workout_meta_live,
                                    workout.durationMinutes,
                                    workout.difficulty.toLocalizedToken(context)
                                ),
                                exerciseSummaries = workout.exercises.map { plannedExercise ->
                                    plannedExercise.toHomeWorkoutExerciseSummary(context)
                                },
                                equipmentLabel = workout.equipment.ifBlank { context.getString(R.string.home_equipment_default) },
                                primaryActionLabel = context.getString(R.string.home_action_start),
                                secondaryActionLabel = context.getString(R.string.home_action_details),
                                planId = workout.planId.ifBlank { dashboard.user.stats.activePlanId },
                                scheduledWorkoutId = workout.id
                            ) else current.workout,
                            nutrition = if (summary != null) {
                                val uid = sessionRepository.getCurrentUserId()
                                if (uid != null) {
                                    val logs = runCatching { trackingRepository.getMealLogs(uid, todayDateKey) }.getOrDefault(emptyList())
                                    buildNutritionFromMealLogs(
                                        mealLogs = logs,
                                        caloriesTarget = summary.targets.calories
                                    )
                                } else {
                                    emptyNutrition(
                                        context = context,
                                        caloriesTarget = summary.targets.calories
                                    )
                                }
                            } else current.nutrition,
                            streak = buildStreakUi(
                                context = context,
                                currentStreak = dashboard.user.stats.currentStreak,
                                bestStreak = dashboard.user.stats.bestStreak,
                                lastActiveDate = dashboard.user.stats.lastActiveDate,
                                activeDates = dashboard.user.stats.streakActiveDates
                            ),
                            insight = if (summary?.insightText?.isNotBlank() == true) HomeInsightUi(
                                title = context.getString(R.string.home_insight_title),
                                message = summary.insightText,
                                actions = homeContentConfig.insightActions
                            ) else current.insight,
                            planWorkoutTask = planWorkoutTask,
                            tasks = mergeHomeTasks(
                                customTasks = current.customTasks,
                                planWorkoutTask = planWorkoutTask
                            )
                        ).applyContent(context, homeContentConfig)
                    }
                }
        }
    }

    internal fun addTask(
        title: String,
        description: String,
        timeMinutes: Int,
        category: HomeTaskCategory,
        reminderEnabled: Boolean
    ) {
        viewModelScope.launch {
            homeTaskRepository.addTask(
                HomeTaskDraft(
                    title = title,
                    description = description,
                    dateKey = todayDateKey,
                    timeMinutes = timeMinutes,
                    category = category,
                    reminderEnabled = reminderEnabled
                )
            )
        }
    }

    internal fun addPresetTask(preset: HomeTaskPresetUi) {
        addTask(
            title = preset.title,
            description = preset.description,
            timeMinutes = preset.timeMinutes,
            category = preset.category,
            reminderEnabled = preset.reminderEnabled
        )
    }

    internal fun setTaskStatus(taskId: Long, status: HomeTaskStatus) {
        if (taskId <= 0L) return
        viewModelScope.launch {
            homeTaskRepository.updateTaskStatus(
                taskId = taskId,
                status = status
            )
        }
    }

    internal fun toggleTaskReminder(taskId: Long, enabled: Boolean) {
        if (taskId <= 0L) return
        viewModelScope.launch {
            homeTaskRepository.updateTaskReminder(taskId, enabled)
        }
    }

    internal fun deleteTask(taskId: Long) {
        if (taskId <= 0L) return
        viewModelScope.launch {
            homeTaskRepository.deleteTask(taskId)
        }
    }

    internal fun toggleNotifications() {
        _uiState.update { it.copy(showNotifications = !it.showNotifications) }
    }

    internal fun dismissNotifications() {
        _uiState.update { it.copy(showNotifications = false) }
    }

    internal fun markNotificationRead(notificationId: Long) {
        viewModelScope.launch {
            appNotificationRepository.markAsRead(notificationId)
        }
    }

    internal fun markAllNotificationsRead() {
        viewModelScope.launch {
            appNotificationRepository.markAllAsRead()
        }
    }

    internal fun deleteNotification(notificationId: Long) {
        viewModelScope.launch {
            appNotificationRepository.deleteNotification(notificationId)
        }
    }

    private fun observeTasks() {
        viewModelScope.launch {
            homeTaskRepository.observeTasks(todayDateKey).collect { tasks ->
                val customTasks = tasks.filterNot { it.isDefault }.map { task ->
                    HomeTaskUi(
                        id = task.id,
                        category = task.category,
                        title = task.title,
                        description = task.description,
                        time = formatTaskTime(task.timeMinutes),
                        status = task.status,
                        reminderEnabled = task.reminderEnabled
                    )
                }
                _uiState.update { current ->
                    current.copy(
                        customTasks = customTasks,
                        tasks = mergeHomeTasks(
                            customTasks = customTasks,
                            planWorkoutTask = current.planWorkoutTask
                        )
                    )
                }
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            appNotificationRepository.observeNotifications().collect { notifications ->
                _uiState.update { current ->
                    current.copy(
                        notifications = notifications.map { notification ->
                            HomeNotificationUi(
                                id = notification.id,
                                title = notification.title,
                                message = notification.message,
                                time = formatNotificationTime(context, notification.createdAt),
                                icon = notification.type.toNotificationIcon(),
                                isRead = notification.isRead
                            )
                        }
                    )
                }
            }
        }
        viewModelScope.launch {
            appNotificationRepository.observeUnreadCount().collect { unreadCount ->
                _uiState.update { it.copy(unreadNotificationCount = unreadCount) }
            }
        }
    }

    fun toggleEditBodyDialog(show: Boolean) {
        _uiState.update { it.copy(showEditBodyDialog = show) }
    }

    fun updateBodyMetrics(newHeightCm: Int?, newWeightKg: Int?, newTargetWeightKg: Int?) {
        viewModelScope.launch {
            val uid = sessionRepository.getCurrentUserId() ?: return@launch
            val user = getCurrentUserUseCase() ?: return@launch
            val updatedProfile = user.profile.copy(
                heightCm = newHeightCm ?: user.profile.heightCm,
                weightKg = newWeightKg ?: user.profile.weightKg,
                targetWeightKg = newTargetWeightKg ?: user.profile.targetWeightKg
            )
            userRepository.updateProfile(uid, updatedProfile)
            if (newWeightKg != null && newWeightKg != user.profile.weightKg) {
                trackingRepository.saveBodyMeasurement(
                    uid = uid,
                    measurement = com.example.fitty.domain.model.BodyMeasurement(
                        dateKey = todayDateKey,
                        weightKg = newWeightKg.toFloat(),
                        source = "manual"
                    )
                )
            }
            val h = updatedProfile.heightCm
            val w = updatedProfile.weightKg
            val bmi = if (h != null && h > 0 && w != null && w > 0) {
                w.toFloat() / ((h.toFloat() / 100f) * (h.toFloat() / 100f))
            } else null
            _uiState.update {
                it.copy(
                    heightCm = updatedProfile.heightCm,
                    weightKg = updatedProfile.weightKg,
                    targetWeightKg = updatedProfile.targetWeightKg,
                    bmi = bmi,
                    showEditBodyDialog = false
                )
            }
        }
    }

    internal fun loadTodaySummary() {
        viewModelScope.launch {
            val uid = sessionRepository.getCurrentUserId() ?: return@launch

            // Fetch daily summary and meal logs independently so one failure doesn't block the other
            val summary = runCatching { trackingRepository.getDailySummary(uid, todayDateKey) }.getOrNull()
            val mealLogs = runCatching { trackingRepository.getMealLogs(uid, todayDateKey) }.getOrDefault(emptyList())
            val user = runCatching { getCurrentUserUseCase() }.getOrNull()

            val workoutsCompleted = summary?.progress?.workoutsCompleted ?: 0
            val workoutTarget = summary?.targets?.workouts ?: 1
            val waterMl = summary?.progress?.waterMl ?: 0
            val waterTarget = summary?.targets?.waterMl ?: homeBehaviorConfig.waterTargetMl
            val waterL = "%.1f".format(waterMl / 1000f)
            val waterTL = "%.1f".format(waterTarget / 1000f)
            val caloriesTarget = summary?.targets?.calories ?: 2100

            // Build nutrition: prefer meal logs, fall back to daily summary data
            val nutritionUi = if (mealLogs.isNotEmpty()) {
                buildNutritionFromMealLogs(
                    mealLogs = mealLogs,
                    caloriesTarget = caloriesTarget
                )
            } else if (summary != null && summary.progress.caloriesConsumed > 0) {
                // Fallback: build from daily summary when meal logs query returned empty
                buildNutritionFromDailySummary(
                    summary = summary
                )
            } else {
                emptyNutrition(context = context, caloriesTarget = caloriesTarget)
            }

            // Use mealsLogged from daily summary if meal logs are empty (query may have failed)
            val mealsLogged = if (mealLogs.isNotEmpty()) mealLogs.size
                else summary?.progress?.mealsLogged ?: 0
            val mealTarget = user?.settings?.mealTargetPerDay ?: 0

            _uiState.update { current ->
                current.copy(
                    focusMetrics = listOf(
                        HomeFocusMetricUi(
                            label = context.getString(R.string.home_metric_workout),
                            value = "$workoutsCompleted/$workoutTarget"
                        ),
                        HomeFocusMetricUi(
                            label = context.getString(R.string.home_metric_meals_logged),
                            value = if (mealTarget > 0) "$mealsLogged/$mealTarget" else "$mealsLogged"
                        ),
                        HomeFocusMetricUi(
                            label = context.getString(R.string.home_metric_water),
                            value = context.getString(
                                R.string.home_water_progress,
                                waterL.toFloatOrNull() ?: 0f,
                                waterTL.toFloatOrNull() ?: 0f
                            )
                        )
                    ),
                    nutrition = nutritionUi
                )
            }
        }
    }

    private fun buildNutritionFromMealLogs(
        mealLogs: List<com.example.fitty.domain.model.MealLog>,
        caloriesTarget: Int
    ): HomeNutritionUi {
        val totalCalories = mealLogs.sumOf { it.totalCalories }
        val totalProtein = mealLogs.sumOf { it.totalProtein }
        val totalCarbs = mealLogs.sumOf { it.totalCarbs }
        val totalFat = mealLogs.sumOf { it.totalFat }
        val totalMacroGrams = (totalProtein + totalCarbs + totalFat).coerceAtLeast(1)

        val proteinRatio = totalProtein.toFloat() / totalMacroGrams
        val carbsRatio = totalCarbs.toFloat() / totalMacroGrams
        val fatRatio = totalFat.toFloat() / totalMacroGrams

        // Group meals by type and build meal rows
        val mealRows = if (mealLogs.isEmpty()) {
            listOf(
                HomeMealUi(context.getString(R.string.home_meal_breakfast), "—"),
                HomeMealUi(context.getString(R.string.home_meal_lunch), "—"),
                HomeMealUi(context.getString(R.string.home_meal_snack), "—")
            )
        } else {
            mealLogs.groupBy { it.mealType.ifBlank { context.getString(R.string.home_meal_type_other) } }.map { (type, logs) ->
                val cal = logs.sumOf { it.totalCalories }
                HomeMealUi(
                    label = type.toLocalizedToken(context, defaultValue = context.getString(R.string.home_meal_type_other)),
                    calories = context.getString(R.string.home_calories_value, cal)
                )
            }
        }

        return HomeNutritionUi(
            sectionTitle = context.getString(R.string.home_nutrition_section_title),
            summary = context.getString(R.string.home_nutrition_summary, totalCalories, caloriesTarget),
            macros = if (mealLogs.isEmpty()) {
                listOf(
                    HomeMacroProgressUi(context.getString(R.string.home_macro_protein), 0f),
                    HomeMacroProgressUi(context.getString(R.string.home_macro_carbs), 0f),
                    HomeMacroProgressUi(context.getString(R.string.home_macro_fat), 0f)
                )
            } else {
                listOf(
                    HomeMacroProgressUi(context.getString(R.string.home_macro_protein), proteinRatio),
                    HomeMacroProgressUi(context.getString(R.string.home_macro_carbs), carbsRatio),
                    HomeMacroProgressUi(context.getString(R.string.home_macro_fat), fatRatio)
                )
            },
            meals = mealRows,
            primaryActionLabel = context.getString(R.string.home_action_log_meal),
            secondaryActionLabel = context.getString(R.string.home_action_details)
        )
    }

    /**
     * Fallback: build nutrition UI from DailySummary when meal_logs query fails
     * but daily summary has aggregated data (written by ConfirmMealLogUseCase).
     */
    private fun buildNutritionFromDailySummary(
        summary: com.example.fitty.domain.model.DailySummary
    ): HomeNutritionUi {
        val totalCalories = summary.progress.caloriesConsumed
        val totalProtein = summary.progress.proteinGrams
        val totalCarbs = summary.progress.carbsGrams
        val totalFat = summary.progress.fatGrams
        val totalMacroGrams = (totalProtein + totalCarbs + totalFat).coerceAtLeast(1)
        val caloriesTarget = summary.targets.calories

        val proteinRatio = totalProtein.toFloat() / totalMacroGrams
        val carbsRatio = totalCarbs.toFloat() / totalMacroGrams
        val fatRatio = totalFat.toFloat() / totalMacroGrams

        val mealsCount = summary.progress.mealsLogged
        val mealRows = if (mealsCount > 0) {
            listOf(
                HomeMealUi(
                    label = context.getString(R.string.home_meal_breakfast),
                    calories = context.getString(R.string.home_calories_value, totalCalories)
                )
            )
        } else {
            listOf(
                HomeMealUi(context.getString(R.string.home_meal_breakfast), "—"),
                HomeMealUi(context.getString(R.string.home_meal_lunch), "—"),
                HomeMealUi(context.getString(R.string.home_meal_snack), "—")
            )
        }

        return HomeNutritionUi(
            sectionTitle = context.getString(R.string.home_nutrition_section_title),
            summary = context.getString(R.string.home_nutrition_summary, totalCalories, caloriesTarget),
            macros = listOf(
                HomeMacroProgressUi(context.getString(R.string.home_macro_protein), proteinRatio),
                HomeMacroProgressUi(context.getString(R.string.home_macro_carbs), carbsRatio),
                HomeMacroProgressUi(context.getString(R.string.home_macro_fat), fatRatio)
            ),
            meals = mealRows,
            primaryActionLabel = context.getString(R.string.home_action_log_meal),
            secondaryActionLabel = context.getString(R.string.home_action_details)
        )
    }

}

@Composable
fun HomeRoute(
    onNavigateToPlan: (planId: String, scheduledWorkoutId: String) -> Unit = { _, _ -> },
    onNavigateToTrack: () -> Unit = {},
    onNavigateToCoach: () -> Unit = {},
    onStartWorkout: (planId: String, scheduledWorkoutId: String) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Refresh data every time the Home tab becomes visible (e.g. after Track meal)
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HomeScreen(
        state = state,
        onStartWorkout = {
            onStartWorkout(state.workout.planId, state.workout.scheduledWorkoutId)
        },
        onWorkoutDetails = {
            onNavigateToPlan(state.workout.planId, state.workout.scheduledWorkoutId)
        },
        onLogMeal = onNavigateToTrack,
        onNutritionDetails = onNavigateToTrack,
        onAskCoach = onNavigateToCoach,
        onToggleNotifications = viewModel::toggleNotifications,
        onDismissNotifications = viewModel::dismissNotifications,
        onAddTask = viewModel::addTask,
        onAddPresetTask = viewModel::addPresetTask,
        onSetTaskStatus = viewModel::setTaskStatus,
        onToggleTaskReminder = viewModel::toggleTaskReminder,
        onDeleteTask = viewModel::deleteTask,
        onMarkNotificationRead = viewModel::markNotificationRead,
        onMarkAllNotificationsRead = viewModel::markAllNotificationsRead,
        onDeleteNotification = viewModel::deleteNotification,
        onEditBodyMetrics = { viewModel.toggleEditBodyDialog(true) },
        onDismissEditBody = { viewModel.toggleEditBodyDialog(false) },
        onSaveBodyMetrics = viewModel::updateBodyMetrics
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartWorkout: () -> Unit = {},
    onWorkoutDetails: () -> Unit = {},
    onLogMeal: () -> Unit = {},
    onNutritionDetails: () -> Unit = {},
    onAskCoach: () -> Unit = {},
    onToggleNotifications: () -> Unit = {},
    onDismissNotifications: () -> Unit = {},
    onAddTask: (String, String, Int, HomeTaskCategory, Boolean) -> Unit = { _, _, _, _, _ -> },
    onAddPresetTask: (HomeTaskPresetUi) -> Unit = {},
    onSetTaskStatus: (Long, HomeTaskStatus) -> Unit = { _, _ -> },
    onToggleTaskReminder: (Long, Boolean) -> Unit = { _, _ -> },
    onDeleteTask: (Long) -> Unit = {},
    onMarkNotificationRead: (Long) -> Unit = {},
    onMarkAllNotificationsRead: () -> Unit = {},
    onDeleteNotification: (Long) -> Unit = {},
    onEditBodyMetrics: () -> Unit = {},
    onDismissEditBody: () -> Unit = {},
    onSaveBodyMetrics: (Int?, Int?, Int?) -> Unit = { _, _, _ -> }
) {
    var showAddTaskDialog by rememberSaveable { mutableStateOf(false) }

    if (state.showNotifications) {
        NotificationDialog(
            notifications = state.notifications,
            unreadCount = state.unreadNotificationCount,
            onDismiss = onDismissNotifications,
            onMarkRead = onMarkNotificationRead,
            onMarkAllRead = onMarkAllNotificationsRead,
            onDelete = onDeleteNotification
        )
    }
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, description, timeMinutes, category, reminderEnabled ->
                onAddTask(title, description, timeMinutes, category, reminderEnabled)
                showAddTaskDialog = false
            }
        )
    }

    FittyLazyScreen {
        item { HomeTopBar(state = state, onNotificationClick = onToggleNotifications) }
        item { TodaySummaryCard(state = state, onStartToday = onStartWorkout, onViewPlan = onWorkoutDetails) }
        // Quick Actions Row
        item {
            QuickActionsRow(
                onWorkout = onStartWorkout,
                onScanMeal = onLogMeal,
                onTrackProgress = onNutritionDetails
            )
        }
        // Body Metrics Card
        item {
            BodyMetricsCard(
                heightCm = state.heightCm,
                weightKg = state.weightKg,
                targetWeightKg = state.targetWeightKg,
                bmi = state.bmi,
                onEdit = onEditBodyMetrics
            )
        }
        item { StreakCard(state.streak) }
        item {
            TodayTasksSection(
                state = state,
                onAddTask = { showAddTaskDialog = true },
                onAddPresetTask = onAddPresetTask,
                onSetTaskStatus = onSetTaskStatus,
                onToggleTaskReminder = onToggleTaskReminder,
                onDeleteTask = onDeleteTask
            )
        }
        item { WorkoutTodayCard(state.workout, onStart = onStartWorkout, onDetails = onWorkoutDetails) }
        item { NutritionSummaryCard(state.nutrition, onLogMeal = onLogMeal, onDetails = onNutritionDetails) }
        item { AIInsightCard(state.insight, onAskCoach = onAskCoach) }
        item { AchievementPreviewCard(state.achievement) }
        item { Spacer(modifier = Modifier.height(90.dp)) }
    }

    // Edit body metrics dialog
    if (state.showEditBodyDialog) {
        EditBodyMetricsDialog(
            currentHeightCm = state.heightCm,
            currentWeightKg = state.weightKg,
            currentTargetWeightKg = state.targetWeightKg,
            onDismiss = onDismissEditBody,
            onSave = onSaveBodyMetrics
        )
    }
}

@Composable
private fun HomeTopBar(state: HomeUiState, onNotificationClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Gradient avatar with glow
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(12.dp, CircleShape, ambientColor = FittyPink.copy(alpha = 0.3f), spotColor = FittyPink.copy(alpha = 0.25f))
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(FittyGradientStart, FittyGradientEnd))),
                contentAlignment = Alignment.Center
            ) {
                if (state.avatarUrl.isNullOrBlank()) {
                    Text(
                        text = state.avatarInitial,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    AsyncImage(
                        model = state.avatarUrl,
                        contentDescription = state.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(52.dp).clip(CircleShape)
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.home_greeting_hi_with_name, state.displayName),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.greetingSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Notification bell — no border/background
        Box(modifier = Modifier.size(48.dp)) {
            IconButton(onClick = onNotificationClick) {
                Icon(imageVector = Icons.Outlined.Notifications, contentDescription = stringResource(R.string.home_notifications_content_desc), tint = MaterialTheme.colorScheme.onSurface)
            }
            if (state.unreadNotificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 4.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(FittyPink),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.unreadNotificationCount.coerceAtMost(99).toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onWorkout: () -> Unit,
    onScanMeal: () -> Unit,
    onTrackProgress: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionChip(
            icon = Icons.Outlined.FitnessCenter,
            label = stringResource(R.string.home_quick_workout),
            onClick = onWorkout,
            modifier = Modifier.weight(1f)
        )
        QuickActionChip(
            icon = Icons.Outlined.Restaurant,
            label = stringResource(R.string.home_quick_scan_meal),
            onClick = onScanMeal,
            modifier = Modifier.weight(1f)
        )
        QuickActionChip(
            icon = Icons.Outlined.BarChart,
            label = stringResource(R.string.home_quick_progress),
            onClick = onTrackProgress,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(FittyPink.copy(alpha = 0.12f), FittyPinkLight.copy(alpha = 0.08f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = FittyPink, modifier = Modifier.size(22.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TodaySummaryCard(state: HomeUiState, onStartToday: () -> Unit = {}, onViewPlan: () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            FittyGradientStart,
                            FittyGradientEnd,
                            FittyPink.copy(alpha = 0.9f)
                        )
                    )
                )
        ) {
            // Decorative circle overlays
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = (-30).dp, y = (-20).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            )
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            state.focusTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            state.focusDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    // Circular daily progress
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val metricsProgress = state.focusMetrics.firstOrNull()?.value?.let {
                            val parts = it.split("/")
                            if (parts.size == 2) {
                                val current = parts[0].trim().toFloatOrNull() ?: 0f
                                val total = parts[1].trim().toFloatOrNull() ?: 1f
                                if (total > 0) current / total else 0f
                            } else 0f
                        } ?: 0f
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(60.dp),
                            strokeWidth = 5.dp,
                            color = Color.White.copy(alpha = 0.2f),
                            trackColor = Color.Transparent
                        )
                        CircularProgressIndicator(
                            progress = { metricsProgress },
                            modifier = Modifier.size(60.dp),
                            strokeWidth = 5.dp,
                            color = Color.White,
                            trackColor = Color.Transparent
                        )
                        Icon(
                            Icons.Outlined.SelfImprovement,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                // Metrics row with pill backgrounds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.focusMetrics.take(3).forEach { metric ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    metric.value,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    metric.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onStartToday,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = FittyPink),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text(state.focusPrimaryActionLabel, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onViewPlan,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text(state.focusSecondaryActionLabel, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusMetric(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayTasksSection(
    state: HomeUiState,
    onAddTask: () -> Unit,
    onAddPresetTask: (HomeTaskPresetUi) -> Unit,
    onSetTaskStatus: (Long, HomeTaskStatus) -> Unit,
    onToggleTaskReminder: (Long, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit
) {
    val presetTasks = remember(state.tasks, state.presetTasks) {
        state.presetTasks.filterNot { preset ->
            state.tasks.any { task ->
                task.title.equals(preset.title, ignoreCase = true) &&
                    task.time == formatTaskTime(preset.timeMinutes)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.tasksSectionTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            TextButton(onClick = onAddTask) {
                Text(
                    text = "+ ${state.tasksSectionActionLabel}",
                    color = FittyPink,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (presetTasks.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_task_quick_add_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetTasks.take(4).forEach { preset ->
                            AssistChip(
                                onClick = { onAddPresetTask(preset) },
                                label = { Text("${preset.title} • ${formatTaskTime(preset.timeMinutes)}") }
                            )
                        }
                    }
                }
            }
        }
        if (state.tasks.isEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.home_tasks_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.tasks.forEach { task ->
                    TaskCard(
                        task = task,
                        onSelectStatus = { status -> onSetTaskStatus(task.id, status) },
                        onToggleReminder = { enabled -> onToggleTaskReminder(task.id, enabled) },
                        onDelete = { onDeleteTask(task.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskCard(
    task: HomeTaskUi,
    onSelectStatus: (HomeTaskStatus) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val icon = when (task.category) {
        HomeTaskCategory.Workout -> Icons.Outlined.FitnessCenter
        HomeTaskCategory.Meal -> Icons.Outlined.Restaurant
        HomeTaskCategory.Water -> Icons.Outlined.WaterDrop
        HomeTaskCategory.Custom -> Icons.Outlined.SelfImprovement
    }
    val completed = task.status == HomeTaskStatus.Completed
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.status == HomeTaskStatus.InProgress) 5.dp else 2.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (completed) FittyPink.copy(alpha = 0.18f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (completed) FittyPink.copy(alpha = 0.10f) else FittyPinkLight.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (completed) Icons.Outlined.CheckCircle else icon,
                        contentDescription = null,
                        tint = FittyPink,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (task.description.isNotBlank()) {
                        Text(task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(task.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(task.status.toDisplayLabel(context), style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(14.dp)
                )
                if (!task.isPlanBacked) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TaskStatusOption(
                        selected = task.status == HomeTaskStatus.Todo,
                        label = stringResource(R.string.home_task_status_todo),
                        onClick = { if (!task.isPlanBacked) onSelectStatus(HomeTaskStatus.Todo) }
                    )
                    TaskStatusOption(
                        selected = task.status == HomeTaskStatus.InProgress,
                        label = stringResource(R.string.home_task_status_in_progress),
                        onClick = { if (!task.isPlanBacked) onSelectStatus(HomeTaskStatus.InProgress) }
                    )
                    TaskStatusOption(
                        selected = task.status == HomeTaskStatus.Completed,
                        label = stringResource(R.string.home_task_status_completed),
                        onClick = { if (!task.isPlanBacked) onSelectStatus(HomeTaskStatus.Completed) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = if (task.reminderEnabled) FittyPink else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            stringResource(R.string.home_task_reminder),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = task.reminderEnabled,
                        onCheckedChange = onToggleReminder,
                        enabled = !task.isPlanBacked,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = FittyPink,
                            uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakCard(state: HomeStreakUi) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FittySectionHeader(state.sectionTitle)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                }
                Column {
                    Text(state.currentLabel, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(state.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.dayLabels.forEachIndexed { index, day ->
                    val isActive = state.activeDayFlags.getOrElse(index) { false }
                    DayIndicator(day, active = isActive, current = index == state.currentDayIndex)
                }
            }
            Text(state.bestLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DayIndicator(day: String, active: Boolean, current: Boolean) {
    val color = when {
        current -> FittyPink
        active -> FittyPinkLight
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(day, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WorkoutTodayCard(workout: HomeWorkoutUi, onStart: () -> Unit = {}, onDetails: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FittySectionHeader(workout.sectionTitle)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        ) {
            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(workout.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(workout.meta, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    if (workout.exerciseSummaries.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.home_workout_plan_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            workout.exerciseSummaries.take(3).forEach { summary ->
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    AssistChip(onClick = onDetails, label = { Text(workout.equipmentLabel) })
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onStart, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = FittyPink), modifier = Modifier.weight(1f)) {
                            Text(workout.primaryActionLabel)
                        }
                        OutlinedButton(onClick = onDetails, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                            Text(workout.secondaryActionLabel)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FittyPink.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = FittyPink, modifier = Modifier.size(30.dp))
                }
            }
        }
    }
}

@Composable
private fun NutritionSummaryCard(nutrition: HomeNutritionUi, onLogMeal: () -> Unit = {}, onDetails: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FittySectionHeader(nutrition.sectionTitle)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(nutrition.summary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                nutrition.macros.forEach { macro ->
                    MacroProgress(label = macro.label, progress = macro.progress)
                }
                nutrition.meals.forEach { meal ->
                    MealRow(label = meal.label, calories = meal.calories)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onLogMeal, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = FittyPink), modifier = Modifier.weight(1f)) {
                        Text(nutrition.primaryActionLabel)
                    }
                    OutlinedButton(onClick = onDetails, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                        Text(nutrition.secondaryActionLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroProgress(label: String, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = FittyPink, trackColor = FittyPink.copy(alpha = 0.12f))
    }
}

@Composable
private fun MealRow(label: String, calories: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(calories, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AIInsightCard(insight: HomeInsightUi, onAskCoach: () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(FittyPink.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Psychology, contentDescription = null, tint = FittyPink, modifier = Modifier.size(20.dp))
                }
                Text(insight.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(insight.message, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                insight.actions.forEach { actionLabel ->
                    AssistChip(onClick = onAskCoach, label = { Text(actionLabel) })
                }
            }
        }
    }
}

@Composable
private fun AchievementPreviewCard(achievement: HomeAchievementPreviewUi) {
    Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.tertiaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.LocalDining, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(achievement.itemLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(achievement.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text(achievement.actionLabel, color = FittyPink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun initialHomeUiState(context: Context, content: HomeContentConfig): HomeUiState {
    return HomeUiState(
        isLoading = true,
        displayName = context.getString(R.string.home_display_name_default),
        avatarUrl = null,
        greetingTitle = context.getString(R.string.home_loading_profile),
        greetingSubtitle = context.getString(R.string.home_subtitle_default),
        focusTitle = context.getString(R.string.home_focus_title),
        focusDescription = context.getString(R.string.home_focus_description_default),
        focusMetrics = listOf(
            HomeFocusMetricUi(context.getString(R.string.home_metric_workout), "--"),
            HomeFocusMetricUi(context.getString(R.string.home_metric_meals_logged), "--"),
            HomeFocusMetricUi(context.getString(R.string.home_metric_water), "--")
        ),
        focusPrimaryActionLabel = context.getString(R.string.home_action_start_today),
        focusSecondaryActionLabel = context.getString(R.string.home_action_view_plan),
        tasksSectionTitle = context.getString(R.string.home_tasks_title),
        tasksSectionActionLabel = context.getString(R.string.home_action_add_task),
        tasks = emptyList(),
        streak = buildStreakUi(
            context = context,
            currentStreak = 0,
            bestStreak = 0,
            lastActiveDate = "",
            activeDates = emptyList()
        ),
        workout = HomeWorkoutUi(
            sectionTitle = context.getString(R.string.home_workout_section_title),
            name = content.emptyState.workoutTitle,
            meta = content.emptyState.workoutBody,
            exerciseSummaries = emptyList(),
            equipmentLabel = context.getString(R.string.home_equipment_default),
            primaryActionLabel = context.getString(R.string.home_action_start),
            secondaryActionLabel = context.getString(R.string.home_action_details)
        ),
        nutrition = emptyNutrition(context),
        insight = HomeInsightUi(
            title = context.getString(R.string.home_insight_title),
            message = content.emptyState.insightMessage,
            actions = content.insightActions
        ),
        achievement = HomeAchievementPreviewUi(
            sectionTitle = context.getString(R.string.home_achievement_item_label),
            itemLabel = context.getString(R.string.home_achievement_item_label),
            subtitle = content.emptyState.achievementMessage,
            actionLabel = context.getString(R.string.home_action_view_all)
        ),
        notifications = emptyList(),
        presetTasks = emptyList()
    )
}

private fun FittyUser.toHomeUiState(context: Context, content: HomeContentConfig): HomeUiState {
    val resolvedName = preferredDisplayName(
        defaultValue = context.getString(R.string.home_display_name_default)
    )
    val durationMinutes = onboarding.workoutDurationMinutes
    val goalLabel = profile.primaryGoal.toDisplayLabel(defaultValue = context.getString(R.string.home_goal_default))
    val fitnessLabel = profile.fitnessLevel.toDisplayLabel(defaultValue = context.getString(R.string.home_fitness_default))
    val equipmentLabel = onboarding.equipmentAccess.toDisplayLabel(defaultValue = context.getString(R.string.home_equipment_default))
    val workoutDays = onboarding.workoutDays.formatWorkoutDays(context)
    val workoutMetaParts = buildList {
        add(durationMinutes?.let { "$it min" } ?: context.getString(R.string.home_duration_not_set))
        add(fitnessLabel)
        add(workoutDays)
    }

    return HomeUiState(
        isLoading = false,
        displayName = resolvedName,
        avatarInitial = resolvedName.firstOrNull()?.uppercaseChar()?.toString() ?: "F",
        avatarUrl = photoUrl,
        greetingTitle = context.getString(
            R.string.home_greeting_title_with_name,
            resolvedName
        ),
        greetingSubtitle = if (guest) context.getString(R.string.home_guest_subtitle) else context.getString(R.string.home_subtitle_default),
        focusTitle = context.getString(R.string.home_focus_title),
        focusDescription = durationMinutes?.let {
            context.getString(R.string.home_focus_with_duration, it, goalLabel.lowercase(Locale.US))
        } ?: context.getString(R.string.home_focus_without_duration, goalLabel.lowercase(Locale.US)),
        focusMetrics = listOf(
            HomeFocusMetricUi(label = context.getString(R.string.home_metric_workout), value = "--"),
            HomeFocusMetricUi(label = context.getString(R.string.home_metric_meals_logged), value = "--"),
            HomeFocusMetricUi(label = context.getString(R.string.home_metric_water), value = "--")
        ),
        focusPrimaryActionLabel = context.getString(R.string.home_action_start_today),
        focusSecondaryActionLabel = context.getString(R.string.home_action_view_plan),
        tasksSectionTitle = context.getString(R.string.home_tasks_title),
        tasksSectionActionLabel = context.getString(R.string.home_action_add_task),
        tasks = emptyList(),
        streak = buildStreakUi(
            context = context,
            currentStreak = stats.currentStreak,
            bestStreak = stats.bestStreak,
            lastActiveDate = stats.lastActiveDate,
            activeDates = stats.streakActiveDates
        ),
        workout = HomeWorkoutUi(
            sectionTitle = context.getString(R.string.home_workout_section_title),
            name = content.emptyState.workoutTitle,
            meta = if (onboardingCompleted) {
                context.getString(
                    R.string.home_workout_waiting_plan_body,
                    workoutMetaParts[0],
                    workoutMetaParts[1],
                    workoutMetaParts[2]
                )
            } else {
                context.getString(R.string.home_workout_empty_body)
            },
            exerciseSummaries = emptyList(),
            equipmentLabel = equipmentLabel,
            primaryActionLabel = context.getString(R.string.home_action_start),
            secondaryActionLabel = context.getString(R.string.home_action_details)
        ),
        nutrition = emptyNutrition(context = context),
        insight = HomeInsightUi(
            title = context.getString(R.string.home_insight_title),
            message = content.emptyState.insightMessage,
            actions = content.insightActions
        ),
        achievement = HomeAchievementPreviewUi(
            sectionTitle = context.getString(R.string.home_achievement_item_label),
            itemLabel = context.getString(R.string.home_achievement_item_label),
            subtitle = if (stats.achievementsUnlocked > 0) {
                context.getString(R.string.home_achievement_count, stats.achievementsUnlocked)
            } else {
                content.emptyState.achievementMessage
            },
            actionLabel = context.getString(R.string.home_action_view_all)
        ),
        notifications = emptyList(),
        presetTasks = emptyList()
    )
}

private fun HomeUiState.applyContent(context: Context, content: HomeContentConfig): HomeUiState {
    val emptyWorkout = workout.copy(
        name = workout.name.ifBlank { content.emptyState.workoutTitle },
        meta = workout.meta.ifBlank { content.emptyState.workoutBody }
    )
    return copy(
        workout = emptyWorkout,
        insight = insight.copy(
            message = insight.message.ifBlank { content.emptyState.insightMessage },
            actions = if (insight.actions.isEmpty()) content.insightActions else insight.actions
        ),
        achievement = achievement.copy(
            subtitle = achievement.subtitle.ifBlank { content.emptyState.achievementMessage }
        ),
        presetTasks = buildPresetTasks(
            workout = emptyWorkout,
            content = content
        )
    )
}

private fun buildPresetTasks(
    workout: HomeWorkoutUi,
    content: HomeContentConfig
): List<HomeTaskPresetUi> {
    val workoutTitle = workout.name.takeIf {
        it.isNotBlank() && it != workout.sectionTitle
    } ?: content.suggestedTaskPresets.firstOrNull { it.category == HomeTaskCategory.Workout }?.title.orEmpty()
    return content.suggestedTaskPresets.map { template ->
        val title = if (template.category == HomeTaskCategory.Workout) workoutTitle else template.title
        val description = if (template.category == HomeTaskCategory.Workout && workout.meta.isNotBlank()) {
            workout.meta
        } else {
            template.description
        }
        HomeTaskPresetUi(
            title = title,
            description = description,
            timeMinutes = template.timeMinutes,
            category = template.category,
            reminderEnabled = template.reminderEnabled
        )
    }
}

private fun buildDefaultTasks(
    context: Context,
    user: FittyUser,
    dateKey: String,
    workoutTitle: String?,
    content: HomeContentConfig
): List<HomeTaskDraft> {
    val preferredWorkoutTime = preferredWorkoutMinutes(user.onboarding.preferredTime)
    val includeWorkoutTask = workoutTitle?.isNotBlank() == true || user.onboarding.workoutDays.isEmpty() ||
        user.onboarding.workoutDays.matchesToday()
    val defaults = mutableListOf<HomeTaskDraft>()

    content.defaultTaskTemplates.forEach { template ->
        if (template.category == HomeTaskCategory.Workout && !includeWorkoutTask) return@forEach
        val resolvedTitle = if (template.category == HomeTaskCategory.Workout) {
            workoutTitle?.takeIf { it.isNotBlank() } ?: template.title
        } else {
            template.title
        }
        val resolvedDescription = if (template.category == HomeTaskCategory.Workout && workoutTitle?.isNotBlank() == true) {
            context.getString(R.string.home_task_session_desc, workoutTitle)
        } else {
            template.description
        }
        defaults += HomeTaskDraft(
            title = resolvedTitle,
            description = resolvedDescription,
            dateKey = dateKey,
            timeMinutes = if (template.category == HomeTaskCategory.Workout) preferredWorkoutTime else template.timeMinutes,
            category = template.category,
            reminderEnabled = template.reminderEnabled,
            isDefault = true
        )
    }

    return defaults
}

private fun emptyNutrition(context: Context, caloriesTarget: Int? = null): HomeNutritionUi {
    return HomeNutritionUi(
        sectionTitle = context.getString(R.string.home_nutrition_section_title),
        summary = caloriesTarget?.let {
            context.getString(R.string.home_nutrition_summary, 0, it)
        } ?: context.getString(R.string.home_nutrition_summary_unknown),
        macros = listOf(
            HomeMacroProgressUi(context.getString(R.string.home_macro_protein), 0f),
            HomeMacroProgressUi(context.getString(R.string.home_macro_carbs), 0f),
            HomeMacroProgressUi(context.getString(R.string.home_macro_fat), 0f)
        ),
        meals = listOf(
            HomeMealUi(context.getString(R.string.home_meal_breakfast), "—"),
            HomeMealUi(context.getString(R.string.home_meal_lunch), "—"),
            HomeMealUi(context.getString(R.string.home_meal_snack), "—")
        ),
        primaryActionLabel = context.getString(R.string.home_action_log_meal),
        secondaryActionLabel = context.getString(R.string.home_action_details)
    )
}

private fun com.example.fitty.domain.model.WorkoutExercise.toHomeWorkoutExerciseSummary(
    context: Context
): String {
    val repsLabel = reps?.takeIf { it.isNotBlank() } ?: context.getString(R.string.workout_reps_not_set)
    val weightLabel = targetWeightKg?.let { weight ->
        if (weight % 1f == 0f) "${weight.toInt()} kg" else String.format(Locale.US, "%.1f kg", weight)
    }
    val prescription = when {
        sets > 0 && weightLabel != null -> context.getString(
            R.string.workout_sets_summary_with_weight,
            sets,
            repsLabel,
            weightLabel
        )
        sets > 0 -> context.getString(R.string.workout_sets_summary, sets, repsLabel)
        durationSeconds != null -> context.getString(R.string.workout_seconds_format, durationSeconds)
        else -> repsLabel
    }
    return "${name.ifBlank { exerciseId }}: $prescription"
}

/**
 * Builds streak UI with actual weekday labels and per-day active flags.
 * Shows Mon-Sun for the current week, with today highlighted.
 */
private fun buildStreakUi(
    context: Context,
    currentStreak: Int,
    bestStreak: Int,
    lastActiveDate: String,
    activeDates: List<String>
): HomeStreakUi {
    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val todayDateKey = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val isActiveToday = lastActiveDate == todayDateKey || activeDates.contains(todayDateKey)
    val normalizedCurrentStreak = if (currentStreak == 0 && isActiveToday) 1 else currentStreak
    val dayLabels = listOf("2", "3", "4", "5", "6", "7", "CN")
    val todayIndex = (today.dayOfWeek.value - 1).coerceIn(0, 6)

    // Build active flags: check if each day of this week is in activeDates
    val activeDayFlags = (0..6).map { offset ->
        val dateStr = startOfWeek.plusDays(offset.toLong())
            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        activeDates.contains(dateStr)
    }

    return HomeStreakUi(
        sectionTitle = context.getString(R.string.home_streak_title),
        currentLabel = context.getString(R.string.home_streak_current, normalizedCurrentStreak),
        subtitle = context.getString(R.string.home_streak_subtitle),
        bestLabel = context.getString(R.string.home_streak_best, maxOf(normalizedCurrentStreak, bestStreak)),
        dayLabels = dayLabels,
        activeDayFlags = activeDayFlags,
        currentDayIndex = todayIndex
    )
}

@Composable
private fun NotificationDialog(
    notifications: List<HomeNotificationUi>,
    unreadCount: Int,
    onDismiss: () -> Unit,
    onMarkRead: (Long) -> Unit,
    onMarkAllRead: () -> Unit,
    onDelete: (Long) -> Unit
) {
    var showUnreadOnly by rememberSaveable { mutableStateOf(false) }
    val visibleNotifications = remember(notifications, showUnreadOnly) {
        if (showUnreadOnly) notifications.filter { !it.isRead } else notifications
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FittyPink.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = FittyPink, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(stringResource(R.string.home_notifications_heading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (unreadCount > 0) {
                                Text(stringResource(R.string.home_notifications_unread_count, unreadCount), style = MaterialTheme.typography.labelSmall, color = FittyPink)
                            }
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.home_notifications_close), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NotificationTab(
                        label = stringResource(R.string.home_notifications_filter_all),
                        selected = !showUnreadOnly,
                        modifier = Modifier.weight(1f),
                        onClick = { showUnreadOnly = false }
                    )
                    NotificationTab(
                        label = stringResource(R.string.home_notifications_filter_unread),
                        selected = showUnreadOnly,
                        modifier = Modifier.weight(1f),
                        onClick = { showUnreadOnly = true }
                    )
                }

                // Mark all read
                if (unreadCount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(stringResource(R.string.home_notifications_mark_all), style = MaterialTheme.typography.labelSmall, color = FittyPink, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onMarkAllRead))
                    }
                }

                // Notification items
                if (visibleNotifications.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        visibleNotifications.forEach { notification ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (notification.isRead) Color.Transparent else FittyPink.copy(alpha = 0.04f))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(contentAlignment = Alignment.TopStart) {
                                    Box(
                                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(FittyPink.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(notification.icon, contentDescription = null, tint = FittyPink, modifier = Modifier.size(18.dp))
                                    }
                                    if (!notification.isRead) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FittyPink))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(notification.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f))
                                        Text(notification.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(notification.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        if (!notification.isRead) {
                                            Text(stringResource(R.string.home_notification_read), style = MaterialTheme.typography.labelSmall, color = FittyPink, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onMarkRead(notification.id) })
                                        }
                                        Text(stringResource(R.string.home_notification_delete), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.clickable { onDelete(notification.id) })
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty state
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(FittyPink.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = FittyPinkLight, modifier = Modifier.size(28.dp))
                        }
                        Text(stringResource(R.string.home_notifications_empty_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.home_notifications_empty_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) FittyPink else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TaskStatusOption(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int, HomeTaskCategory, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(HomeTaskCategory.Custom) }
    var reminderEnabled by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val categoryOptions = remember {
        listOf(
            HomeTaskCategory.Workout,
            HomeTaskCategory.Meal,
            HomeTaskCategory.Water,
            HomeTaskCategory.Custom
        )
    }
    val titleRequiredMessage = stringResource(R.string.home_task_title_required)
    val invalidTimeMessage = stringResource(R.string.home_task_invalid_time)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_add_task_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.home_task_field_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = stringResource(R.string.home_task_category),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    categoryOptions.chunked(2).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowOptions.forEach { option ->
                                AddTaskCategoryOption(
                                    label = option.toLabel(context),
                                    icon = option.toIcon(),
                                    selected = category == option,
                                    onClick = { category = option },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowOptions.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.home_task_field_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                OutlinedTextField(
                    value = timeText,
                    onValueChange = {
                        timeText = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.home_task_field_time)) },
                    placeholder = { Text(stringResource(R.string.home_task_field_time_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = if (reminderEnabled) FittyPink else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        stringResource(R.string.home_task_reminder),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = FittyPink,
                            uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                        )
                    )
                }
                errorMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.home_task_cancel))
                    }
                    Button(
                        onClick = {
                            val timeMinutes = parseTimeToMinutes(timeText)
                            when {
                                title.isBlank() -> errorMessage = titleRequiredMessage
                                timeMinutes == null -> errorMessage = invalidTimeMessage
                                else -> onSave(
                                    title.trim(),
                                    description.trim(),
                                    timeMinutes,
                                    category,
                                    reminderEnabled
                                )
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FittyPink),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.home_task_save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTaskCategoryOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val borderColor = if (selected) {
        FittyPink.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
    }
    val iconContainer = if (selected) {
        FittyPink.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = modifier
            .height(62.dp)
            .clip(shape)
            .background(
                brush = if (selected) {
                    Brush.horizontalGradient(
                        listOf(
                            FittyPink.copy(alpha = 0.18f),
                            FittyPinkLight.copy(alpha = 0.42f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
                        )
                    )
                },
                shape = shape
            )
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) FittyPink else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) FittyPink else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun insightMessageFor(context: Context, goalLabel: String, mealsLogged: Int): String {
    return when {
        mealsLogged == 0 -> context.getString(R.string.home_insight_no_meals)
        goalLabel.contains("Muscle", ignoreCase = true) -> context.getString(R.string.home_insight_gain_muscle, goalLabel)
        else -> context.getString(R.string.home_insight_default, goalLabel)
    }
}

private fun estimateCaloriesLogged(): Int = 1240

private fun greetingForNow(context: Context): String {
    val h = LocalTime.now().hour
    return when {
        h < 12 -> context.getString(R.string.home_good_morning)
        h < 18 -> context.getString(R.string.home_good_afternoon)
        else -> context.getString(R.string.home_good_evening)
    }
}

private fun HomeUiState.preserveDynamicState(current: HomeUiState): HomeUiState = copy(
    tasks = current.tasks,
    customTasks = current.customTasks,
    planWorkoutTask = current.planWorkoutTask,
    notifications = current.notifications,
    unreadNotificationCount = current.unreadNotificationCount,
    showNotifications = current.showNotifications,
    contentSources = current.contentSources,
    // Always preserve real tracking data loaded by loadTodaySummary so refreshUser doesn't overwrite
    focusMetrics = current.focusMetrics,
    nutrition = current.nutrition
)

private fun mergeHomeTasks(
    customTasks: List<HomeTaskUi>,
    planWorkoutTask: HomeTaskUi?
): List<HomeTaskUi> {
    return listOfNotNull(planWorkoutTask) + customTasks
}

private fun buildPlanWorkoutTask(
    user: FittyUser,
    workout: ScheduledWorkout?,
    workoutsCompleted: Int
): HomeTaskUi? {
    val resolvedWorkout = workout ?: return null
    if (resolvedWorkout.title.isBlank()) return null
    val timeMinutes = preferredWorkoutMinutes(user.onboarding.preferredTime)
    val status = if (workoutsCompleted > 0 || resolvedWorkout.status.equals("completed", ignoreCase = true)) {
        HomeTaskStatus.Completed
    } else {
        HomeTaskStatus.Todo
    }
    return HomeTaskUi(
        id = PLAN_WORKOUT_TASK_ID,
        category = HomeTaskCategory.Workout,
        title = resolvedWorkout.title,
        description = contextWorkoutTaskDescription(resolvedWorkout),
        time = formatTaskTime(timeMinutes),
        status = status,
        reminderEnabled = false,
        isPlanBacked = true
    )
}

private fun contextWorkoutTaskDescription(workout: ScheduledWorkout): String {
    return listOf(
        "${workout.durationMinutes} min",
        workout.difficulty.ifBlank { workout.equipment }.ifBlank { workout.title }
    ).joinToString(" • ")
}

private const val PLAN_WORKOUT_TASK_ID = -1L

private fun HomeTaskStatus.toDisplayLabel(context: Context): String = when (this) {
    HomeTaskStatus.Todo -> context.getString(R.string.home_task_status_todo)
    HomeTaskStatus.InProgress -> context.getString(R.string.home_task_status_in_progress)
    HomeTaskStatus.Completed -> context.getString(R.string.home_task_status_completed)
}

private fun AppNotificationType.toNotificationIcon(): ImageVector = when (this) {
    AppNotificationType.Workout -> Icons.Outlined.FitnessCenter
    AppNotificationType.Meal -> Icons.Outlined.Restaurant
    AppNotificationType.Reminder -> Icons.Outlined.Notifications
    AppNotificationType.Streak -> Icons.Outlined.LocalFireDepartment
    AppNotificationType.Progress -> Icons.Outlined.BarChart
    AppNotificationType.General -> Icons.Outlined.Notifications
}

private fun HomeTaskCategory.toLabel(context: Context): String = when (this) {
    HomeTaskCategory.Workout -> context.getString(R.string.home_task_category_workout)
    HomeTaskCategory.Meal -> context.getString(R.string.home_task_category_meal)
    HomeTaskCategory.Water -> context.getString(R.string.home_task_category_water)
    HomeTaskCategory.Custom -> context.getString(R.string.home_task_category_custom)
}

private fun HomeTaskCategory.toIcon(): ImageVector = when (this) {
    HomeTaskCategory.Workout -> Icons.Outlined.FitnessCenter
    HomeTaskCategory.Meal -> Icons.Outlined.Restaurant
    HomeTaskCategory.Water -> Icons.Outlined.WaterDrop
    HomeTaskCategory.Custom -> Icons.Outlined.SelfImprovement
}

private fun formatTaskTime(timeMinutes: Int): String {
    val normalized = timeMinutes.coerceIn(0, 23 * 60 + 59)
    val time = LocalTime.of(normalized / 60, normalized % 60)
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun formatNotificationTime(context: Context, createdAt: Long): String {
    val diff = System.currentTimeMillis() - createdAt
    return when {
        diff < 60_000L -> context.getString(R.string.home_notification_just_now)
        diff < 3_600_000L -> context.getString(R.string.home_notification_minutes_ago, diff / 60_000L)
        diff < 86_400_000L -> context.getString(R.string.home_notification_hours_ago, diff / 3_600_000L)
        else -> LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(createdAt),
            ZoneId.systemDefault()
        ).format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
    }
}

private fun parseTimeToMinutes(value: String): Int? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun String.toDisplayLabel(defaultValue: String): String {
    if (isBlank()) return defaultValue
    return split('_', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
            }
        }
}

private fun String.toLocalizedToken(context: Context, defaultValue: String = toDisplayLabel(this)): String {
    return when (trim().lowercase(Locale.ROOT)) {
        "breakfast" -> context.getString(R.string.home_meal_breakfast)
        "lunch" -> context.getString(R.string.home_meal_lunch)
        "dinner" -> context.getString(R.string.home_meal_dinner)
        "snack" -> context.getString(R.string.home_meal_snack)
        "other" -> context.getString(R.string.home_meal_type_other)
        "easy" -> context.getString(R.string.common_label_easy)
        "medium" -> context.getString(R.string.common_label_medium)
        "hard" -> context.getString(R.string.common_label_hard)
        "beginner" -> context.getString(R.string.plan_level_beginner)
        "intermediate" -> context.getString(R.string.common_label_intermediate)
        "advanced" -> context.getString(R.string.common_label_advanced)
        "monday" -> context.getString(R.string.common_day_monday)
        "tuesday" -> context.getString(R.string.common_day_tuesday)
        "wednesday" -> context.getString(R.string.common_day_wednesday)
        "thursday" -> context.getString(R.string.common_day_thursday)
        "friday" -> context.getString(R.string.common_day_friday)
        "saturday" -> context.getString(R.string.common_day_saturday)
        "sunday" -> context.getString(R.string.common_day_sunday)
        else -> toDisplayLabel(defaultValue)
    }
}

private fun List<String>.formatWorkoutDays(context: Context): String {
    if (isEmpty()) return context.getString(R.string.home_choose_workout_days)
    return joinToString(", ") { value -> value.toLocalizedToken(context) }
}

private fun List<String>.matchesToday(): Boolean {
    if (isEmpty()) return false
    val today = LocalDate.now().dayOfWeek
    val shortName = today.getDisplayName(TextStyle.SHORT, Locale.getDefault()).lowercase(Locale.ROOT)
    val fullName = today.getDisplayName(TextStyle.FULL, Locale.getDefault()).lowercase(Locale.ROOT)
    return any { value ->
        val normalized = value.trim().lowercase(Locale.ROOT)
        normalized == shortName || normalized == fullName
    }
}

private fun preferredWorkoutMinutes(preferredTime: String): Int {
    return when (preferredTime.trim().lowercase(Locale.ROOT)) {
        "morning" -> 7 * 60
        "afternoon" -> 13 * 60
        "evening" -> 18 * 60
        else -> 18 * 60
    }
}

@Composable
private fun BodyMetricsCard(
    heightCm: Int?,
    weightKg: Int?,
    targetWeightKg: Int?,
    bmi: Float?,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(FittyPink.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.AccessibilityNew, contentDescription = null, tint = FittyPink, modifier = Modifier.size(22.dp))
                    }
                    Text(stringResource(R.string.home_body_metrics_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.home_edit), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium) }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BodyMetricTile(value = heightCm?.toString() ?: "--", unit = "cm", label = stringResource(R.string.home_body_metric_height), modifier = Modifier.weight(1f))
                BodyMetricTile(value = weightKg?.toString() ?: "--", unit = "kg", label = stringResource(R.string.home_body_metric_weight), modifier = Modifier.weight(1f))
                BodyMetricTile(value = bmi?.let { "%.1f".format(it) } ?: "--", unit = "", label = stringResource(R.string.home_body_metric_bmi), modifier = Modifier.weight(1f))
            }
            if (targetWeightKg != null && weightKg != null) {
                val progress = when {
                    targetWeightKg == weightKg -> 1f
                    targetWeightKg > weightKg -> (weightKg.toFloat() / targetWeightKg.toFloat()).coerceIn(0f, 1f)
                    else -> (targetWeightKg.toFloat() / weightKg.toFloat()).coerceIn(0f, 1f)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.home_target_weight, targetWeightKg), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(((progress * 100).toInt()).toString() + "%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = FittyPink)
                    }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = FittyPink, trackColor = FittyPink.copy(alpha = 0.12f))
                }
            }
        }
    }
}

@Composable
private fun BodyMetricTile(value: String, unit: String, label: String, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (unit.isNotBlank()) Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EditBodyMetricsDialog(currentHeightCm: Int?, currentWeightKg: Int?, currentTargetWeightKg: Int?, onDismiss: () -> Unit, onSave: (Int?, Int?, Int?) -> Unit) {
    var height by remember { mutableStateOf(currentHeightCm?.toString() ?: "") }
    var weight by remember { mutableStateOf(currentWeightKg?.toString() ?: "") }
    var targetWeight by remember { mutableStateOf(currentTargetWeightKg?.toString() ?: "") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(FittyPink.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AccessibilityNew, null, tint = FittyPink, modifier = Modifier.size(24.dp)) }
                    Text(stringResource(R.string.home_update_body_metrics), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(value = height, onValueChange = { height = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.home_body_input_height)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.home_body_input_weight)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = targetWeight, onValueChange = { targetWeight = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.home_body_input_target_weight)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                val h = height.toIntOrNull(); val w = weight.toIntOrNull()
                if (h != null && h > 0 && w != null && w > 0) {
                    val previewBmi = w.toFloat() / ((h.toFloat() / 100f) * (h.toFloat() / 100f))
                    val bmiCat = when {
                        previewBmi < 18.5f -> stringResource(R.string.home_bmi_underweight)
                        previewBmi < 25f -> stringResource(R.string.home_bmi_normal)
                        previewBmi < 30f -> stringResource(R.string.home_bmi_overweight)
                        else -> stringResource(R.string.home_bmi_obese)
                    }
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = FittyPink.copy(alpha = 0.07f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.home_bmi_preview), style = MaterialTheme.typography.labelMedium)
                            Text("%.1f".format(previewBmi) + " (" + bmiCat + ")", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = FittyPink)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.common_cancel)) }
                    Button(onClick = { onSave(height.toIntOrNull(), weight.toIntOrNull(), targetWeight.toIntOrNull()) }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = FittyPink), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.common_save), fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
