package com.example.fitty.feature_home

import android.app.Application
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
import androidx.compose.material.icons.outlined.CalendarMonth
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.data.firebase.FittyFirebaseRepository
import com.example.fitty.data.firebase.FittyUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.Locale

data class HomeUiState(
    val isLoading: Boolean = true,
    val displayName: String = "Fitty User",
    val avatarInitial: String = "F",
    val greetingTitle: String = "Welcome back",
    val greetingSubtitle: String = "Let's make today count",
    val focusDescription: String = "Finish onboarding to generate your daily workout focus.",
    val workoutTarget: String = "0/1",
    val mealsTarget: String = "0/3",
    val waterTarget: String = "0L / 2.5L",
    val workoutName: String = "Starter Workout",
    val workoutMeta: String = "Set up your plan to see today's session",
    val equipmentLabel: String = "No equipment selected",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FittyFirebaseRepository()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        refreshUser()
    }

    fun refreshUser() {
        viewModelScope.launch {
            val user = repository.getCurrentUser()
            _uiState.update { current ->
                if (user == null) {
                    current.copy(isLoading = false)
                } else {
                    user.toHomeUiState()
                }
            }
        }
    }
}

@Composable
fun HomeRoute(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshUser()
    }

    HomeScreen(state = state)
}

@Composable
fun HomeScreen(state: HomeUiState) {
    FittyLazyScreen {
        item { HomeTopBar(state = state) }
        item { TodaySummaryCard(state = state) }
        item { QuickActionsRow() }
        item { TodayTasksSection() }
        item { StreakCard(state = state) }
        item { WorkoutTodayCard(state = state) }
        item { NutritionSummaryCard() }
        item { AIInsightCard() }
        item { AchievementPreviewCard() }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun HomeTopBar(state: HomeUiState) {
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
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.avatarInitial,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row {
            BadgedIconButton(icon = Icons.Outlined.Notifications, hasBadge = true)
            IconButton(onClick = { }) {
                Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription = null)
            }
        }
    }
}

@Composable
private fun BadgedIconButton(icon: ImageVector, hasBadge: Boolean) {
    Box {
        IconButton(onClick = { }) {
            Icon(imageVector = icon, contentDescription = null)
        }
        if (hasBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
            )
        }
    }
}

@Composable
private fun TodaySummaryCard(state: HomeUiState) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's Focus",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = state.focusDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.SelfImprovement,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FocusMetric("Workout", state.workoutTarget)
                FocusMetric("Meals Logged", state.mealsTarget)
                FocusMetric("Water", state.waterTarget)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start Today")
                }
                OutlinedButton(
                    onClick = { },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View My Plan")
                }
            }
        }
    }
}

@Composable
private fun FocusMetric(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.86f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuickActionsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickAction("Log Meal", Icons.Outlined.CameraAlt, MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f))
        QuickAction("Start Workout", Icons.Outlined.FitnessCenter, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
        QuickAction("Body Scan", Icons.Outlined.AccessibilityNew, MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f))
        QuickAction("Ask Coach", Icons.Outlined.Psychology, MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.clickable { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TodayTasksSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Today's Tasks", action = "See All")
        TaskCard(
            icon = Icons.Outlined.FitnessCenter,
            title = "Complete your 30-minute workout",
            description = "Full Body Strength",
            time = "Before 7:00 PM",
            status = "In Progress",
            highlighted = true
        )
        TaskCard(
            icon = Icons.Outlined.Restaurant,
            title = "Log lunch before 1:30 PM",
            description = "Scan or enter your meal",
            time = "1:30 PM",
            status = "To Do"
        )
        TaskCard(
            icon = Icons.Outlined.WaterDrop,
            title = "Drink 2 more glasses of water",
            description = "1.2L logged so far",
            time = "Next hour",
            status = "To Do"
        )
        TaskCard(
            icon = Icons.Outlined.CheckCircle,
            title = "Morning stretch",
            description = "10 minutes completed",
            time = "Done",
            status = "Done",
            done = true
        )
    }
}

@Composable
private fun TaskCard(
    icon: ImageVector,
    title: String,
    description: String,
    time: String,
    status: String,
    highlighted: Boolean = false,
    done: Boolean = false
) {
    val borderColor = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val container = if (done) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
    Card(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (highlighted) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (done) Icons.Outlined.CheckCircle else icon,
                contentDescription = null,
                tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AssistChip(onClick = { }, label = { Text(status) })
        }
    }
}

@Composable
private fun StreakCard(state: HomeUiState) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable { }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Your Streak")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(34.dp)
                )
                Column {
                    Text("${state.currentStreak} Days", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Keep logging workouts and meals to extend your streak.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, day ->
                    DayIndicator(day = day, active = index < state.currentStreak.coerceAtMost(7), current = index == 5)
                }
            }
            Text(
                "Best Streak: ${state.bestStreak} days",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DayIndicator(day: String, active: Boolean, current: Boolean) {
    val color = when {
        current -> MaterialTheme.colorScheme.secondary
        active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(day, style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
    }
}

@Composable
private fun WorkoutTodayCard(state: HomeUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Today's Workout")
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.workoutName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(state.workoutMeta, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AssistChip(onClick = { }, label = { Text(state.equipmentLabel) })
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                            Text("Start")
                        }
                        OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                            Text("Details")
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NutritionSummaryCard() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Today's Nutrition")
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("1,240 / 2,100 kcal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                MacroProgress("Protein", 0.46f)
                MacroProgress("Carbs", 0.58f)
                MacroProgress("Fat", 0.38f)
                MealRow("Breakfast", "420 kcal")
                MealRow("Lunch", "610 kcal")
                MealRow("Snack", "210 kcal")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                        Text("Log Meal")
                    }
                    OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                        Text("Details")
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
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
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
private fun AIInsightCard() {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text("AI Insight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "Your protein intake is lower than usual today. Add a high-protein dinner to stay on track.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text("Apply") })
                AssistChip(onClick = { }, label = { Text("Ask Why") })
                AssistChip(onClick = { }, label = { Text("Dismiss") })
            }
        }
    }
}

@Composable
private fun AchievementPreviewCard() {
    Card(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.LocalDining, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Column(modifier = Modifier.weight(1f)) {
                Text("Recent Achievement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("First 10 Meals Logged", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("View All", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (action != null) {
            Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun FittyUser.toHomeUiState(): HomeUiState {
    val resolvedName = displayName.ifBlank {
        email.substringBefore("@").ifBlank { "Fitty User" }
    }
    val durationMinutes = onboarding.workoutDurationMinutes
    val goalLabel = profile.primaryGoal.toDisplayLabel(defaultValue = "your goal")
    val fitnessLabel = profile.fitnessLevel.toDisplayLabel(defaultValue = "Beginner")
    val equipmentLabel = onboarding.equipmentAccess.toDisplayLabel(defaultValue = "No equipment selected")
    val workoutDays = onboarding.workoutDays.formatWorkoutDays()
    val workoutMetaParts = buildList {
        add(durationMinutes?.let { "$it min" } ?: "Duration not set")
        add(fitnessLabel)
        add(workoutDays)
    }

    return HomeUiState(
        isLoading = false,
        displayName = resolvedName,
        avatarInitial = resolvedName.firstOrNull()?.uppercaseChar()?.toString() ?: "F",
        greetingTitle = "${greetingForNow()}, ${resolvedName.substringBefore(" ")}",
        greetingSubtitle = if (guest) "You're browsing in guest mode" else "Let's make today count",
        focusDescription = buildString {
            append(durationMinutes?.let { "$it min workout" } ?: "Workout plan")
            append(" focused on ")
            append(goalLabel.lowercase(Locale.US))
        },
        workoutTarget = if (onboardingCompleted) "1/1" else "0/1",
        mealsTarget = "${stats.mealsLogged}/3",
        waterTarget = "0L / 2.5L",
        workoutName = if (onboardingCompleted) "$goalLabel Session" else "Complete onboarding",
        workoutMeta = workoutMetaParts.joinToString(" | "),
        equipmentLabel = equipmentLabel,
        currentStreak = stats.currentStreak,
        bestStreak = stats.bestStreak
    )
}

private fun greetingForNow(): String {
    val currentHour = LocalTime.now().hour
    return when {
        currentHour < 12 -> "Good Morning"
        currentHour < 18 -> "Good Afternoon"
        else -> "Good Evening"
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

private fun List<String>.formatWorkoutDays(): String {
    if (isEmpty()) return "Choose workout days"
    return joinToString(", ") { day ->
        day.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
        }
    }
}
