package com.example.fitty.feature_home

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import coil.compose.AsyncImage
import androidx.lifecycle.viewModelScope
import com.example.fitty.R
import com.example.fitty.core.designsystem.component.FittySectionHeader
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.domain.model.AppNotificationType
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.HomeTaskCategory
import com.example.fitty.domain.model.HomeTaskDraft
import com.example.fitty.domain.model.HomeTaskStatus
import com.example.fitty.domain.model.preferredDisplayName
import com.example.fitty.domain.repository.AppNotificationRepository
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
    val equipmentLabel: String,
    val primaryActionLabel: String,
    val secondaryActionLabel: String
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
    val workout: HomeWorkoutUi = HomeWorkoutUi("", "", "", "", "", ""),
    val nutrition: HomeNutritionUi = HomeNutritionUi("", "", emptyList(), emptyList(), "", ""),
    val insight: HomeInsightUi = HomeInsightUi("", "", emptyList()),
    val achievement: HomeAchievementPreviewUi = HomeAchievementPreviewUi("", "", "", ""),
    val showNotifications: Boolean = false,
    val notifications: List<HomeNotificationUi> = emptyList(),
    val unreadNotificationCount: Int = 0
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
    private val homeTaskRepository: HomeTaskRepository,
    private val appNotificationRepository: AppNotificationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialHomeUiState(context))
    val uiState: StateFlow<HomeUiState> = _uiState
    private val todayDateKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    init {
        observeTasks()
        observeNotifications()
        refreshUser()
    }

    fun refreshUser() {
        viewModelScope.launch {
            // Load user for profile display
            runCatching { getCurrentUserUseCase() }
                .onSuccess { user ->
                    val goalLabel = user?.profile?.primaryGoal?.toDisplayLabel(
                        defaultValue = context.getString(R.string.home_goal_default)
                    ) ?: context.getString(R.string.home_default_task_goal)
                    ensureTodayTasks(goalLabel)
                    _uiState.update { current ->
                        if (user == null) current.copy(
                            isLoading = false,
                            greetingTitle = context.getString(
                                R.string.home_greeting_default,
                                context.getString(R.string.home_display_name_default)
                            )
                        ) else user.toHomeUiState(context).preserveDynamicState(current)
                    }
                }
                .onFailure {
                    ensureTodayTasks(context.getString(R.string.home_default_task_goal))
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            greetingTitle = context.getString(
                                R.string.home_greeting_default,
                                context.getString(R.string.home_display_name_default)
                            )
                        )
                    }
                }

            // Load dashboard data (daily summary, today workout, active plan)
            runCatching { getHomeDashboardUseCase() }
                .onSuccess { dashboard ->
                    if (dashboard == null) return@onSuccess
                    _uiState.update { current ->
                        val summary = dashboard.dailySummary
                        val workout = dashboard.todayWorkout
                        current.copy(
                            focusMetrics = listOf(
                                HomeFocusMetricUi(
                                    context.getString(R.string.home_metric_workout),
                                    "${summary?.progress?.workoutsCompleted ?: 0}/1"
                                ),
                                HomeFocusMetricUi(
                                    context.getString(R.string.home_metric_meals_logged),
                                    "${summary?.mealsLoggedCount ?: 0}/3"
                                ),
                                HomeFocusMetricUi(
                                    context.getString(R.string.home_metric_water),
                                    "${summary?.progress?.waterMl ?: 0} / ${summary?.targets?.waterMl ?: 2500} ml"
                                )
                            ),
                            workout = if (workout != null) HomeWorkoutUi(
                                sectionTitle = context.getString(R.string.home_workout_section_title),
                                name = workout.title.ifBlank { dashboard.activePlanName ?: "" },
                                meta = "${workout.durationMinutes} min • ${workout.difficulty}",
                                equipmentLabel = workout.equipment.ifBlank { context.getString(R.string.home_equipment_default) },
                                primaryActionLabel = context.getString(R.string.home_action_start),
                                secondaryActionLabel = context.getString(R.string.home_action_details)
                            ) else current.workout,
                            nutrition = if (summary != null) defaultNutrition(
                                context = context,
                                summary = context.getString(
                                    R.string.home_nutrition_summary,
                                    summary.progress.caloriesConsumed,
                                    summary.targets.calories
                                )
                            ) else current.nutrition,
                            streak = buildStreakUi(
                                context = context,
                                currentStreak = summary?.currentStreak ?: current.streak.currentDayIndex,
                                bestStreak = 0, // from user stats
                                activeDates = emptyList()
                            ),
                            insight = if (summary?.insightText?.isNotBlank() == true) HomeInsightUi(
                                title = context.getString(R.string.home_insight_title),
                                message = summary.insightText,
                                actions = defaultInsightActions(context)
                            ) else current.insight
                        )
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

    internal fun setTaskInProgress(taskId: Long, enabled: Boolean) {
        viewModelScope.launch {
            homeTaskRepository.updateTaskStatus(
                taskId = taskId,
                status = if (enabled) HomeTaskStatus.InProgress else HomeTaskStatus.Todo
            )
        }
    }

    internal fun setTaskCompleted(taskId: Long, enabled: Boolean) {
        viewModelScope.launch {
            homeTaskRepository.updateTaskStatus(
                taskId = taskId,
                status = if (enabled) HomeTaskStatus.Completed else HomeTaskStatus.Todo
            )
        }
    }

    internal fun toggleTaskReminder(taskId: Long, enabled: Boolean) {
        viewModelScope.launch {
            homeTaskRepository.updateTaskReminder(taskId, enabled)
        }
    }

    internal fun deleteTask(taskId: Long) {
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
                _uiState.update { current ->
                    current.copy(tasks = tasks.map { task ->
                        HomeTaskUi(
                            id = task.id,
                            category = task.category,
                            title = task.title,
                            description = task.description,
                            time = formatTaskTime(task.timeMinutes),
                            status = task.status,
                            reminderEnabled = task.reminderEnabled
                        )
                    })
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
                                time = formatNotificationTime(notification.createdAt),
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

    private suspend fun ensureTodayTasks(goalLabel: String) {
        homeTaskRepository.ensureTasks(
            dateKey = todayDateKey,
            defaults = defaultTaskDrafts(context = context, goalLabel = goalLabel)
        )
    }
}

@Composable
fun HomeRoute(
    onNavigateToPlan: () -> Unit = {},
    onNavigateToTrack: () -> Unit = {},
    onNavigateToCoach: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    HomeScreen(
        state = state,
        onStartWorkout = onNavigateToPlan,
        onWorkoutDetails = onNavigateToPlan,
        onLogMeal = onNavigateToTrack,
        onNutritionDetails = onNavigateToTrack,
        onAskCoach = onNavigateToCoach,
        onToggleNotifications = viewModel::toggleNotifications,
        onDismissNotifications = viewModel::dismissNotifications,
        onAddTask = viewModel::addTask,
        onSetTaskInProgress = viewModel::setTaskInProgress,
        onSetTaskCompleted = viewModel::setTaskCompleted,
        onToggleTaskReminder = viewModel::toggleTaskReminder,
        onDeleteTask = viewModel::deleteTask,
        onMarkNotificationRead = viewModel::markNotificationRead,
        onMarkAllNotificationsRead = viewModel::markAllNotificationsRead,
        onDeleteNotification = viewModel::deleteNotification
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
    onSetTaskInProgress: (Long, Boolean) -> Unit = { _, _ -> },
    onSetTaskCompleted: (Long, Boolean) -> Unit = { _, _ -> },
    onToggleTaskReminder: (Long, Boolean) -> Unit = { _, _ -> },
    onDeleteTask: (Long) -> Unit = {},
    onMarkNotificationRead: (Long) -> Unit = {},
    onMarkAllNotificationsRead: () -> Unit = {},
    onDeleteNotification: (Long) -> Unit = {}
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
        item {
            TodayTasksSection(
                state = state,
                onAddTask = { showAddTaskDialog = true },
                onSetTaskInProgress = onSetTaskInProgress,
                onSetTaskCompleted = onSetTaskCompleted,
                onToggleTaskReminder = onToggleTaskReminder,
                onDeleteTask = onDeleteTask
            )
        }
        item { StreakCard(state = state.streak) }
        item { WorkoutTodayCard(workout = state.workout, onStart = onStartWorkout, onDetails = onWorkoutDetails) }
        item { NutritionSummaryCard(nutrition = state.nutrition, onLogMeal = onLogMeal, onDetails = onNutritionDetails) }
        item { AIInsightCard(insight = state.insight, onAskCoach = onAskCoach) }
        item { AchievementPreviewCard(achievement = state.achievement) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(6.dp, CircleShape)
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
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                }
            }
            Column {
                Text(
                    text = state.greetingTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
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
        IconButton(onClick = onNotificationClick) {
            Box {
                Icon(imageVector = Icons.Outlined.Notifications, contentDescription = "Notifications")
                if (state.unreadNotificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(FittyPink),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.unreadNotificationCount.coerceAtMost(99).toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodaySummaryCard(state: HomeUiState, onStartToday: () -> Unit = {}, onViewPlan: () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(FittyGradientStart, FittyGradientEnd)))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        state.focusTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        state.focusDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.SelfImprovement,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.focusMetrics.forEach { metric ->
                    FocusMetric(label = metric.label, value = metric.value)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStartToday,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = FittyPink),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(state.focusPrimaryActionLabel, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onViewPlan,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(state.focusSecondaryActionLabel)
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

@Composable
private fun TodayTasksSection(
    state: HomeUiState,
    onAddTask: () -> Unit,
    onSetTaskInProgress: (Long, Boolean) -> Unit,
    onSetTaskCompleted: (Long, Boolean) -> Unit,
    onToggleTaskReminder: (Long, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FittySectionHeader(
            title = state.tasksSectionTitle,
            action = state.tasksSectionActionLabel,
            onActionClick = onAddTask
        )
        if (state.tasks.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
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
            state.tasks.forEach { task ->
                TaskCard(
                    task = task,
                    onSetInProgress = { enabled -> onSetTaskInProgress(task.id, enabled) },
                    onSetCompleted = { enabled -> onSetTaskCompleted(task.id, enabled) },
                    onToggleReminder = { enabled -> onToggleTaskReminder(task.id, enabled) },
                    onDelete = { onDeleteTask(task.id) }
                )
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: HomeTaskUi,
    onSetInProgress: (Boolean) -> Unit,
    onSetCompleted: (Boolean) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val icon = when (task.category) {
        HomeTaskCategory.Workout -> Icons.Outlined.FitnessCenter
        HomeTaskCategory.Meal -> Icons.Outlined.Restaurant
        HomeTaskCategory.Water -> Icons.Outlined.WaterDrop
        HomeTaskCategory.Custom -> Icons.Outlined.SelfImprovement
    }
    val completed = task.status == HomeTaskStatus.Completed
    val inProgress = task.status == HomeTaskStatus.InProgress
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (inProgress) 3.dp else 1.dp),
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (completed) FittyPink.copy(alpha = 0.10f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (completed) Icons.Outlined.CheckCircle else icon,
                        contentDescription = null,
                        tint = if (completed) FittyPink else MaterialTheme.colorScheme.primary,
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
                    label = { Text(task.status.toDisplayLabel(), style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(12.dp)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = inProgress,
                        onCheckedChange = onSetInProgress
                    )
                    Text(stringResource(R.string.home_task_doing), style = MaterialTheme.typography.bodySmall)
                    Checkbox(
                        checked = completed,
                        onCheckedChange = onSetCompleted
                    )
                    Text(stringResource(R.string.home_task_completed), style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.home_task_reminder), style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = task.reminderEnabled,
                        onCheckedChange = onToggleReminder
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

private fun initialHomeUiState(context: Context): HomeUiState {
    return HomeUiState(
        isLoading = true,
        displayName = context.getString(R.string.home_display_name_default),
        avatarUrl = null,
        greetingTitle = context.getString(R.string.home_loading_profile),
        greetingSubtitle = context.getString(R.string.home_subtitle_default),
        focusTitle = context.getString(R.string.home_focus_title),
        focusDescription = context.getString(R.string.home_focus_description_default),
        focusMetrics = listOf(
            HomeFocusMetricUi(context.getString(R.string.home_metric_workout), "0/1"),
            HomeFocusMetricUi(context.getString(R.string.home_metric_meals_logged), "0/3"),
            HomeFocusMetricUi(context.getString(R.string.home_metric_water), context.getString(R.string.home_water_target))
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
            activeDates = emptyList()
        ),
        workout = HomeWorkoutUi(
            sectionTitle = context.getString(R.string.home_workout_section_title),
            name = context.getString(R.string.home_workout_complete_onboarding),
            meta = context.getString(R.string.home_focus_description_default),
            equipmentLabel = context.getString(R.string.home_equipment_default),
            primaryActionLabel = context.getString(R.string.home_action_start),
            secondaryActionLabel = context.getString(R.string.home_action_details)
        ),
        nutrition = defaultNutrition(context),
        insight = HomeInsightUi(
            title = context.getString(R.string.home_insight_title),
            message = context.getString(R.string.home_insight_default, context.getString(R.string.home_goal_default)),
            actions = defaultInsightActions(context)
        ),
        achievement = HomeAchievementPreviewUi(
            sectionTitle = context.getString(R.string.home_achievement_item_label),
            itemLabel = context.getString(R.string.home_achievement_item_label),
            subtitle = context.getString(R.string.home_achievement_locked),
            actionLabel = context.getString(R.string.home_action_view_all)
        ),
        notifications = emptyList()
    )
}

private fun FittyUser.toHomeUiState(context: Context): HomeUiState {
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
            HomeFocusMetricUi(label = context.getString(R.string.home_metric_workout), value = if (onboardingCompleted) "1/1" else "0/1"),
            HomeFocusMetricUi(label = context.getString(R.string.home_metric_meals_logged), value = "${stats.mealsLogged}/3"),
            HomeFocusMetricUi(label = context.getString(R.string.home_metric_water), value = context.getString(R.string.home_water_target))
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
            activeDates = stats.streakActiveDates
        ),
        workout = HomeWorkoutUi(
            sectionTitle = context.getString(R.string.home_workout_section_title),
            name = if (onboardingCompleted) {
                context.getString(R.string.home_workout_session_name, goalLabel)
            } else {
                context.getString(R.string.home_workout_complete_onboarding)
            },
            meta = context.getString(
                R.string.home_workout_meta_format,
                workoutMetaParts[0],
                workoutMetaParts[1],
                workoutMetaParts[2]
            ),
            equipmentLabel = equipmentLabel,
            primaryActionLabel = context.getString(R.string.home_action_start),
            secondaryActionLabel = context.getString(R.string.home_action_details)
        ),
        nutrition = defaultNutrition(
            context = context,
            summary = context.getString(
                R.string.home_nutrition_summary,
                estimateCaloriesLogged(),
                estimateCaloriesTarget(profile.weightKg, profile.primaryGoal)
            )
        ),
        insight = HomeInsightUi(
            title = context.getString(R.string.home_insight_title),
            message = insightMessageFor(context = context, goalLabel = goalLabel, mealsLogged = stats.mealsLogged),
            actions = defaultInsightActions(context)
        ),
        achievement = HomeAchievementPreviewUi(
            sectionTitle = context.getString(R.string.home_achievement_item_label),
            itemLabel = context.getString(R.string.home_achievement_item_label),
            subtitle = if (stats.mealsLogged >= 10) context.getString(R.string.home_achievement_unlocked) else context.getString(R.string.home_achievement_locked),
            actionLabel = context.getString(R.string.home_action_view_all)
        ),
        notifications = emptyList()
    )
}

private fun defaultTaskDrafts(context: Context, goalLabel: String): List<HomeTaskDraft> = listOf(
    HomeTaskDraft(
        title = context.getString(R.string.home_task_workout_title),
        description = "$goalLabel session",
        dateKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
        timeMinutes = 7 * 60,
        category = HomeTaskCategory.Workout,
        reminderEnabled = false,
        status = HomeTaskStatus.Todo,
        isDefault = true
    ),
    HomeTaskDraft(
        title = context.getString(R.string.home_task_log_lunch_title),
        description = context.getString(R.string.home_task_log_lunch_desc),
        dateKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
        timeMinutes = 12 * 60 + 30,
        category = HomeTaskCategory.Meal,
        reminderEnabled = false,
        status = HomeTaskStatus.Todo,
        isDefault = true
    ),
    HomeTaskDraft(
        title = context.getString(R.string.home_task_drink_water_title),
        description = context.getString(R.string.home_task_drink_water_desc),
        dateKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
        timeMinutes = 15 * 60,
        category = HomeTaskCategory.Water,
        reminderEnabled = false,
        status = HomeTaskStatus.Todo,
        isDefault = true
    )
)

private fun defaultNutrition(context: Context, summary: String = context.getString(R.string.home_nutrition_summary, 1240, 2100)): HomeNutritionUi {
    return HomeNutritionUi(
        sectionTitle = context.getString(R.string.home_nutrition_section_title),
        summary = summary,
        macros = listOf(
            HomeMacroProgressUi(context.getString(R.string.home_macro_protein), 0.46f),
            HomeMacroProgressUi(context.getString(R.string.home_macro_carbs), 0.58f),
            HomeMacroProgressUi(context.getString(R.string.home_macro_fat), 0.38f)
        ),
        meals = listOf(
            HomeMealUi(context.getString(R.string.home_meal_breakfast), context.getString(R.string.home_calories_420)),
            HomeMealUi(context.getString(R.string.home_meal_lunch), context.getString(R.string.home_calories_610)),
            HomeMealUi(context.getString(R.string.home_meal_snack), context.getString(R.string.home_calories_210))
        ),
        primaryActionLabel = context.getString(R.string.home_action_log_meal),
        secondaryActionLabel = context.getString(R.string.home_action_details)
    )
}

private fun defaultInsightActions(context: Context): List<String> = listOf(
    context.getString(R.string.home_action_apply),
    context.getString(R.string.home_action_ask_why),
    context.getString(R.string.home_action_dismiss)
)

/**
 * Builds streak UI with actual weekday labels and per-day active flags.
 * Shows Mon-Sun for the current week, with today highlighted.
 */
private fun buildStreakUi(
    context: Context,
    currentStreak: Int,
    bestStreak: Int,
    activeDates: List<String>
): HomeStreakUi {
    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val dayLabels = (0..6).map { offset ->
        val d = startOfWeek.plusDays(offset.toLong())
        d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3)
    }
    val todayIndex = (today.dayOfWeek.value - 1).coerceIn(0, 6)

    // Build active flags: check if each day of this week is in activeDates
    val activeDayFlags = (0..6).map { offset ->
        val dateStr = startOfWeek.plusDays(offset.toLong())
            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        activeDates.contains(dateStr)
    }

    return HomeStreakUi(
        sectionTitle = context.getString(R.string.home_streak_title),
        currentLabel = context.getString(R.string.home_streak_current, currentStreak),
        subtitle = context.getString(R.string.home_streak_subtitle),
        bestLabel = context.getString(R.string.home_streak_best, maxOf(currentStreak, bestStreak)),
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
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.home_notifications_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = "$unreadCount unread",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (unreadCount > 0) {
                        TextButton(onClick = onMarkAllRead) {
                            Text(stringResource(R.string.home_notifications_mark_all_read), color = FittyPink, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (notifications.isEmpty()) {
                    Text(
                        stringResource(R.string.home_notifications_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    notifications.forEach { notification ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (notification.isRead) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (notification.isRead) {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                } else {
                                    FittyPink.copy(alpha = 0.18f)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(FittyPink.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            notification.icon,
                                            contentDescription = null,
                                            tint = FittyPink,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                notification.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                notification.time,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            notification.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (!notification.isRead) {
                                        TextButton(onClick = { onMarkRead(notification.id) }) {
                                            Text(stringResource(R.string.home_notification_read))
                                        }
                                    }
                                    TextButton(onClick = { onDelete(notification.id) }) {
                                        Text(stringResource(R.string.home_notification_delete))
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FittyPink),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.home_action_close), fontWeight = FontWeight.Bold)
                }
            }
        }
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
    val titleRequiredMessage = stringResource(R.string.home_task_title_required)
    val invalidTimeMessage = stringResource(R.string.home_task_invalid_time)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_add_task_title),
                    style = MaterialTheme.typography.titleLarge,
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
                Text(
                    text = stringResource(R.string.home_task_category),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        HomeTaskCategory.Workout,
                        HomeTaskCategory.Meal,
                        HomeTaskCategory.Water,
                        HomeTaskCategory.Custom
                    ).forEach { option ->
                        OutlinedButton(
                            onClick = { category = option },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (category == option) FittyPink.copy(alpha = 0.1f) else Color.Transparent
                            )
                        ) {
                            Text(
                                text = option.name,
                                color = if (category == option) FittyPink else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.home_task_reminder), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
                errorMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
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
                        shape = RoundedCornerShape(14.dp),
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

private fun insightMessageFor(context: Context, goalLabel: String, mealsLogged: Int): String {
    return when {
        mealsLogged == 0 -> context.getString(R.string.home_insight_no_meals)
        goalLabel.contains("Muscle", ignoreCase = true) -> context.getString(R.string.home_insight_gain_muscle, goalLabel)
        else -> context.getString(R.string.home_insight_default, goalLabel)
    }
}

private fun estimateCaloriesLogged(): Int = 1240

private fun estimateCaloriesTarget(weightKg: Int?, goal: String): Int {
    return when {
        weightKg == null -> 2100
        goal == "gain_muscle" -> weightKg * 34
        goal == "lose_weight" -> weightKg * 28
        else -> weightKg * 30
    }
}

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
    notifications = current.notifications,
    unreadNotificationCount = current.unreadNotificationCount,
    showNotifications = current.showNotifications
)

private fun HomeTaskStatus.toDisplayLabel(): String = when (this) {
    HomeTaskStatus.Todo -> "To Do"
    HomeTaskStatus.InProgress -> "In Progress"
    HomeTaskStatus.Completed -> "Completed"
}

private fun AppNotificationType.toNotificationIcon(): ImageVector = when (this) {
    AppNotificationType.Workout -> Icons.Outlined.FitnessCenter
    AppNotificationType.Meal -> Icons.Outlined.Restaurant
    AppNotificationType.Reminder -> Icons.Outlined.Notifications
    AppNotificationType.Streak -> Icons.Outlined.LocalFireDepartment
    AppNotificationType.Progress -> Icons.Outlined.BarChart
    AppNotificationType.General -> Icons.Outlined.Notifications
}

private fun formatTaskTime(timeMinutes: Int): String {
    val normalized = timeMinutes.coerceIn(0, 23 * 60 + 59)
    val time = LocalTime.of(normalized / 60, normalized % 60)
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun formatNotificationTime(createdAt: Long): String {
    val diff = System.currentTimeMillis() - createdAt
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
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

private fun List<String>.formatWorkoutDays(context: Context): String {
    if (isEmpty()) return context.getString(R.string.home_choose_workout_days)
    return joinToString(", ") { value ->
        value.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
        }
    }
}
