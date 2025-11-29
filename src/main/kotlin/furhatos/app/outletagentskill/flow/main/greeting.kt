package furhatos.app.outletagentskill.flow.main

// 引入上级目录的 Parent 状态
import furhatos.app.outletagentskill.flow.Parent

import furhatos.flow.kotlin.*
import furhatos.gestures.Gestures
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

// =========================================================================
// 1. 数据记录结构 (LOGGING)
// =========================================================================

data class InteractionLog(
    val questionIndex: Int,
    val questionType: String,
    val robotText: String,
    val userText: String,
    val durationMs: Long,
    val wordCount: Int,
    val responseLatency: Long
)

val sessionLogs = mutableListOf<InteractionLog>()

fun saveLogsToCSV() {
    val filename = "experiment_log_${System.currentTimeMillis()}.csv"
    val file = File(filename)
    
    // 写入表头
    file.appendText("QuestionID,Type,RobotQuestion,UserAnswer,Duration_ms,WordCount,Latency_ms\n")
    
    // 写入数据
    sessionLogs.forEach {
        val cleanRobot = it.robotText.replace(",", " ").replace("\n", " ").replace("\r", "")
        val cleanUser = it.userText.replace(",", " ").replace("\n", " ").replace("\r", "")
        file.appendText("${it.questionIndex},${it.questionType},${cleanRobot},${cleanUser},${it.durationMs},${it.wordCount},${it.responseLatency}\n")
    }
    
    println("Data saved successfully to: ${file.absolutePath}")
}

// =========================================================================
// 2. 配置与 API (CONFIGURATION)
// =========================================================================

//  请在此填入您的 Gemini API KEY
private const val API_KEY = "" 

val EXPERIMENT_QUESTIONS = listOf(
    "Could you tell me about a personal achievement you are proud of recently?",
    "What's one thing you are currently feeling very grateful for in your life?",
    "Can you describe a time when you felt disappointed or frustrated lately?"
)

// 随机过渡语列表 (当一个话题结束时随机使用)
val TRANSITION_PHRASES = listOf(
    "I understand.",
    "I see, thank you for sharing that.",
    "That makes sense.",
    "Got it.",
    "That is interesting to hear.",
    "I appreciate you telling me that."
)

var currentQuestionIndex = 0

// Gemini API 调用逻辑
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
        
        // 处理 Prompt 中的特殊字符
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
// 3. 实验流程 (FLOW)
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
    var lastRobotText: String = ""
    var robotFinishTime: Long = 0L

    onEntry {
        if (currentQuestionIndex >= EXPERIMENT_QUESTIONS.size) {
            goto(ExperimentEnd)
        } else {
            // 决定机器人要说的话：是新的主问题，还是追问
            val textToSay = if (currentFollowUpQuestion.isEmpty()) {
                conversationHistory = "" // 新的主问题开始，清空之前的追问历史
                EXPERIMENT_QUESTIONS[currentQuestionIndex]
            } else {
                currentFollowUpQuestion
            }
            
            lastRobotText = textToSay
            furhat.say(textToSay)
            
            // 记录说完话的时间点，用于计算延迟
            robotFinishTime = System.currentTimeMillis()
            
            // 监听用户回答 (最长60秒，静音超时2秒)
            furhat.listen(endSil = 2000, maxSpeech = 60000, timeout = 10000)
        }
    }

    onResponse {
        val userText = it.text
        
        // --- 1. 计算 Metrics ---
        val answerDurationMs: Long = it.speech.length.toLong()
        val timeNow: Long = System.currentTimeMillis()
        val totalTimeElapsed: Long = timeNow - robotFinishTime
        var latencyMs: Long = totalTimeElapsed - answerDurationMs
        if (latencyMs < 0) latencyMs = 0 
        val wordCount = userText.split("\\s+".toRegex()).size
        val qType = if (currentFollowUpQuestion.isEmpty()) "Main" else "FollowUp"

        // --- 2. 记录日志 (Logging) ---
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

        conversationHistory += "User: $userText. "

        // --- 3. 思考行为 (Thinking Behavior) ---
        
        // 步骤 A: 移开视线，表示思考
        furhat.gesture(Gestures.GazeAway)
        
        
        // 这既模拟了思考，也填补了 API 请求的等待时间
        furhat.say("I see...") 
        
        // 步骤 C: 转回视线，微笑
        furhat.gesture(Gestures.Smile)

        // --- 4. 调用 Gemini API ---
        val nextMove = getNextMoveFromGemini(
            mainQuestion = EXPERIMENT_QUESTIONS[currentQuestionIndex],
            history = conversationHistory,
            latestUserAnswer = userText
        )

        // --- 5. 处理 API 结果 ---
        if (nextMove == "STOP:END") {
            // 情况 A: 话题结束，准备进入下一题
            // 随机选一句过渡语 (例如 "I understand")
            val randomPhrase = TRANSITION_PHRASES.random()
            furhat.say(randomPhrase)
            
            currentQuestionIndex++
            currentFollowUpQuestion = ""
            delay(1000)
            reentry() // 重新进入 ActiveInterview，触发 onEntry 说下一道主问题
        } else {
            // 情况 B: 继续追问
            conversationHistory += "Robot: $nextMove. "
            currentFollowUpQuestion = nextMove
            reentry() // 重新进入 ActiveInterview，触发 onEntry 说出追问
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
        
        // 实验结束，返回 Idle 状态
        goto(Idle)
    }
}