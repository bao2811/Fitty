package com.example.fitty.data.remote.exercise

import com.google.gson.annotations.SerializedName

data class ExerciseDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("instructions") val instructions: String? = null,
    @SerializedName("muscleGroup") val muscleGroup: String? = null,
    @SerializedName("target") val target: String? = null,
    @SerializedName("calories") val calories: Int? = null,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("difficulty") val difficulty: String? = null,
    @SerializedName("equipment") val equipment: String? = null,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerializedName("gifUrl") val gifUrl: String? = null,
    @SerializedName("videoUrl") val videoUrl: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("version") val version: String? = null
)

data class ExercisePageDto(
    @SerializedName("items") val items: List<ExerciseDto> = emptyList(),
    @SerializedName("nextCursor") val nextCursor: String? = null,
    @SerializedName("apiVersion") val apiVersion: String? = null,
    @SerializedName("deltaToken") val deltaToken: String? = null,
    @SerializedName("serverTime") val serverTime: String? = null
)
