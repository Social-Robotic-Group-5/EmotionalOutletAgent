package furhatos.app.outletagentskill.flow.main

import furhatos.app.outletagentskill.flow.Parent
import furhatos.app.outletagentskill.flow.main.Idle
import furhatos.flow.kotlin.*
import furhatos.gestures.Gestures
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// =========================================================================
// 1. DATA LOGGING STRUCTURE 
// =========================================================================

// Data model for CSV rows
data class InteractionLog(
    val questionIndex: Int,      // Which main question (0, 1, 2)
    val questionType: String,    // "Main" or "FollowUp"
    val robotText: String,       // What the robot asked
    val userText: String,        // What the user answered
    val durationMs: Long,        // Duration of user's speech in ms
    val wordCount: Int,          // Approximate word count
    val responseLatency: Long    // How long user hesitated before speaking (ms)
)

// Global list to store logs
val sessionLogs = mutableListOf<InteractionLog>()

// Function to save logs to CSV at the end
fun saveLogsToCSV() {
    // Generate filename with timestamp
    val filename = "experiment_log_${System.currentTimeMillis()}.csv"
    val file = File(filename)
    
    // Write Header
    file.appendText("QuestionID,Type,RobotQuestion,UserAnswer,Duration_ms,WordCount,Latency_ms\n")
    
    // Write Data
    sessionLogs.forEach {
        // Remove commas/newlines in text to prevent breaking CSV format
        val cleanRobot = it.robotText.replace(",", " ").replace("\n", " ").replace("\r", "")
        val cleanUser = it.userText.replace(",", " ").replace("\n", " ").replace("\r", "")
        
        file.appendText("${it.questionIndex},${it.questionType},${cleanRobot},${cleanUser},${it.durationMs},${it.wordCount},${it.responseLatency}\n")
    }
    
    println(" Data saved successfully to: ${file.absolutePath}")
}

// =========================================================================
// 2. CONFIGURATION & LLM
// =========================================================================

//  YOUR API KEY HERE
private const val API_KEY = ""

val EXPERIMENT_QUESTIONS = listOf(
    "Could you tell me about a personal achievement you are proud of recently?",
    "What's one thing you are currently feeling very grateful for in your life?",
    "Can you describe a time when you felt disappointed or frustrated lately?"
)

var currentQuestionIndex = 0

// Gemini API Call
private fun getNextMoveFromGemini(mainQuestion: String, history: String, latestUserAnswer: String): String {
    try {
        val promptText = """
            You are a backend API helper for a psychology experiment robot.
            CONTEXT:
            Topic: "$mainQuestion"
            History: $history
            User said: "$latestUserAnswer"
            
            TASK:
            1. Analyze user response.
            2. DECISION:
               - If finished/empty/short (e.g. "No", "That's it"): Output ONLY: STOP:END
               - If engaged: Generate short follow-up question. Output ONLY question text.
            
            RULES: NO "Analysis:", NO quotes.
        """.trimIndent()

        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$API_KEY")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        val safePrompt = promptText.replace("\"", "\\\"").replace("\n", " ")
        val jsonBody = """{"contents": [{"parts": [{"text": "$safePrompt"}]}]}"""
        connection.outputStream.use { it.write(jsonBody.toByteArray()) }

        if (connection.responseCode == 200) {
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val rawResult = extractTextFromJson(responseText) ?: "STOP:END"
            return cleanResponse(rawResult)
        }
    } catch (e: Exception) { e.printStackTrace() }
    return "STOP:END"
}

private fun cleanResponse(text: String): String {
    if (text.contains("STOP:END", ignoreCase = true)) return "STOP:END"
    return text.replace(Regex("^(Question|Robot):\\s*", RegexOption.IGNORE_CASE), "").trim()
}

private fun extractTextFromJson(json: String): String? {
    try {
        val idx1 = json.indexOf("\"text\"")
        if (idx1 == -1) return null
        val idx2 = json.indexOf(":", idx1)
        val idx3 = json.indexOf("\"", idx2 + 1)
        var idx4 = -1
        for (i in idx3 + 1 until json.length) {
            if (json[i] == '"' && json[i - 1] != '\\') { idx4 = i; break }
        }
        if (idx3 != -1 && idx4 != -1) {
            return json.substring(idx3 + 1, idx4).replace("\\n", " ").replace("\\\"", "\"").trim()
        }
    } catch (e: Exception) {}
    return null
}

// =========================================================================
// 3. EXPERIMENT FLOW
// =========================================================================

val Greeting: State = state(Parent) {
    onEntry {
        sessionLogs.clear()
        currentQuestionIndex = 0
        furhat.say("Hi, thank you for participating.")
        furhat.gesture(Gestures.Smile)
        furhat.say("I would like to get to know you better through three questions.")
        delay(1000)
        goto(ActiveInterview)
    }
}

val ActiveInterview: State = state(Parent) {

    var currentFollowUpQuestion: String = "" 
    var conversationHistory: String = ""
    
    // Variables for metrics
    var lastRobotText: String = ""
    var robotFinishTime: Long = 0L

    onEntry {
        if (currentQuestionIndex >= EXPERIMENT_QUESTIONS.size) {
            goto(ExperimentEnd)
        } else {
            // Determine text to say
            val textToSay = if (currentFollowUpQuestion.isEmpty()) {
                conversationHistory = "" // Reset history for new main question
                EXPERIMENT_QUESTIONS[currentQuestionIndex]
            } else {
                currentFollowUpQuestion
            }
            
            lastRobotText = textToSay

            // Speak
            furhat.say(textToSay)
            
            // Capture timestamp (Long) right after speaking
            robotFinishTime = System.currentTimeMillis()

            // Listen (Wait 2s for end silence, 60s max length, 10s timeout)
            furhat.listen(endSil = 2000, maxSpeech = 60000, timeout = 10000)
        }
    }

    onResponse {
        val userText = it.text
        
        // --- 1. FIX: TYPE CONVERSION FOR METRICS ---
        
        // it.speech.length returns Int (ms). Convert to Long for calculation.
        val answerDurationMs: Long = it.speech.length.toLong()
        
        // Current time
        val timeNow: Long = System.currentTimeMillis()
        
        // Calculate Total Time (Latency + Talking)
        val totalTimeElapsed: Long = timeNow - robotFinishTime
        
        // Calculate Latency (Total - Talking). Ensure it's not negative.
        var latencyMs: Long = totalTimeElapsed - answerDurationMs
        if (latencyMs < 0) latencyMs = 0 

        // Word Count
        val wordCount = userText.split("\\s+".toRegex()).size
        val qType = if (currentFollowUpQuestion.isEmpty()) "Main" else "FollowUp"

        // --- 2. LOGGING ---
        val newLog = InteractionLog(
            questionIndex = currentQuestionIndex,
            questionType = qType,
            robotText = lastRobotText,
            userText = userText,
            durationMs = answerDurationMs,
            wordCount = wordCount,
            responseLatency = latencyMs
        )
        sessionLogs.add(newLog)

        // Update history
        conversationHistory += "User: $userText. "

        // --- 3. ROBOT BEHAVIOR ---
        furhat.gesture(Gestures.GazeAway)
        delay(3000)
        furhat.gesture(Gestures.Smile)

        val nextMove = getNextMoveFromGemini(
            mainQuestion = EXPERIMENT_QUESTIONS[currentQuestionIndex],
            history = conversationHistory,
            latestUserAnswer = userText
        )

        if (nextMove == "STOP:END") {
            furhat.say("Thank you for sharing.")
            currentQuestionIndex++
            currentFollowUpQuestion = ""
            delay(1000)
            reentry()
        } else {
            conversationHistory += "Robot: $nextMove. "
            currentFollowUpQuestion = nextMove
            reentry()
        }
    }

    onNoResponse {
        furhat.say("Okay, let's move on.")
        currentQuestionIndex++
        currentFollowUpQuestion = ""
        reentry()
    }
}

val ExperimentEnd: State = state(Parent) {
    onEntry {
        furhat.gesture(Gestures.Smile)
        furhat.say("That was the last question. Thank you so much.")
        
        try {
            saveLogsToCSV()
            furhat.say("I have saved our conversation data.")
        } catch (e: Exception) {
            println("Error saving logs: ${e.message}")
        }
        
        goto(Idle)
    }
}