package com.liukscot.reminders.data

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

// Snoozing reschedules the reminder: every option below moves its due time for real, rather than
// silencing one notification and leaving it due in the past.
enum class SnoozeKind {
    Plus15m,
    Plus1h,
    Later,
    Tomorrow,
    Weekend,
    // The delay behind the notification's one-tap snooze button, set by the user in Settings.
    Quick,
    PickDateTime,
}

// `at` is null only for PickDateTime, which has no time of its own — it asks the user for one.
data class SnoozeChoice(val kind: SnoozeKind, val at: LocalDateTime?)

private const val AFTERNOON_HOUR = 15
private const val EVENING_HOUR = 18
private const val NIGHT_HOUR = 21
private const val MORNING_HOUR = 9

// "Later" means the next meaningful slot of the day rather than a fixed delay: at 09:30 that is
// this evening, at 22:00 it is tomorrow morning. Bands are closed at the top, so every hour of the
// clock lands in exactly one and the result is always in the future.
internal fun laterFrom(now: LocalDateTime): LocalDateTime {
    val today = now.toLocalDate()
    return when (now.hour) {
        in 4..8 -> today.atTime(AFTERNOON_HOUR, 0)
        in 9..14 -> today.atTime(EVENING_HOUR, 0)
        in 15..17 -> today.atTime(NIGHT_HOUR, 0)
        // 18:00–23:59 rolls over to tomorrow; 00:00–03:59 is already the small hours of the day it
        // belongs to, so its "next morning" is this one.
        in 18..23 -> today.plusDays(1).atTime(MORNING_HOUR, 0)
        else -> today.atTime(MORNING_HOUR, 0)
    }
}

// next() is strictly after today, which is exactly the rule: on a weekday it finds this week's
// Saturday, and on Saturday or Sunday it skips to the one after.
internal fun weekendFrom(now: LocalDateTime): LocalDateTime =
    now.toLocalDate().with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).atTime(now.toLocalTime())

// Options that land on the same instant are the same offer twice — on a Friday, "tomorrow" and
// "this weekend" are both Saturday. The first one wins, so the fixed options keep their place and
// the user's quick snooze is what drops out when it duplicates one of them.
fun snoozeChoices(now: LocalDateTime, quickSnooze: Duration): List<SnoozeChoice> =
    listOf(
        SnoozeChoice(SnoozeKind.Plus15m, now.plusMinutes(15)),
        SnoozeChoice(SnoozeKind.Plus1h, now.plusHours(1)),
        SnoozeChoice(SnoozeKind.Later, laterFrom(now)),
        SnoozeChoice(SnoozeKind.Tomorrow, now.plusDays(1)),
        SnoozeChoice(SnoozeKind.Weekend, weekendFrom(now)),
        SnoozeChoice(SnoozeKind.Quick, now.plus(quickSnooze)),
    ).distinctBy { it.at } + SnoozeChoice(SnoozeKind.PickDateTime, null)
