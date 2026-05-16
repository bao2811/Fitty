package com.example.fitty.data.remote.exercise

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ExerciseApiService {
    @GET("v1/exercises")
    suspend fun getExercises(
        @Query("cursor") cursor: String? = null,
        @Query("pageSize") pageSize: Int = 100,
        @Query("updatedAfter") updatedAfter: String? = null,
        @Query("search") search: String? = null,
        @Query("muscleGroup") muscleGroup: String? = null,
        @Query("difficulty") difficulty: String? = null
    ): ExercisePageDto

    @Streaming
    @GET
    suspend fun downloadFile(
        @Url fileUrl: String
    ): ResponseBody
}
