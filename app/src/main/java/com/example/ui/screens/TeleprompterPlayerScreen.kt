package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Script
import com.example.ui.TeleprompterViewModel
import com.example.ui.components.ReadingGuideBand
import com.example.ui.theme.GlassBar
import com.example.ui.theme.GlassOverlay
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioDarkSurface
import com.example.ui.theme.StudioGreenLive
import com.example.ui.theme.StudioRedRecord
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.voice.VoiceState

@Composable
fun TeleprompterPlayerScreen(
    viewModel: TeleprompterViewModel,
    script: Script,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val countdown by viewModel.countdown.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val spokenSnippet by viewModel.spokenSnippet.collectAsState()
    val scrollFraction by viewModel.currentScrollFraction.collectAsState()

    val scrollState = rememberScrollState()
    var isControlsVisible by remember { mutableStateOf(true) }
    var activeSettingsTab by remember { mutableStateOf<String?>(null) } // "speed", "font", "color"

    BackHandler {
        viewModel.pausePlayback()
        onNavigateBack()
    }

    // Auto-scroll loop using requestAnimationFrame equivalent (withFrameNanos)
    LaunchedEffect(isPlaying, settings.scrollSpeed) {
        if (isPlaying) {
            var lastTime = 0L
            while (isPlaying) {
                withFrameNanos { time ->
                    if (lastTime != 0L) {
                        val deltaSeconds = (time - lastTime) / 1_000_000_000f
                        // Convert words per minute into smooth pixel scroll speed
                        // 130 wpm roughly corresponds to ~60-80 pixels per second
                        val pixelsPerSecond = (settings.scrollSpeed.toFloat() / 130f) * 65f * (settings.fontSizeSp / 28f)
                        val deltaY = pixelsPerSecond * deltaSeconds
                        kotlinx.coroutines.runBlocking {
                            scrollState.scrollBy(deltaY)
                        }
                    }
                    lastTime = time
                }
            }
        }
    }

    // Voice Follow scroll sync
    LaunchedEffect(scrollFraction) {
        if (settings.voiceFollowEnabled && scrollState.maxValue > 0) {
            val targetScroll = (scrollFraction * scrollState.maxValue).toInt()
            scrollState.animateScrollTo(targetScroll)
        }
    }

    val backgroundColor = Color(settings.backgroundColor).copy(alpha = settings.backgroundOpacity)
    val textColor = Color(settings.textColor)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isControlsVisible = !isControlsVisible
            }
    ) {
        // Content Area with Mirroring support
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .graphicsLayer {
                    // Mirror mode flips horizontally for glass teleprompters
                    scaleX = if (settings.mirrorMode) -1f else 1f
                }
        ) {
            // Reading Focus Guide Band in the upper-middle reading sweet-spot
            ReadingGuideBand(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 160.dp),
                opacity = 0.15f
            )

            // Scrollable Script Text
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 28.dp),
                horizontalAlignment = when (settings.textAlignment) {
                    TextAlign.Start -> Alignment.Start
                    TextAlign.End -> Alignment.End
                    else -> Alignment.CenterHorizontally
                }
            ) {
                // Generous top padding so start of script begins right at the reading guide line
                Spacer(modifier = Modifier.height(180.dp))

                Text(
                    text = script.content,
                    style = TextStyle(
                        fontSize = settings.fontSizeSp.sp,
                        lineHeight = (settings.fontSizeSp * 1.55f).sp, // generous line height for Hindi Devanagari
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        textAlign = settings.textAlignment
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prompter_script_text")
                )

                // Generous bottom spacer so end of script can scroll all the way to the top
                Spacer(modifier = Modifier.height(600.dp))
            }
        }

        // Countdown Overlay (3.. 2.. 1.. GO!)
        AnimatedVisibility(
            visible = countdown > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(StudioCyan.copy(alpha = 0.9f))
                    .border(4.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdown.toString(),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    color = StudioBlack
                )
            }
        }

        // Voice Follow Status Indicator Banner
        AnimatedVisibility(
            visible = settings.voiceFollowEnabled,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassOverlay),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyan.copy(alpha = 0.7f)),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (voiceState == VoiceState.MATCHING) StudioGreenLive else StudioCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (voiceState) {
                            VoiceState.LISTENING -> "Voice Follow: Listening for speech..."
                            VoiceState.MATCHING -> "Voice Follow: Synced (${spokenSnippet.takeLast(25)})"
                            VoiceState.UNSUPPORTED -> "Speech Recognition unavailable"
                            VoiceState.ERROR -> "Voice Follow error (switched to manual)"
                            else -> "Voice Follow active"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Top Navigation Bar (Collapsible)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = GlassBar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.pausePlayback()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("prompter_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = script.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "${settings.scrollSpeed} WPM • ${settings.fontSizeSp.toInt()} SP • ${if (settings.mirrorMode) "Mirrored" else "Standard"}",
                            color = StudioCyan,
                            fontSize = 11.sp
                        )
                    }

                    // Voice Follow toggle
                    IconButton(
                        onClick = { viewModel.toggleVoiceFollow() },
                        modifier = Modifier.testTag("prompter_voice_follow_button")
                    ) {
                        Icon(
                            imageVector = if (settings.voiceFollowEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Voice Follow",
                            tint = if (settings.voiceFollowEnabled) StudioCyan else TextMuted
                        )
                    }

                    // Mirror Mode toggle
                    IconButton(
                        onClick = {
                            val next = !settings.mirrorMode
                            viewModel.updateSettings { it.copy(mirrorMode = next) }
                            Toast.makeText(context, if (next) "Mirror Mode ON" else "Mirror Mode OFF", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("prompter_mirror_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = "Mirror Mode",
                            tint = if (settings.mirrorMode) StudioCyan else Color.White
                        )
                    }
                }
            }
        }

        // Bottom Controls HUD (Collapsible)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassBar)
                    .padding(16.dp)
            ) {
                // Secondary Settings Drawer Panel
                if (activeSettingsTab != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            when (activeSettingsTab) {
                                "speed" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Scroll Speed", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${settings.scrollSpeed} WPM", color = StudioCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Slider(
                                        value = settings.scrollSpeed.toFloat(),
                                        onValueChange = { speed ->
                                            viewModel.updateSettings { it.copy(scrollSpeed = speed.toInt()) }
                                        },
                                        valueRange = 50f..320f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = StudioCyan,
                                            activeTrackColor = StudioCyan,
                                            inactiveTrackColor = StudioCardBorder
                                        ),
                                        modifier = Modifier.testTag("scroll_speed_slider")
                                    )
                                }
                                "font" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Font Size", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${settings.fontSizeSp.toInt()} SP", color = StudioCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Slider(
                                        value = settings.fontSizeSp,
                                        onValueChange = { size ->
                                            viewModel.updateSettings { it.copy(fontSizeSp = size) }
                                        },
                                        valueRange = 18f..56f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = StudioCyan,
                                            activeTrackColor = StudioCyan,
                                            inactiveTrackColor = StudioCardBorder
                                        ),
                                        modifier = Modifier.testTag("font_size_slider")
                                    )
                                    // Alignment toggles
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        IconButton(onClick = { viewModel.updateSettings { it.copy(textAlignment = TextAlign.Start) } }) {
                                            Icon(
                                                Icons.Default.FormatAlignLeft,
                                                contentDescription = "Left",
                                                tint = if (settings.textAlignment == TextAlign.Start) StudioCyan else TextMuted
                                            )
                                        }
                                        IconButton(onClick = { viewModel.updateSettings { it.copy(textAlignment = TextAlign.Center) } }) {
                                            Icon(
                                                Icons.Default.FormatAlignCenter,
                                                contentDescription = "Center",
                                                tint = if (settings.textAlignment == TextAlign.Center) StudioCyan else TextMuted
                                            )
                                        }
                                        IconButton(onClick = { viewModel.updateSettings { it.copy(textAlignment = TextAlign.End) } }) {
                                            Icon(
                                                Icons.Default.FormatAlignRight,
                                                contentDescription = "Right",
                                                tint = if (settings.textAlignment == TextAlign.End) StudioCyan else TextMuted
                                            )
                                        }
                                    }
                                }
                                "color" -> {
                                    Text("Text Color", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        ColorChip(Color.White, settings.textColor == 0xFFFFFFFF) {
                                            viewModel.updateSettings { it.copy(textColor = 0xFFFFFFFF) }
                                        }
                                        ColorChip(StudioCyan, settings.textColor == 0xFF00E5FF) {
                                            viewModel.updateSettings { it.copy(textColor = 0xFF00E5FF) }
                                        }
                                        ColorChip(StudioAmber, settings.textColor == 0xFFF59E0B) {
                                            viewModel.updateSettings { it.copy(textColor = 0xFFF59E0B) }
                                        }
                                        ColorChip(StudioGreenLive, settings.textColor == 0xFF10B981) {
                                            viewModel.updateSettings { it.copy(textColor = 0xFF10B981) }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Background Opacity", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Slider(
                                        value = settings.backgroundOpacity,
                                        onValueChange = { opacity ->
                                            viewModel.updateSettings { it.copy(backgroundOpacity = opacity) }
                                        },
                                        valueRange = 0.2f..1f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = StudioCyan,
                                            activeTrackColor = StudioCyan,
                                            inactiveTrackColor = StudioCardBorder
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Playback Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Restart button
                    IconButton(
                        onClick = {
                            kotlinx.coroutines.runBlocking {
                                scrollState.scrollTo(0)
                            }
                            viewModel.restartPlayback()
                        },
                        modifier = Modifier.testTag("prompter_restart_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = Color.White)
                    }

                    // Speed toggle tab
                    IconButton(
                        onClick = {
                            activeSettingsTab = if (activeSettingsTab == "speed") null else "speed"
                        },
                        modifier = Modifier.testTag("prompter_speed_tab_button")
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = if (activeSettingsTab == "speed") StudioCyan else Color.White
                        )
                    }

                    // Primary Play/Pause Button
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(StudioCyan)
                            .clickable { viewModel.togglePlayPause() }
                            .testTag("prompter_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = StudioBlack,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Font settings toggle tab
                    IconButton(
                        onClick = {
                            activeSettingsTab = if (activeSettingsTab == "font") null else "font"
                        },
                        modifier = Modifier.testTag("prompter_font_tab_button")
                    ) {
                        Icon(
                            Icons.Default.FormatSize,
                            contentDescription = "Font",
                            tint = if (activeSettingsTab == "font") StudioCyan else Color.White
                        )
                    }

                    // Styling & Color tab
                    IconButton(
                        onClick = {
                            activeSettingsTab = if (activeSettingsTab == "color") null else "color"
                        },
                        modifier = Modifier.testTag("prompter_color_tab_button")
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Settings",
                            tint = if (activeSettingsTab == "color") StudioCyan else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorChip(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.White else StudioCardBorder,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}
