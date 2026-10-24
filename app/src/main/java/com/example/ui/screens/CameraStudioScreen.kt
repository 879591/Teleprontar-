package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.CameraFilter
import com.example.model.Script
import com.example.ui.TeleprompterViewModel
import com.example.ui.components.ConfirmationDialog
import com.example.ui.theme.GlassBar
import com.example.ui.theme.GlassOverlay
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CameraStudioScreen(
    viewModel: TeleprompterViewModel,
    script: Script,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isPausedRecording by viewModel.isPausedRecording.collectAsState()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsState()
    val recordedVideoUri by viewModel.recordedVideoUri.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true &&
                permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    var cameraSelector by remember {
        mutableStateOf(if (settings.cameraFacingFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA)
    }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Teleprompter inside camera state
    var isPrompterPlaying by remember { mutableStateOf(false) }
    val prompterScrollState = rememberScrollState()
    var isCameraSettingsOpen by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(0) }

    BackHandler {
        if (isRecording) {
            activeRecording?.stop()
            viewModel.stopRecording(null)
        }
        onNavigateBack()
    }

    // Auto-scroll loop for the overlay teleprompter
    LaunchedEffect(isPrompterPlaying, settings.scrollSpeed) {
        if (isPrompterPlaying) {
            var lastTime = 0L
            while (isPrompterPlaying) {
                withFrameNanos { time ->
                    if (lastTime != 0L) {
                        val deltaSeconds = (time - lastTime) / 1_000_000_000f
                        val pixelsPerSecond = (settings.scrollSpeed.toFloat() / 130f) * 60f * (settings.fontSizeSp / 24f)
                        val deltaY = pixelsPerSecond * deltaSeconds
                        kotlinx.coroutines.runBlocking {
                            prompterScrollState.scrollBy(deltaY)
                        }
                    }
                    lastTime = time
                }
            }
        }
    }

    // Bind CameraX Lifecycle
    LaunchedEffect(cameraSelector, hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                previewView?.let { pv ->
                    preview.surfaceProvider = pv.surfaceProvider
                }

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()
                val vc = VideoCapture.withOutput(recorder)
                videoCapture = vc

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    vc
                )
            } catch (e: Exception) {
                Log.e("CameraStudio", "Camera binding failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                activeRecording?.stop()
                activeRecording = null
            } catch (e: Exception) {
                Log.e("CameraStudio", "Dispose error: ${e.message}")
            }
        }
    }

    fun startRecordingFlow() {
        if (videoCapture == null) {
            Toast.makeText(context, "Camera is initializing, please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        // Run countdown
        scope.launch {
            if (settings.countdownSeconds > 0) {
                for (i in settings.countdownSeconds downTo 1) {
                    countdownValue = i
                    delay(1000)
                }
                countdownValue = 0
            }

            val videoFile = File(
                context.cacheDir,
                "REC_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())}.mp4"
            )
            val outputOptions = FileOutputOptions.Builder(videoFile).build()

            val pending = videoCapture!!.output.prepareRecording(context, outputOptions)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                pending.withAudioEnabled()
            }

            activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        viewModel.startRecordingTimer()
                        isPrompterPlaying = true // Start auto-scroll automatically with recording!
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!event.hasError()) {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                videoFile
                            )
                            viewModel.stopRecording(uri)
                        } else {
                            viewModel.stopRecording(null)
                            Toast.makeText(context, "Recording failed: ${event.error}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    fun stopRecordingFlow() {
        try {
            activeRecording?.stop()
            activeRecording = null
            isPrompterPlaying = false
        } catch (e: Exception) {
            Log.e("CameraStudio", "Stop error: ${e.message}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
    ) {
        if (hasCameraPermission) {
            // Live CameraX Preview View
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewView = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Mirror preview for front camera if enabled
                        scaleX = if (settings.cameraMirror && cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) -1f else 1f
                    }
            )

            // Optional Color Tint Filter Overlay
            if (settings.cameraFilter != CameraFilter.NONE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when (settings.cameraFilter) {
                                CameraFilter.COOL -> Color(0x2200E5FF)
                                CameraFilter.WARM -> Color(0x22F59E0B)
                                CameraFilter.STUDIO -> Color(0x188B5CF6)
                                CameraFilter.MONO -> Color(0x33000000)
                                else -> Color.Transparent
                            }
                        )
                )
            }
        } else {
            // Permission Denied / Request Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Camera & Microphone Permission Required",
                    style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "To record videos with the teleprompter studio, please grant Camera and Microphone access.",
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("grant_camera_permission_button")
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Teleprompter Script Overlay (Positioned right below status bar, near top camera lens!)
        Card(
            modifier = Modifier
                .fillMaxWidth(settings.overlayWidthPercent)
                .height(240.dp)
                .align(Alignment.TopCenter)
                .padding(top = 70.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(settings.backgroundColor).copy(alpha = settings.backgroundOpacity)
            ),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, StudioCyan.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Mini Overlay Controls Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GlassBar)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EYE-CONTACT PROMPTER",
                        color = StudioCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    // Overlay Play/Pause
                    IconButton(
                        onClick = { isPrompterPlaying = !isPrompterPlaying },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPrompterPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPrompterPlaying) "Pause Prompter" else "Play Prompter",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Reset Scroll
                    IconButton(
                        onClick = {
                            kotlinx.coroutines.runBlocking {
                                prompterScrollState.scrollTo(0)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                // Script text scrolling zone
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Reading Sweet Spot Focus Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .align(Alignment.Center)
                            .background(StudioCyan.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, StudioCyan.copy(alpha = 0.35f), shape = RoundedCornerShape(8.dp))
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(prompterScrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(70.dp))
                        Text(
                            text = script.content,
                            style = TextStyle(
                                fontSize = (settings.fontSizeSp * 0.85f).sp,
                                lineHeight = (settings.fontSizeSp * 1.35f).sp,
                                color = Color(settings.textColor),
                                textAlign = settings.textAlignment,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(200.dp))
                    }
                }
            }
        }

        // Top Status Bar: Back, Recording Indicator / Timer, Flip Camera, Settings
        Surface(
            color = GlassBar,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isRecording) {
                            activeRecording?.stop()
                            viewModel.stopRecording(null)
                        }
                        onNavigateBack()
                    },
                    modifier = Modifier.testTag("camera_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                // Recording Status / Timer
                if (isRecording) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(StudioRedRecord)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REC  ${Script.formatDuration(recordingDurationSec)}",
                            color = StudioRedRecord,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Text(
                        text = "Camera Studio",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Flip Front / Rear Camera
                IconButton(
                    onClick = {
                        val nextFacing = cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA
                        cameraSelector = if (nextFacing) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                        viewModel.updateSettings { it.copy(cameraFacingFront = nextFacing) }
                    },
                    modifier = Modifier.testTag("camera_switch_button")
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White)
                }

                // Camera & Prompter Settings Button
                IconButton(
                    onClick = { isCameraSettingsOpen = !isCameraSettingsOpen },
                    modifier = Modifier.testTag("camera_settings_button")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Settings", tint = if (isCameraSettingsOpen) StudioCyan else Color.White)
                }
            }
        }

        // Big Countdown overlay
        AnimatedVisibility(
            visible = countdownValue > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(StudioCyan.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdownValue.toString(),
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Black,
                    color = StudioBlack
                )
            }
        }

        // Camera Settings Bottom Sheet / Drawer
        AnimatedVisibility(
            visible = isCameraSettingsOpen,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassOverlay),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Camera & Overlay Settings", color = StudioCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Filters Row
                    Text("Color Filter", color = TextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CameraFilter.values().forEach { filter ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateSettings { it.copy(cameraFilter = filter) } },
                                color = if (settings.cameraFilter == filter) StudioCyan else StudioCardSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
                            ) {
                                Text(
                                    text = filter.label,
                                    color = if (settings.cameraFilter == filter) StudioBlack else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prompter Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Prompter Speed", color = TextSecondary, fontSize = 12.sp)
                        Text("${settings.scrollSpeed} WPM", color = StudioCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = settings.scrollSpeed.toFloat(),
                        onValueChange = { viewModel.updateSettings { s -> s.copy(scrollSpeed = it.toInt()) } },
                        valueRange = 50f..300f,
                        colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan)
                    )

                    // Video Mirror Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Front Camera Mirroring", color = TextSecondary, fontSize = 12.sp)
                        Button(
                            onClick = { viewModel.updateSettings { it.copy(cameraMirror = !it.cameraMirror) } },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (settings.cameraMirror) StudioCyan else StudioCardSurface,
                                contentColor = if (settings.cameraMirror) StudioBlack else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (settings.cameraMirror) "Enabled" else "Disabled", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Bottom Recording Action Controls Bar
        Surface(
            color = GlassBar,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Secondary action: Pause/Resume recording
                if (isRecording) {
                    IconButton(
                        onClick = {
                            if (isPausedRecording) {
                                activeRecording?.resume()
                                viewModel.resumeRecording()
                                isPrompterPlaying = true
                            } else {
                                activeRecording?.pause()
                                viewModel.pauseRecording()
                                isPrompterPlaying = false
                            }
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(StudioCardSurface)
                    ) {
                        Icon(
                            imageVector = if (isPausedRecording) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPausedRecording) "Resume" else "Pause",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(50.dp))
                }

                // Primary Record Button (Large circular button)
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(StudioRedRecord)
                        .clickable {
                            if (isRecording) {
                                stopRecordingFlow()
                            } else {
                                startRecordingFlow()
                            }
                        }
                        .testTag("camera_record_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(StudioRedRecord)
                        )
                    }
                }

                // Right spacer or Quick Prompter play button
                IconButton(
                    onClick = { isPrompterPlaying = !isPrompterPlaying },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(if (isPrompterPlaying) StudioCyan else StudioCardSurface)
                ) {
                    Icon(
                        imageVector = if (isPrompterPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Prompter Play",
                        tint = if (isPrompterPlaying) StudioBlack else Color.White
                    )
                }
            }
        }
    }

    // Video Preview Dialog upon recording completion
    recordedVideoUri?.let { uri ->
        VideoPreviewDialog(
            videoUri = uri,
            onDismiss = { viewModel.clearRecordedVideo() },
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Recorded Video"))
            },
            onDelete = {
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    Log.e("CameraStudio", "Delete error: ${e.message}")
                }
                viewModel.clearRecordedVideo()
                Toast.makeText(context, "Recording deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun VideoPreviewDialog(
    videoUri: Uri,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Recording Complete 🎉", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Review your recorded video below:", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                // Video Player View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(videoUri)
                                val mc = MediaController(ctx)
                                mc.setAnchorView(this)
                                setMediaController(mc)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioBlack),
                modifier = Modifier.testTag("preview_share_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Video", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.testTag("preview_delete_button")
                ) {
                    Text("Delete", color = StudioRedRecord)
                }
                TextButton(onClick = onDismiss) {
                    Text("Done", color = TextSecondary)
                }
            }
        },
        containerColor = StudioCardSurface,
        shape = RoundedCornerShape(20.dp)
    )

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = "Delete Recording",
            message = "Are you sure you want to delete this recorded video?",
            confirmButtonText = "Delete",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
