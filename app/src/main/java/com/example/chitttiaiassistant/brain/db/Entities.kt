package com.example.chitttiaiassistant.brain.db

import java.util.UUID

data class TaskMemoryEntity(
    val taskId: String = UUID.randomUUID().toString(),
    val taskTitle: String,
    val context: String,
    val actionsTakenJson: String,
    val resultJson: String,
    val errorMessage: String? = null,
    val outcome: String, // "SUCCESS", "FAILED", "CANCELLED"
    val timestamp: Long = System.currentTimeMillis()
)

data class TaskFeedbackEntity(
    val feedbackId: String = UUID.randomUUID().toString(),
    val taskId: String,
    val rating: Int, // 1 to 5
    val correctionText: String? = null,
    val isApproved: Boolean,
    val rejectionReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class SkillEntity(
    val skillId: String = UUID.randomUUID().toString(),
    val name: String,
    val purpose: String,
    val prerequisitesJson: String,
    val stepsJson: String,
    val examplesJson: String,
    val limitations: String,
    val confidenceScore: Double, // 0.0 to 1.0
    val version: Int = 1,
    val status: String = "DRAFT", // "DRAFT", "ACTIVE", "DISABLED", "ROLLED_BACK"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class PracticeSessionEntity(
    val sessionId: String = UUID.randomUUID().toString(),
    val taskTitle: String,
    val sandboxInputsJson: String,
    val sandboxOutputsJson: String,
    val errorLog: String? = null,
    val evaluationResult: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ApprovalRequestEntity(
    val requestId: String = UUID.randomUUID().toString(),
    val targetId: String, // Task ID or Skill ID
    val actionDescription: String,
    val riskLevel: String, // "LOW", "MODERATE", "HIGH_IMPACT", "RESTRICTED"
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val reasonText: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
