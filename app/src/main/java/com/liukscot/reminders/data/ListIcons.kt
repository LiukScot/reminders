package com.liukscot.reminders.data

import androidx.annotation.DrawableRes
import com.liukscot.reminders.R

// Maps a TaskList.icon token (stored as a plain string so it survives Room
// migrations independent of drawable renames) to its Lucide vector drawable.
object ListIcons {
    private val icons = mapOf(
        "inbox" to R.drawable.ic_inbox,
        "shopping-cart" to R.drawable.ic_shopping_cart,
        "briefcase" to R.drawable.ic_briefcase,
        "map-pin" to R.drawable.ic_map_pin,
        "heart" to R.drawable.ic_heart,
    )

    @DrawableRes
    fun resolve(icon: String): Int = icons[icon] ?: R.drawable.ic_inbox
}
