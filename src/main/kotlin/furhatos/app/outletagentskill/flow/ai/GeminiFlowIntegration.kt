package furhatos.app.outletagentskill.flow.ai

import furhatos.app.outletagentskill.flow.main.conversation_history
import furhatos.flow.kotlin.FlowControlRunner
import furhatos.flow.kotlin.users

val geminiClient = GeminiClient()

// Store conversation history in user data
fun FlowControlRunner.getConversationHistory(): List<Pair<String, String>> {
    return users.current.conversation_history
}

fun FlowControlRunner.addToHistory(role: String, message: String) {
    val history = getConversationHistory().toMutableList()
    history.add(Pair(role, message))

    // Keep only last 5 exchanges to stay within free tier limits
    if (history.size > 10) {
        history.removeAt(0)
        history.removeAt(0)
    }

    users.current.conversation_history = history
}

fun FlowControlRunner.clearHistory() {
    users.current.conversation_history = emptyList<Pair<String, String>>()
}