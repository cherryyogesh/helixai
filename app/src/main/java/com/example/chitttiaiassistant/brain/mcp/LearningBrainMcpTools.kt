package com.example.chitttiaiassistant.brain.mcp

import android.content.Context
import com.example.chitttiaiassistant.brain.db.ApprovalRequestEntity
import com.example.chitttiaiassistant.brain.db.LearningBrainDbHelper
import com.example.chitttiaiassistant.brain.db.SkillEntity
import com.example.chitttiaiassistant.brain.db.TaskFeedbackEntity
import com.example.chitttiaiassistant.brain.db.TaskMemoryEntity
import com.example.chitttiaiassistant.brain.evaluator.SkillEvaluator
import com.example.chitttiaiassistant.brain.sandbox.PracticeSandbox
import com.example.chitttiaiassistant.brain.sandbox.SandboxResult
import com.example.chitttiaiassistant.brain.safeguards.ApprovalGuard
import org.json.JSONObject

class LearningBrainMcpTools(context: Context) {

    val dbHelper = LearningBrainDbHelper(context)
    val evaluator = SkillEvaluator(dbHelper)
    val sandbox = PracticeSandbox(dbHelper)
    val approvalGuard = ApprovalGuard(dbHelper)

    // 1. record_task
    fun recordTask(
        title: String,
        context: String,
        actionsTaken: List<String>,
        resultSummary: String,
        errorMessage: String? = null,
        outcome: String = "SUCCESS"
    ): TaskMemoryEntity {
        val memory = TaskMemoryEntity(
            taskTitle = title,
            context = context,
            actionsTakenJson = org.json.JSONArray(actionsTaken).toString(),
            resultJson = JSONObject().apply { put("summary", resultSummary) }.toString(),
            errorMessage = errorMessage,
            outcome = outcome
        )
        dbHelper.insertTaskMemory(memory)
        return memory
    }

    // 2. search_task_memory
    fun searchTaskMemory(query: String): List<TaskMemoryEntity> {
        return dbHelper.searchTaskMemories(query)
    }

    // 3. record_feedback
    fun recordFeedback(
        taskId: String,
        rating: Int,
        correctionText: String? = null,
        isApproved: Boolean,
        rejectionReason: String? = null
    ): TaskFeedbackEntity {
        val feedback = TaskFeedbackEntity(
            taskId = taskId,
            rating = rating.coerceIn(1, 5),
            correctionText = correctionText,
            isApproved = isApproved,
            rejectionReason = rejectionReason
        )
        dbHelper.insertFeedback(feedback)
        return feedback
    }

    // 4. extract_lesson
    fun extractLesson(taskId: String): Map<String, Any> {
        val memories = dbHelper.searchTaskMemories("")
        val memory = memories.find { it.taskId == taskId }
            ?: return mapOf("error" to "Task memory not found for ID $taskId")

        val feedbacks = dbHelper.getFeedbackForTask(taskId)
        val feedback = feedbacks.firstOrNull()

        val lesson = evaluator.extractLessonFromTask(memory, feedback)
        return mapOf(
            "taskTitle" to lesson.taskTitle,
            "lessonText" to lesson.lessonText,
            "confidenceScore" to lesson.recommendedConfidenceScore,
            "shouldDraftSkill" to lesson.shouldDraftSkill
        )
    }

    // 5. list_skills
    fun listSkills(): List<SkillEntity> {
        return dbHelper.getAllSkills()
    }

    // 6. create_skill_draft
    fun createSkillDraft(
        name: String,
        purpose: String,
        prerequisites: List<String>,
        steps: List<String>,
        examples: List<String>,
        confidenceScore: Double
    ): SkillEntity {
        return evaluator.createSkillDraftFromLesson(
            name = name,
            purpose = purpose,
            prerequisites = prerequisites,
            steps = steps,
            examples = examples,
            confidenceScore = confidenceScore
        )
    }

    // 7. practice_task
    fun practiceTask(
        taskTitle: String,
        simulatedInputs: Map<String, String>
    ): SandboxResult {
        return sandbox.executePracticeTask(taskTitle, simulatedInputs)
    }

    // 8. evaluate_attempt
    fun evaluateAttempt(skillId: String): Map<String, Any> {
        val score = evaluator.evaluateSkillReliability(skillId)
        val isReliable = score >= 0.75
        return mapOf(
            "skillId" to skillId,
            "confidenceScore" to score,
            "isReliable" to isReliable,
            "status" to if (isReliable) "RELIABLE" else "NEEDS_IMPROVEMENT"
        )
    }

    // 9. request_human_approval
    fun requestHumanApproval(
        targetId: String,
        actionDescription: String,
        riskLevel: String
    ): ApprovalRequestEntity {
        return approvalGuard.createApprovalRequest(targetId, actionDescription, riskLevel)
    }

    // 10. promote_skill
    fun promoteSkill(skillId: String): Boolean {
        return approvalGuard.promoteSkillToActive(skillId)
    }

    // 11. disable_skill
    fun disableSkill(skillId: String): Boolean {
        return approvalGuard.disableSkill(skillId)
    }

    // 12. rollback_skill
    fun rollbackSkill(skillId: String, previousVersion: Int): Boolean {
        return approvalGuard.rollbackSkill(skillId, previousVersion)
    }

    // 13. delete_memory
    fun deleteMemory(taskId: String): Boolean {
        return dbHelper.deleteTaskMemory(taskId)
    }

    // 14. get_learning_report
    fun getLearningReport(): Map<String, Any> {
        val allMemories = dbHelper.searchTaskMemories("")
        val allSkills = dbHelper.getAllSkills()
        val activeSkills = allSkills.filter { it.status == "ACTIVE" }
        val draftSkills = allSkills.filter { it.status == "DRAFT" }

        return mapOf(
            "totalMemories" to allMemories.size,
            "totalSkills" to allSkills.size,
            "activeSkills" to activeSkills.size,
            "draftSkills" to draftSkills.size,
            "emergencyStopActive" to approvalGuard.isEmergencyStopped(),
            "status" to "Learning Brain Operational"
        )
    }
}
