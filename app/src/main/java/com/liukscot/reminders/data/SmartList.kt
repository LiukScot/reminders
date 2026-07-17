package com.liukscot.reminders.data

import com.liukscot.reminders.R

// Ref: Reminders App Mockup "Home" screen 2x2 card grid. The mockup's fourth card is "No date";
// Overdue replaces it — what needs acting on beats what merely lacks a date.
enum class SmartList(val label: String, val icon: Int) {
    Flagged("Flagged", R.drawable.ic_flag),
    All("All", R.drawable.ic_inbox),
    Completed("Completed", R.drawable.ic_check),
    Overdue("Overdue", R.drawable.ic_clock),
}
