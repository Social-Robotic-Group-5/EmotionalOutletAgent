package furhatos.app.outletagentskill.flow.ai

data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val parts: List<Part>,
    val role: String = "user"
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Double = 0.7,
    val topK: Int = 40,
    val topP: Double = 0.95,
    val maxOutputTokens: Int = 1024,
    val stopSequences: List<String>? = null
)

data class GeminiResponse(
    val candidates: List<Candidate>,
    val usageMetadata: UsageMetadata? = null
)

data class Candidate(
    val content: Content,
    val finishReason: String? = null
)

data class UsageMetadata(
    val promptTokenCount: Int,
    val candidatesTokenCount: Int,
    val totalTokenCount: Int
)