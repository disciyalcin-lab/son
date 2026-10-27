package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.QuranWordDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Kur'an 300", appName)
    }

    @Test
    fun `verify top 300 words dataset loaded and strictly sorted by frequency descending`() {
        val words = QuranWordDataProvider.getInitialWords()
        assertEquals(300, words.size)

        // Verify IDs 1..300 are strictly sequential
        words.forEachIndexed { index, word ->
            assertEquals("Word at index $index must have id ${index + 1}", index + 1, word.id)
            assertTrue(
                "Word #${word.id} (${word.arabic}) frequency (${word.frequency}) must be >= 28",
                word.frequency >= 28
            )
        }

        // Verify strict descending frequency order across all 300 words
        words.zipWithNext().forEach { (current, next) ->
            assertTrue(
                "Word #${current.id} (${current.frequency}) must have frequency >= Word #${next.id} (${next.frequency})",
                current.frequency >= next.frequency
            )
        }

        // Check top 5 words (من, الله, ما, لا, في)
        assertEquals("مِنْ", words[0].arabic)
        assertEquals(3226, words[0].frequency)
        assertEquals("اللَّه", words[1].arabic)
        assertEquals(2699, words[1].frequency)
        assertEquals("مَا", words[2].arabic)
        assertEquals(2613, words[2].frequency)
        assertEquals("لَا", words[3].arabic)
        assertEquals(1728, words[3].frequency)
        assertEquals("فِي", words[4].arabic)
        assertEquals(1701, words[4].frequency)

        // Verify decks 1 to 12
        val deckCounts = (1..12).map { deck ->
            words.count { it.deck == deck }
        }
        assertEquals(12, deckCounts.size)
        deckCounts.forEach { count ->
            assertEquals(25, count)
        }

        // Verify total frequency of learned words covers major part of Quran
        val totalFrequency = words.sumOf { it.frequency }
        assertTrue("Cumulative frequency should exceed 40,000 occurrences", totalFrequency > 40000)
    }
}
