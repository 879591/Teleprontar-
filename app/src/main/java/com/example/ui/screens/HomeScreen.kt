package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Script
import com.example.ui.ScriptFilter
import com.example.ui.SortOrder
import com.example.ui.TeleprompterViewModel
import com.example.ui.components.ConfirmationDialog
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioCyanDark
import com.example.ui.theme.StudioDarkSurface
import com.example.ui.theme.StudioRedRecord
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TeleprompterViewModel,
    onNavigateToEditor: (Script?) -> Unit,
    onNavigateToPlayer: (Script) -> Unit,
    onNavigateToCamera: (Script) -> Unit,
    onNavigateToInAppFloating: (Script) -> Unit
) {
    val context = LocalContext.current
    val scripts by viewModel.filteredScripts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    var isSearchVisible by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var scriptToDelete by remember { mutableStateOf<Script?>(null) }

    // TXT file import launcher
    val importTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()
                    val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Imported Script"
                    viewModel.saveScript(
                        title = fileName,
                        content = content,
                        language = if (content.any { it in '\u0900'..'\u097F' }) "hi" else "en"
                    )
                    Toast.makeText(context, "Script imported successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error importing file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val hasOverlayPermission = Settings.canDrawOverlays(context)

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioDarkSurface)
            ) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Teleprompter",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = StudioCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyan)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = StudioCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { isSearchVisible = !isSearchVisible },
                            modifier = Modifier.testTag("search_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = { importTxtLauncher.launch("text/plain") },
                            modifier = Modifier.testTag("import_txt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = "Import TXT",
                                tint = StudioCyan
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { isSortMenuExpanded = true },
                                modifier = Modifier.testTag("sort_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    tint = TextPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = isSortMenuExpanded,
                                onDismissRequest = { isSortMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Last Modified") },
                                    onClick = {
                                        viewModel.sortOrder.value = SortOrder.DATE_DESC
                                        isSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Script Title (A-Z)") },
                                    onClick = {
                                        viewModel.sortOrder.value = SortOrder.TITLE_ASC
                                        isSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reading Duration") },
                                    onClick = {
                                        viewModel.sortOrder.value = SortOrder.DURATION_DESC
                                        isSortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = StudioDarkSurface
                    )
                )

                // Search Bar Expandable
                AnimatedVisibility(visible = isSearchVisible) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("Search scripts (Hindi / English)...", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("script_search_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioCyan,
                            unfocusedBorderColor = StudioCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = StudioCardSurface,
                            unfocusedContainerColor = StudioCardSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == ScriptFilter.ALL,
                        onClick = { viewModel.selectedFilter.value = ScriptFilter.ALL },
                        label = { Text("All (${scripts.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioCyan.copy(alpha = 0.2f),
                            selectedLabelColor = StudioCyan
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == ScriptFilter.FAVORITES,
                        onClick = { viewModel.selectedFilter.value = ScriptFilter.FAVORITES },
                        label = { Text("★ Favorites") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioAmber.copy(alpha = 0.2f),
                            selectedLabelColor = StudioAmber
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == ScriptFilter.HINDI,
                        onClick = { viewModel.selectedFilter.value = ScriptFilter.HINDI },
                        label = { Text("हिंदी Scripts") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioCyan.copy(alpha = 0.2f),
                            selectedLabelColor = StudioCyan
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == ScriptFilter.ENGLISH,
                        onClick = { viewModel.selectedFilter.value = ScriptFilter.ENGLISH },
                        label = { Text("English") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioCyan.copy(alpha = 0.2f),
                            selectedLabelColor = StudioCyan
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEditor(null) },
                containerColor = StudioCyan,
                contentColor = StudioBlack,
                shape = CircleShape,
                modifier = Modifier.testTag("create_script_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New Script", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = StudioBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Android Overlay Permission Banner
            if (!hasOverlayPermission) {
                item {
                    OverlayPermissionBanner(
                        onRequestPermission = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }

            if (scripts.isEmpty()) {
                item {
                    EmptyScriptState(
                        isSearching = searchQuery.isNotBlank(),
                        onCreateClick = { onNavigateToEditor(null) }
                    )
                }
            } else {
                items(scripts, key = { it.id }) { script ->
                    ScriptCard(
                        script = script,
                        onOpen = { onNavigateToEditor(script) },
                        onPrompter = { onNavigateToPlayer(script) },
                        onCamera = { onNavigateToCamera(script) },
                        onFloating = {
                            if (Settings.canDrawOverlays(context)) {
                                viewModel.launchFloatingService(context, script)
                                Toast.makeText(context, "Floating Teleprompter active over apps!", Toast.LENGTH_SHORT).show()
                            } else {
                                onNavigateToInAppFloating(script)
                            }
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(script) },
                        onShare = { shareScript(context, script) },
                        onExportTxt = { exportScriptAsTxt(context, script) },
                        onDelete = { scriptToDelete = script }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    scriptToDelete?.let { script ->
        ConfirmationDialog(
            title = "Delete Script",
            message = "Are you sure you want to delete '${script.title}'? This action cannot be undone.",
            confirmButtonText = "Delete",
            isDestructive = true,
            onConfirm = { viewModel.deleteScript(script) },
            onDismiss = { scriptToDelete = null }
        )
    }
}

@Composable
fun OverlayPermissionBanner(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioAmber.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(StudioAmber.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = StudioAmber)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable Floating Teleprompter",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Read scripts floating over Camera, Zoom, or YouTube apps",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            TextButton(
                onClick = onRequestPermission,
                modifier = Modifier.testTag("enable_overlay_button")
            ) {
                Text("Enable", color = StudioCyan, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ScriptCard(
    script: Script,
    onOpen: () -> Unit,
    onPrompter: () -> Unit,
    onCamera: () -> Unit,
    onFloating: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onExportTxt: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val formattedDate = remember(script.updatedAt) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(script.updatedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("script_card_${script.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title, Fav & Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = script.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (script.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (script.isFavorite) StudioAmber else TextMuted
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextMuted)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Script") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpen()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Script") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as TXT") },
                            leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onExportTxt()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Script", color = StudioRedRecord) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StudioRedRecord) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Snippet
            Text(
                text = script.content,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // Tags row: Word count, duration, language tag, date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TagBadge(text = "${script.wordCount} words")
                TagBadge(text = "⏱ ${Script.formatDuration(script.estimatedDurationSec)}")
                if (script.language == "hi") {
                    TagBadge(text = "हिंदी", color = StudioCyan)
                } else if (script.language == "mixed") {
                    TagBadge(text = "हिंदी + EN", color = StudioAmber)
                }

                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row: Prompter, Camera, Floating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CardActionButton(
                    icon = Icons.Default.PlayArrow,
                    label = "Prompter",
                    color = StudioCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onPrompter
                )

                CardActionButton(
                    icon = Icons.Default.CameraAlt,
                    label = "Studio",
                    color = StudioCyanDark,
                    modifier = Modifier.weight(1f),
                    onClick = onCamera
                )

                CardActionButton(
                    icon = Icons.Default.Layers,
                    label = "Floating",
                    color = StudioAmber,
                    modifier = Modifier.weight(1f),
                    onClick = onFloating
                )
            }
        }
    }
}

@Composable
fun TagBadge(text: String, color: Color = TextSecondary) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CardActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyScriptState(isSearching: Boolean, onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(StudioCyan.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearching) Icons.Default.Search else Icons.Default.Edit,
                contentDescription = null,
                tint = StudioCyan,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSearching) "No matching scripts found" else "No scripts yet",
            style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isSearching) "Try a different search query in Hindi or English." else "Create your first video script or import a TXT file to start recording like a pro.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (!isSearching) {
            Spacer(modifier = Modifier.height(20.dp))
            androidx.compose.material3.Button(
                onClick = onCreateClick,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = StudioCyan,
                    contentColor = StudioBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Script", fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun shareScript(context: Context, script: Script) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TITLE, script.title)
        putExtra(Intent.EXTRA_SUBJECT, script.title)
        putExtra(Intent.EXTRA_TEXT, "${script.title}\n\n${script.content}")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Script")
    context.startActivity(shareIntent)
}

fun exportScriptAsTxt(context: Context, script: Script) {
    try {
        val file = java.io.File(context.cacheDir, "${script.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.txt")
        file.writeText("${script.title}\n\n${script.content}")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "text/plain"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export TXT File"))
    } catch (e: Exception) {
        // Fallback to text sharing
        shareScript(context, script)
    }
}
