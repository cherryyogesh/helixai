package com.example.chitttiaiassistant.brain.safeguards

import com.example.chitttiaiassistant.brain.db.ApprovalRequestEntity
import com.example.chitttiaiassistant.brain.db.LearningBrainDbHelper

class ApprovalGuard(private val dbHelper: LearningBrainDbHelper) {

    private val denyList = listOf("delete_database", "format_sdcard", "factory_reset", "uninstall_app")
    private var isEmergencyStopActive = false

    fun triggerEmergencyStop() {
        isEmergencyStopActive = true
    }

    fun resetEmergencyStop() {
        isEmergencyStopActive = false
    }

    fun isEmergencyStopped(): Boolean = isEmergencyStopActive

    fun requiresApproval(actionDescription: String, riskLevel: String): Boolean {
        if (isEmergencyStopActive) return true
        if (riskLevel == "HIGH_IMPACT" || riskLevel == "RESTRICTED") return true
        val lower = actionDescription.lowercase()
        return denyList.any { lower.contains(it) }
    }

    fun createApprovalRequest(
        targetId: String,
        actionDescription: String,
        riskLevel: String
    ): ApprovalRequestEntity {
        val request = ApprovalRequestEntity(
            targetId = targetId,
            actionDescription = actionDescription,
            riskLevel = riskLevel,
            status = "PENDING"
        )
        dbHelper.insertApprovalRequest(request)
        return request
    }

    fun approveRequest(requestId: String): Boolean {
        dbHelper.updateApprovalStatus(requestId, "APPROVED", "Approved by human user")
        return true
    }

    fun rejectRequest(requestId: String, reason: String): Boolean {
        dbHelper.updateApprovalStatus(requestId, "REJECTED", reason)
        return true
    }

    fun promoteSkillToActive(skillId: String): Boolean {
        dbHelper.updateSkillStatus(skillId, "ACTIVE")
        return true
    }

    fun disableSkill(skillId: String): Boolean {
        dbHelper.updateSkillStatus(skillId, "DISABLED")
        return true
    }

    fun rollbackSkill(skillId: String, previousVersion: Int): Boolean {
        dbHelper.updateSkillStatus(skillId, "ROLLED_BACK", previousVersion)
        return true
    }
}
