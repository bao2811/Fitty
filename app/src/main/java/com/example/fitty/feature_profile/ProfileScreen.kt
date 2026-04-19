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
import com.example.fitty.data.preferences.AppPreferencesDataSource
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferencesDataSource(application.applicationContext)

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
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
    ProfileScreen(onLogout = { viewModel.logout(onLogout) })
}

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    FittyLazyScreen {
        item { ProfileHeader() }
        item { GoalSummaryCard() }
        item { BodyMetricsSection() }
        item { PreferenceSection() }
        item { ReminderSettingsSection() }
        item { AchievementsRow() }
        item { LinkedAppsSection() }
        item { PrivacySection() }
        item { AppSettingsSection() }
        item { LogoutSection(onLogout = onLogout) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun ProfileHeader() {
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
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Anna Nguyen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Beginner • Fat Loss Goal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { }, label = { Text("Level 3") })
                    AssistChip(onClick = { }, label = { Text("12-day streak") })
                }
            }
            OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp)) {
                Text("Edit")
            }
        }
    }
}

@Composable
private fun GoalSummaryCard() {
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
                Text("Lose Weight", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Target Weight: 58 kg • Review in 6 weeks", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { 0.42f }, modifier = Modifier.fillMaxWidth())
            }
            Button(onClick = { }, shape = RoundedCornerShape(8.dp)) {
                Text("Update")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BodyMetricsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Body Metrics")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricTile("165 cm", "Height", Icons.Outlined.Straighten)
            MetricTile("61.5 kg", "Weight", Icons.Outlined.MonitorWeight)
            MetricTile("22.6", "BMI", Icons.Outlined.HealthAndSafety)
            MetricTile("2,100", "Calorie Target", Icons.Outlined.Restaurant)
            MetricTile("2.5L", "Water Goal", Icons.Outlined.WaterDrop)
            MetricTile("4 days", "Training Days", Icons.Outlined.FitnessCenter)
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
private fun PreferenceSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Preferences")
        SettingsCard {
            SettingsRow(Icons.Outlined.FitnessCenter, "Workout Preference", "Home workouts • 4 days/week • Evening")
            SettingsRow(Icons.Outlined.Schedule, "Training Days", "Mon, Wed, Fri, Sat")
            SettingsRow(Icons.Outlined.Badge, "Equipment Access", "Home, basic equipment")
            SettingsRow(Icons.Outlined.Restaurant, "Dietary Preference", "High protein • lactose-free")
            SettingsRow(Icons.Outlined.Language, "Language", "English")
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
private fun AchievementsRow() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Achievements", "See All")
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AchievementBadge("First Workout", Icons.Outlined.FitnessCenter)
            AchievementBadge("7-Day Streak", Icons.Outlined.EmojiEvents)
            AchievementBadge("10 Meals Logged", Icons.Outlined.Restaurant)
            AchievementBadge("5 Workouts", Icons.Outlined.MonitorHeart)
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
private fun PrivacySection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Privacy & AI")
        SettingsCard {
            SettingsRow(Icons.Outlined.Lock, "Manage AI Data", "Control chat and recommendation history")
            SettingsRow(Icons.Outlined.HealthAndSafety, "Body Scan Privacy", "Control how body analysis images are stored")
            SettingsRow(Icons.Outlined.Restaurant, "Photo Storage Permission", "Meal and body photos")
            SettingsRow(Icons.Outlined.Badge, "Terms & Privacy Policy", "Fitty data rules")
        }
    }
}

@Composable
private fun AppSettingsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("App Settings")
        SettingsCard {
            ToggleRow(Icons.Outlined.DarkMode, "Dark Mode", "Use system setting", false)
            SettingsRow(Icons.Outlined.Language, "App Language", "English")
            SettingsRow(Icons.Outlined.Settings, "Units", "kg • cm • kcal")
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
