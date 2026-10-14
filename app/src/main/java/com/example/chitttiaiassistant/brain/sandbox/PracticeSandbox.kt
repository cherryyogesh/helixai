package com.example.chitttiaiassistant.brain.sandbox

import com.example.chitttiaiassistant.brain.db.LearningBrainDbHelper
import com.example.chitttiaiassistant.brain.db.PracticeSessionEntity
import org.json.JSONObject

data class SandboxResult(
    val sessionId: String,
    val taskTitle: String,
    val isSuccess: Boolean,
    val outputsJson: String,
    val errorLog: String?
)

class PracticeSandbox(private val dbHelper: LearningBrainDbHelper) {

    fun executePracticeTask(
        taskTitle: String,
        simulatedInputs: Map<String, String>
    ): SandboxResult {
        val inputsJson = JSONObject(simulatedInputs as Map<*, *>).toString()
        val outputsMap = mutableMapOf<String, String>()
        var isSuccess = true
        var errorLog: String? = null

        try {
            outputsMap["status"] = "SIMULATED_SUCCESS"
            outputsMap["simulated_action"] = "Executed '$taskTitle' in isolated sandbox"
            outputsMap["input_param_count"] = simulatedInputs.size.toString()

            // Test input validity
            for ((key, value) in simulatedInputs) {
                if (value.contains("DROP TABLE") || value.contains("rm -rf")) {
                    isSuccess = false
                    errorLog = "Dangerous command pattern detected in input param '$key'"
                    break
                }
            }
        } catch (e: Exception) {
            isSuccess = false
            errorLog = e.localizedMessage ?: "Sandbox execution error"
        }

        val outputsJson = JSONObject(outputsMap as Map<*, *>).toString()
        val session = PracticeSessionEntity(
            taskTitle = taskTitle,
            sandboxInputsJson = inputsJson,
            sandboxOutputsJson = outputsJson,
            errorLog = errorLog,
            evaluationResult = if (isSuccess) "PASSED" else "FAILED"
        )

        dbHelper.insertPracticeSession(session)

        return SandboxResult(
            sessionId = session.sessionId,
            taskTitle = taskTitle,
            isSuccess = isSuccess,
            outputsJson = outputsJson,
            errorLog = errorLog
        )
    }
}
