package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TeleprompterViewModel
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioDarkSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TeleprompterViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val scripts by viewModel.filteredScripts.collectAsState()

    var activeDialog by remember { mutableStateOf<String?>(null) } // "cloud_oauth", "tutorials", "about", "privacy", "terms"

    // JSON Backup Export Launcher
    val exportBackup = {
        try {
            val jsonArray = JSONArray()
            scripts.forEach { script ->
                val obj = JSONObject().apply {
                    put("id", script.id)
                    put("title", script.title)
                    put("content", script.content)
                    put("language", script.language)
                    put("wordCount", script.wordCount)
                    put("estimatedDurationSec", script.estimatedDurationSec)
                    put("isFavorite", script.isFavorite)
                }
                jsonArray.put(obj)
            }
            val cacheFile = java.io.File(context.cacheDir, "teleprompter_scripts_backup.json")
            cacheFile.writeText(jsonArray.toString(2))
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Teleprompter Pro Scripts Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Save Scripts Backup"))
        } catch (e: Exception) {
            Toast.makeText(context, "Backup export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Cloud", color = TextPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioDarkSurface)
            )
        },
        containerColor = StudioBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cloud Sync & Account Section
            Text("ACCOUNT & CLOUD SYNC", color = StudioCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(StudioCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = StudioCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google Drive Cloud Sync", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Status: Local Only (Offline Storage)", color = StudioAmber, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Scripts are saved securely in your on-device Room database. To enable cross-device Google Drive synchronization, configure the Google OAuth client credentials.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { activeDialog = "cloud_oauth" },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioBlack),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_sync_setup_button")
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Configure Google Cloud Sync", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Local Data Backup & Restore
            Text("BACKUP & EXPORT", color = StudioCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingActionItem(
                        icon = Icons.Default.Backup,
                        title = "Export All Scripts (${scripts.size})",
                        subtitle = "Download JSON backup of all your Hindi & English scripts",
                        onClick = { exportBackup() }
                    )
                }
            }

            // Voice Recognition Language Settings
            Text("SPEECH & VOICE FOLLOW", color = StudioCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Voice Follow Primary Language", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.speechLanguage == "hi-IN",
                            onClick = {
                                viewModel.updateSettings { it.copy(speechLanguage = "hi-IN") }
                                Toast.makeText(context, "Voice Follow set to Hindi (हिन्दी)", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("हिन्दी (Hindi - India)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioCyan.copy(alpha = 0.2f),
                                selectedLabelColor = StudioCyan
                            )
                        )
                        FilterChip(
                            selected = settings.speechLanguage == "en-US",
                            onClick = {
                                viewModel.updateSettings { it.copy(speechLanguage = "en-US") }
                                Toast.makeText(context, "Voice Follow set to English", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("English (US / Global)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioCyan.copy(alpha = 0.2f),
                                selectedLabelColor = StudioCyan
                            )
                        )
                    }
                }
            }

            // More & Information Section
            Text("INFO & SUPPORT", color = StudioCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingActionItem(
                        icon = Icons.Default.Help,
                        title = "Teleprompter Tutorials & Tips",
                        subtitle = "Eye contact techniques, glass rigging, and speed guide",
                        onClick = { activeDialog = "tutorials" }
                    )
                    SettingActionItem(
                        icon = Icons.Default.Share,
                        title = "Share Teleprompter Pro",
                        subtitle = "Recommend this studio teleprompter to fellow creators",
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Check out Teleprompter Pro for Android - with Hindi Devanagari support, Camera Studio recording, and Floating Overlay!")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share App"))
                        }
                    )
                    SettingActionItem(
                        icon = Icons.Default.Info,
                        title = "About Developer",
                        subtitle = "Version 1.0 • Built with Jetpack Compose & CameraX",
                        onClick = { activeDialog = "about" }
                    )
                    SettingActionItem(
                        icon = Icons.Default.Lock,
                        title = "Privacy Policy",
                        subtitle = "Zero tracking • Local on-device script security",
                        onClick = { activeDialog = "privacy" }
                    )
                    SettingActionItem(
                        icon = Icons.Default.Description,
                        title = "Terms & Conditions",
                        subtitle = "Usage rights and camera guidelines",
                        onClick = { activeDialog = "terms" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Informational Dialogs
    when (activeDialog) {
        "cloud_oauth" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Google Drive Cloud Sync Setup", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Column {
                        Text(
                            text = "To enable automatic Google Drive cloud backup:\n\n1. Register an OAuth 2.0 Client ID in your Google Cloud Console for package: ${context.packageName}\n2. Request the 'https://www.googleapis.com/auth/drive.file' scope.\n3. Add your Web/Android client ID in your environment secrets.\n\nAll scripts currently remain safely stored on your device in local Room database.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { activeDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioBlack)
                    ) {
                        Text("Understood")
                    }
                },
                containerColor = StudioCardSurface,
                shape = RoundedCornerShape(18.dp)
            )
        }
        "tutorials" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Creator Tips & Tutorials", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("1. Natural Eye Contact", fontWeight = FontWeight.Bold, color = StudioCyan, fontSize = 14.sp)
                        Text("Position the floating teleprompter as close to your camera lens as possible. This ensures you maintain direct gaze with your audience.", color = TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("2. Optimal Speaking Pace", fontWeight = FontWeight.Bold, color = StudioCyan, fontSize = 14.sp)
                        Text("120 - 140 WPM is ideal for conversational YouTube and explanatory videos. For rapid news, use 150 - 180 WPM.", color = TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("3. Beam-Splitter Glass Rigging", fontWeight = FontWeight.Bold, color = StudioCyan, fontSize = 14.sp)
                        Text("Turn on 'Mirror Mode' in player controls when using external teleprompter glass hardware.", color = TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("4. Voice Follow (Speech Sync)", fontWeight = FontWeight.Bold, color = StudioCyan, fontSize = 14.sp)
                        Text("Enable Voice Follow to let the script automatically advance at your natural speaking rhythm in both Hindi and English.", color = TextSecondary, fontSize = 13.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { activeDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioBlack)
                    ) {
                        Text("Got It")
                    }
                },
                containerColor = StudioCardSurface,
                shape = RoundedCornerShape(18.dp)
            )
        }
        "about" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("About Teleprompter Pro", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Text(
                        text = "Teleprompter Pro is engineered for professional video creators, journalists, educators, and public speakers.\n\nKey Highlights:\n• Complete Hindi Devanagari & English text rendering\n• Camera Studio with live recording & teleprompter HUD\n• True Android system floating overlay over other apps\n• Smart Voice Follow speech recognition\n• 100% offline local storage with Room database",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { activeDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioBlack)
                    ) {
                        Text("Close")
                    }
                },
                containerColor = StudioCardSurface,
                shape = RoundedCornerShape(18.dp)
            )
        }
        "privacy" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Text(
                        text = "Your privacy is paramount:\n\n• Your scripts are stored strictly locally on your device.\n• Camera and microphone are accessed only while you are actively in Camera Studio recording or using Voice Follow.\n• No video recordings or audio are uploaded to any external server without your explicit action.\n• No advertising trackers or data brokers.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { activeDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioBlack)
                    ) {
                        Text("Close")
                    }
                },
                containerColor = StudioCardSurface,
                shape = RoundedCornerShape(18.dp)
            )
        }
        "terms" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Text(
                        text = "By using Teleprompter Pro, you retain full copyright ownership of all authored scripts and recorded videos. The software is provided as-is for video presentation and speech assistance.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { activeDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioBlack)
                    ) {
                        Text("Close")
                    }
                },
                containerColor = StudioCardSurface,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
fun SettingActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(StudioCyan.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
    }
}
