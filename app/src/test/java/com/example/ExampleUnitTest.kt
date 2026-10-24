package com.example

import com.example.model.Script
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testWordCountEnglish() {
        val text = "Welcome to the professional studio teleprompter."
        assertEquals(6, Script.calculateWordCount(text))
    }

    @Test
    fun testWordCountHindi() {
        val text = "नमस्ते दोस्तों आज के इस वीडियो में आपका स्वागत है।"
        val count = Script.calculateWordCount(text)
        assertEquals(10, count)
    }

    @Test
    fun testDurationCalculation() {
        // 130 words at 130 WPM should take exactly 60 seconds
        val duration = Script.calculateDurationSec(130, 130)
        assertEquals(60, duration)
        assertEquals("01:00", Script.formatDuration(duration))
    }

    @Test
    fun testEmptyScript() {
        assertEquals(0, Script.calculateWordCount(""))
        assertEquals(0, Script.calculateDurationSec(0))
        assertEquals("00:00", Script.formatDuration(0))
    }
}
