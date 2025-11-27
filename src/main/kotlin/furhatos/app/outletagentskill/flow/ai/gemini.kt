package furhatos.app.outletagentskill.flow.ai

val GEMINI_API_KEY = System.getenv("GEMINI_API_KEY") ?: error("GEMINI_API_KEY not set")
const val GEMINI_MODEL = "gemini-2.5-flash"
const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent"
// Free tier limits: 15 requests per minute, 1 million tokens per minute
const val REQUEST_TIMEOUT_SECONDS = 30L
