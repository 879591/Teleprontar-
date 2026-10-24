package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Script
import com.example.ui.TeleprompterViewModel
import com.example.ui.theme.GlassBar
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioDarkSurface
import com.example.ui.theme.StudioRedRecord
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppFloatingScreen(
    viewModel: TeleprompterViewModel,
    script: Script,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    var offsetX by remember { mutableFloatStateOf(40f) }
    var offsetY by remember { mutableFloatStateOf(160f) }
    var panelWidthDp by remember { mutableFloatStateOf(320f) }
    var panelHeightDp by remember { mutableFloatStateOf(300f) }
    var transparency by remember { mutableFloatStateOf(0.85f) }
    var isPlaying by remember { mutableStateOf(false) }

    val prompterScroll = rememberScrollState()

    // Auto-scroll loop
    LaunchedEffect(isPlaying, settings.scrollSpeed) {
        if (isPlaying) {
            var lastTime = 0L
            while (isPlaying) {
                withFrameNanos { time ->
                    if (lastTime != 0L) {
                        val deltaSeconds = (time - lastTime) / 1_000_000_000f
                        val pixelsPerSecond = (settings.scrollSpeed.toFloat() / 130f) * 60f
                        val deltaY = pixelsPerSecond * deltaSeconds
                        kotlinx.coroutines.runBlocking {
                            prompterScroll.scrollBy(deltaY)
                        }
                    }
                    lastTime = time
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Floating Teleprompter", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Launch Native System Overlay if permission granted
                    Button(
                        onClick = {
                            if (Settings.canDrawOverlays(context)) {
                                viewModel.launchFloatingService(context, script)
                                Toast.makeText(context, "System floating window launched!", Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                                Toast.makeText(context, "Grant 'Display over other apps' permission", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioCyan,
                            contentColor = StudioBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("launch_system_overlay_button")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("System Overlay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioDarkSurface)
            )
        },
        containerColor = StudioBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background preview illustration showing other content beneath floating prompter
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "In-App Draggable Floating Mode",
                    style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Touch and drag the top cyan header bar to reposition the window.\n• Tap 'System Overlay' to float over TikTok, YouTube, Camera, or Zoom outside this app!\n• Adjust transparency and dimensions below.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Transparency & Size Controls Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Panel Opacity", color = TextSecondary, fontSize = 13.sp)
                            Text("${(transparency * 100).toInt()}%", color = StudioCyan, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = transparency,
                            onValueChange = { transparency = it },
                            valueRange = 0.2f..1f,
                            colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Panel Height", color = TextSecondary, fontSize = 13.sp)
                            Text("${panelHeightDp.toInt()} dp", color = StudioCyan, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = panelHeightDp,
                            onValueChange = { panelHeightDp = it },
                            valueRange = 200f..500f,
                            colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan)
                        )
                    }
                }
            }

            // Draggable In-App Floating Panel Widget
            Card(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .size(panelWidthDp.dp, panelHeightDp.dp),
                colors = CardDefaults.cardColors(
                    containerColor = StudioDarkSurface.copy(alpha = transparency)
                ),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, StudioCyan)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Draggable Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StudioCyan.copy(alpha = 0.25f))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount.x
                                    offsetY += dragAmount.y
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DragHandle, contentDescription = "Drag Handle", tint = StudioCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = script.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioRedRecord, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Scrolling Text Body
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(prompterScroll)
                        ) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = script.content,
                                style = TextStyle(
                                    fontSize = (settings.fontSizeSp * 0.8f).sp,
                                    lineHeight = (settings.fontSizeSp * 1.3f).sp,
                                    color = Color.White,
                                    textAlign = settings.textAlignment,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(180.dp))
                        }
                    }

                    // Bottom Floating Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassBar)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { isPlaying = !isPlaying },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioBlack),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPlaying) "Pause" else "Play", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = {
                                kotlinx.coroutines.runBlocking {
                                    prompterScroll.scrollTo(0)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        Text("${settings.scrollSpeed} wpm", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
