package com.liukscot.reminders.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LikePatternTest {
    @Test
    fun `wraps a plain query so it matches anywhere in the field`() {
        assertEquals("%cake%", likePattern("cake"))
    }

    @Test
    fun `escapes LIKE wildcards so they match themselves`() {
        assertEquals("%50\\%%", likePattern("50%"))
        assertEquals("%a\\_b%", likePattern("a_b"))
    }

    @Test
    fun `escapes the escape character before the wildcards it introduces`() {
        assertEquals("%a\\\\b%", likePattern("a\\b"))
        // A backslash typed right before a wildcard must not end up escaping it.
        assertEquals("%\\\\\\%%", likePattern("\\%"))
    }

    @Test
    fun `leaves an empty query as a match-everything pattern`() {
        assertEquals("%%", likePattern(""))
    }
}
