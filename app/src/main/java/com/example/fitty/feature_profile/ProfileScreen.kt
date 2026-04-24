package com.example.fitty.feature_profile

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.data.firebase.FittyFirebaseRepository
import com.example.fitty.data.firebase.FittyUser
import com.example.fitty.data.preferences.AppPreferencesDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

data class ProfileUiState(
    val isLoading: Boolean = true,
    val displayName: String = "Fitty User",
    val email: String = "",
    val avatarInitial: String = "F",
    val profileLabel: String = "Complete onboarding to personalize your profile",
    val currentGoal: String = "Set your goal",
    val targetWeightLabel: String = "Target weight not set",
    val goalProgress: Float = 0f,
    val goalProgressLabel: String = "Profile setup incomplete",
    val heightLabel: String = "-- cm",
    val weightLabel: String = "-- kg",
    val bmiLabel: String = "--",
    val calorieTargetLabel: String = "-- kcal",
    val waterGoalLabel: String = "2.5L",
    val trainingDaysCountLabel: String = "0 days",
    val workoutPreferenceLabel: String = "Not set",
    val trainingDaysLabel: String = "Not set",
    val equipmentLabel: String = "Not set",
    val dietaryLabel: String = "Not set",
    val languageLabel: String = "English",
    val themeLabel: String = "System",
    val unitsLabel: String = "kg | cm | kcal",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalWorkouts: Int = 0,
    val mealsLogged: Int = 0,
    val achievementsUnlocked: Int = 0,
    val aiConsentEnabled: Boolean = true,
    val photoStorageEnabled: Boolean = true,
    val isGuest: Boolean = false
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferencesDataSource(application.applicationContext)
    private val repository = FittyFirebaseRepository()
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

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
                    user.toProfileUiState()
                }
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.signOut()
            preferences.clearSession()
            onComplete()
        }
    }
}

@Composable
fun ProfileRoute(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshUser()
    }

    ProfileScreen(
        state = state,
        onLogout = { viewModel.logout(onLogout) }
    )
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onLogout: () -> Unit
) {
    FittyLazyScreen {
        item { ProfileHeader(state = state) }
        item { GoalSummaryCard(state = state) }
        item { BodyMetricsSection(state = state) }
        item { PreferenceSection(state = state) }
        item { ReminderSettingsSection() }
        item { AchievementsRow(state = state) }
        item { LinkedAppsSection() }
        item { PrivacySection(state = state) }
        item { AppSettingsSection(state = state) }
        item { LogoutSection(onLogout = onLogout) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun ProfileHeader(state: ProfileUiState) {
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
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.avatarInitial,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (state.isGuest) Icons.Outlined.Person else Icons.Outlined.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (state.isGuest) "Guest mode" else state.email.ifBlank { "Email not available" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = state.profileLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { }, label = { Text("${state.currentStreak}-day streak") })
                    AssistChip(onClick = { }, label = { Text(state.currentGoal) })
                }
            }
            OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp)) {
                Text("Edit")
            }
        }
    }
}

@Composable
private fun GoalSummaryCard(state: ProfileUiState) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.TrackChanges,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Current Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(state.currentGoal, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(state.targetWeightLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { state.goalProgress }, modifier = Modifier.fillMaxWidth())
                Text(
                    state.goalProgressLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = { }, shape = RoundedCornerShape(8.dp)) {
                Text("Update")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BodyMetricsSection(state: ProfileUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Body Metrics")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricTile(state.heightLabel, "Height", Icons.Outlined.Straighten)
            MetricTile(state.weightLabel, "Weight", Icons.Outlined.MonitorWeight)
            MetricTile(state.bmiLabel, "BMI", Icons.Outlined.HealthAndSafety)
            MetricTile(state.calorieTargetLabel, "Calorie Target", Icons.Outlined.Restaurant)
            MetricTile(state.waterGoalLabel, "Water Goal", Icons.Outlined.WaterDrop)
            MetricTile(state.trainingDaysCountLabel, "Training Days", Icons.Outlined.FitnessCenter)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text("Update Measurements")
            }
            OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text("View Progress")
            }
        }
    }
}

@Composable
private fun MetricTile(value: String, label: String, icon: ImageVector) {
    Card(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth(0.48f)
            .height(92.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PreferenceSection(state: ProfileUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Preferences")
        SettingsCard {
            SettingsRow(Icons.Outlined.FitnessCenter, "Workout Preference", state.workoutPreferenceLabel)
            SettingsRow(Icons.Outlined.Schedule, "Training Days", state.trainingDaysLabel)
            SettingsRow(Icons.Outlined.Badge, "Equipment Access", state.equipmentLabel)
            SettingsRow(Icons.Outlined.Restaurant, "Dietary Preference", state.dietaryLabel)
            SettingsRow(Icons.Outlined.Language, "Language", state.languageLabel)
        }
    }
}

@Composable
private fun ReminderSettingsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Notifications & Reminders")
        SettingsCard {
            ReminderRow("Workout Reminder", "6:00 PM", true)
            ReminderRow("Meal Reminder", "Breakfast, lunch, dinner", true)
            ReminderRow("Water Reminder", "Every 2 hours", true)
            ReminderRow("Sleep Reminder", "10:30 PM", false)
            ReminderRow("Streak Alert", "When one task remains", true)
        }
    }
}

@Composable
private fun AchievementsRow(state: ProfileUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Achievements", "See All")
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AchievementBadge("${state.totalWorkouts} Workouts", Icons.Outlined.FitnessCenter)
            AchievementBadge("${state.currentStreak}-Day Streak", Icons.Outlined.EmojiEvents)
            AchievementBadge("${state.mealsLogged} Meals Logged", Icons.Outlined.Restaurant)
            AchievementBadge("${state.achievementsUnlocked} Unlocked", Icons.Outlined.MonitorHeart)
        }
    }
}

@Composable
private fun AchievementBadge(title: String, icon: ImageVector) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
        modifier = Modifier.size(width = 132.dp, height = 96.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LinkedAppsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Linked Health & Devices")
        SettingsCard {
            SettingsRow(Icons.Outlined.HealthAndSafety, "Health Connect", "Not Connected")
            SettingsRow(Icons.Outlined.Watch, "Smartwatch Sync", "Not Connected")
            SettingsRow(Icons.Outlined.MonitorHeart, "Heart Rate Access", "Connected")
        }
    }
}

@Composable
private fun PrivacySection(state: ProfileUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Privacy & AI")
        SettingsCard {
            SettingsRow(
                Icons.Outlined.Lock,
                "Manage AI Data",
                if (state.aiConsentEnabled) "AI assistance is enabled" else "AI assistance is disabled"
            )
            SettingsRow(Icons.Outlined.HealthAndSafety, "Body Scan Privacy", "Control how body analysis images are stored")
            SettingsRow(
                Icons.Outlined.Restaurant,
                "Photo Storage Permission",
                if (state.photoStorageEnabled) "Meal and body photos are allowed" else "Photo storage is disabled"
            )
            SettingsRow(Icons.Outlined.Badge, "Terms & Privacy Policy", "Fitty data rules")
        }
    }
}

@Composable
private fun AppSettingsSection(state: ProfileUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("App Settings")
        SettingsCard {
            ToggleRow(Icons.Outlined.DarkMode, "Dark Mode", state.themeLabel, state.themeLabel.equals("Dark", ignoreCase = true))
            SettingsRow(Icons.Outlined.Language, "App Language", state.languageLabel)
            SettingsRow(Icons.Outlined.Settings, "Units", state.unitsLabel)
            SettingsRow(Icons.Outlined.Badge, "Help Center", "Guides and support")
            SettingsRow(Icons.Outlined.Badge, "About Fitty", "Version 1.0")
        }
    }
}

@Composable
private fun LogoutSection(onLogout: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FittyPrimaryButton(text = "Log Out", onClick = onLogout)
        OutlinedButton(
            onClick = { },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
                text = "Delete Account",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReminderRow(title: String, subtitle: String, enabled: Boolean) {
    var checked by remember { mutableStateOf(enabled) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { checked = it })
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, subtitle: String, enabled: Boolean) {
    var checked by remember { mutableStateOf(enabled) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { checked = it })
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

private fun FittyUser.toProfileUiState(): ProfileUiState {
    val resolvedName = displayName.ifBlank {
        email.substringBefore("@").ifBlank { "Fitty User" }
    }
    val goal = profile.primaryGoal.toDisplayLabel(defaultValue = "Set your goal")
    val fitness = profile.fitnessLevel.toDisplayLabel(defaultValue = "Beginner")
    val preferredTime = onboarding.preferredTime.toDisplayLabel(defaultValue = "Any time")
    val duration = onboarding.workoutDurationMinutes?.let { "$it min/session" } ?: "Duration not set"
    val trainingDays = onboarding.workoutDays.formatWorkoutDays()
    val heightValue = profile.heightCm
    val weightValue = profile.weightKg
    val targetWeightValue = profile.targetWeightKg
    val workoutDaysCount = onboarding.workoutDays.size
    val progress = profileCompletionProgress()

    return ProfileUiState(
        isLoading = false,
        displayName = resolvedName,
        email = email,
        avatarInitial = resolvedName.firstOrNull()?.uppercaseChar()?.toString() ?: "F",
        profileLabel = "$fitness | $goal",
        currentGoal = goal,
        targetWeightLabel = targetWeightValue?.let { "Target Weight: $it ${settings.weightUnit}" } ?: "Target weight not set",
        goalProgress = progress,
        goalProgressLabel = "Profile setup ${(progress * 100).roundToInt()}% complete",
        heightLabel = heightValue?.let { "$it ${settings.heightUnit}" } ?: "-- ${settings.heightUnit}",
        weightLabel = weightValue?.let { "$it ${settings.weightUnit}" } ?: "-- ${settings.weightUnit}",
        bmiLabel = calculateBmi(weightValue, heightValue),
        calorieTargetLabel = estimateCalories(weightValue, profile.primaryGoal, settings.energyUnit),
        waterGoalLabel = "2.5L",
        trainingDaysCountLabel = "$workoutDaysCount days",
        workoutPreferenceLabel = listOf(fitness, duration, preferredTime).joinToString(" | "),
        trainingDaysLabel = trainingDays,
        equipmentLabel = onboarding.equipmentAccess.toDisplayLabel(defaultValue = "Not set"),
        dietaryLabel = onboarding.nutritionStyle.toDisplayLabel(defaultValue = "Not set"),
        languageLabel = settings.language.toLanguageLabel(),
        themeLabel = settings.themeMode.toDisplayLabel(defaultValue = "System"),
        unitsLabel = "${settings.weightUnit} | ${settings.heightUnit} | ${settings.energyUnit}",
        currentStreak = stats.currentStreak,
        bestStreak = stats.bestStreak,
        totalWorkouts = stats.totalWorkouts,
        mealsLogged = stats.mealsLogged,
        achievementsUnlocked = stats.achievementsUnlocked,
        aiConsentEnabled = settings.aiConsent,
        photoStorageEnabled = settings.photoStorageEnabled,
        isGuest = guest
    )
}

private fun FittyUser.profileCompletionProgress(): Float {
    val checkpoints = listOf(
        profile.age != null,
        profile.heightCm != null,
        profile.weightKg != null,
        profile.targetWeightKg != null,
        profile.primaryGoal.isNotBlank(),
        profile.fitnessLevel.isNotBlank(),
        onboarding.workoutDays.isNotEmpty(),
        onboarding.workoutDurationMinutes != null,
        onboarding.preferredTime.isNotBlank()
    )
    return checkpoints.count { it }.toFloat() / checkpoints.size.toFloat()
}

private fun calculateBmi(weightKg: Int?, heightCm: Int?): String {
    if (weightKg == null || heightCm == null || heightCm == 0) return "--"
    val heightMeters = heightCm / 100f
    return String.format(Locale.US, "%.1f", weightKg / (heightMeters * heightMeters))
}

private fun estimateCalories(weightKg: Int?, goal: String, energyUnit: String): String {
    val baseCalories = when {
        weightKg == null -> null
        goal == "gain_muscle" -> weightKg * 34
        goal == "lose_weight" -> weightKg * 28
        else -> weightKg * 30
    }
    return baseCalories?.let { "$it $energyUnit" } ?: "-- $energyUnit"
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
    if (isEmpty()) return "Not set"
    return joinToString(", ") { day ->
        day.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
        }
    }
}

private fun String.toLanguageLabel(): String {
    return when (lowercase(Locale.US)) {
        "vi" -> "Vietnamese"
        "en" -> "English"
        else -> toDisplayLabel(defaultValue = "English")
    }
}
