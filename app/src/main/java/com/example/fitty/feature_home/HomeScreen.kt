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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.preferredDisplayName
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
import java.time.LocalTime
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

enum class HomeQuickActionType {
    LogMeal,
    Workout,
    BodyScan,
    Coach
}

enum class HomeTaskType {
    Workout,
    Meal,
    Water,
    Done
}

data class HomeFocusMetricUi(
    val label: String,
    val value: String
)

data class HomeQuickActionUi(
    val label: String,
    val type: HomeQuickActionType
)

data class HomeTaskUi(
    val type: HomeTaskType,
    val title: String,
    val description: String,
    val time: String,
    val status: String,
    val highlighted: Boolean = false,
    val done: Boolean = false
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
    val quickActions: List<HomeQuickActionUi> = emptyList(),
    val tasksSectionTitle: String = "",
    val tasksSectionActionLabel: String = "",
    val tasks: List<HomeTaskUi> = emptyList(),
    val streak: HomeStreakUi = HomeStreakUi("", "", "", "", emptyList(), emptyList(), 0),
    val workout: HomeWorkoutUi = HomeWorkoutUi("", "", "", "", "", ""),
    val nutrition: HomeNutritionUi = HomeNutritionUi("", "", emptyList(), emptyList(), "", ""),
    val insight: HomeInsightUi = HomeInsightUi("", "", emptyList()),
    val achievement: HomeAchievementPreviewUi = HomeAchievementPreviewUi("", "", "", ""),
    val showNotifications: Boolean = false,
    val notifications: List<HomeNotificationUi> = emptyList()
)

data class HomeNotificationUi(
    val title: String,
    val message: String,
    val time: String,
    val icon: ImageVector
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getHomeDashboardUseCase: com.example.fitty.domain.usecase.home.GetHomeDashboardUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialHomeUiState(context))
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        refreshUser()
    }

    fun refreshUser() {
        viewModelScope.launch {
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
                        ) else user.toHomeUiState(context)
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

    internal fun toggleNotifications() {
        _uiState.update { it.copy(showNotifications = !it.showNotifications) }
    }

    internal fun dismissNotifications() {
        _uiState.update { it.copy(showNotifications = false) }
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
        onQuickAction = { type ->
            when (type) {
                HomeQuickActionType.LogMeal -> onNavigateToTrack()
                HomeQuickActionType.Workout -> onNavigateToPlan()
                HomeQuickActionType.BodyScan -> onNavigateToTrack()
                HomeQuickActionType.Coach -> onNavigateToCoach()
            }
        },
        onStartWorkout = onNavigateToPlan,
        onWorkoutDetails = onNavigateToPlan,
        onLogMeal = onNavigateToTrack,
        onNutritionDetails = onNavigateToTrack,
        onAskCoach = onNavigateToCoach,
        onToggleNotifications = viewModel::toggleNotifications,
        onDismissNotifications = viewModel::dismissNotifications
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onQuickAction: (HomeQuickActionType) -> Unit = {},
    onStartWorkout: () -> Unit = {},
    onWorkoutDetails: () -> Unit = {},
    onLogMeal: () -> Unit = {},
    onNutritionDetails: () -> Unit = {},
    onAskCoach: () -> Unit = {},
    onToggleNotifications: () -> Unit = {},
    onDismissNotifications: () -> Unit = {}
) {
    // Notification dialog
    if (state.showNotifications) {
        NotificationDialog(
            notifications = state.notifications,
            onDismiss = onDismissNotifications
        )
    }

    FittyLazyScreen {
        item { HomeTopBar(state = state, onNotificationClick = onToggleNotifications) }
        item { TodaySummaryCard(state = state, onStartToday = onStartWorkout, onViewPlan = onWorkoutDetails) }
        item { QuickActionsRow(actions = state.quickActions, onAction = onQuickAction) }
        item { TodayTasksSection(state = state) }
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
                if (state.notifications.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FittyPink)
                    )
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
private fun QuickActionsRow(actions: List<HomeQuickActionUi>, onAction: (HomeQuickActionType) -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        actions.forEach { action ->
            QuickAction(action = action, modifier = Modifier.weight(1f), onClick = { onAction(action.type) })
        }
    }
}

@Composable
private fun QuickAction(action: HomeQuickActionUi, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val icon = when (action.type) {
        HomeQuickActionType.LogMeal -> Icons.Outlined.CameraAlt
        HomeQuickActionType.Workout -> Icons.Outlined.FitnessCenter
        HomeQuickActionType.BodyScan -> Icons.Outlined.AccessibilityNew
        HomeQuickActionType.Coach -> Icons.Outlined.Psychology
    }
    val containerColor = when (action.type) {
        HomeQuickActionType.LogMeal -> FittyPink.copy(alpha = 0.1f)
        HomeQuickActionType.Workout -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        HomeQuickActionType.BodyScan -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
        HomeQuickActionType.Coach -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
            Text(action.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TodayTasksSection(state: HomeUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FittySectionHeader(title = state.tasksSectionTitle, action = state.tasksSectionActionLabel)
        state.tasks.forEach { task ->
            TaskCard(task = task)
        }
    }
}

@Composable
private fun TaskCard(task: HomeTaskUi) {
    val icon = when (task.type) {
        HomeTaskType.Workout -> Icons.Outlined.FitnessCenter
        HomeTaskType.Meal -> Icons.Outlined.Restaurant
        HomeTaskType.Water -> Icons.Outlined.WaterDrop
        HomeTaskType.Done -> Icons.Outlined.CheckCircle
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.highlighted) 3.dp else 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (task.done) FittyPink.copy(alpha = 0.18f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (task.done) FittyPink.copy(alpha = 0.10f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (task.done) Icons.Outlined.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (task.done) FittyPink else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(task.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AssistChip(
                onClick = { /* read-only status */ },
                enabled = false,
                label = { Text(task.status, style = MaterialTheme.typography.labelSmall) },
                shape = RoundedCornerShape(12.dp)
            )
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
        quickActions = defaultQuickActions(context),
        tasksSectionTitle = context.getString(R.string.home_tasks_title),
        tasksSectionActionLabel = context.getString(R.string.home_action_view_all),
        tasks = defaultTasks(context),
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
        notifications = defaultNotifications()
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
        quickActions = defaultQuickActions(context),
        tasksSectionTitle = context.getString(R.string.home_tasks_title),
        tasksSectionActionLabel = context.getString(R.string.home_action_view_all),
        tasks = buildTasks(context = context, goalLabel = goalLabel),
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
        notifications = defaultNotifications()
    )
}

private fun buildTasks(context: Context, goalLabel: String): List<HomeTaskUi> {
    return listOf(
        HomeTaskUi(
            type = HomeTaskType.Workout,
            title = context.getString(R.string.home_task_workout_title),
            description = "$goalLabel session",
            time = context.getString(R.string.home_task_workout_time),
            status = context.getString(R.string.home_task_in_progress),
            highlighted = true
        ),
        HomeTaskUi(
            type = HomeTaskType.Meal,
            title = context.getString(R.string.home_task_log_lunch_title),
            description = context.getString(R.string.home_task_log_lunch_desc),
            time = context.getString(R.string.home_task_log_lunch_time),
            status = context.getString(R.string.home_task_todo)
        ),
        HomeTaskUi(
            type = HomeTaskType.Water,
            title = context.getString(R.string.home_task_drink_water_title),
            description = context.getString(R.string.home_task_drink_water_desc),
            time = context.getString(R.string.home_task_drink_water_time),
            status = context.getString(R.string.home_task_todo)
        ),
        HomeTaskUi(
            type = HomeTaskType.Done,
            title = context.getString(R.string.home_task_stretch_title),
            description = context.getString(R.string.home_task_stretch_desc),
            time = context.getString(R.string.home_task_done),
            status = context.getString(R.string.home_task_done),
            done = true
        )
    )
}

private fun defaultQuickActions(context: Context): List<HomeQuickActionUi> = listOf(
    HomeQuickActionUi(label = context.getString(R.string.home_action_log_meal), type = HomeQuickActionType.LogMeal),
    HomeQuickActionUi(label = context.getString(R.string.home_quick_action_workout), type = HomeQuickActionType.Workout),
    HomeQuickActionUi(label = context.getString(R.string.home_quick_action_body_scan), type = HomeQuickActionType.BodyScan),
    HomeQuickActionUi(label = context.getString(R.string.home_quick_action_coach), type = HomeQuickActionType.Coach)
)

private fun defaultTasks(context: Context): List<HomeTaskUi> = buildTasks(
    context = context,
    goalLabel = context.getString(R.string.home_default_task_goal)
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

private fun defaultNotifications(): List<HomeNotificationUi> = listOf(
    HomeNotificationUi(
        title = "Workout Reminder",
        message = "Time for your daily workout! Stay consistent to keep your streak.",
        time = "Just now",
        icon = Icons.Outlined.FitnessCenter
    ),
    HomeNotificationUi(
        title = "Meal Logging",
        message = "Don't forget to log your lunch. Tracking meals helps reach your goals faster.",
        time = "1h ago",
        icon = Icons.Outlined.Restaurant
    ),
    HomeNotificationUi(
        title = "Streak Update",
        message = "You're on a roll! Complete today's activity to extend your streak.",
        time = "3h ago",
        icon = Icons.Outlined.LocalFireDepartment
    ),
    HomeNotificationUi(
        title = "Weekly Progress",
        message = "Your weekly summary is ready. Check your progress in the Track tab.",
        time = "Yesterday",
        icon = Icons.Outlined.BarChart
    )
)

@Composable
private fun NotificationDialog(
    notifications: List<HomeNotificationUi>,
    onDismiss: () -> Unit
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
                    Text("Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${notifications.size} new",
                        style = MaterialTheme.typography.labelMedium,
                        color = FittyPink,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (notifications.isEmpty()) {
                    Text(
                        "No notifications yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    notifications.forEach { notification ->
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
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
                    Text("Dismiss", fontWeight = FontWeight.Bold)
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
