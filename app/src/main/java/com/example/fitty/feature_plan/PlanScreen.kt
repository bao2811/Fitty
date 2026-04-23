package com.example.fitty.feature_plan

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.fitty.core.ui.FittyLazyScreen

private data class SampleExercise(
    val id: String,
    val title: String,
    val summary: String,
    val muscle: String,
    val level: String,
    val equipment: String,
    val repsOrDuration: String,
    val mediaUrl: String,
    val description: String,
    val steps: List<String>,
    val mistakes: List<String>,
    val tips: List<String>,
    val easierVariation: String,
    val harderVariation: String,
    val targetMuscles: String
)

private data class SampleWorkout(
    val title: String,
    val summary: String,
    val duration: String,
    val level: String,
    val equipment: String,
    val exercises: List<SampleExercise>
)

private val beginnerExerciseLibrary = listOf(
    SampleExercise(
        id = "bodyweight_squat",
        title = "Bodyweight Squat",
        summary = "Legs • Beginner • 12 reps",
        muscle = "Legs",
        level = "Beginner",
        equipment = "No equipment",
        repsOrDuration = "12 reps",
        mediaUrl = "https://media.giphy.com/media/1C1ipHPEs4Vjwglwza/giphy.gif",
        description = "A safe lower-body starter move that helps new users learn squat form and balance.",
        steps = listOf(
            "Stand with feet shoulder-width apart and brace your core.",
            "Push hips back while bending knees.",
            "Drive through your heels to return to standing."
        ),
        mistakes = listOf(
            "Knees collapse inward.",
            "Heels lift off the floor.",
            "Chest drops too far forward."
        ),
        tips = listOf(
            "Keep your chest lifted.",
            "Move slowly on the way down.",
            "Use a chair behind you if needed."
        ),
        easierVariation = "Chair Squat",
        harderVariation = "Jump Squat",
        targetMuscles = "Quadriceps • Glutes • Core"
    ),
    SampleExercise(
        id = "incline_push_up",
        title = "Incline Push Up",
        summary = "Chest • Beginner • 10 reps",
        muscle = "Chest",
        level = "Beginner",
        equipment = "Bench or table",
        repsOrDuration = "10 reps",
        mediaUrl = "https://media.giphy.com/media/kYvaNlsFBgq3xZ8fRn/giphy.gif",
        description = "An easier push-up variation that builds pressing strength without overwhelming beginners.",
        steps = listOf(
            "Place hands on a sturdy elevated surface.",
            "Keep a straight line from shoulders to heels.",
            "Lower chest with control and press back up."
        ),
        mistakes = listOf(
            "Hips sag downward.",
            "Elbows flare too wide.",
            "Shoulders shrug up to ears."
        ),
        tips = listOf(
            "Choose a higher surface if needed.",
            "Keep your core tight throughout.",
            "Exhale as you press away."
        ),
        easierVariation = "Wall Push Up",
        harderVariation = "Knee Push Up",
        targetMuscles = "Chest • Shoulders • Triceps"
    ),
    SampleExercise(
        id = "plank",
        title = "Plank",
        summary = "Core • Beginner • 30 sec",
        muscle = "Core",
        level = "Beginner",
        equipment = "Mat optional",
        repsOrDuration = "30 sec",
        mediaUrl = "https://media.giphy.com/media/vZwHcmIRWzhWbPV7kx/giphy.gif",
        description = "A full-body hold that teaches posture, bracing, and tension control.",
        steps = listOf(
            "Place forearms under shoulders.",
            "Lift hips so your body forms a straight line.",
            "Hold while breathing slowly and keeping core engaged."
        ),
        mistakes = listOf(
            "Hips drop too low.",
            "Hips rise too high.",
            "Breath is held for too long."
        ),
        tips = listOf(
            "Squeeze glutes to protect your lower back.",
            "Keep your neck neutral.",
            "Start with shorter holds if needed."
        ),
        easierVariation = "Knee Plank",
        harderVariation = "Plank Shoulder Tap",
        targetMuscles = "Core • Shoulders • Glutes"
    ),
    SampleExercise(
        id = "reverse_lunge",
        title = "Reverse Lunge",
        summary = "Legs • Beginner • 10 reps/side",
        muscle = "Legs",
        level = "Beginner",
        equipment = "No equipment",
        repsOrDuration = "10 reps/side",
        mediaUrl = "https://media.giphy.com/media/jp7eu9mD42asbXVapr/giphy.gif",
        description = "A balanced beginner lunge variation that improves leg strength and coordination.",
        steps = listOf(
            "Stand tall with feet hip-width apart.",
            "Step one foot backward and lower both knees.",
            "Push through the front foot to return to standing."
        ),
        mistakes = listOf(
            "Leaning too far forward.",
            "Front knee turns inward.",
            "Stride is too narrow."
        ),
        tips = listOf(
            "Take a long enough step back.",
            "Move under control.",
            "Use a wall lightly for support if needed."
        ),
        easierVariation = "Split Squat Hold",
        harderVariation = "Walking Lunge",
        targetMuscles = "Quadriceps • Glutes • Hamstrings"
    ),
    SampleExercise(
        id = "glute_bridge",
        title = "Glute Bridge",
        summary = "Glutes • Beginner • 15 reps",
        muscle = "Glutes",
        level = "Beginner",
        equipment = "No equipment",
        repsOrDuration = "15 reps",
        mediaUrl = "https://media.giphy.com/media/26FPJIbqE5Rhkah4Q/giphy.gif",
        description = "A simple floor movement that teaches hip extension and glute activation.",
        steps = listOf(
            "Lie on your back with knees bent and feet flat.",
            "Press through the heels and lift hips up.",
            "Pause briefly at the top, then lower slowly."
        ),
        mistakes = listOf(
            "Lower back arches too much.",
            "Toes do most of the pushing.",
            "Knees drift inward or outward."
        ),
        tips = listOf(
            "Squeeze your glutes at the top.",
            "Keep ribs down.",
            "Slow reps help beginners feel the right muscles."
        ),
        easierVariation = "Short Range Bridge",
        harderVariation = "Single-Leg Bridge",
        targetMuscles = "Glutes • Hamstrings • Core"
    )
)

private val beginnerStarterWorkout = SampleWorkout(
    title = "Full Body Basics",
    summary = "Full Body • Beginner • ~220 kcal",
    duration = "30 min",
    level = "Beginner",
    equipment = "No equipment",
    exercises = beginnerExerciseLibrary.take(4)
)

@Composable
fun PlanScreen() {
    var selectedTab by remember { mutableStateOf("Today") }
    var selectedExercise by remember { mutableStateOf(beginnerExerciseLibrary.first()) }
    val tabs = listOf("Today", "Programs", "Library")

    FittyLazyScreen {
        item { PracticeTopBar(title = if (selectedTab == "Programs") "Programs" else "Practice") }
        item { PracticeTabs(tabs = tabs, selectedTab = selectedTab, onSelected = { selectedTab = it }) }
        when (selectedTab) {
            "Today" -> {
                item { TodayPracticeSection(beginnerStarterWorkout) }
                item {
                    WorkoutSessionDetailPreview(
                        workout = beginnerStarterWorkout,
                        onExerciseSelected = { selectedExercise = it }
                    )
                }
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
                item {
                    ExerciseLibrarySection(
                        exercises = beginnerExerciseLibrary,
                        selectedExerciseId = selectedExercise.id,
                        onExerciseSelected = { selectedExercise = it }
                    )
                }
                item { ExerciseDetailPreview(selectedExercise) }
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
private fun TodayPracticeSection(workout: SampleWorkout) {
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
                    ExerciseMediaPreview(
                        exercise = workout.exercises.first(),
                        modifier = Modifier.size(92.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(workout.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(workout.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PracticeStat(Icons.Outlined.Timer, workout.duration, Modifier.weight(1f))
                    PracticeStat(Icons.Outlined.Speed, workout.level, Modifier.weight(1f))
                    PracticeStat(Icons.Outlined.Home, workout.equipment, Modifier.weight(1f))
                }
                Text(
                    text = "Each exercise includes a looping GIF demo so beginners can copy the movement before training.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    workout.exercises.take(3).forEach { exercise ->
                        ExerciseMediaPreview(
                            exercise = exercise,
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                        )
                    }
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
private fun WorkoutSessionDetailPreview(
    workout: SampleWorkout,
    onExerciseSelected: (SampleExercise) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Workout Session Detail")
        InfoCard(
            icon = Icons.Outlined.PlayArrow,
            title = "How this workout works",
            body = "${workout.exercises.size} exercises • 3 rounds • beginner pace • tap any move to preview its GIF"
        )
        workout.exercises.forEachIndexed { index, exercise ->
            ExerciseSessionItem(
                number = (index + 1).toString(),
                exercise = exercise,
                onClick = { onExerciseSelected(exercise) }
            )
        }
        NotesCard(
            title = "Coach Notes",
            notes = listOf(
                "Preview each GIF first if the move is new to you.",
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
private fun ExerciseSessionItem(
    number: String,
    exercise: SampleExercise,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberCircle(number)
            ExerciseMediaPreview(exercise = exercise, modifier = Modifier.size(62.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${exercise.repsOrDuration} • ${exercise.muscle} • ${exercise.level}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        listOf("All", "Fat Loss", "Muscle Gain", "Beginner", "Home", "Gym", "Short Workouts", "Mobility", "Strength")
            .forEachIndexed { index, label ->
                FilterChip(selected = index == 0, onClick = { }, label = { Text(label) })
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
            tags = listOf("Beginner", "Home", "No equipment")
        )
        ProgramCard(
            title = "Home Strength Foundation",
            goal = "Strength",
            meta = "6 weeks • 3 workouts/week • 30-40 min/session",
            tags = listOf("Beginner", "Dumbbells", "Full body")
        )
        ProgramCard(
            title = "Mobility Reset",
            goal = "Mobility",
            meta = "2 weeks • 5 sessions/week • 15-20 min/session",
            tags = listOf("Recovery", "Home", "Stretching")
        )
    }
}

@Composable
private fun ProgramCard(
    title: String,
    goal: String,
    meta: String,
    tags: List<String>
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ThumbnailBox(icon = Icons.Outlined.FitnessCenter, modifier = Modifier.size(70.dp))
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
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PracticeStat(Icons.Outlined.CalendarMonth, "4 weeks", Modifier.weight(1f))
            PracticeStat(Icons.Outlined.FitnessCenter, "4 days/week", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PracticeStat(Icons.Outlined.Timer, "20-30 min", Modifier.weight(1f))
            PracticeStat(Icons.Outlined.Home, "No equipment", Modifier.weight(1f))
        }
        NotesCard(
            title = "Why this program",
            notes = listOf(
                "Suitable for beginners.",
                "No equipment required.",
                "Short sessions, easy to follow at home."
            )
        )
    }
}

@Composable
private fun ExerciseLibrarySection(
    exercises: List<SampleExercise>,
    selectedExerciseId: String,
    onExerciseSelected: (SampleExercise) -> Unit
) {
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
            listOf("Muscle Group", "Equipment", "Difficulty", "GIF Demo", "Legs", "Core", "Beginner", "No equipment")
                .forEachIndexed { index, label ->
                    FilterChip(selected = index == 6, onClick = { }, label = { Text(label) })
                }
        }
        Text(
            text = "These sample exercises live in the exercise library and show a GIF preview so beginners can follow along visually.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        exercises.forEach { exercise ->
            ExerciseLibraryItem(
                exercise = exercise,
                selected = selectedExerciseId == exercise.id,
                onClick = { onExerciseSelected(exercise) }
            )
        }
    }
}

@Composable
private fun ExerciseLibraryItem(
    exercise: SampleExercise,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExerciseMediaPreview(exercise = exercise, modifier = Modifier.size(72.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(exercise.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "GIF demo • ${exercise.equipment}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun ExerciseDetailPreview(exercise: SampleExercise) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Exercise Detail")
        ExerciseHeroCard(exercise)
        InfoCard(
            icon = Icons.Outlined.PlayArrow,
            title = "Why beginners use this",
            body = exercise.description
        )
        NotesCard(title = "How to do it", notes = exercise.steps)
        NotesCard(title = "Common mistakes", notes = exercise.mistakes, icon = Icons.Outlined.WarningAmber)
        NotesCard(title = "Trainer tips", notes = exercise.tips)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoCard(
                icon = Icons.Outlined.Home,
                title = "Easier",
                body = exercise.easierVariation,
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                icon = Icons.Outlined.Speed,
                title = "Harder",
                body = exercise.harderVariation,
                modifier = Modifier.weight(1f)
            )
        }
        InfoCard(
            icon = Icons.Outlined.AccessibilityNew,
            title = "Target muscles",
            body = exercise.targetMuscles
        )
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
private fun ExerciseHeroCard(exercise: SampleExercise) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ExerciseMediaPreview(
                exercise = exercise,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(exercise.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${exercise.level} • ${exercise.muscle} • ${exercise.equipment}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ChipRow(listOf("GIF Demo", exercise.repsOrDuration, exercise.muscle, exercise.level))
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
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
        ChipRow(listOf("Fat Loss", "Muscle Gain", "General Fitness", "Mobility", "Strength"))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectorBox("Weeks", "4", Modifier.weight(1f))
            SelectorBox("Workouts/week", "4", Modifier.weight(1f))
        }
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
        beginnerExerciseLibrary.take(3).forEach { exercise ->
            BuilderExerciseItem(exercise.title, exercise.repsOrDuration)
        }
        OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text("Add Exercise", modifier = Modifier.padding(start = 8.dp))
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
            Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
            }
        }
    }
}

@Composable
private fun ExerciseMediaPreview(
    exercise: SampleExercise,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = rememberGifImageLoader()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.TopEnd
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(exercise.mediaUrl)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = exercise.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        AssistChip(
            onClick = { },
            label = { Text("GIF") },
            leadingIcon = {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
private fun rememberGifImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}

@Composable
private fun HeaderImageCard(title: String, subtitle: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
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
            Column(modifier = Modifier.weight(1f)) {
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
                Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(labels: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 14.dp)
    ) {
        labels.forEachIndexed { index, label ->
            AssistChip(
                onClick = { },
                label = { Text(label) },
                leadingIcon = if (index == 0) {
                    { Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                }
            )
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
