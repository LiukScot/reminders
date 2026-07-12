package com.liukscot.reminders.ui.navigation

import androidx.annotation.DrawableRes
import com.liukscot.reminders.R

// Bottom nav tabs, mirroring Reminders App Mockup's `nav` array
// (Lists/Day/Week/Settings) — icons are the mockup's Lucide glyphs.
enum class Destination(val route: String, val label: String, @DrawableRes val icon: Int) {
    Lists(route = "lists", label = "Lists", icon = R.drawable.ic_list_checks),
    Day(route = "day", label = "Day", icon = R.drawable.ic_sun),
    Week(route = "week", label = "Week", icon = R.drawable.ic_calendar_range),
    Settings(route = "settings", label = "Settings", icon = R.drawable.ic_settings),
}
