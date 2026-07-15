package com.liukscot.reminders.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class RecurrenceFrequency { DAILY, WEEKLY, MONTHLY, YEARLY }

private val WEEKDAY_CODES = mapOf(
    DayOfWeek.MONDAY to "MO",
    DayOfWeek.TUESDAY to "TU",
    DayOfWeek.WEDNESDAY to "WE",
    DayOfWeek.THURSDAY to "TH",
    DayOfWeek.FRIDAY to "FR",
    DayOfWeek.SATURDAY to "SA",
    DayOfWeek.SUNDAY to "SU",
)

data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val byDay: Set<DayOfWeek> = emptySet(),
) {
    companion object {
        fun fromTask(task: Task): RecurrenceRule? {
            val freq = task.recurrenceFreq?.let { runCatching { RecurrenceFrequency.valueOf(it) }.getOrNull() } ?: return null
            val byDay = task.recurrenceByDay
                ?.split(",")
                ?.mapNotNull { code -> WEEKDAY_CODES.entries.firstOrNull { it.value == code }?.key }
                ?.toSet()
                .orEmpty()
            return RecurrenceRule(freq, task.recurrenceInterval.coerceAtLeast(1), byDay)
        }

        fun encodeByDay(days: Set<DayOfWeek>): String? =
            days.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(",") { WEEKDAY_CODES.getValue(it) }
    }
}

fun Task.withRecurrence(rule: RecurrenceRule?, anchor: Long?): Task = copy(
    recurrenceFreq = rule?.frequency?.name,
    recurrenceInterval = rule?.interval ?: 1,
    recurrenceByDay = rule?.byDay?.let(RecurrenceRule::encodeByDay),
    recurrenceAnchor = if (rule != null) anchor else null,
)

// ponytail: a full RRULE engine is overkill here. A brute-force day scan is simple, obviously
// correct, and fast enough — worst case is `interval` weeks of iteration for a byDay cadence.
private const val MAX_SCAN_DAYS = 3660L // ~10 years, a safety net against a malformed rule

fun nextOccurrence(anchorMillis: Long, rule: RecurrenceRule, zone: ZoneId = ZoneId.systemDefault()): Long {
    val anchor = Instant.ofEpochMilli(anchorMillis).atZone(zone)
    if (rule.frequency != RecurrenceFrequency.WEEKLY || rule.byDay.isEmpty()) {
        val next = when (rule.frequency) {
            RecurrenceFrequency.DAILY -> anchor.plusDays(rule.interval.toLong())
            RecurrenceFrequency.WEEKLY -> anchor.plusWeeks(rule.interval.toLong())
            RecurrenceFrequency.MONTHLY -> anchor.plusMonths(rule.interval.toLong())
            RecurrenceFrequency.YEARLY -> anchor.plusYears(rule.interval.toLong())
        }
        return next.toInstant().toEpochMilli()
    }

    val anchorWeekStart = anchor.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    var candidate = anchor.plusDays(1)
    repeat(MAX_SCAN_DAYS.toInt()) {
        val candidateWeekStart = candidate.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekOffset = ChronoUnit.WEEKS.between(anchorWeekStart, candidateWeekStart)
        if (weekOffset % rule.interval == 0L && candidate.dayOfWeek in rule.byDay) {
            return candidate.toInstant().toEpochMilli()
        }
        candidate = candidate.plusDays(1)
    }
    error("No matching recurrence day found within $MAX_SCAN_DAYS days for $rule")
}
