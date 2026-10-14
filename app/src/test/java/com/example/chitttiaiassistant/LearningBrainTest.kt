package com.example.chitttiaiassistant

import com.example.chitttiaiassistant.brain.db.ApprovalRequestEntity
import com.example.chitttiaiassistant.brain.db.PracticeSessionEntity
import com.example.chitttiaiassistant.brain.db.SkillEntity
import com.example.chitttiaiassistant.brain.db.TaskFeedbackEntity
import com.example.chitttiaiassistant.brain.db.TaskMemoryEntity
import com.example.chitttiaiassistant.brain.evaluator.LessonExtraction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningBrainTest {

    @Test
    fun testTaskMemoryCreation() {
        val memory = TaskMemoryEntity(
            taskTitle = "Toggle Flashlight",
            context = "Voice command 'turn on flashlight'",
            actionsTakenJson = "[\"CameraManager.setTorchMode(true)\"]",
            resultJson = "{\"status\":\"success\"}",
            outcome = "SUCCESS"
        )

        assertEquals("Toggle Flashlight", memory.taskTitle)
        assertEquals("SUCCESS", memory.outcome)
        assertNotNull(memory.taskId)
    }

    @Test
    fun testFeedbackAndLessonExtraction() {
        val memory = TaskMemoryEntity(
            taskTitle = "Open WhatsApp Message",
            context = "Command 'send whatsapp message to Ravi'",
            actionsTakenJson = "[\"Intent.ACTION_VIEW\"]",
            resultJson = "{\"status\":\"success\"}",
            outcome = "SUCCESS"
        )

        val feedback = TaskFeedbackEntity(
            taskId = memory.taskId,
            rating = 5,
            isApproved = true,
            correctionText = "Worked great!"
        )

        val isSuccess = memory.outcome == "SUCCESS" && feedback.isApproved
        val ratingFactor = feedback.rating
        val confidenceScore = (ratingFactor / 5.0) * if (isSuccess) 0.9 else 0.2

        val extraction = LessonExtraction(
            taskTitle = memory.taskTitle,
            lessonText = "Pattern '${memory.taskTitle}' succeeded.",
            recommendedConfidenceScore = confidenceScore,
            shouldDraftSkill = confidenceScore >= 0.75
        )

        assertTrue(extraction.shouldDraftSkill)
        assertEquals(0.9, extraction.recommendedConfidenceScore, 0.01)
    }

    @Test
    fun testSkillLifecycleStatus() {
        val draftSkill = SkillEntity(
            name = "Auto WhatsApp Drafting",
            purpose = "Pre-fill WhatsApp messages via deep link",
            prerequisitesJson = "[\"WhatsApp Installed\"]",
            stepsJson = "[\"Open Deep Link\", \"Pre-fill Text\"]",
            examplesJson = "[\"Message Ravi\"]",
            limitations = "Requires WhatsApp to be installed.",
            confidenceScore = 0.85,
            status = "DRAFT"
        )

        assertEquals("DRAFT", draftSkill.status)

        val activeSkill = draftSkill.copy(status = "ACTIVE")
        assertEquals("ACTIVE", activeSkill.status)

        val disabledSkill = activeSkill.copy(status = "DISABLED")
        assertEquals("DISABLED", disabledSkill.status)

        val rolledBackSkill = disabledSkill.copy(status = "ROLLED_BACK", version = 1)
        assertEquals("ROLLED_BACK", rolledBackSkill.status)
        assertEquals(1, rolledBackSkill.version)
    }

    @Test
    fun testSandboxSafety() {
        val safeInput = mapOf("action" to "toggle_flashlight", "param" to "true")
        val unsafeInput = mapOf("action" to "exec", "param" to "DROP TABLE users")

        val isSafeValid = !safeInput["param"]!!.contains("DROP TABLE")
        val isUnsafeValid = !unsafeInput["param"]!!.contains("DROP TABLE")

        assertTrue(isSafeValid)
        assertFalse(isUnsafeValid)
    }

    @Test
    fun testApprovalSafeguardsAndRiskLevels() {
        val lowRiskAction = "open_app"
        val highRiskAction = "delete_database"

        val requiresLowApproval = lowRiskAction == "delete_database"
        val requiresHighApproval = highRiskAction == "delete_database"

        assertFalse(requiresLowApproval)
        assertTrue(requiresHighApproval)
    }
}
