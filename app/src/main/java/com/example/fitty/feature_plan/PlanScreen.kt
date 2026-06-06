package com.example.fitty.feature_plan

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitty.R
import com.example.fitty.core.ui.ContentDebugSource
import com.example.fitty.core.ui.ContentSourceState
import com.example.fitty.core.ui.AppLocaleManager
import com.example.fitty.core.ui.ExerciseSyncSuccessStyle
import com.example.fitty.core.ui.toStatusText
import com.example.fitty.data.content.LocalContentFallbacks
import com.example.fitty.domain.model.PracticeCategoryContent
import com.example.fitty.domain.model.ExerciseQuery
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.usecase.exercise.ObserveExerciseSyncStateUseCase
import com.example.fitty.domain.usecase.exercise.SyncExercisesUseCase
import com.example.fitty.ui.theme.FittyPink
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

// ── Category definition ─────────────────────────────────────────────

/**
 * Represents a muscle-group / exercise category shown on the Practice grid.
 *
 * [bodyPartKeys] maps to the `bodyPart` field stored in Firestore exercises.
 * [assetImage] is the filename of the category thumbnail in the assets folder.
 */
internal data class ExerciseCategoryDef(
    val id: String,
    val label: String,
    val bodyPartKeys: List<String>,
    val assetImage: String,
    val cardColor: Color = Color(0xFFE8DEF8)
)

/** The fixed category grid – order matches the reference design. */
private val PRACTICE_CATEGORIES = emptyList<ExerciseCategoryDef>()

// ── UI state ─────────────────────────────────────────────────────────

internal data class PracticeCategoryUi(
    val def: ExerciseCategoryDef,
    val exerciseCount: Int = 0
)

internal data class PracticeUiState(
    val categories: List<PracticeCategoryUi> = emptyList(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val syncStatusCode: String? = null,
    val syncMessage: String? = null,
    val contentSources: List<ContentDebugSource> = emptyList()
)

// ── ViewModel ────────────────────────────────────────────────────────

@HiltViewModel
class PlanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localContentFallbacks: LocalContentFallbacks,
    private val contentRepository: ContentRepository,
    private val exerciseRepository: ExerciseCatalogRepository,
    private val sessionRepository: SessionRepository,
    private val observeExerciseSyncStateUseCase: ObserveExerciseSyncStateUseCase,
    private val syncExercisesUseCase: SyncExercisesUseCase
) : ViewModel() {

    private var practiceCategories: List<ExerciseCategoryDef> = localContentFallbacks.practiceCategories().map { it.toUiCategory() }
    private val _uiState = MutableStateFlow(
        PracticeUiState(
            categories = practiceCategories.map { PracticeCategoryUi(it, 0) },
            contentSources = listOf(
                ContentDebugSource("Practice categories", ContentSourceState.Fallback, "Using local fallback until remote load completes")
            )
        )
    )
    internal val uiState: StateFlow<PracticeUiState> = _uiState

    init {
        loadContent()
        observeExercises()
        observeSyncState()
        syncMetadata()
    }

    private fun loadContent() {
        viewModelScope.launch {
            val language = AppLocaleManager.resolveStoredLanguage(context)
            practiceCategories = contentRepository.getPracticeCategories(language).map { it.toUiCategory() }
            val usedFallback = contentRepository.usedFallbackFor("practice_categories")
            _uiState.update { state ->
                state.copy(
                    categories = practiceCategories.map { category ->
                        val existingCount = state.categories.firstOrNull { it.def.id == category.id }?.exerciseCount ?: 0
                        PracticeCategoryUi(category, existingCount)
                    },
                    contentSources = listOf(
                        ContentDebugSource(
                            "Practice categories",
                            if (usedFallback) ContentSourceState.Fallback else ContentSourceState.Remote,
                            contentRepository.fallbackDetailFor("practice_categories")
                                ?: if (usedFallback) "Using local fallback" else "Loaded language=$language from Firebase"
                        )
                    )
                )
            }
        }
    }

    private fun observeExercises() {
        viewModelScope.launch {
            exerciseRepository.observeExercises(ExerciseQuery(limit = 500)).collect { exercises ->
                val categoryUis = practiceCategories.map { def ->
                    val matching = exercises.filter { exercise ->
                        def.bodyPartKeys.any { key ->
                            exercise.bodyPart.equals(key, ignoreCase = true)
                        }
                    }
                    PracticeCategoryUi(
                        def = def,
                        exerciseCount = matching.size
                    )
                }
                _uiState.update {
                    it.copy(
                        categories = categoryUis,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            observeExerciseSyncStateUseCase().collect { syncState ->
                _uiState.update { state ->
                    state.copy(
                        isSyncing = syncState.isSyncing,
                        syncStatusCode = syncState.statusCode,
                        syncMessage = syncState.toStatusText(
                            context = context,
                            successStyle = ExerciseSyncSuccessStyle.Count
                        )
                    )
                }
            }
        }
    }

    private fun syncMetadata() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncStatusCode = null, syncMessage = null) }
            val result = syncExercisesUseCase(force = false)
            _uiState.update { state ->
                state.copy(
                    isSyncing = false,
                    syncMessage = result.fold(
                        onSuccess = { report ->
                            context.getString(R.string.plan_sync_success, report.usable)
                        },
                        onFailure = { error ->
                            error.message ?: context.getString(R.string.plan_sync_cached_fallback)
                        }
                    )
                )
            }
        }
    }
}

// ── Route ────────────────────────────────────────────────────────────

@Composable
fun PlanRoute(
    onCategorySelected: (categoryId: String, categoryLabel: String, bodyPartKeys: List<String>) -> Unit = { _, _, _ -> },
    onStartQuickWorkout: () -> Unit = {},
    viewModel: PlanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    PracticeScreen(
        state = state,
        onCategoryClicked = { category ->
            onCategorySelected(
                category.def.id,
                category.def.label,
                category.def.bodyPartKeys
            )
        },
        onStartQuickWorkout = onStartQuickWorkout
    )
}

// ── Screen ───────────────────────────────────────────────────────────

@Composable
private fun PracticeScreen(
    state: PracticeUiState,
    onCategoryClicked: (PracticeCategoryUi) -> Unit,
    onStartQuickWorkout: () -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Title ────────────────────────────────────────────────
        item(span = { GridItemSpan(2) }) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.plan_practice_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                state.syncMessage?.let { syncMessage ->
                    Text(
                        text = syncMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // ── Quick Workout Card ───────────────────────────────────
        item(span = { GridItemSpan(2) }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FittyPink),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onStartQuickWorkout)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = stringResource(R.string.plan_quick_workout_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.plan_quick_workout_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ── Category cards ───────────────────────────────────────
        items(state.categories, key = { it.def.id }) { category ->
            PracticeCategoryCard(
                category = category,
                onClick = { onCategoryClicked(category) }
            )
        }

        // ── Loading indicator ────────────────────────────────────
        if (state.isLoading) {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FittyPink)
                }
            }
        }

        // Bottom spacer
        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Category card ────────────────────────────────────────────────────

@Composable
private fun PracticeCategoryCard(
    category: PracticeCategoryUi,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val categoryLabel = category.def.label
    val assetBitmap = remember(category.def.assetImage) {
        try {
            context.assets.open(category.def.assetImage).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = category.def.cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image from assets
            if (assetBitmap != null) {
                Image(
                    bitmap = assetBitmap,
                    contentDescription = categoryLabel,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                )
                // Gradient overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
            } else {
                // Fallback icon when no image available
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Category label at top-left
            Text(
                text = categoryLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (assetBitmap != null) Color.White else Color(0xFF1C1B2B),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
            )

            // Exercise count badge at bottom-right
            if (category.exerciseCount > 0) {
                Text(
                    text = stringResource(R.string.plan_category_exercise_count, category.exerciseCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun PracticeCategoryContent.toUiCategory(): ExerciseCategoryDef {
    return ExerciseCategoryDef(
        id = id,
        label = label,
        bodyPartKeys = bodyPartKeys,
        assetImage = assetImage,
        cardColor = cardColorHex.toColorOrDefault()
    )
}

private fun String.toColorOrDefault(): Color {
    return runCatching { Color(android.graphics.Color.parseColor(this)) }
        .getOrDefault(Color(0xFFE8DEF8))
}
