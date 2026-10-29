package com.example.budsbattery.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private val NO_WAITS = listOf(0L, 0L, 0L)

class LinkCheckTest {

    @Test
    fun `a profile that still reports the buds at the drop is checked again until it clears`() = runBlocking {
        // Android drops the audio profile a moment after the link: the first look still sees the buds
        val answers = ArrayDeque(listOf("buds", null))

        assertNull(awaitGone(NO_WAITS) { answers.removeFirst() })
    }

    @Test
    fun `buds that stay on the profile through every check are still connected`() = runBlocking {
        var checks = 0

        val result = awaitGone(NO_WAITS) {
            checks++
            "buds"
        }

        assertEquals("buds", result)
        assertEquals(NO_WAITS.size, checks)
    }

    @Test
    fun `buds already gone are reported after one check`() = runBlocking {
        var checks = 0

        val result = awaitGone(NO_WAITS) {
            checks++
            null
        }

        assertNull(result)
        assertEquals(1, checks)
    }
}
