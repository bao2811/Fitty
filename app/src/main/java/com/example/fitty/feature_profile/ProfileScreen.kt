package com.example.fitty.feature_profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.fitty.R
import com.example.fitty.core.designsystem.component.FittyMetricTile
import com.example.fitty.core.designsystem.component.FittyPrimaryButton
import com.example.fitty.core.designsystem.component.FittySectionHeader
import com.example.fitty.core.designsystem.component.FittySettingsRow
import com.example.fitty.core.ui.FittyLazyScreen
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.preferredDisplayName
import com.example.fitty.domain.usecase.auth.LogoutUseCase
import com.example.fitty.domain.usecase.user.GetCurrentUserUseCase
import com.example.fitty.domain.usecase.user.UpdateProfileInfoUseCase
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
    val avatarInitial: String = "F", val avatarUrl: String? = null, val profileLabel: String = "",
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
    val aiConsentEnabled: Boolean = true, val photoStorageEnabled: Boolean = true, val isGuest: Boolean = false,
    val isEditing: Boolean = false, val editName: String = "", val isSaving: Boolean = false, val editError: String? = null,
    val bodyFatLabel: String = "--", val postureScoreLabel: String = "--"
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val deleteAccountUseCase: com.example.fitty.domain.usecase.auth.DeleteAccountUseCase,
    private val updateGoalUseCase: com.example.fitty.domain.usecase.user.UpdateGoalUseCase,
    private val updateProfileInfoUseCase: UpdateProfileInfoUseCase,
    private val trackingRepository: com.example.fitty.domain.repository.TrackingRepository,
    private val sessionRepository: com.example.fitty.domain.repository.SessionRepository,
    private val updateSettingsUseCase: com.example.fitty.domain.usecase.user.UpdateSettingsUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialProfileUiState(context))
    val uiState: StateFlow<ProfileUiState> = _uiState
    private var cachedUser: com.example.fitty.domain.model.FittyUser? = null
    init { refreshUser() }

    fun refreshUser() {
        viewModelScope.launch {
            runCatching { getCurrentUserUseCase() }
                .onSuccess { user ->
                    cachedUser = user
                    _uiState.update { if (user == null) it.copy(isLoading = false) else user.toProfileUiState(context) }
                }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }
            loadLiveBodyMetrics()
        }
    }

    private suspend fun loadLiveBodyMetrics() {
        val uid = sessionRepository.getCurrentUserId() ?: return
        runCatching {
            val measurements = trackingRepository.getBodyMeasurements(uid, 1)
            val latestScan = trackingRepository.getLatestBodyScan(uid)
            val stats = trackingRepository.getProgressStats(uid, 30)
            _uiState.update { state ->
                val liveWeight = measurements.firstOrNull()?.weightKg
                val user = cachedUser
                val heightCm = user?.profile?.heightCm
                val weightForBmi = liveWeight ?: user?.profile?.weightKg?.toFloat()
                val liveBmi = if (heightCm != null && heightCm > 0 && weightForBmi != null && weightForBmi > 0f) {
                    String.format(Locale.US, "%.1f", weightForBmi / ((heightCm / 100f) * (heightCm / 100f)))
                } else state.bmiLabel
                state.copy(
                    weightLabel = liveWeight?.let { "%.0f ${user?.settings?.weightUnit ?: "kg"}".format(it) } ?: state.weightLabel,
                    bmiLabel = liveBmi,
                    currentStreak = stats.currentStreak,
                    bestStreak = stats.bestStreak,
                    totalWorkouts = stats.totalWorkouts,
                    mealsLogged = stats.totalMealsLogged,
                    bodyFatLabel = latestScan?.estimatedBodyFatPercent?.let { "%.1f%%".format(it) } ?: state.bodyFatLabel,
                    postureScoreLabel = latestScan?.postureScore?.let { "$it/100" } ?: state.postureScoreLabel
                )
            }
        }
    }

    fun startEdit() { _uiState.update { it.copy(isEditing = true, editName = it.displayName, editError = null) } }
    fun cancelEdit() { _uiState.update { it.copy(isEditing = false, editError = null) } }
    fun onEditNameChanged(v: String) { _uiState.update { it.copy(editName = v, editError = null) } }

    fun saveDisplayName() {
        val name = _uiState.value.editName.trim()
        if (name.isBlank()) { _uiState.update { it.copy(editError = "Tên không được để trống") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            updateProfileInfoUseCase.updateName(name)
                .onSuccess { _uiState.update { it.copy(isSaving = false, isEditing = false, displayName = name, avatarInitial = name.first().uppercaseChar().toString()) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, editError = e.message) } }
        }
    }

    fun uploadAvatar(uri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            updateProfileInfoUseCase.uploadAvatar(uri)
                .onSuccess { url -> _uiState.update { it.copy(isSaving = false, avatarUrl = url) } }
                .onFailure { _uiState.update { it.copy(isSaving = false) } }
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val user = cachedUser ?: return@launch
            val newTheme = when (user.settings.themeMode) {
                "dark" -> "light"; "light" -> "system"; else -> "dark"
            }
            val newSettings = user.settings.copy(themeMode = newTheme)
            updateSettingsUseCase(newSettings).onSuccess {
                cachedUser = user.copy(settings = newSettings)
                _uiState.update { it.copy(themeLabel = newTheme.toDisplayLabel(context.getString(R.string.profile_theme_default))) }
            }
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val user = cachedUser ?: return@launch
            val newLang = if (user.settings.language == "vi") "en" else "vi"
            val newSettings = user.settings.copy(language = newLang)
            updateSettingsUseCase(newSettings).onSuccess {
                cachedUser = user.copy(settings = newSettings)
                _uiState.update { it.copy(languageLabel = newLang.toLanguageLabel(context)) }
            }
        }
    }

    fun toggleAiConsent() {
        viewModelScope.launch {
            val user = cachedUser ?: return@launch
            val newSettings = user.settings.copy(aiConsent = !user.settings.aiConsent)
            updateSettingsUseCase(newSettings).onSuccess {
                cachedUser = user.copy(settings = newSettings)
                _uiState.update { it.copy(aiConsentEnabled = newSettings.aiConsent) }
            }
        }
    }

    fun togglePhotoStorage() {
        viewModelScope.launch {
            val user = cachedUser ?: return@launch
            val newSettings = user.settings.copy(photoStorageEnabled = !user.settings.photoStorageEnabled)
            updateSettingsUseCase(newSettings).onSuccess {
                cachedUser = user.copy(settings = newSettings)
                _uiState.update { it.copy(photoStorageEnabled = newSettings.photoStorageEnabled) }
            }
        }
    }

    fun logout(onComplete: () -> Unit) { viewModelScope.launch { logoutUseCase(); onComplete() } }
    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch { deleteAccountUseCase().onSuccess { onComplete() } }
    }
}

@Composable
fun ProfileRoute(onLogout: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    ProfileScreen(state, onLogout = { viewModel.logout(onLogout) }, onDeleteAccount = { viewModel.deleteAccount(onLogout) },
        onStartEdit = viewModel::startEdit, onCancelEdit = viewModel::cancelEdit,
        onEditNameChanged = viewModel::onEditNameChanged, onSaveName = viewModel::saveDisplayName,
        onAvatarPicked = viewModel::uploadAvatar,
        onToggleTheme = viewModel::toggleTheme, onToggleLanguage = viewModel::toggleLanguage,
        onToggleAiConsent = viewModel::toggleAiConsent, onTogglePhotoStorage = viewModel::togglePhotoStorage)
}

@Composable
fun ProfileScreen(
    state: ProfileUiState, onLogout: () -> Unit, onDeleteAccount: () -> Unit = {},
    onStartEdit: () -> Unit = {}, onCancelEdit: () -> Unit = {},
    onEditNameChanged: (String) -> Unit = {}, onSaveName: () -> Unit = {},
    onAvatarPicked: (String) -> Unit = {},
    onToggleTheme: () -> Unit = {}, onToggleLanguage: () -> Unit = {},
    onToggleAiConsent: () -> Unit = {}, onTogglePhotoStorage: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.profile_delete_account)) },
            text = { Text("Hành động này không thể hoàn tác. Tất cả dữ liệu sẽ bị xóa vĩnh viễn.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { Button(onClick = { showDeleteDialog = false; onDeleteAccount() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Xóa vĩnh viễn") } },
            dismissButton = { OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Hủy") } })
    }

    FittyLazyScreen {
        item { ProfileHeader(state, onStartEdit, onCancelEdit, onEditNameChanged, onSaveName, onAvatarPicked) }
        item { StatsOverview(state) }
        item { GoalSummaryCard(state) }
        item { BodyMetricsSection(state) }
        item { PreferenceSection(state) }
        item { AppSettingsSection(state, onToggleTheme, onToggleLanguage) }
        item { PrivacySection(state, onToggleAiConsent, onTogglePhotoStorage) }
        item { LogoutSection(onLogout, onDeleteAccount = { showDeleteDialog = true }) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun ProfileHeader(
    state: ProfileUiState, onStartEdit: () -> Unit, onCancelEdit: () -> Unit,
    onEditNameChanged: (String) -> Unit, onSaveName: () -> Unit, onAvatarPicked: (String) -> Unit
) {
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onAvatarPicked(it.toString()) }
    }

    Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
        Row(
            modifier = Modifier.background(Brush.horizontalGradient(listOf(FittyGradientStart, FittyGradientEnd))).padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier.size(72.dp).shadow(12.dp, CircleShape).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.avatarUrl.isNullOrBlank()) {
                        Text(state.avatarInitial, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = FittyPink)
                    } else {
                        AsyncImage(model = state.avatarUrl, contentDescription = state.displayName, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp).clip(CircleShape))
                    }
                }
                if (state.isEditing) {
                    Box(
                        modifier = Modifier.size(26.dp).clip(CircleShape).background(FittyPink).clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                }
            }

            // Name & Email - horizontal info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state.isEditing) {
                    OutlinedTextField(
                        value = state.editName, onValueChange = onEditNameChanged,
                        textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
                        singleLine = true, isError = state.editError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = Color.White, errorBorderColor = Color(0xFFFF6B6B)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = state.editError?.let { { Text(it, color = Color(0xFFFF6B6B)) } }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onCancelEdit, shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            modifier = Modifier.height(36.dp)) { Text("Hủy", style = MaterialTheme.typography.labelMedium) }
                        Button(onClick = onSaveName, shape = RoundedCornerShape(12.dp), enabled = !state.isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = FittyPink),
                            modifier = Modifier.height(36.dp)) {
                            if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = FittyPink)
                            else Text("Lưu", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Text(state.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (state.isGuest) Icons.Outlined.Person else Icons.Outlined.Email, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                        Text(if (state.isGuest) stringResource(R.string.profile_guest_mode) else state.email.ifBlank { "N/A" },
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(state.profileLabel, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                }
            }

            // Edit button on the right side
            if (!state.isEditing) {
                IconButton(onClick = onStartEdit, modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))) {
                    Icon(Icons.Outlined.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun StatsOverview(state: ProfileUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MiniStat("🔥", "${state.currentStreak}", "Chuỗi", Modifier.weight(1f))
        MiniStat("💪", "${state.totalWorkouts}", "Buổi tập", Modifier.weight(1f))
        MiniStat("🥗", "${state.mealsLogged}", "Bữa ăn", Modifier.weight(1f))
        MiniStat("⭐", "${state.bestStreak}", "Kỷ lục", Modifier.weight(1f))
    }
}

@Composable
private fun MiniStat(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalSummaryCard(state: ProfileUiState) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(FittyPink.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.TrackChanges, null, tint = FittyPink, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mục tiêu", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(state.currentGoal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FittyPink)
                }
            }
            Text(state.targetWeightLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(progress = { state.goalProgress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = FittyPink, trackColor = FittyPink.copy(alpha = 0.10f))
            Text(state.goalProgressLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            FittyMetricTile(state.bodyFatLabel, "Body Fat", Icons.Outlined.AccessibilityNew, Modifier.fillMaxWidth(0.48f))
            FittyMetricTile(state.postureScoreLabel, "Posture", Icons.Outlined.SelfImprovement, Modifier.fillMaxWidth(0.48f))
        }
    }
}

@Composable private fun PreferenceSection(state: ProfileUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FittySectionHeader(stringResource(R.string.profile_preferences))
        SettingsCard {
            FittySettingsRow(Icons.Outlined.FitnessCenter, stringResource(R.string.profile_workout_preference), state.workoutPreferenceLabel)
            FittySettingsRow(Icons.Outlined.Schedule, stringResource(R.string.profile_training_days), state.trainingDaysLabel)
            FittySettingsRow(Icons.Outlined.Restaurant, stringResource(R.string.profile_dietary_preference), state.dietaryLabel)
        }
    }
}

@Composable private fun AppSettingsSection(state: ProfileUiState, onToggleTheme: () -> Unit, onToggleLanguage: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FittySectionHeader(stringResource(R.string.profile_app_settings))
        SettingsCard {
            ClickableSettingsRow(Icons.Outlined.DarkMode, stringResource(R.string.profile_theme), state.themeLabel, onClick = onToggleTheme)
            ClickableSettingsRow(Icons.Outlined.Language, stringResource(R.string.profile_app_language), state.languageLabel, onClick = onToggleLanguage)
            FittySettingsRow(Icons.Outlined.Settings, stringResource(R.string.profile_units), state.unitsLabel)
        }
    }
}

@Composable private fun PrivacySection(state: ProfileUiState, onToggleAiConsent: () -> Unit, onTogglePhotoStorage: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FittySectionHeader("Quyền riêng tư")
        SettingsCard {
            SwitchSettingsRow(Icons.Outlined.Psychology, "AI phân tích", "Cho phép AI phân tích dữ liệu", state.aiConsentEnabled, onToggleAiConsent)
            SwitchSettingsRow(Icons.Outlined.PhotoCamera, "Lưu ảnh", "Lưu trữ ảnh quét trên đám mây", state.photoStorageEnabled, onTogglePhotoStorage)
        }
    }
}

@Composable
private fun LogoutSection(onLogout: () -> Unit, onDeleteAccount: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FittyPrimaryButton(text = stringResource(R.string.profile_log_out), onClick = onLogout)
        OutlinedButton(onClick = onDeleteAccount, shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.profile_delete_account), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) { Column(modifier = Modifier.padding(vertical = 6.dp), content = content) }
}

@Composable
private fun ClickableSettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(FittyPink.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = FittyPink, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchSettingsRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(FittyPink.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = FittyPink, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

// ── Helpers ──
private fun initialProfileUiState(context: Context): ProfileUiState {
    return ProfileUiState(isLoading = true, displayName = context.getString(R.string.profile_display_name_default),
        profileLabel = context.getString(R.string.profile_profile_label_default), currentGoal = context.getString(R.string.profile_set_goal),
        targetWeightLabel = context.getString(R.string.profile_target_weight_not_set), goalProgressLabel = context.getString(R.string.profile_setup_incomplete),
        heightLabel = context.getString(R.string.profile_height_placeholder), weightLabel = context.getString(R.string.profile_weight_placeholder),
        bmiLabel = context.getString(R.string.profile_bmi_placeholder), calorieTargetLabel = context.getString(R.string.profile_calorie_placeholder),
        waterGoalLabel = context.getString(R.string.profile_water_goal_default), trainingDaysCountLabel = context.getString(R.string.profile_training_days_count_default),
        workoutPreferenceLabel = context.getString(R.string.profile_not_set), trainingDaysLabel = context.getString(R.string.profile_not_set),
        equipmentLabel = context.getString(R.string.profile_not_set), dietaryLabel = context.getString(R.string.profile_not_set),
        languageLabel = context.getString(R.string.profile_language_default), themeLabel = context.getString(R.string.profile_theme_default),
        unitsLabel = context.getString(R.string.profile_units_default))
}

private fun FittyUser.toProfileUiState(context: Context): ProfileUiState {
    val resolvedName = preferredDisplayName(defaultValue = context.getString(R.string.profile_display_name_default))
    val goal = profile.primaryGoal.toDisplayLabel(context.getString(R.string.profile_set_goal))
    val fitness = profile.fitnessLevel.toDisplayLabel(context.getString(R.string.home_fitness_default))
    val preferredTime = onboarding.preferredTime.toDisplayLabel(context.getString(R.string.profile_any_time))
    val trainingDays = onboarding.workoutDays.formatWorkoutDays(context)
    val progress = profileCompletionProgress()
    return ProfileUiState(isLoading = false, displayName = resolvedName, email = email,
        avatarInitial = resolvedName.firstOrNull()?.uppercaseChar()?.toString() ?: "F", avatarUrl = photoUrl,
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
