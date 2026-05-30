package com.example.fitty.data.exercise

import com.example.fitty.domain.model.Exercise

interface ExerciseGifDownloader {
    suspend fun download(exercise: Exercise): Result<String>
}
