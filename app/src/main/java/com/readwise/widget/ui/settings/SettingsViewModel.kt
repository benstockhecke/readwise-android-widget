package com.readwise.widget.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import com.readwise.widget.ReadwiseApp
import com.readwise.widget.data.BookEntity
import com.readwise.widget.data.TagEntity
import com.readwise.widget.widget.HighlightWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Success(val count: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ReadwiseApp
    private val settings = app.settingsDataStore
    private val repository = app.highlightRepository

    // ── Settings State Flows ───────────────────────────────────────────

    val apiToken: StateFlow<String> = settings.apiToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val refreshIntervalMinutes: StateFlow<Int> = settings.refreshIntervalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 30)

    val filterBookId: StateFlow<Long?> = settings.filterBookId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val filterTagName: StateFlow<String?> = settings.filterTagName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val fontSize: StateFlow<Float> = settings.widgetFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 16f)

    val fontFamily: StateFlow<String> = settings.widgetFontFamily
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "default")

    val backgroundColor: StateFlow<Long> = settings.widgetBackgroundColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0xFFFFFFFFL)

    val textColor: StateFlow<Long> = settings.widgetTextColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0xFF1C1B1FL)

    val sourceColor: StateFlow<Long> = settings.widgetSourceColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0xFF49454FL)

    val cornerRadius: StateFlow<Float> = settings.widgetCornerRadius
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 16f)

    val borderWidth: StateFlow<Float> = settings.widgetBorderWidth
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    val borderColor: StateFlow<Long> = settings.widgetBorderColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0xFF000000L)

    val padding: StateFlow<Float> = settings.widgetPadding
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 16f)

    val useDynamicColors: StateFlow<Boolean> = settings.useDynamicColors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val maxHighlightLength: StateFlow<Int> = settings.maxHighlightLength
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 300)

    // ── Save State ────────────────────────────────────────────────────

    private val _saveState = MutableStateFlow(false)
    val saveState: StateFlow<Boolean> = _saveState.asStateFlow()

    // ── Sync State ─────────────────────────────────────────────────────

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // ── Filter Lists ───────────────────────────────────────────────────

    private val _books = MutableStateFlow<List<BookEntity>>(emptyList())
    val books: StateFlow<List<BookEntity>> = _books.asStateFlow()

    private val _tags = MutableStateFlow<List<TagEntity>>(emptyList())
    val tags: StateFlow<List<TagEntity>> = _tags.asStateFlow()

    // ── Highlight Count ────────────────────────────────────────────────

    private val _highlightCount = MutableStateFlow(0)
    val highlightCount: StateFlow<Int> = _highlightCount.asStateFlow()

    init {
        loadFilters()
        loadHighlightCount()
    }

    // ── Actions ────────────────────────────────────────────────────────

    fun setApiToken(token: String) {
        viewModelScope.launch { settings.setApiToken(token) }
    }

    fun setRefreshIntervalMinutes(minutes: Int) {
        viewModelScope.launch { settings.setRefreshIntervalMinutes(minutes) }
    }

    fun setFilterBookId(bookId: Long?) {
        viewModelScope.launch { settings.setFilterBookId(bookId) }
    }

    fun setFilterTagName(tagName: String?) {
        viewModelScope.launch { settings.setFilterTagName(tagName) }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch { settings.setWidgetFontSize(size) }
    }

    fun setFontFamily(family: String) {
        viewModelScope.launch { settings.setWidgetFontFamily(family) }
    }

    fun setBackgroundColor(color: Long) {
        viewModelScope.launch { settings.setWidgetBackgroundColor(color) }
    }

    fun setTextColor(color: Long) {
        viewModelScope.launch { settings.setWidgetTextColor(color) }
    }

    fun setSourceColor(color: Long) {
        viewModelScope.launch { settings.setWidgetSourceColor(color) }
    }

    fun setCornerRadius(radius: Float) {
        viewModelScope.launch { settings.setWidgetCornerRadius(radius) }
    }

    fun setBorderWidth(width: Float) {
        viewModelScope.launch { settings.setWidgetBorderWidth(width) }
    }

    fun setBorderColor(color: Long) {
        viewModelScope.launch { settings.setWidgetBorderColor(color) }
    }

    fun setPadding(padding: Float) {
        viewModelScope.launch { settings.setWidgetPadding(padding) }
    }

    fun setUseDynamicColors(enabled: Boolean) {
        viewModelScope.launch { settings.setUseDynamicColors(enabled) }
    }

    fun setMaxHighlightLength(length: Int) {
        viewModelScope.launch { settings.setMaxHighlightLength(length) }
    }

    fun save() {
        viewModelScope.launch {
            HighlightWidget().updateAll(app)
            _saveState.value = true
            kotlinx.coroutines.delay(2000)
            _saveState.value = false
        }
    }

    fun sync() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                repository.syncHighlights()
                val count = repository.getHighlightCount()
                _highlightCount.value = count
                _syncState.value = SyncState.Success(count)
                loadFilters()
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadFilters() {
        viewModelScope.launch {
            try {
                _books.value = repository.getBooks()
                _tags.value = repository.getTags()
            } catch (_: Exception) {
                // DB might be empty on first launch
            }
        }
    }

    private fun loadHighlightCount() {
        viewModelScope.launch {
            try {
                _highlightCount.value = repository.getHighlightCount()
            } catch (_: Exception) {
                _highlightCount.value = 0
            }
        }
    }
}
