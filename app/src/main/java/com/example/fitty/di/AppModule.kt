package com.example.fitty.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.WorkManager
import com.example.fitty.BuildConfig
import com.example.fitty.data.exercise.ExerciseGifDownloadManager
import com.example.fitty.data.exercise.ExerciseGifDownloader
import com.example.fitty.data.exercise.OfflineFirstExerciseRepository
import com.example.fitty.data.firebase.FirebaseAuthRepository
import com.example.fitty.data.firebase.FirebaseCoachRepository
import com.example.fitty.data.firebase.FirebaseContentRepository
import com.example.fitty.data.firebase.FirebaseNotificationTokenRepository
import com.example.fitty.data.firebase.FirebaseOnboardingRepository
import com.example.fitty.data.firebase.FirebasePlanRepository
import com.example.fitty.data.firebase.FirebaseStartupRepository
import com.example.fitty.data.firebase.FirebaseTrackingRepository
import com.example.fitty.data.firebase.FirebaseUserRepository
import com.example.fitty.data.firebase.FirebaseWorkoutSessionRepository
import com.example.fitty.data.local.FittyDatabase
import com.example.fitty.data.local.exercise.ExerciseDao
import com.example.fitty.data.local.exercise.ExerciseHistoryDao
import com.example.fitty.data.local.exercise.ExerciseSyncStateDao
import com.example.fitty.data.local.notification.AppNotificationDao
import com.example.fitty.data.local.notification.RoomAppNotificationRepository
import com.example.fitty.data.local.task.HomeTaskDao
import com.example.fitty.data.local.task.RoomHomeTaskRepository
import com.example.fitty.data.remote.GeminiCoachEngine
import com.example.fitty.data.remote.GeminiMealAnalysisEngine
import com.example.fitty.data.remote.GeminiBodyScanAnalysisEngine
import com.example.fitty.data.remote.exercise.ExerciseApiService
import com.example.fitty.data.preferences.AppPreferencesDataSource
import com.example.fitty.data.preferences.PreferencesSessionRepository
import com.example.fitty.domain.repository.AppNotificationRepository
import com.example.fitty.domain.model.BodyScanAnalysisEngine
import com.example.fitty.domain.model.CoachEngine
import com.example.fitty.domain.model.MealAnalysisEngine
import com.example.fitty.domain.repository.AuthRepository
import com.example.fitty.domain.repository.CoachRepository
import com.example.fitty.domain.repository.ContentRepository
import com.example.fitty.domain.repository.ExerciseCatalogRepository
import com.example.fitty.domain.repository.HomeTaskRepository
import com.example.fitty.domain.repository.NotificationTokenRepository
import com.example.fitty.domain.repository.OnboardingRepository
import com.example.fitty.domain.repository.PlanRepository
import com.example.fitty.domain.repository.SessionRepository
import com.example.fitty.domain.repository.StartupRepository
import com.example.fitty.domain.repository.TrackingRepository
import com.example.fitty.domain.repository.UserRepository
import com.example.fitty.domain.repository.WorkoutSessionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds @Singleton
    abstract fun bindStartupRepository(impl: FirebaseStartupRepository): StartupRepository

    @Binds @Singleton
    abstract fun bindOnboardingRepository(impl: FirebaseOnboardingRepository): OnboardingRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: FirebaseUserRepository): UserRepository

    @Binds @Singleton
    abstract fun bindNotificationTokenRepository(impl: FirebaseNotificationTokenRepository): NotificationTokenRepository

    @Binds @Singleton
    abstract fun bindHomeTaskRepository(impl: RoomHomeTaskRepository): HomeTaskRepository

    @Binds @Singleton
    abstract fun bindAppNotificationRepository(impl: RoomAppNotificationRepository): AppNotificationRepository

    @Binds @Singleton
    abstract fun bindExerciseCatalogRepository(impl: OfflineFirstExerciseRepository): ExerciseCatalogRepository

    @Binds @Singleton
    abstract fun bindExerciseGifDownloader(impl: ExerciseGifDownloadManager): ExerciseGifDownloader

    @Binds @Singleton
    abstract fun bindSessionRepository(impl: PreferencesSessionRepository): SessionRepository

    @Binds @Singleton
    abstract fun bindPlanRepository(impl: FirebasePlanRepository): PlanRepository

    @Binds @Singleton
    abstract fun bindTrackingRepository(impl: FirebaseTrackingRepository): TrackingRepository

    @Binds @Singleton
    abstract fun bindCoachRepository(impl: FirebaseCoachRepository): CoachRepository

    @Binds @Singleton
    abstract fun bindContentRepository(impl: FirebaseContentRepository): ContentRepository

    @Binds @Singleton
    abstract fun bindWorkoutSessionRepository(impl: FirebaseWorkoutSessionRepository): WorkoutSessionRepository

    @Binds @Singleton
    abstract fun bindCoachEngine(impl: GeminiCoachEngine): CoachEngine

    @Binds @Singleton
    abstract fun bindMealAnalysisEngine(impl: GeminiMealAnalysisEngine): MealAnalysisEngine

    @Binds @Singleton
    abstract fun bindBodyScanAnalysisEngine(impl: GeminiBodyScanAnalysisEngine): BodyScanAnalysisEngine
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideAppPreferencesDataSource(
        @ApplicationContext context: Context
    ): AppPreferencesDataSource = AppPreferencesDataSource(context)

    @Provides @Singleton
    fun provideFittyDatabase(
        @ApplicationContext context: Context
    ): FittyDatabase = Room.databaseBuilder(
        context,
        FittyDatabase::class.java,
        "fitty.db"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()

    @Provides
    fun provideHomeTaskDao(database: FittyDatabase): HomeTaskDao = database.homeTaskDao()

    @Provides
    fun provideAppNotificationDao(database: FittyDatabase): AppNotificationDao = database.appNotificationDao()

    @Provides
    fun provideExerciseDao(database: FittyDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun provideExerciseSyncStateDao(database: FittyDatabase): ExerciseSyncStateDao = database.exerciseSyncStateDao()

    @Provides
    fun provideExerciseHistoryDao(database: FittyDatabase): ExerciseHistoryDao = database.exerciseHistoryDao()

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)

    @Provides @Singleton
    fun provideGson(): Gson = Gson()

    @Provides @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    @Provides @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides @Singleton
    fun provideRetrofit(
        gson: Gson,
        okHttpClient: OkHttpClient
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.WORKOUTX_BASE_URL.ensureTrailingSlash())
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides @Singleton
    fun provideExerciseApiService(
        retrofit: Retrofit
    ): ExerciseApiService = retrofit.create(ExerciseApiService::class.java)

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `exercises` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `bodyPart` TEXT NOT NULL,
                    `target` TEXT NOT NULL,
                    `equipment` TEXT NOT NULL,
                    `gifUrl` TEXT NOT NULL,
                    `localGifPath` TEXT NOT NULL,
                    `gifVersion` INTEGER NOT NULL,
                    `isDownloaded` INTEGER NOT NULL,
                    `updatedAt` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `exercises_new` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `muscleGroup` TEXT NOT NULL,
                    `bodyPart` TEXT NOT NULL,
                    `target` TEXT NOT NULL,
                    `caloriesBurned` INTEGER NOT NULL,
                    `durationSeconds` INTEGER NOT NULL,
                    `difficulty` TEXT NOT NULL,
                    `equipment` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `instructions` TEXT NOT NULL,
                    `thumbnailUrl` TEXT NOT NULL,
                    `gifUrl` TEXT NOT NULL,
                    `videoUrl` TEXT NOT NULL,
                    `localThumbnailPath` TEXT NOT NULL,
                    `localGifPath` TEXT NOT NULL,
                    `localVideoPath` TEXT NOT NULL,
                    `gifVersion` INTEGER NOT NULL,
                    `isDownloaded` INTEGER NOT NULL,
                    `isFavorite` INTEGER NOT NULL,
                    `remoteVersion` TEXT NOT NULL,
                    `updatedAt` TEXT NOT NULL,
                    `syncStatus` TEXT NOT NULL,
                    `mediaDownloadProgress` REAL NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO exercises_new (
                    id, name, muscleGroup, bodyPart, target, caloriesBurned, durationSeconds,
                    difficulty, equipment, description, instructions, thumbnailUrl, gifUrl, videoUrl,
                    localThumbnailPath, localGifPath, localVideoPath, gifVersion, isDownloaded,
                    isFavorite, remoteVersion, updatedAt, syncStatus, mediaDownloadProgress
                )
                SELECT
                    id,
                    name,
                    bodyPart,
                    bodyPart,
                    target,
                    0,
                    0,
                    '',
                    equipment,
                    '',
                    '',
                    '',
                    gifUrl,
                    '',
                    '',
                    localGifPath,
                    '',
                    gifVersion,
                    isDownloaded,
                    0,
                    '',
                    updatedAt,
                    'legacy',
                    CASE WHEN isDownloaded = 1 THEN 1.0 ELSE 0.0 END
                FROM exercises
                """.trimIndent()
            )
            database.execSQL("DROP TABLE exercises")
            database.execSQL("ALTER TABLE exercises_new RENAME TO exercises")
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `exercise_sync_state` (
                    `id` TEXT NOT NULL,
                    `isSyncing` INTEGER NOT NULL,
                    `isOnline` INTEGER NOT NULL,
                    `lastSuccessfulSyncAt` TEXT,
                    `lastAttemptedSyncAt` TEXT,
                    `apiVersion` TEXT,
                    `deltaToken` TEXT,
                    `totalExercises` INTEGER NOT NULL,
                    `downloadedImages` INTEGER NOT NULL,
                    `downloadedGifs` INTEGER NOT NULL,
                    `downloadedVideos` INTEGER NOT NULL,
                    `progress` REAL NOT NULL,
                    `lastErrorMessage` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `exercise_history` (
                    `exerciseId` TEXT NOT NULL,
                    `lastViewedAt` TEXT NOT NULL,
                    PRIMARY KEY(`exerciseId`)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                ALTER TABLE exercises
                ADD COLUMN gifStoragePath TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                ALTER TABLE exercises
                ADD COLUMN thumbnailStoragePath TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                ALTER TABLE exercise_sync_state
                ADD COLUMN statusCode TEXT
                """.trimIndent()
            )
        }
    }

    private fun String.ensureTrailingSlash(): String =
        if (endsWith("/")) this else "$this/"
}
