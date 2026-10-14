package com.example.chitttiaiassistant.brain.evaluator

import com.example.chitttiaiassistant.brain.db.LearningBrainDbHelper
import com.example.chitttiaiassistant.brain.db.SkillEntity
import com.example.chitttiaiassistant.brain.db.TaskFeedbackEntity
import com.example.chitttiaiassistant.brain.db.TaskMemoryEntity
import org.json.JSONArray

data class LessonExtraction(
    val taskTitle: String,
    val lessonText: String,
    val recommendedConfidenceScore: Double,
    val shouldDraftSkill: Boolean
)

class SkillEvaluator(private val dbHelper: LearningBrainDbHelper) {

    fun extractLessonFromTask(
        taskMemory: TaskMemoryEntity,
        feedback: TaskFeedbackEntity?
    ): LessonExtraction {
        val isSuccess = taskMemory.outcome == "SUCCESS" && (feedback == null || feedback.isApproved)
        val ratingFactor = feedback?.rating ?: if (isSuccess) 4 else 1

        val confidenceScore = (ratingFactor / 5.0) * if (isSuccess) 0.9 else 0.2

        val lessonText = if (isSuccess) {
            "Pattern '${taskMemory.taskTitle}' succeeded with actions ${taskMemory.actionsTakenJson}. Rating: $ratingFactor/5."
        } else {
            "Pattern '${taskMemory.taskTitle}' failed or was rejected. Error: ${taskMemory.errorMessage ?: feedback?.rejectionReason ?: "Unknown"}. Correction: ${feedback?.correctionText ?: "None"}."
        }

        val shouldDraftSkill = isSuccess && confidenceScore >= 0.75

        return LessonExtraction(
            taskTitle = taskMemory.taskTitle,
            lessonText = lessonText,
            recommendedConfidenceScore = confidenceScore,
            shouldDraftSkill = shouldDraftSkill
        )
    }

    fun createSkillDraftFromLesson(
        name: String,
        purpose: String,
        prerequisites: List<String>,
        steps: List<String>,
        examples: List<String>,
        confidenceScore: Double
    ): SkillEntity {
        val skill = SkillEntity(
            name = name,
            purpose = purpose,
            prerequisitesJson = JSONArray(prerequisites).toString(),
            stepsJson = JSONArray(steps).toString(),
            examplesJson = JSONArray(examples).toString(),
            limitations = "Requires human verification for high-impact actions.",
            confidenceScore = confidenceScore,
            version = 1,
            status = "DRAFT"
        )
        dbHelper.insertOrUpdateSkill(skill)
        return skill
    }

    fun evaluateSkillReliability(skillId: String): Double {
        val skills = dbHelper.getAllSkills()
        val skill = skills.find { it.skillId == skillId } ?: return 0.0
        return skill.confidenceScore
    }
}
