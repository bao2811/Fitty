package com.example.fitty.feature_plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fitty.core.ui.FittyLazyScreen

@Composable
fun PlanScreen() {
    var selectedTab by remember { mutableStateOf("Today") }
    val tabs = listOf("Today", "Programs", "Library")

    FittyLazyScreen {
        item { PracticeTopBar(title = if (selectedTab == "Programs") "Programs" else "Practice") }
        item { PracticeTabs(tabs = tabs, selectedTab = selectedTab, onSelected = { selectedTab = it }) }
        when (selectedTab) {
            "Today" -> {
                item { TodayPracticeSection() }
                item { WorkoutSessionDetailPreview() }
                item { CreatePlanSection() }
                item { CustomWeeklyPlannerPreview() }
            }
            "Programs" -> {
                item { ProgramsBannerCard() }
                item { ProgramFilterChips() }
                item { ProgramListSection() }
                item { ProgramDetailPreview() }
            }
            else -> {
                item { ExerciseLibrarySection() }
                item { ExerciseDetailPreview() }
                item { BuildWorkoutSection() }
                item { MyCustomPlansSection() }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun PracticeTopBar(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Guided programs, exercise library, and custom workout builder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row {
            IconButton(onClick = { }) {
                Icon(Icons.Outlined.Search, contentDescription = null)
            }
            IconButton(onClick = { }) {
                Icon(Icons.Outlined.Tune, contentDescription = null)
            }
        }
    }
}

@Composable
private fun PracticeTabs(
    tabs: List<String>,
    selectedTab: String,
    onSelected: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        tabs.forEach { tab ->
            FilterChip(
                selected = selectedTab == tab,
                onClick = { onSelected(tab) },
                label = { Text(tab) }
            )
        }
    }
}

@Composable
private fun TodayPracticeSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Today's Practice")
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ThumbnailBox(icon = Icons.Outlined.FitnessCenter, modifier = Modifier.size(74.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Full Body Basics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Full Body • Beginner • ~220 kcal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PracticeStat(Icons.Outlined.Timer, "30 min", Modifier.weight(1f))
                    PracticeStat(Icons.Outlined.Speed, "Beginner", Modifier.weight(1f))
                    PracticeStat(Icons.Outlined.Home, "No equipment", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                        Text("Start Workout")
                    }
                    OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                        Text("Edit Session")
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSessionDetailPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Workout Session Detail")
        InstructionSummaryCard()
        ExerciseListCard()
        NotesCard(
            title = "Coach Notes",
            notes = listOf(
                "Keep your back straight.",
                "Slow down if you lose balance.",
                "Rest longer if needed."
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text("Start")
            }
            OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text("Replace")
            }
        }
    }
}

@Composable
private fun InstructionSummaryCard() {
    InfoCard(
        icon = Icons.Outlined.PlayArrow,
        title = "How this workout works",
        body = "6 exercises • 3 rounds • 30 seconds each • 15 seconds rest"
    )
}

@Composable
private fun ExerciseListCard() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ExerciseSessionItem("1", "Bodyweight Squat", "15 reps", "Legs", "Beginner")
        ExerciseSessionItem("2", "Push-up on Knees", "10 reps", "Chest", "Beginner")
        ExerciseSessionItem("3", "Glute Bridge", "15 reps", "Glutes", "Beginner")
        ExerciseSessionItem("4", "Plank", "30 sec", "Core", "Beginner")
    }
}

@Composable
private fun ExerciseSessionItem(
    number: String,
    title: String,
    reps: String,
    muscle: String,
    level: String
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberCircle(number)
            ThumbnailBox(icon = Icons.Outlined.AccessibilityNew, modifier = Modifier.size(42.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("$reps • $muscle • $level", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun ProgramsBannerCard() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Start with guided fitness programs",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Choose a ready-made training plan based on your goal, level, and available time.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Button(onClick = { }, shape = RoundedCornerShape(8.dp)) {
                Text("Find My Best Program")
            }
        }
    }
}

@Composable
private fun ProgramFilterChips() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        listOf("All", "Fat Loss", "Muscle Gain", "Beginner", "Home", "Gym", "Short Workouts", "Mobility", "Strength").forEachIndexed { index, label ->
            FilterChip(
                selected = index == 0,
                onClick = { },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun ProgramListSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Ready-made Programs")
        ProgramCard(
            title = "Beginner Fat Loss Starter",
            goal = "Fat Loss",
            meta = "4 weeks • 4 workouts/week • 20-30 min/session",
            tags = listOf("Beginner", "Home", "No equipment"),
            icon = Icons.Outlined.FitnessCenter
        )
        ProgramCard(
            title = "Home Strength Foundation",
            goal = "Strength",
            meta = "6 weeks • 3 workouts/week • 30-40 min/session",
            tags = listOf("Beginner", "Dumbbells", "Full body"),
            icon = Icons.Outlined.Speed
        )
        ProgramCard(
            title = "Mobility Reset",
            goal = "Mobility",
            meta = "2 weeks • 5 sessions/week • 15-20 min/session",
            tags = listOf("Recovery", "Home", "Stretching"),
            icon = Icons.Outlined.SelfImprovement
        )
    }
}

@Composable
private fun ProgramCard(
    title: String,
    goal: String,
    meta: String,
    tags: List<String>,
    icon: ImageVector
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ThumbnailBox(icon = icon, modifier = Modifier.size(70.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(goal, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ChipRow(tags)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Text("View Program")
                }
                Button(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Text("Start Plan")
                }
            }
        }
    }
}

@Composable
private fun ProgramDetailPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Program Detail Preview")
        HeaderImageCard(
            title = "Beginner Full Body Reset",
            subtitle = "A beginner-friendly 4-week program designed to improve stamina, build consistency, and reduce body fat.",
            icon = Icons.Outlined.FitnessCenter
        )
        ProgramSummarySection()
        BenefitsSection()
        WeeklyOverviewSection()
        SessionPreviewSection()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text("Start This Program")
            }
            OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text("Customize")
            }
        }
    }
}

@Composable
private fun ProgramSummarySection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PracticeStat(Icons.Outlined.CalendarMonth, "4 weeks", Modifier.weight(1f))
            PracticeStat(Icons.Outlined.FitnessCenter, "4 days/week", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PracticeStat(Icons.Outlined.Timer, "20-30 min", Modifier.weight(1f))
            PracticeStat(Icons.Outlined.Home, "No equipment", Modifier.weight(1f))
        }
    }
}

@Composable
private fun BenefitsSection() {
    NotesCard(
        title = "Why this program",
        notes = listOf(
            "Suitable for beginners.",
            "No equipment required.",
            "Designed for fat loss and healthy habit building.",
            "Short sessions, easy to follow at home."
        )
    )
}

@Composable
private fun WeeklyOverviewSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Weekly Overview")
        WeekBlock("Week 1", listOf("Day 1: Full Body Basics", "Day 2: Recovery Stretch", "Day 3: Lower Body Core", "Day 4: Beginner Cardio"))
        WeekBlock("Week 2", listOf("Day 1: Strength Basics", "Day 2: Mobility Reset", "Day 3: Cardio Intervals", "Day 4: Full Body Flow"))
    }
}

@Composable
private fun WeekBlock(title: String, days: List<String>) {
    InfoCard(
        icon = Icons.Outlined.CalendarMonth,
        title = title,
        body = days.joinToString(separator = "\n")
    )
}

@Composable
private fun SessionPreviewSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Session Preview")
        SessionPreviewCard("Full Body Basics", "30 min • Full body • ~220 kcal • 6 exercises")
        SessionPreviewCard("Recovery Stretch", "18 min • Mobility • ~80 kcal • 5 exercises")
        SessionPreviewCard("Beginner Cardio", "24 min • Cardio • ~180 kcal • 7 exercises")
    }
}

@Composable
private fun SessionPreviewCard(title: String, body: String) {
    InfoCard(icon = Icons.Outlined.PlayArrow, title = title, body = body)
}

@Composable
private fun ExerciseLibrarySection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Exercise Library")
        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("Search exercises") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            listOf("Muscle Group", "Goal", "Equipment", "Difficulty", "Duration", "Legs", "Core", "Beginner", "No equipment").forEachIndexed { index, label ->
                FilterChip(selected = index == 7, onClick = { }, label = { Text(label) })
            }
        }
        ExerciseLibraryItem("Plank", "Core • Beginner • 30 sec", Icons.Outlined.SelfImprovement)
        ExerciseLibraryItem("Lunges", "Legs • Beginner • 12 reps", Icons.Outlined.AccessibilityNew)
        ExerciseLibraryItem("Push-up on Knees", "Chest • Beginner • 10 reps", Icons.Outlined.FitnessCenter)
        ExerciseLibraryItem("Glute Bridge", "Glutes • Beginner • 15 reps", Icons.Outlined.AccessibilityNew)
    }
}

@Composable
private fun ExerciseLibraryItem(title: String, body: String, icon: ImageVector) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThumbnailBox(icon = icon, modifier = Modifier.size(50.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun ExerciseDetailPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Exercise Detail")
        HeaderImageCard(
            title = "Bodyweight Squat",
            subtitle = "Beginner • Legs • No equipment",
            icon = Icons.Outlined.AccessibilityNew
        )
        NotesCard(
            title = "How to do it",
            notes = listOf(
                "Stand with feet shoulder-width apart.",
                "Push your hips back.",
                "Bend your knees and lower your body.",
                "Keep your chest lifted.",
                "Return to standing."
            )
        )
        NotesCard(
            title = "Common mistakes",
            icon = Icons.Outlined.WarningAmber,
            notes = listOf("Knees collapsing inward.", "Back rounding too much.", "Heels lifting off the floor.")
        )
        NotesCard(
            title = "Trainer tips",
            notes = listOf("Keep core tight.", "Move slowly and with control.", "Breathe out when pushing up.")
        )
        VariationSection()
        TargetMusclesCard()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text("Add")
            }
            OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun VariationSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Variations")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoCard(
                icon = Icons.Outlined.Home,
                title = "Easier",
                body = "Chair Squat",
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                icon = Icons.Outlined.Speed,
                title = "Harder",
                body = "Jump Squat",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TargetMusclesCard() {
    InfoCard(
        icon = Icons.Outlined.AccessibilityNew,
        title = "Target muscles",
        body = "Quadriceps • Glutes • Core"
    )
}

@Composable
private fun CreatePlanSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Create Your Own Plan")
        InfoCard(
            icon = Icons.Outlined.Add,
            title = "Build a workout plan that fits your goal",
            body = "Choose goal, schedule, equipment, training style, then generate a base plan or build manually."
        )
        OutlinedTextField(
            value = "My Home Fat Loss Plan",
            onValueChange = { },
            label = { Text("Plan Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        ChipRow(listOf("Fat Loss", "Muscle Gain", "General Fitness", "Mobility", "Strength", "Recovery"))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectorBox("Weeks", "4", Modifier.weight(1f))
            SelectorBox("Workouts/week", "4", Modifier.weight(1f))
        }
        ChipRow(listOf("No equipment", "Resistance band", "Dumbbells", "Full gym"))
        ChipRow(listOf("Full body", "Upper / Lower", "Push Pull Legs", "Cardio + Strength", "Flexible custom"))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text("Generate")
            }
            OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text("Build Manually")
            }
        }
    }
}

@Composable
private fun CustomWeeklyPlannerPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Custom Weekly Planner")
        listOf(
            "Monday" to "Full Body Beginner • 30 min • 6 exercises",
            "Tuesday" to "Add Workout",
            "Wednesday" to "Core + Cardio • 25 min • 5 exercises",
            "Thursday" to "Rest Day"
        ).forEach { (day, detail) ->
            PlannerDayCard(day, detail)
        }
        Button(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text("Save Weekly Plan")
        }
    }
}

@Composable
private fun PlannerDayCard(day: String, detail: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (detail == "Add Workout") Icons.Outlined.Add else Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(day, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BuildWorkoutSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Build Workout")
        OutlinedTextField(
            value = "Home Strength Session",
            onValueChange = { },
            label = { Text("Workout Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectorBox("Focus Area", "Full Body", Modifier.weight(1f))
            SelectorBox("Difficulty", "Beginner", Modifier.weight(1f))
        }
        BuilderExerciseItem("Squat", "3 sets x 12 reps")
        BuilderExerciseItem("Plank", "3 x 30 sec")
        BuilderExerciseItem("Glute Bridge", "3 x 15 reps")
        OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text("Add Exercise", modifier = Modifier.padding(start = 8.dp))
        }
        ChipRow(listOf("Circuit", "Traditional Sets", "EMOM", "HIIT Timer"))
        OutlinedTextField(
            value = "Avoid knee-heavy exercises. Keep rest between sets at 45 sec.",
            onValueChange = { },
            label = { Text("Personal notes") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text("Save Workout")
            }
            OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text("Preview")
            }
        }
    }
}

@Composable
private fun BuilderExerciseItem(title: String, detail: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.DragIndicator, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun MyCustomPlansSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("My Custom Plans")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            listOf("Active", "Drafts", "Completed", "Saved Templates").forEachIndexed { index, label ->
                FilterChip(selected = index == 0, onClick = { }, label = { Text(label) })
            }
        }
        CustomPlanCard("My Home Strength Plan", "Strength • 4 weeks • 4 days/week • Updated 2 days ago")
        CustomPlanCard("Weekend Mobility", "Mobility • 2 weeks • 2 days/week • Draft")
    }
}

@Composable
private fun CustomPlanCard(title: String, body: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Text("Start")
                }
                OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Text("Edit")
                }
                OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Text("Duplicate")
                }
            }
        }
    }
}

@Composable
private fun HeaderImageCard(title: String, subtitle: String, icon: ImageVector) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThumbnailBox(icon = icon, modifier = Modifier.size(82.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NotesCard(
    title: String,
    notes: List<String>,
    icon: ImageVector = Icons.Outlined.CheckCircle
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            notes.forEach {
                Text("- $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PracticeStat(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SelectorBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChipRow(labels: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { index, label ->
            AssistChip(onClick = { }, label = { Text(label) }, leadingIcon = if (index == 0) {
                { Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else {
                null
            })
        }
    }
}

@Composable
private fun NumberCircle(number: String) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(number, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ThumbnailBox(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}
