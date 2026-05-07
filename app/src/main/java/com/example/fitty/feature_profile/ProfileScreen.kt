package com.example.fitty.feature_profile

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitty.R
import com.example.fitty.core.designsystem.component.FittyMetricTile
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.designsystem.component.FittySectionHeader
import com.example.fitty.core.designsystem.component.FittySettingsRow
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.usecase.auth.LogoutUseCase
import com.example.fitty.domain.usecase.user.GetCurrentUserUseCase
import com.example.fitty.ui.theme.FittyGradientEnd
import com.example.fitty.ui.theme.FittyGradientStart
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true, val displayName: String = "", val email: String = "",
    val avatarInitial: String = "F", val profileLabel: String = "",
    val currentGoal: String = "", val targetWeightLabel: String = "",
    val goalProgress: Float = 0f, val goalProgressLabel: String = "",
    val heightLabel: String = "", val weightLabel: String = "", val bmiLabel: String = "",
    val calorieTargetLabel: String = "", val waterGoalLabel: String = "",
    val trainingDaysCountLabel: String = "", val workoutPreferenceLabel: String = "",
    val trainingDaysLabel: String = "", val equipmentLabel: String = "",
    val dietaryLabel: String = "", val languageLabel: String = "",
    val themeLabel: String = "", val unitsLabel: String = "",
    val currentStreak: Int = 0, val bestStreak: Int = 0, val totalWorkouts: Int = 0,
    val mealsLogged: Int = 0, val achievementsUnlocked: Int = 0,
    val aiConsentEnabled: Boolean = true, val photoStorageEnabled: Boolean = true, val isGuest: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialProfileUiState(context))
    val uiState: StateFlow<ProfileUiState> = _uiState
    init { refreshUser() }
    fun refreshUser() { viewModelScope.launch { val user = getCurrentUserUseCase(); _uiState.update { if (user == null) it.copy(isLoading = false) else user.toProfileUiState(context) } } }
    fun logout(onComplete: () -> Unit) { viewModelScope.launch { logoutUseCase(); onComplete() } }
}

@Composable
fun ProfileRoute(onLogout: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    ProfileScreen(state = state, onLogout = { viewModel.logout(onLogout) })
}

@Composable
fun ProfileScreen(state: ProfileUiState, onLogout: () -> Unit) {
    FittyLazyScreen {
        item { ProfileHeader(state) }; item { GoalSummaryCard(state) }
        item { BodyMetricsSection(state) }; item { PreferenceSection(state) }
        item { AppSettingsSection(state) }; item { LogoutSection(onLogout) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun ProfileHeader(state: ProfileUiState) {
    Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
        Column(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(FittyGradientStart, FittyGradientEnd)))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(68.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.avatarInitial, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = FittyPink)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(state.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (state.isGuest) Icons.Outlined.Person else Icons.Outlined.Email, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                        Text(if (state.isGuest) stringResource(R.string.profile_guest_mode) else state.email.ifBlank { stringResource(R.string.profile_email_not_available) }, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(state.profileLabel, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text(stringResource(R.string.profile_streak_days, state.currentStreak), color = Color.White) }, shape = RoundedCornerShape(12.dp))
                OutlinedButton(onClick = { }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text(stringResource(R.string.profile_edit_profile)) }
            }
        }
    }
}

@Composable
private fun GoalSummaryCard(state: ProfileUiState) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = FittyPink.copy(alpha = 0.08f)), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(FittyPink.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.TrackChanges, null, tint = FittyPink, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.profile_current_goal), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(state.currentGoal, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(state.targetWeightLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { state.goalProgress }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = FittyPink, trackColor = FittyPink.copy(alpha = 0.12f))
                Text(state.goalProgressLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = FittyPink)) { Text(stringResource(R.string.profile_update)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BodyMetricsSection(state: ProfileUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FittySectionHeader(stringResource(R.string.profile_body_metrics))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            FittyMetricTile(state.heightLabel, stringResource(R.string.profile_height), Icons.Outlined.Straighten, Modifier.fillMaxWidth(0.48f))
            FittyMetricTile(state.weightLabel, stringResource(R.string.profile_weight), Icons.Outlined.MonitorWeight, Modifier.fillMaxWidth(0.48f))
            FittyMetricTile(state.bmiLabel, stringResource(R.string.profile_bmi), Icons.Outlined.HealthAndSafety, Modifier.fillMaxWidth(0.48f))
            FittyMetricTile(state.calorieTargetLabel, stringResource(R.string.profile_calorie_target), Icons.Outlined.Restaurant, Modifier.fillMaxWidth(0.48f))
        }
    }
}

@Composable private fun PreferenceSection(state: ProfileUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { FittySectionHeader(stringResource(R.string.profile_preferences)); SettingsCard { FittySettingsRow(Icons.Outlined.FitnessCenter, stringResource(R.string.profile_workout_preference), state.workoutPreferenceLabel); FittySettingsRow(Icons.Outlined.Schedule, stringResource(R.string.profile_training_days), state.trainingDaysLabel); FittySettingsRow(Icons.Outlined.Restaurant, stringResource(R.string.profile_dietary_preference), state.dietaryLabel) } }
}
@Composable private fun AppSettingsSection(state: ProfileUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { FittySectionHeader(stringResource(R.string.profile_app_settings)); SettingsCard { FittySettingsRow(Icons.Outlined.DarkMode, stringResource(R.string.profile_theme), state.themeLabel); FittySettingsRow(Icons.Outlined.Language, stringResource(R.string.profile_app_language), state.languageLabel); FittySettingsRow(Icons.Outlined.Settings, stringResource(R.string.profile_units), state.unitsLabel) } }
}

@Composable
private fun LogoutSection(onLogout: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FittyPrimaryButton(text = stringResource(R.string.profile_log_out), onClick = onLogout)
        OutlinedButton(onClick = { }, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error); Text(stringResource(R.string.profile_delete_account), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) { Column(modifier = Modifier.padding(vertical = 6.dp), content = content) }
}

private fun initialProfileUiState(context: Context): ProfileUiState {
    return ProfileUiState(
        isLoading = true,
        displayName = context.getString(R.string.profile_display_name_default),
        profileLabel = context.getString(R.string.profile_profile_label_default),
        currentGoal = context.getString(R.string.profile_set_goal),
        targetWeightLabel = context.getString(R.string.profile_target_weight_not_set),
        goalProgressLabel = context.getString(R.string.profile_setup_incomplete),
        heightLabel = context.getString(R.string.profile_height_placeholder),
        weightLabel = context.getString(R.string.profile_weight_placeholder),
        bmiLabel = context.getString(R.string.profile_bmi_placeholder),
        calorieTargetLabel = context.getString(R.string.profile_calorie_placeholder),
        waterGoalLabel = context.getString(R.string.profile_water_goal_default),
        trainingDaysCountLabel = context.getString(R.string.profile_training_days_count_default),
        workoutPreferenceLabel = context.getString(R.string.profile_not_set),
        trainingDaysLabel = context.getString(R.string.profile_not_set),
        equipmentLabel = context.getString(R.string.profile_not_set),
        dietaryLabel = context.getString(R.string.profile_not_set),
        languageLabel = context.getString(R.string.profile_language_default),
        themeLabel = context.getString(R.string.profile_theme_default),
        unitsLabel = context.getString(R.string.profile_units_default)
    )
}

private fun FittyUser.toProfileUiState(context: Context): ProfileUiState {
    val resolvedName = displayName.ifBlank { email.substringBefore("@").ifBlank { context.getString(R.string.profile_display_name_default) } }
    val goal = profile.primaryGoal.toDisplayLabel(context.getString(R.string.profile_set_goal)); val fitness = profile.fitnessLevel.toDisplayLabel(context.getString(R.string.home_fitness_default))
    val preferredTime = onboarding.preferredTime.toDisplayLabel(context.getString(R.string.profile_any_time)); val trainingDays = onboarding.workoutDays.formatWorkoutDays(context)
    val progress = profileCompletionProgress()
    return ProfileUiState(isLoading = false, displayName = resolvedName, email = email,
        avatarInitial = resolvedName.firstOrNull()?.uppercaseChar()?.toString() ?: "F",
        profileLabel = context.getString(R.string.profile_level_label, fitness), currentGoal = goal,
        targetWeightLabel = profile.targetWeightKg?.let { context.getString(R.string.profile_target_weight_value, it, settings.weightUnit) } ?: context.getString(R.string.profile_target_weight_not_set),
        goalProgress = progress, goalProgressLabel = context.getString(R.string.profile_setup_complete, (progress * 100).roundToInt()),
        heightLabel = profile.heightCm?.let { context.getString(R.string.profile_measurement_value, it, settings.heightUnit) } ?: context.getString(R.string.profile_measurement_unknown, settings.heightUnit),
        weightLabel = profile.weightKg?.let { context.getString(R.string.profile_measurement_value, it, settings.weightUnit) } ?: context.getString(R.string.profile_measurement_unknown, settings.weightUnit),
        bmiLabel = calculateBmi(profile.weightKg, profile.heightCm), calorieTargetLabel = estimateCalories(context, profile.weightKg, profile.primaryGoal, settings.energyUnit),
        waterGoalLabel = context.getString(R.string.profile_water_goal_default), trainingDaysCountLabel = context.getString(R.string.profile_days_count, onboarding.workoutDays.size),
        workoutPreferenceLabel = context.getString(R.string.profile_workout_preference_format, fitness, preferredTime), trainingDaysLabel = trainingDays,
        equipmentLabel = onboarding.equipmentAccess.toDisplayLabel(context.getString(R.string.profile_not_set)), dietaryLabel = onboarding.nutritionStyle.toDisplayLabel(context.getString(R.string.profile_not_set)),
        languageLabel = settings.language.toLanguageLabel(context), themeLabel = settings.themeMode.toDisplayLabel(context.getString(R.string.profile_theme_default)),
        unitsLabel = context.getString(R.string.profile_units_format, settings.weightUnit, settings.heightUnit, settings.energyUnit),
        currentStreak = stats.currentStreak, bestStreak = stats.bestStreak, totalWorkouts = stats.totalWorkouts,
        mealsLogged = stats.mealsLogged, achievementsUnlocked = stats.achievementsUnlocked,
        aiConsentEnabled = settings.aiConsent, photoStorageEnabled = settings.photoStorageEnabled, isGuest = guest)
}
private fun FittyUser.profileCompletionProgress(): Float {
    val c = listOf(profile.age != null, profile.heightCm != null, profile.weightKg != null, profile.targetWeightKg != null, profile.primaryGoal.isNotBlank(), profile.fitnessLevel.isNotBlank(), onboarding.workoutDays.isNotEmpty(), onboarding.workoutDurationMinutes != null, onboarding.preferredTime.isNotBlank())
    return c.count { it }.toFloat() / c.size.toFloat()
}
private fun calculateBmi(w: Int?, h: Int?): String { if (w == null || h == null || h == 0) return "--"; return String.format(Locale.US, "%.1f", w / ((h / 100f) * (h / 100f))) }
private fun estimateCalories(context: Context, w: Int?, goal: String, unit: String): String { val c = when { w == null -> null; goal == "gain_muscle" -> w * 34; goal == "lose_weight" -> w * 28; else -> w * 30 }; return c?.let { "$it $unit" } ?: context.getString(R.string.profile_measurement_unknown, unit) }
private fun String.toDisplayLabel(d: String): String { if (isBlank()) return d; return split('_', ' ').filter { it.isNotBlank() }.joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.US) else c.toString() } } }
private fun List<String>.formatWorkoutDays(context: Context): String { if (isEmpty()) return context.getString(R.string.profile_not_set); return joinToString(", ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.US) else c.toString() } } }
private fun String.toLanguageLabel(context: Context): String = when (lowercase(Locale.US)) { "vi" -> context.getString(R.string.profile_language_vietnamese); "en" -> context.getString(R.string.profile_language_english); else -> toDisplayLabel(context.getString(R.string.profile_language_default)) }
