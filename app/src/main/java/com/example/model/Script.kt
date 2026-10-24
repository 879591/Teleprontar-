package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class Script(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val wordCount: Int = 0,
    val estimatedDurationSec: Int = 0,
    val language: String = "hi", // "hi", "en", or "mixed"
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val lastReadPosition: Int = 0
) {
    companion object {
        fun calculateWordCount(text: String): Int {
            if (text.isBlank()) return 0
            // Splits on whitespace to support both English words and Hindi / Devanagari words
            return text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
        }

        fun calculateDurationSec(wordCount: Int, wpm: Int = 130): Int {
            if (wordCount <= 0 || wpm <= 0) return 0
            return (wordCount * 60) / wpm
        }

        fun formatDuration(totalSeconds: Int): String {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
    }
}
