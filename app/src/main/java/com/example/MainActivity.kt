package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Script
import com.example.ui.TeleprompterViewModel
import com.example.ui.screens.CameraStudioScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InAppFloatingScreen
import com.example.ui.screens.ScriptEditorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TeleprompterPlayerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioDarkSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

sealed class Screen {
    object Home : Screen()
    data class Editor(val script: Script?) : Screen()
    data class Player(val script: Script) : Screen()
    data class Camera(val script: Script) : Screen()
    data class InAppFloating(val script: Script) : Screen()
    object Settings : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TeleprompterApp()
            }
        }
    }
}

@Composable
fun TeleprompterApp() {
    val viewModel: TeleprompterViewModel = viewModel()
    val allScripts by viewModel.filteredScripts.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    fun getDefaultOrFirstScript(): Script {
        return allScripts.firstOrNull() ?: Script(
            title = "Quick Speech",
            content = "नमस्ते दोस्तों! टेलीप्रॉम्प्टर प्रो में आपका स्वागत है। Welcome to Teleprompter Pro studio."
        )
    }

    val showBottomNav = currentScreen is Screen.Home || currentScreen is Screen.Settings

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = StudioBlack,
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(
                    containerColor = StudioDarkSurface,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Home,
                        onClick = { currentScreen = Screen.Home },
                        icon = { Icon(Icons.Default.Description, contentDescription = "Scripts") },
                        label = { Text("Scripts") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = StudioCyan,
                            selectedTextColor = StudioCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = StudioCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_scripts_tab")
                    )

                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            currentScreen = Screen.Camera(getDefaultOrFirstScript())
                        },
                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Camera Studio") },
                        label = { Text("Camera") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = StudioCyan,
                            selectedTextColor = StudioCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = StudioCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_camera_tab")
                    )

                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            currentScreen = Screen.InAppFloating(getDefaultOrFirstScript())
                        },
                        icon = { Icon(Icons.Default.Layers, contentDescription = "Floating") },
                        label = { Text("Floating") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = StudioCyan,
                            selectedTextColor = StudioCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = StudioCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_floating_tab")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.Settings,
                        onClick = { currentScreen = Screen.Settings },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("More") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = StudioCyan,
                            selectedTextColor = StudioCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = StudioCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_settings_tab")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Home -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToEditor = { script -> currentScreen = Screen.Editor(script) },
                        onNavigateToPlayer = { script -> currentScreen = Screen.Player(script) },
                        onNavigateToCamera = { script -> currentScreen = Screen.Camera(script) },
                        onNavigateToInAppFloating = { script -> currentScreen = Screen.InAppFloating(script) }
                    )
                }
                is Screen.Editor -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    ScriptEditorScreen(
                        viewModel = viewModel,
                        scriptToEdit = screen.script,
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToPlayer = { script -> currentScreen = Screen.Player(script) },
                        onNavigateToCamera = { script -> currentScreen = Screen.Camera(script) },
                        onNavigateToInAppFloating = { script -> currentScreen = Screen.InAppFloating(script) }
                    )
                }
                is Screen.Player -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    TeleprompterPlayerScreen(
                        viewModel = viewModel,
                        script = screen.script,
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }
                is Screen.Camera -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    CameraStudioScreen(
                        viewModel = viewModel,
                        script = screen.script,
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }
                is Screen.InAppFloating -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    InAppFloatingScreen(
                        viewModel = viewModel,
                        script = screen.script,
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }
                is Screen.Settings -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
