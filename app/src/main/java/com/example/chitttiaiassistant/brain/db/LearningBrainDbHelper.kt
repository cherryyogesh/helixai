package com.example.chitttiaiassistant.brain.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LearningBrainDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE task_memories (
                taskId TEXT PRIMARY KEY,
                taskTitle TEXT NOT NULL,
                context TEXT NOT NULL,
                actionsTakenJson TEXT NOT NULL,
                resultJson TEXT NOT NULL,
                errorMessage TEXT,
                outcome TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE task_feedback (
                feedbackId TEXT PRIMARY KEY,
                taskId TEXT NOT NULL,
                rating INTEGER NOT NULL,
                correctionText TEXT,
                isApproved INTEGER NOT NULL,
                rejectionReason TEXT,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE skills (
                skillId TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                purpose TEXT NOT NULL,
                prerequisitesJson TEXT NOT NULL,
                stepsJson TEXT NOT NULL,
                examplesJson TEXT NOT NULL,
                limitations TEXT NOT NULL,
                confidenceScore REAL NOT NULL,
                version INTEGER NOT NULL,
                status TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE practice_sessions (
                sessionId TEXT PRIMARY KEY,
                taskTitle TEXT NOT NULL,
                sandboxInputsJson TEXT NOT NULL,
                sandboxOutputsJson TEXT NOT NULL,
                errorLog TEXT,
                evaluationResult TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE approval_requests (
                requestId TEXT PRIMARY KEY,
                targetId TEXT NOT NULL,
                actionDescription TEXT NOT NULL,
                riskLevel TEXT NOT NULL,
                status TEXT NOT NULL,
                reasonText TEXT,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS task_memories")
        db.execSQL("DROP TABLE IF EXISTS task_feedback")
        db.execSQL("DROP TABLE IF EXISTS skills")
        db.execSQL("DROP TABLE IF EXISTS practice_sessions")
        db.execSQL("DROP TABLE IF EXISTS approval_requests")
        onCreate(db)
    }

    // --- Task Memory CRUD ---
    fun insertTaskMemory(memory: TaskMemoryEntity) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("taskId", memory.taskId)
            put("taskTitle", memory.taskTitle)
            put("context", memory.context)
            put("actionsTakenJson", memory.actionsTakenJson)
            put("resultJson", memory.resultJson)
            put("errorMessage", memory.errorMessage)
            put("outcome", memory.outcome)
            put("timestamp", memory.timestamp)
        }
        db.insertWithOnConflict("task_memories", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun searchTaskMemories(query: String): List<TaskMemoryEntity> {
        val db = readableDatabase
        val list = mutableListOf<TaskMemoryEntity>()
        val cursor = db.rawQuery(
            "SELECT * FROM task_memories WHERE taskTitle LIKE ? OR context LIKE ? OR actionsTakenJson LIKE ? ORDER BY timestamp DESC",
            arrayOf("%$query%", "%$query%", "%$query%")
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    TaskMemoryEntity(
                        taskId = c.getString(c.getColumnIndexOrThrow("taskId")),
                        taskTitle = c.getString(c.getColumnIndexOrThrow("taskTitle")),
                        context = c.getString(c.getColumnIndexOrThrow("context")),
                        actionsTakenJson = c.getString(c.getColumnIndexOrThrow("actionsTakenJson")),
                        resultJson = c.getString(c.getColumnIndexOrThrow("resultJson")),
                        errorMessage = c.getString(c.getColumnIndexOrThrow("errorMessage")),
                        outcome = c.getString(c.getColumnIndexOrThrow("outcome")),
                        timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"))
                    )
                )
            }
        }
        return list
    }

    fun deleteTaskMemory(taskId: String): Boolean {
        val db = writableDatabase
        val count = db.delete("task_memories", "taskId = ?", arrayOf(taskId))
        return count > 0
    }

    // --- Task Feedback CRUD ---
    fun insertFeedback(feedback: TaskFeedbackEntity) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("feedbackId", feedback.feedbackId)
            put("taskId", feedback.taskId)
            put("rating", feedback.rating)
            put("correctionText", feedback.correctionText)
            put("isApproved", if (feedback.isApproved) 1 else 0)
            put("rejectionReason", feedback.rejectionReason)
            put("timestamp", feedback.timestamp)
        }
        db.insertWithOnConflict("task_feedback", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getFeedbackForTask(taskId: String): List<TaskFeedbackEntity> {
        val db = readableDatabase
        val list = mutableListOf<TaskFeedbackEntity>()
        val cursor = db.rawQuery("SELECT * FROM task_feedback WHERE taskId = ? ORDER BY timestamp DESC", arrayOf(taskId))
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    TaskFeedbackEntity(
                        feedbackId = c.getString(c.getColumnIndexOrThrow("feedbackId")),
                        taskId = c.getString(c.getColumnIndexOrThrow("taskId")),
                        rating = c.getInt(c.getColumnIndexOrThrow("rating")),
                        correctionText = c.getString(c.getColumnIndexOrThrow("correctionText")),
                        isApproved = c.getInt(c.getColumnIndexOrThrow("isApproved")) == 1,
                        rejectionReason = c.getString(c.getColumnIndexOrThrow("rejectionReason")),
                        timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"))
                    )
                )
            }
        }
        return list
    }

    // --- Skills CRUD ---
    fun insertOrUpdateSkill(skill: SkillEntity) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("skillId", skill.skillId)
            put("name", skill.name)
            put("purpose", skill.purpose)
            put("prerequisitesJson", skill.prerequisitesJson)
            put("stepsJson", skill.stepsJson)
            put("examplesJson", skill.examplesJson)
            put("limitations", skill.limitations)
            put("confidenceScore", skill.confidenceScore)
            put("version", skill.version)
            put("status", skill.status)
            put("createdAt", skill.createdAt)
            put("updatedAt", skill.updatedAt)
        }
        db.insertWithOnConflict("skills", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllSkills(): List<SkillEntity> {
        val db = readableDatabase
        val list = mutableListOf<SkillEntity>()
        val cursor = db.rawQuery("SELECT * FROM skills ORDER BY updatedAt DESC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    SkillEntity(
                        skillId = c.getString(c.getColumnIndexOrThrow("skillId")),
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        purpose = c.getString(c.getColumnIndexOrThrow("purpose")),
                        prerequisitesJson = c.getString(c.getColumnIndexOrThrow("prerequisitesJson")),
                        stepsJson = c.getString(c.getColumnIndexOrThrow("stepsJson")),
                        examplesJson = c.getString(c.getColumnIndexOrThrow("examplesJson")),
                        limitations = c.getString(c.getColumnIndexOrThrow("limitations")),
                        confidenceScore = c.getDouble(c.getColumnIndexOrThrow("confidenceScore")),
                        version = c.getInt(c.getColumnIndexOrThrow("version")),
                        status = c.getString(c.getColumnIndexOrThrow("status")),
                        createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt")),
                        updatedAt = c.getLong(c.getColumnIndexOrThrow("updatedAt"))
                    )
                )
            }
        }
        return list
    }

    fun updateSkillStatus(skillId: String, newStatus: String, newVersion: Int? = null) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("status", newStatus)
            put("updatedAt", System.currentTimeMillis())
            if (newVersion != null) {
                put("version", newVersion)
            }
        }
        db.update("skills", cv, "skillId = ?", arrayOf(skillId))
    }

    // --- Practice Sessions CRUD ---
    fun insertPracticeSession(session: PracticeSessionEntity) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("sessionId", session.sessionId)
            put("taskTitle", session.taskTitle)
            put("sandboxInputsJson", session.sandboxInputsJson)
            put("sandboxOutputsJson", session.sandboxOutputsJson)
            put("errorLog", session.errorLog)
            put("evaluationResult", session.evaluationResult)
            put("timestamp", session.timestamp)
        }
        db.insertWithOnConflict("practice_sessions", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Approval Requests CRUD ---
    fun insertApprovalRequest(request: ApprovalRequestEntity) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("requestId", request.requestId)
            put("targetId", request.targetId)
            put("actionDescription", request.actionDescription)
            put("riskLevel", request.riskLevel)
            put("status", request.status)
            put("reasonText", request.reasonText)
            put("timestamp", request.timestamp)
        }
        db.insertWithOnConflict("approval_requests", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateApprovalStatus(requestId: String, newStatus: String, reason: String? = null) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("status", newStatus)
            if (reason != null) put("reasonText", reason)
        }
        db.update("approval_requests", cv, "requestId = ?", arrayOf(requestId))
    }

    companion object {
        private const val DATABASE_NAME = "helix_learning_brain.db"
        private const val DATABASE_VERSION = 1
    }
}
