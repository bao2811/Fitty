package com.example.fitty.feature_workout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitty.R
import com.example.fitty.core.ui.AppLocaleManager
import com.example.fitty.core.ui.ContentDebugSource
import com.example.fitty.core.ui.ContentSourceState
import com.example.fitty.data.content.ExercisePrescriptionResolver
import com.example.fitty.data.exercise.ExerciseGifDownloader
import com.example.fitty.domain.model.Exercise
import com.example.fitty.domain.model.ExercisePrescriptionContent
import com.example.fitty.domain.model.ExerciseLog
import com.example.fitty.domain.model.ExerciseQuery
import com.example.fitty.domain.model.FittyUser
import com.example.fitty.domain.model.QuickWorkoutConfig
import com.example.fitty.domain.model.ScheduledWorkout
import com.example.fitty.domain.model.WorkoutExercise
import com.example.fitty.domain.model.WorkoutSession
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import com.example.fitty.domain.repository.PlanRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.WorkoutSessionRepository
import com.example.fitty.domain.usecase.user.GetCurrentUserUseCase
import com.example.fitty.domain.usecase.user.UpdateStreakUseCase
import com.example.fitty.domain.usecase.workout.CompleteWorkoutSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutExerciseItem(
    val logId: String = "",
    val exercise: Exercise,
    val requiredSeconds: Int = 30,
    val plannedSets: Int = 0,
    val targetRepsLabel: String? = null,
    val targetWeightKg: Float? = null,
    val targetWeightLabel: String? = null,
    val targetWeightBasisLabel: String? = null,
    val repsBySetInput: List<String> = emptyList(),
    val weightKgBySetInput: List<String> = emptyList(),
    val elapsedSeconds: Int = 0,
    val isCompleted: Boolean = false,
    val isActive: Boolean = false,
    val isTimerRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isGifLoading: Boolean = false
)

data class WorkoutSessionUiState(
    val sessionId: String = "",
    val title: String = "",
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isResting: Boolean = false,
    val isCompleted: Boolean = false,
    val isSubmittingSession: Boolean = false,
    val totalElapsedSeconds: Int = 0,
    val exerciseItems: List<WorkoutExerciseItem> = emptyList(),
    val activeIndex: Int = 0,
    val estimatedCalories: Int = 0,
    val restElapsedSeconds: Int = 0,
    val restDurationSeconds: Int = 60,
    val isLoadingExercises: Boolean = true,
    val hasResolvedExercises: Boolean = false,
    val error: String? = null,
    val contentSources: List<ContentDebugSource> = emptyList()
) {
    val completedCount: Int get() = exerciseItems.count { it.isCompleted }
    val totalCount: Int get() = exerciseItems.size
    val activeExercise: WorkoutExerciseItem? get() = exerciseItems.getOrNull(activeIndex)
}

@HiltViewModel
class WorkoutSessionViewModel @Inject constructor(
    private val completeWorkoutSessionUseCase: CompleteWorkoutSessionUseCase,
    private val updateStreakUseCase: UpdateStreakUseCase,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseCatalogRepository,
    private val planRepository: PlanRepository,
    private val contentRepository: ContentRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val exercisePrescriptionResolver: ExercisePrescriptionResolver,
    private val gifDownloadManager: ExerciseGifDownloader,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val countdownAudioPlayer = WorkoutCountdownAudioPlayer()
    private val restDurationSeconds = 60

    private fun defaultWeightInput(targetWeightKg: Float?): String {
        val target = targetWeightKg ?: return ""
        return if (target % 1f == 0f) {
            target.toInt().toString()
        } else {
            target.toString()
        }
    }

    private fun targetWeightBasisLabel(targetWeightKg: Float?, targetWeightLabel: String?): String? {
        val weightKg = currentUser?.profile?.weightKg ?: return null
        if (targetWeightKg == null && targetWeightLabel.isNullOrBlank()) return null
        return if (currentLanguage.lowercase(java.util.Locale.US).startsWith("vi")) {
            "Theo cân nặng $weightKg kg"
        } else {
            "Based on ${weightKg} kg body weight"
        }
    }

    private val _uiState = MutableStateFlow(
        WorkoutSessionUiState(
            title = context.getString(R.string.plan_quick_workout_title),
            contentSources = listOf(
                ContentDebugSource("Quick workout config", ContentSourceState.Fallback, "Using local fallback until remote load completes"),
                ContentDebugSource("Exercise prescriptions", ContentSourceState.Fallback, "Using local fallback until remote load completes")
            )
        )
    )
    val uiState: StateFlow<WorkoutSessionUiState> = _uiState

    private var globalTimerRunning = false
    private var restTimerRunning = false
    private var planId: String = ""
    private var scheduledWorkoutId: String = ""
    private var firestoreSessionId: String = ""
    private var scheduledWorkout: ScheduledWorkout? = null
    private var quickWorkoutConfig: QuickWorkoutConfig = QuickWorkoutConfig(
        preferredBodyPartOrder = listOf(
            "chest", "back", "shoulders", "upper arms", "lower arms",
            "waist", "upper legs", "lower legs", "cardio", "neck"
        )
    )
    private var currentLanguage: String = AppLocaleManager.resolveStoredLanguage(context)
    private var currentUser: FittyUser? = null
    private var prescriptionCatalog: List<ExercisePrescriptionContent> = emptyList()

    fun initialize(sessionId: String, planId: String = "", scheduledWorkoutId: String = "") {
        globalTimerRunning = false
        this.planId = planId
        this.scheduledWorkoutId = scheduledWorkoutId
        this.firestoreSessionId = sessionId
        scheduledWorkout = null
        _uiState.value = WorkoutSessionUiState(
            sessionId = sessionId,
            title = context.getString(R.string.plan_quick_workout_title),
            restDurationSeconds = restDurationSeconds,
            contentSources = listOf(
                ContentDebugSource("Quick workout config", ContentSourceState.Fallback, "Using local fallback until remote load completes"),
                ContentDebugSource("Exercise prescriptions", ContentSourceState.Fallback, "Using local fallback until remote load completes")
            )
        )
        loadSessionContextAndExercises()
    }

    private fun loadSessionContextAndExercises() {
        viewModelScope.launch {
            currentLanguage = AppLocaleManager.resolveStoredLanguage(context)
            currentUser = runCatching { getCurrentUserUseCase() }.getOrNull()
            quickWorkoutConfig = contentRepository.getQuickWorkoutConfig(currentLanguage)
            prescriptionCatalog = contentRepository.getExercisePrescriptions(currentLanguage)
            val usedQuickWorkoutFallback = contentRepository.usedFallbackFor("quick_workout_behavior")
            val usedPrescriptionFallback = contentRepository.usedFallbackFor("exercise_prescriptions")
            val sources = listOf(
                ContentDebugSource(
                    "Quick workout config",
                    if (usedQuickWorkoutFallback) ContentSourceState.Fallback else ContentSourceState.Remote,
                    contentRepository.fallbackDetailFor("quick_workout_behavior")
                        ?: if (usedQuickWorkoutFallback) "Using local fallback" else "Loaded language=$currentLanguage from Firebase"
                ),
                ContentDebugSource(
                    "Exercise prescriptions",
                    if (usedPrescriptionFallback) ContentSourceState.Fallback else ContentSourceState.Remote,
                    contentRepository.fallbackDetailFor("exercise_prescriptions")
                        ?: if (usedPrescriptionFallback) "Using local fallback" else "Loaded ${prescriptionCatalog.size} rules from Firebase"
                )
            )
            _uiState.update { it.copy(contentSources = sources) }
            if (planId.isNotBlank() && scheduledWorkoutId.isNotBlank()) {
                val uid = sessionRepository.getCurrentUserId()
                scheduledWorkout = if (uid != null) {
                    runCatching {
                        planRepository.getScheduledWorkout(uid, planId, scheduledWorkoutId)
                    }.getOrNull()
                } else {
                    null
                }
                scheduledWorkout?.let { workout ->
                    _uiState.update {
                        it.copy(
                            title = workout.title.ifBlank {
                                context.getString(R.string.workout_title_scheduled_fallback)
                            }
                        )
                    }
                }
            }
            loadExercises()
        }
    }

    private fun loadExercises() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingExercises = true,
                    hasResolvedExercises = false,
                    isSubmittingSession = false,
                    error = null
                )
            }
            try {
                exerciseRepository.observeExercises(ExerciseQuery(limit = 500)).collect { allExercises ->
                    val items = buildWorkoutItems(allExercises)
                    val shouldKeepLoading = allExercises.isEmpty() && items.isEmpty()
                    if (shouldKeepLoading) {
                        _uiState.update {
                            it.copy(
                                isLoadingExercises = true,
                                hasResolvedExercises = false,
                                error = null
                            )
                        }
                        return@collect
                    }

                    val previousIds = _uiState.value.exerciseItems.map { it.exercise.id }
                    val nextIds = items.map { it.exercise.id }
                    val shouldReplaceItems = previousIds != nextIds || _uiState.value.exerciseItems.isEmpty()

                    _uiState.update { state ->
                        val nextItems = if (shouldReplaceItems) {
                            items.mapIndexed { i, item -> item.copy(isActive = i == 0) }
                        } else {
                            state.exerciseItems
                        }

                        state.copy(
                            exerciseItems = nextItems,
                            activeIndex = if (nextItems.isNotEmpty()) {
                                state.activeIndex.coerceIn(0, nextItems.lastIndex)
                            } else {
                                0
                            },
                            isLoadingExercises = false,
                            hasResolvedExercises = true,
                            error = if (nextItems.isEmpty()) {
                                context.getString(R.string.workout_no_matching_exercises)
                            } else {
                                state.error?.takeIf { state.isSubmittingSession }
                            }
                        )
                    }

                    if (shouldReplaceItems && items.isNotEmpty()) {
                        prefetchGif(0)
                    } else {
                        _uiState.value.activeExercise?.let { active ->
                            val activeIndex = _uiState.value.activeIndex
                            if (active.exercise.localGifPath.isBlank() && active.exercise.gifUrl.isNotBlank()) {
                                prefetchGif(activeIndex)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingExercises = false,
                        hasResolvedExercises = true,
                        error = e.message ?: context.getString(R.string.workout_load_failed)
                    )
                }
            }
        }
    }

    private fun buildWorkoutItems(allExercises: List<Exercise>): List<WorkoutExerciseItem> {
        buildPlannedWorkoutItems(allExercises)?.let { return it }

        val sourceExercises = allExercises
            .filter(::hasUsablePreview)
            .ifEmpty { allExercises.filter(::hasUsableIdentity) }
        if (sourceExercises.isEmpty()) return emptyList()

        val targetCount = quickWorkoutConfig.targetExerciseCount.coerceAtLeast(1)
        val selected = mutableListOf<Exercise>()
        val byBodyPart = sourceExercises
            .sortedWith(compareBy({ it.bodyPart.lowercase() }, { it.name.lowercase() }, { it.id.lowercase() }))
            .groupBy { it.bodyPart.trim().lowercase() }

        val preferredOrder = quickWorkoutConfig.preferredBodyPartOrder

        preferredOrder.forEach { bodyPart ->
            if (selected.size >= targetCount) return@forEach
            val candidate = byBodyPart[bodyPart]
                ?.firstOrNull { exercise -> selected.none { it.id == exercise.id } }
            if (candidate != null) {
                selected += candidate
            }
        }

        if (selected.size < targetCount) {
            val remaining = sourceExercises
                .sortedWith(compareBy({ it.name.lowercase() }, { it.id.lowercase() }))
                .filter { candidate -> selected.none { it.id == candidate.id } }
            selected.addAll(remaining.take(targetCount - selected.size))
        }

        return selected.take(targetCount).map { exercise ->
            val recommendation = exercisePrescriptionResolver.resolve(
                exercise = exercise,
                user = currentUser,
                language = currentLanguage,
                catalog = prescriptionCatalog
            )
            val duration = recommendation?.durationSeconds
                ?: exercise.defaultDurationSeconds
                ?: exercise.durationSeconds.takeIf { it > 0 }
                ?: quickWorkoutConfig.defaultDurationSeconds
            val plannedSets = recommendation?.sets
                ?: if (exercise.defaultRepsText.isNotBlank()) quickWorkoutConfig.defaultSets else 0
            val targetWeight = recommendation?.targetWeightKg
            WorkoutExerciseItem(
                exercise = exercise,
                requiredSeconds = duration,
                plannedSets = plannedSets,
                targetRepsLabel = recommendation?.reps ?: exercise.defaultRepsText.ifBlank { null },
                targetWeightKg = targetWeight,
                targetWeightLabel = recommendation?.targetWeightLabel,
                targetWeightBasisLabel = targetWeightBasisLabel(targetWeight, recommendation?.targetWeightLabel),
                repsBySetInput = List(plannedSets) { "" },
                weightKgBySetInput = List(plannedSets) { defaultWeightInput(targetWeight) }
            )
        }
    }

    private fun buildPlannedWorkoutItems(allExercises: List<Exercise>): List<WorkoutExerciseItem>? {
        val plannedExercises = scheduledWorkout?.exercises.orEmpty()
        if (plannedExercises.isEmpty()) return null

        val exercisesById = allExercises.associateBy { it.id }
        val items = plannedExercises.mapNotNull { planned ->
            val exercise = exercisesById[planned.exerciseId] ?: return@mapNotNull null
            val plannedSets = planned.sets.coerceAtLeast(0)
            WorkoutExerciseItem(
                exercise = exercise,
                requiredSeconds = planned.durationSeconds
                    ?: exercise.defaultDurationSeconds
                    ?: exercise.durationSeconds.takeIf { it > 0 }
                    ?: 30,
                plannedSets = plannedSets,
                targetRepsLabel = planned.reps,
                targetWeightKg = planned.targetWeightKg,
                targetWeightLabel = null,
                targetWeightBasisLabel = targetWeightBasisLabel(planned.targetWeightKg, null),
                repsBySetInput = List(plannedSets) { "" },
                weightKgBySetInput = List(plannedSets) { defaultWeightInput(planned.targetWeightKg) }
            )
        }
        return items.takeIf { it.isNotEmpty() }
    }

    private fun hasUsablePreview(exercise: Exercise): Boolean {
        return exercise.localThumbnailPath.isNotBlank() ||
            exercise.thumbnailUrl.isNotBlank() ||
            exercise.localGifPath.isNotBlank() ||
            exercise.gifUrl.isNotBlank() ||
            exercise.mediaUrl.isNotBlank()
    }

    private fun hasUsableIdentity(exercise: Exercise): Boolean {
        return exercise.id.isNotBlank() && exercise.name.isNotBlank()
    }

    private fun prefetchGif(index: Int) {
        val item = _uiState.value.exerciseItems.getOrNull(index) ?: return
        val exercise = item.exercise
        if (exercise.localGifPath.isBlank() && exercise.gifUrl.isNotBlank()) {
            viewModelScope.launch {
                updateItem(index) { it.copy(isGifLoading = true) }
                gifDownloadManager.download(exercise)
                val updated = exerciseRepository.getExercise(exercise.id)
                if (updated != null) {
                    updateItem(index) { it.copy(exercise = updated, isGifLoading = false) }
                } else {
                    updateItem(index) { it.copy(isGifLoading = false) }
                }
            }
        }
    }

    private suspend fun syncExerciseLogIds(uid: String, sessionId: String) {
        val savedSession = workoutSessionRepository.getSession(uid, sessionId) ?: return
        val savedLogs = savedSession.exercises.sortedBy { it.orderIndex }
        _uiState.update { state ->
            state.copy(
                exerciseItems = state.exerciseItems.mapIndexed { index, item ->
                    item.copy(logId = savedLogs.getOrNull(index)?.id.orEmpty())
                }
            )
        }
    }

    fun selectExercise(index: Int) {
        if (_uiState.value.isResting) return
        _uiState.update { state ->
            state.copy(
                activeIndex = index,
                exerciseItems = state.exerciseItems.mapIndexed { i, item ->
                    item.copy(isActive = i == index)
                }
            )
        }
        prefetchGif(index)
    }

    fun updateSetReps(setIndex: Int, value: String) {
        val idx = _uiState.value.activeIndex
        updateItem(idx) { item ->
            if (setIndex !in item.repsBySetInput.indices) return@updateItem item
            item.copy(
                repsBySetInput = item.repsBySetInput.mapIndexed { index, current ->
                    if (index == setIndex) value.filter(Char::isDigit) else current
                }
            )
        }
    }

    fun updateSetWeight(setIndex: Int, value: String) {
        val idx = _uiState.value.activeIndex
        updateItem(idx) { item ->
            if (setIndex !in item.weightKgBySetInput.indices) return@updateItem item
            val sanitized = value.filterIndexed { index, char ->
                char.isDigit() || (char == '.' && value.take(index).none { it == '.' })
            }
            item.copy(
                weightKgBySetInput = item.weightKgBySetInput.mapIndexed { index, current ->
                    if (index == setIndex) sanitized else current
                }
            )
        }
    }

    fun startWorkout() {
        if (_uiState.value.isRunning || _uiState.value.isSubmittingSession) return
        _uiState.update { it.copy(isSubmittingSession = true, error = null) }
        viewModelScope.launch {
            val uid = sessionRepository.getCurrentUserId()
            if (uid == null) {
                _uiState.update {
                    it.copy(
                        isSubmittingSession = false,
                        error = context.getString(R.string.workout_start_failed)
                    )
                }
                return@launch
            }

            val result = runCatching {
                val exerciseLogs = _uiState.value.exerciseItems.mapIndexed { index, item ->
                    ExerciseLog(
                        exerciseId = item.exercise.id,
                        name = item.exercise.name,
                        orderIndex = index,
                        plannedSets = item.plannedSets,
                        completedSets = 0,
                        completed = false
                    )
                }
                val session = WorkoutSession(
                    planId = planId,
                    scheduledWorkoutId = scheduledWorkoutId,
                    title = _uiState.value.title,
                    source = if (planId.isNotBlank()) "plan" else "quick_template",
                    status = "in_progress",
                    startedAt = System.currentTimeMillis(),
                    plannedExercises = _uiState.value.exerciseItems.map { item ->
                        WorkoutExercise(
                            exerciseId = item.exercise.id,
                            name = item.exercise.name,
                            sets = item.plannedSets,
                            reps = item.targetRepsLabel,
                            durationSeconds = item.requiredSeconds.takeIf { item.plannedSets == 0 },
                            targetWeightKg = item.targetWeightKg
                        )
                    },
                    exercises = exerciseLogs
                )
                workoutSessionRepository.startSession(uid, session)
            }.getOrElse { throwable ->
                Result.failure(throwable)
            }

            result
                .onSuccess { newId ->
                    firestoreSessionId = newId
                    _uiState.update {
                        it.copy(
                            sessionId = newId,
                            isRunning = true,
                            isPaused = false,
                            isSubmittingSession = false,
                            error = null
                        )
                    }
                    syncExerciseLogIds(uid = uid, sessionId = newId)
                    startGlobalTimer()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            isSubmittingSession = false,
                            error = error.message ?: context.getString(R.string.workout_start_failed)
                        )
                    }
                }
        }
    }

    fun startExerciseTimer() {
        if (_uiState.value.isResting) return
        val idx = _uiState.value.activeIndex
        val item = _uiState.value.exerciseItems.getOrNull(idx) ?: return
        if (item.isCompleted || item.isTimerRunning) return
        updateItem(idx) { it.copy(isTimerRunning = true, isPaused = false) }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value.exerciseItems.getOrNull(idx) ?: break
                if (!current.isTimerRunning || current.isPaused) {
                    if (!current.isTimerRunning) break
                    continue
                }
                val nextElapsedSeconds = current.elapsedSeconds + 1
                updateItem(idx) { it.copy(elapsedSeconds = nextElapsedSeconds) }
                if (WorkoutCountdownSignal.shouldPlayTick(nextElapsedSeconds, current.requiredSeconds)) {
                    countdownAudioPlayer.playTick()
                }
            }
        }
    }

    fun pauseExerciseTimer() {
        if (_uiState.value.isResting) return
        val idx = _uiState.value.activeIndex
        updateItem(idx) { it.copy(isPaused = !it.isPaused) }
    }

    fun completeExercise() {
        val idx = _uiState.value.activeIndex
        val item = _uiState.value.exerciseItems.getOrNull(idx) ?: return
        if (item.elapsedSeconds < item.requiredSeconds) return
        updateItem(idx) { it.copy(isTimerRunning = false, isCompleted = true, isActive = false) }
        _uiState.update {
            it.copy(
                estimatedCalories = it.estimatedCalories + (
                    item.elapsedSeconds * (quickWorkoutConfig.caloriesPerMinute / 60f)
                ).toInt()
            )
        }
        viewModelScope.launch {
            persistCompletedExercise(idx)
        }
        val next = _uiState.value.exerciseItems.indexOfFirst { !it.isCompleted && it != item }
        if (next >= 0) {
            prepareRestForNextExercise(next)
        }
    }

    fun skipRest() {
        if (!_uiState.value.isResting) return
        stopRestTimer()
        _uiState.update {
            it.copy(
                isResting = false,
                restElapsedSeconds = 0
            )
        }
    }

    fun finishWorkout(onComplete: () -> Unit) {
        if (_uiState.value.isSubmittingSession) return
        globalTimerRunning = false
        stopRestTimer()
        val state = _uiState.value
        val sessionId = firestoreSessionId.ifBlank { state.sessionId }
        if (sessionId.isBlank()) {
            _uiState.update {
                it.copy(
                    isRunning = false,
                    isSubmittingSession = false,
                    error = context.getString(R.string.workout_finish_failed)
                )
            }
            return
        }
        _uiState.update { it.copy(isSubmittingSession = true, error = null) }
        val exerciseLogs = state.exerciseItems.mapIndexedNotNull { index, item ->
            item.takeIf { it.isCompleted }?.toCompletedExerciseLog(index)
        }
        viewModelScope.launch {
            completeWorkoutSessionUseCase(
                sessionId = sessionId,
                durationMinutes = state.totalElapsedSeconds / 60,
                caloriesBurned = state.estimatedCalories,
                completionRate = if (state.totalCount > 0) {
                    state.completedCount.toFloat() / state.totalCount
                } else {
                    1f
                },
                perceivedEffort = null,
                exercises = exerciseLogs,
                planId = planId,
                scheduledWorkoutId = scheduledWorkoutId
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isCompleted = true,
                            isRunning = false,
                            isPaused = false,
                            isResting = false,
                            isSubmittingSession = false,
                            restElapsedSeconds = 0,
                            error = null
                        )
                    }
                    runCatching { updateStreakUseCase(reason = "workout", incrementActivityCounters = false) }
                    onComplete()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCompleted = false,
                            isRunning = true,
                            isResting = false,
                            isSubmittingSession = false,
                            error = error.message ?: context.getString(R.string.workout_finish_failed)
                        )
                    }
                    startGlobalTimer()
                }
        }
    }

    private suspend fun persistCompletedExercise(index: Int) {
        val uid = sessionRepository.getCurrentUserId() ?: return
        val sessionId = firestoreSessionId.ifBlank { _uiState.value.sessionId }
        if (sessionId.isBlank()) return

        var item = _uiState.value.exerciseItems.getOrNull(index) ?: return
        if (item.logId.isBlank()) {
            syncExerciseLogIds(uid = uid, sessionId = sessionId)
            item = _uiState.value.exerciseItems.getOrNull(index) ?: return
        }
        if (item.logId.isBlank() || !item.isCompleted) return

        val exerciseLog = item.toCompletedExerciseLog(index)
        workoutSessionRepository.updateExerciseLog(uid, sessionId, exerciseLog)
            .onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: context.getString(R.string.workout_finish_failed))
                }
            }
    }

    private fun WorkoutExerciseItem.toCompletedExerciseLog(orderIndex: Int): ExerciseLog {
        val parsedReps = repsBySetInput.mapNotNull { input -> input.toIntOrNull() }
        val parsedWeights = weightKgBySetInput.mapNotNull { input -> input.toFloatOrNull() }
        return ExerciseLog(
            id = logId,
            exerciseId = exercise.id,
            name = exercise.name,
            orderIndex = orderIndex,
            plannedSets = plannedSets,
            completedSets = when {
                plannedSets > 0 && parsedReps.isNotEmpty() -> parsedReps.size
                plannedSets > 0 && isCompleted -> plannedSets
                isCompleted -> 1
                else -> 0
            },
            repsBySet = parsedReps,
            weightKgBySet = parsedWeights,
            durationSeconds = elapsedSeconds,
            completed = true
        )
    }

    private fun startGlobalTimer() {
        globalTimerRunning = true
        viewModelScope.launch {
            while (globalTimerRunning) {
                delay(1000)
                if (globalTimerRunning) {
                    _uiState.update { it.copy(totalElapsedSeconds = it.totalElapsedSeconds + 1) }
                }
            }
        }
    }

    private fun prepareRestForNextExercise(nextIndex: Int) {
        stopRestTimer()
        _uiState.update { state ->
            state.copy(
                activeIndex = nextIndex,
                isResting = true,
                restElapsedSeconds = 0,
                exerciseItems = state.exerciseItems.mapIndexed { i, current ->
                    current.copy(isActive = i == nextIndex)
                }
            )
        }
        prefetchGif(nextIndex)
        startRestTimer()
    }

    private fun startRestTimer() {
        restTimerRunning = true
        viewModelScope.launch {
            while (restTimerRunning) {
                delay(1000)
                if (!restTimerRunning) break
                val state = _uiState.value
                if (!state.isResting) break
                val nextElapsed = state.restElapsedSeconds + 1
                if (nextElapsed >= state.restDurationSeconds) {
                    _uiState.update {
                        it.copy(
                            isResting = false,
                            restElapsedSeconds = 0
                        )
                    }
                    restTimerRunning = false
                } else {
                    _uiState.update { it.copy(restElapsedSeconds = nextElapsed) }
                }
            }
        }
    }

    private fun stopRestTimer() {
        restTimerRunning = false
    }

    private fun updateItem(index: Int, transform: (WorkoutExerciseItem) -> WorkoutExerciseItem) {
        _uiState.update { state ->
            state.copy(
                exerciseItems = state.exerciseItems.mapIndexed { i, item ->
                    if (i == index) transform(item) else item
                }
            )
        }
    }

    override fun onCleared() {
        stopRestTimer()
        countdownAudioPlayer.release()
        super.onCleared()
    }
}
