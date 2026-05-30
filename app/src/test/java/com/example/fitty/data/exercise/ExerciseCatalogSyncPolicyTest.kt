package com.example.fitty.data.exercise

import com.example.fitty.data.firebase.ExerciseFirestoreDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseCatalogSyncPolicyTest {

    @Test
    fun `validate normalizes abs to waist`() {
        val result = ExerciseCatalogSyncPolicy.validate(
            baseDocument(bodyPart = "abs")
        )

        assertEquals("waist", result.normalized?.bodyPart)
        assertEquals("waist", result.normalized?.muscleGroup)
        assertNull(result.issue)
    }

    @Test
    fun `validate rejects generic legs body part`() {
        val result = ExerciseCatalogSyncPolicy.validate(
            baseDocument(bodyPart = "legs")
        )

        assertNull(result.normalized)
        assertEquals("invalid_body_part", result.issue)
    }

    @Test
    fun `validate uses muscle group when body part missing`() {
        val result = ExerciseCatalogSyncPolicy.validate(
            baseDocument(bodyPart = "", muscleGroup = "back")
        )

        assertEquals("back", result.normalized?.bodyPart)
        assertEquals("back", result.normalized?.muscleGroup)
        assertNull(result.issue)
    }

    @Test
    fun `validation summary reports empty remote status`() {
        val summary = ExerciseCatalogSyncPolicy.ValidationSummary(fetched = 0, usable = 0)

        assertEquals(ExerciseCatalogSyncPolicy.STATUS_EMPTY_REMOTE_DATA, summary.statusCode)
    }

    @Test
    fun `validation summary reports invalid mapping when all documents are rejected by body part`() {
        val summary = ExerciseCatalogSyncPolicy.ValidationSummary(
            fetched = 3,
            usable = 0,
            droppedInvalidBodyPart = 3
        )

        assertEquals(ExerciseCatalogSyncPolicy.STATUS_INVALID_REMOTE_MAPPING, summary.statusCode)
    }

    private fun baseDocument(
        bodyPart: String = "chest",
        muscleGroup: String = ""
    ) = ExerciseFirestoreDocument(
        id = "push-up",
        name = "Push Up",
        bodyPart = bodyPart,
        muscleGroup = muscleGroup,
        target = "chest",
        equipment = "bodyweight",
        difficulty = "Beginner",
        description = "desc",
        instructions = "steps",
        thumbnailUrl = "",
        thumbnailStoragePath = "",
        gifUrl = "",
        gifStoragePath = "",
        videoUrl = "",
        gifVersion = 0,
        updatedAt = "2026-05-22T00:00:00Z"
    )
}
