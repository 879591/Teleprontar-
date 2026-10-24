package com.example.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

enum class CameraFilter(val label: String) {
    NONE("Original"),
    COOL("Cool Blue"),
    WARM("Warm Sunset"),
    STUDIO("Studio Glow"),
    MONO("Black & White")
}

data class PrompterSettings(
    val scrollSpeed: Int = 130, // words per minute (approx 40 - 300)
    val fontSizeSp: Float = 28f, // 18sp to 54sp
    val textColor: Long = 0xFFFFFFFF, // White
    val backgroundColor: Long = 0xFF000000, // Black
    val backgroundOpacity: Float = 0.85f,
    val textAlignment: TextAlign = TextAlign.Center,
    val mirrorMode: Boolean = false,
    val countdownSeconds: Int = 3,
    val voiceFollowEnabled: Boolean = false,
    val cameraFacingFront: Boolean = true,
    val cameraMirror: Boolean = true,
    val cameraExposure: Float = 0f,
    val cameraFilter: CameraFilter = CameraFilter.NONE,
    val overlayWidthPercent: Float = 0.92f,
    val overlayHeightPercent: Float = 0.45f,
    val speechLanguage: String = "hi-IN" // "hi-IN" (Hindi) or "en-US" (English)
)
