package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Script
import com.example.ui.TeleprompterViewModel
import com.example.ui.components.ConfirmationDialog
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioCyanDark
import com.example.ui.theme.StudioDarkSurface
import com.example.ui.theme.StudioGreenLive
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(
    viewModel: TeleprompterViewModel,
    scriptToEdit: Script?,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Script) -> Unit,
    onNavigateToCamera: (Script) -> Unit,
    onNavigateToInAppFloating: (Script) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(scriptToEdit?.title ?: "") }
    var content by remember { mutableStateOf(scriptToEdit?.content ?: "") }
    var language by remember { mutableStateOf(scriptToEdit?.language ?: "hi") }
    var isSaved by remember { mutableStateOf(true) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val wordCount = remember(content) { Script.calculateWordCount(content) }
    val durationSec = remember(wordCount) { Script.calculateDurationSec(wordCount) }

    // Auto-save debounce effect (saves automatically after 2 seconds of inactivity)
    LaunchedEffect(title, content, language) {
        if (title != (scriptToEdit?.title ?: "") || content != (scriptToEdit?.content ?: "")) {
            isSaved = false
            delay(1500)
            if (content.isNotBlank() || title.isNotBlank()) {
                viewModel.saveScript(
                    id = scriptToEdit?.id ?: 0,
                    title = if (title.isBlank()) "Untitled Script" else title,
                    content = content,
                    language = language
                )
                isSaved = true
            }
        }
    }

    // Unsaved changes back press interception
    BackHandler(enabled = !isSaved) {
        showUnsavedDialog = true
    }

    // TXT file import launcher
    val importTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val importedText = reader.readText()
                    content = if (content.isBlank()) importedText else "$content\n\n$importedText"
                    if (title.isBlank()) {
                        title = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Imported Script"
                    }
                    Toast.makeText(context, "TXT content imported!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun buildCurrentScript(): Script {
        return Script(
            id = scriptToEdit?.id ?: 0,
            title = if (title.isBlank()) "Untitled Script" else title,
            content = content,
            wordCount = wordCount,
            estimatedDurationSec = durationSec,
            language = language
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (scriptToEdit == null) "New Script" else "Edit Script",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isSaved) StudioGreenLive else StudioCyan, shape = RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSaved) "Saved" else "Saving changes...",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isSaved) {
                                showUnsavedDialog = true
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Paste from clipboard button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    content = if (content.isBlank()) text else "$content\n$text"
                                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("paste_button")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = TextPrimary)
                    }

                    // Import TXT button
                    IconButton(
                        onClick = { importTxtLauncher.launch("text/plain") },
                        modifier = Modifier.testTag("editor_import_txt_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import TXT", tint = TextPrimary)
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            if (content.isNotBlank()) {
                                shareScript(context, buildCurrentScript())
                            }
                        },
                        modifier = Modifier.testTag("editor_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary)
                    }

                    // Manual Save button
                    IconButton(
                        onClick = {
                            viewModel.saveScript(
                                id = scriptToEdit?.id ?: 0,
                                title = if (title.isBlank()) "Untitled Script" else title,
                                content = content,
                                language = language
                            )
                            isSaved = true
                            Toast.makeText(context, "Script saved successfully", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("editor_save_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = StudioCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioDarkSurface)
            )
        },
        bottomBar = {
            // Quick Launch Bar: Prompter, Camera Studio, Floating
            Surface(
                color = StudioDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val script = buildCurrentScript()
                            viewModel.saveScript(script.id, script.title, script.content, script.language)
                            onNavigateToPlayer(script)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioCyan,
                            contentColor = StudioBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("launch_prompter_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prompter", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val script = buildCurrentScript()
                            viewModel.saveScript(script.id, script.title, script.content, script.language)
                            onNavigateToCamera(script)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioCyanDark,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("launch_camera_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Studio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val script = buildCurrentScript()
                            viewModel.saveScript(script.id, script.title, script.content, script.language)
                            if (Settings.canDrawOverlays(context)) {
                                viewModel.launchFloatingService(context, script)
                                Toast.makeText(context, "Floating overlay active!", Toast.LENGTH_SHORT).show()
                            } else {
                                onNavigateToInAppFloating(script)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioCardSurface,
                            contentColor = StudioCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyan.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("launch_floating_button")
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Floating", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        },
        containerColor = StudioBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Script Title (e.g. YouTube Video Intro / हिंदी स्क्रिप्ट)", color = TextMuted) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioCyan,
                    unfocusedBorderColor = StudioCardBorder,
                    focusedContainerColor = StudioCardSurface,
                    unfocusedContainerColor = StudioCardSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("script_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata & Language Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = StudioCardSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
                ) {
                    Text(
                        text = "$wordCount words • ⏱ ${Script.formatDuration(durationSec)} min",
                        color = StudioCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                FilterChip(
                    selected = language == "hi",
                    onClick = { language = "hi" },
                    label = { Text("हिंदी") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioCyan.copy(alpha = 0.2f),
                        selectedLabelColor = StudioCyan
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                FilterChip(
                    selected = language == "en",
                    onClick = { language = "en" },
                    label = { Text("English") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioCyan.copy(alpha = 0.2f),
                        selectedLabelColor = StudioCyan
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large Script Content Input with Hindi & English support
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = {
                    Text(
                        text = "यहाँ अपना स्क्रिप्ट लिखें या पेस्ट करें...\n\nType or paste your video script here. Full support for Unicode, Hindi Devanagari text, English, and emojis.\n\nUse natural sentence breaks for comfortable teleprompter delivery.",
                        color = TextMuted,
                        lineHeight = 24.sp
                    )
                },
                textStyle = TextStyle(
                    fontSize = 17.sp,
                    lineHeight = 28.sp, // generous line height for Hindi Devanagari matras
                    color = TextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioCyan,
                    unfocusedBorderColor = StudioCardBorder,
                    focusedContainerColor = StudioDarkSurface,
                    unfocusedContainerColor = StudioDarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("script_content_input")
            )
        }
    }

    if (showUnsavedDialog) {
        ConfirmationDialog(
            title = "Unsaved Changes",
            message = "You have unsaved changes in your script. Would you like to discard them or save before leaving?",
            confirmButtonText = "Discard",
            isDestructive = true,
            onConfirm = {
                showUnsavedDialog = false
                onNavigateBack()
            },
            onDismiss = {
                // Save and leave
                viewModel.saveScript(
                    id = scriptToEdit?.id ?: 0,
                    title = if (title.isBlank()) "Untitled Script" else title,
                    content = content,
                    language = language
                )
                showUnsavedDialog = false
                onNavigateBack()
            }
        )
    }
}
