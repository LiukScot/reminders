package com.liukscot.reminders.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.json.JSONObject

// What the AI turns a voice clip into: a reminder the user still confirms before saving. All
// scheduling fields are optional — the model fills what it heard, the user edits the rest.
data class VoiceTaskDraft(
    val title: String,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val recurrence: RecurrenceRule? = null,
    val list: String? = null,
)

// Shared across both providers: they receive the same instruction and the same JSON shape, so the
// app-side mapping (parseVoiceTaskDraft) is provider-agnostic. `today` is injected so the model
// resolves relative dates ("tomorrow") against the device clock; `lists` lets it route the reminder
// to the list the user named ("in the shopping list").
fun voiceTaskInstruction(today: LocalDate, lists: List<String>): String = """
    You convert a spoken reminder into a single JSON object with this exact shape:
    {"title": string, "date": string|null, "time": string|null, "recurrence": {"frequency": "DAILY"|"WEEKLY"|"MONTHLY"|"YEARLY", "interval": number, "byDay": ["MONDAY"..."SUNDAY"]}|null, "list": string|null}
    - title: the task only, with the date/time/recurrence AND list words removed. E.g. "buy pasta in the shopping list" → title "buy pasta".
    - date: ISO yyyy-MM-dd, or null if none was said. Today is $today; resolve relative dates against it.
    - time: 24h HH:mm, or null if no time was said.
    - recurrence: null unless the reminder repeats. interval defaults to 1; byDay only for weekly-on-specific-days.
    - list: exactly one of [${lists.joinToString(", ") { "\"$it\"" }}] if the user named a list (match loosely, e.g. "spesa"/"groceries"), otherwise null.
    Reply with only the JSON, no prose.
""".trimIndent()

// Parses the model's JSON reply into a draft. Throws if the reply isn't valid JSON (a failed call
// the caller must surface). Individual malformed optional fields degrade to null rather than fail
// the whole parse — AI output is best-effort, and a bad date shouldn't drop a good title.
fun parseVoiceTaskDraft(rawJson: String): VoiceTaskDraft {
    val obj = JSONObject(rawJson)
    return VoiceTaskDraft(
        title = obj.optString("title").trim(),
        date = obj.optString("date").ifBlank { null }?.let { parseIsoDate(it) },
        time = obj.optString("time").ifBlank { null }?.let { parseIsoTime(it) },
        recurrence = obj.optJSONObject("recurrence")?.let(::parseRecurrenceObject),
        list = obj.optString("list").ifBlank { null },
    )
}

private fun parseIsoDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value) }.getOrNull() // best-effort: unparseable date -> no date

private fun parseIsoTime(value: String): LocalTime? =
    runCatching { LocalTime.parse(value) }.getOrNull() // best-effort: unparseable time -> no time

private fun parseRecurrenceObject(obj: JSONObject): RecurrenceRule? {
    val frequency = obj.optString("frequency").ifBlank { null }
        ?.let { name -> RecurrenceFrequency.entries.firstOrNull { it.name == name.uppercase() } }
        ?: return null
    val byDay = obj.optJSONArray("byDay")?.let { arr ->
        (0 until arr.length()).mapNotNull { i ->
            DayOfWeek.entries.firstOrNull { it.name == arr.optString(i).uppercase() }
        }.toSet()
    } ?: emptySet()
    return RecurrenceRule(frequency, obj.optInt("interval", 1).coerceAtLeast(1), byDay)
}
