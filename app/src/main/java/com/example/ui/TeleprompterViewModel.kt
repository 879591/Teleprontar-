package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CameraFilter
import com.example.data.PrompterSettings
import com.example.data.ScriptRepository
import com.example.model.Script
import com.example.service.FloatingTeleprompterService
import com.example.voice.VoiceFollowManager
import com.example.voice.VoiceState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScriptFilter {
    ALL, FAVORITES, HINDI, ENGLISH
}

enum class SortOrder {
    DATE_DESC, TITLE_ASC, DURATION_DESC
}

class TeleprompterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScriptRepository
    private var voiceFollowManager: VoiceFollowManager? = null

    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow(ScriptFilter.ALL)
    val sortOrder = MutableStateFlow(SortOrder.DATE_DESC)

    private val _settings = MutableStateFlow(PrompterSettings())
    val settings: StateFlow<PrompterSettings> = _settings.asStateFlow()

    private val _activeScript = MutableStateFlow<Script?>(null)
    val activeScript: StateFlow<Script?> = _activeScript.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentScrollFraction = MutableStateFlow(0f)
    val currentScrollFraction: StateFlow<Float> = _currentScrollFraction.asStateFlow()

    private val _countdown = MutableStateFlow(0)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    // Voice Follow state
    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _spokenSnippet = MutableStateFlow("")
    val spokenSnippet: StateFlow<String> = _spokenSnippet.asStateFlow()

    // Recording state
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPausedRecording = MutableStateFlow(false)
    val isPausedRecording: StateFlow<Boolean> = _isPausedRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _recordedVideoUri = MutableStateFlow<Uri?>(null)
    val recordedVideoUri: StateFlow<Uri?> = _recordedVideoUri.asStateFlow()

    private var recordTimerJob: Job? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ScriptRepository(db.scriptDao())

        voiceFollowManager = VoiceFollowManager(application) { wordIndex, progressFraction ->
            _currentScrollFraction.value = progressFraction
        }

        viewModelScope.launch {
            voiceFollowManager?.voiceState?.collect { state ->
                _voiceState.value = state
            }
        }
        viewModelScope.launch {
            voiceFollowManager?.latestSpokenText?.collect { text ->
                _spokenSnippet.value = text
            }
        }
    }

    val filteredScripts: StateFlow<List<Script>> = combine(
        repository.allScripts,
        searchQuery,
        selectedFilter,
        sortOrder
    ) { all, query, filter, sort ->
        var list = if (query.isBlank()) {
            all
        } else {
            all.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
            }
        }

        list = when (filter) {
            ScriptFilter.ALL -> list
            ScriptFilter.FAVORITES -> list.filter { it.isFavorite }
            ScriptFilter.HINDI -> list.filter { it.language == "hi" || it.language == "mixed" }
            ScriptFilter.ENGLISH -> list.filter { it.language == "en" }
        }

        when (sort) {
            SortOrder.DATE_DESC -> list.sortedByDescending { it.updatedAt }
            SortOrder.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
            SortOrder.DURATION_DESC -> list.sortedByDescending { it.estimatedDurationSec }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectScript(script: Script?) {
        _activeScript.value = script
        _currentScrollFraction.value = 0f
        _isPlaying.value = false
        if (script != null) {
            voiceFollowManager?.setScript(script.content, _settings.value.speechLanguage)
        }
    }

    fun saveScript(
        id: Long = 0,
        title: String,
        content: String,
        language: String = "hi"
    ) {
        viewModelScope.launch {
            val script = Script(
                id = id,
                title = if (title.isBlank()) "Untitled Script" else title.trim(),
                content = content.trim(),
                language = language,
                isFavorite = _activeScript.value?.isFavorite ?: false
            )
            val newId = repository.saveScript(script)
            val saved = repository.getScriptByIdOnce(if (id == 0L) newId else id)
            _activeScript.value = saved
            saved?.let {
                voiceFollowManager?.setScript(it.content, _settings.value.speechLanguage)
            }
        }
    }

    fun deleteScript(script: Script) {
        viewModelScope.launch {
            repository.deleteScript(script)
            if (_activeScript.value?.id == script.id) {
                _activeScript.value = null
            }
        }
    }

    fun toggleFavorite(script: Script) {
        viewModelScope.launch {
            repository.toggleFavorite(script.id, !script.isFavorite)
            if (_activeScript.value?.id == script.id) {
                _activeScript.value = _activeScript.value?.copy(isFavorite = !script.isFavorite)
            }
        }
    }

    fun updateSettings(transform: (PrompterSettings) -> PrompterSettings) {
        _settings.value = transform(_settings.value)
        _activeScript.value?.let {
            voiceFollowManager?.setScript(it.content, _settings.value.speechLanguage)
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlaybackWithCountdown()
        }
    }

    fun startPlaybackWithCountdown() {
        val count = _settings.value.countdownSeconds
        if (count > 0) {
            viewModelScope.launch {
                for (i in count downTo 1) {
                    _countdown.value = i
                    delay(1000)
                }
                _countdown.value = 0
                _isPlaying.value = true
                if (_settings.value.voiceFollowEnabled) {
                    voiceFollowManager?.startListening()
                }
            }
        } else {
            _isPlaying.value = true
            if (_settings.value.voiceFollowEnabled) {
                voiceFollowManager?.startListening()
            }
        }
    }

    fun pausePlayback() {
        _isPlaying.value = false
        voiceFollowManager?.stopListening()
    }

    fun restartPlayback() {
        _currentScrollFraction.value = 0f
        _isPlaying.value = false
        voiceFollowManager?.stopListening()
    }

    fun setScrollFraction(fraction: Float) {
        _currentScrollFraction.value = fraction.coerceIn(0f, 1f)
    }

    fun toggleVoiceFollow() {
        val next = !_settings.value.voiceFollowEnabled
        updateSettings { it.copy(voiceFollowEnabled = next) }
        if (next && _isPlaying.value) {
            voiceFollowManager?.startListening()
        } else if (!next) {
            voiceFollowManager?.stopListening()
        }
    }

    // Recording methods
    fun startRecordingTimer() {
        _isRecording.value = true
        _isPausedRecording.value = false
        _recordingDurationSec.value = 0
        recordTimerJob?.cancel()
        recordTimerJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                if (!_isPausedRecording.value) {
                    _recordingDurationSec.value += 1
                }
            }
        }
    }

    fun pauseRecording() {
        _isPausedRecording.value = true
    }

    fun resumeRecording() {
        _isPausedRecording.value = false
    }

    fun stopRecording(videoUri: Uri?) {
        _isRecording.value = false
        _isPausedRecording.value = false
        recordTimerJob?.cancel()
        _recordedVideoUri.value = videoUri
    }

    fun clearRecordedVideo() {
        _recordedVideoUri.value = null
    }

    // Floating overlay service launcher
    fun launchFloatingService(context: Context, script: Script) {
        if (Settings.canDrawOverlays(context)) {
            val intent = Intent(context, FloatingTeleprompterService::class.java).apply {
                putExtra(FloatingTeleprompterService.EXTRA_TITLE, script.title)
                putExtra(FloatingTeleprompterService.EXTRA_CONTENT, script.content)
                putExtra(FloatingTeleprompterService.EXTRA_SPEED, _settings.value.scrollSpeed)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun stopFloatingService(context: Context) {
        val intent = Intent(context, FloatingTeleprompterService::class.java).apply {
            action = FloatingTeleprompterService.ACTION_STOP
        }
        context.startService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        voiceFollowManager?.stopListening()
        recordTimerJob?.cancel()
    }
}
