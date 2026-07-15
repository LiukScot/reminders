package com.liukscot.reminders.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AiProviderTest {
    @Test
    fun `default is Mistral`() {
        assertEquals(AiProvider.MISTRAL, AiProvider.DEFAULT)
    }

    @Test
    fun `round-trips through the persisted key`() {
        AiProvider.entries.forEach { provider ->
            assertEquals(provider, AiProvider.fromKey(provider.name))
        }
    }

    @Test
    fun `unknown or missing key falls back to the default`() {
        assertEquals(AiProvider.DEFAULT, AiProvider.fromKey(null))
        assertEquals(AiProvider.DEFAULT, AiProvider.fromKey(""))
        assertEquals(AiProvider.DEFAULT, AiProvider.fromKey("OpenAI"))
    }
}
