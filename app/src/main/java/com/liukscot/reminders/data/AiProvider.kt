package com.liukscot.reminders.data

// The AI backend every AI-powered feature (voice-to-task #19, future NLU) calls. Both providers
// offer single-call audio → structured-JSON, so features depend on this choice, not a hardcoded one.
enum class AiProvider(val displayName: String) {
    MISTRAL("Mistral"),
    GOOGLE("Google");

    companion object {
        val DEFAULT = MISTRAL

        // Persisted as the enum name; anything unknown (or unset) falls back to the default.
        fun fromKey(key: String?): AiProvider = entries.firstOrNull { it.name == key } ?: DEFAULT
    }
}
