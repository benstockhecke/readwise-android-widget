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

/**
 * Represents the lifecycle of a Readwise data sync operation.
 */
sealed class SyncState {
    /** No sync is in progress and no result is available. */
    data object Idle : SyncState()

    /** A sync is currently running. */
    data object Syncing : SyncState()

    /**
     * The sync completed successfully.
     * @property count Number of highlights now stored in the local database.
     */
    data class Success(val count: Int) : SyncState()

    /**
     * The sync failed with an error.
     * @property message Human-readable description of the failure.
     */
    data class Error(val message: String) : SyncState()
}

/**
 * [AndroidViewModel] that backs the settings screen.
 *
 * Exposes all user-configurable settings from [SettingsDataStore] as [StateFlow]s
 * and provides action functions that persist changes and trigger widget updates.
 *
 * Also manages the sync lifecycle ([syncState]) and populates the book/tag filter
 * lists ([books], [tags]) from the local database.
 *
 * @param application Application instance used to access app-level singletons.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ReadwiseApp
    private val settings = app.settingsDataStore
    private val repository = app.highlightRepository

    // ── Settings State Flows ───────────────────────────────────────────
    // Each flow is converted to a StateFlow so the UI always has a current value.
    // WhileSubscribed(5_000) keeps the upstream active for 5 seconds after the
    // last subscriber disappears, tolerating brief configuration changes.

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

    /**
     * Transient flag indicating that settings have just been saved and the widget
     * has been updated. Resets to `false` after ~2 seconds.
     */
    private val _saveState = MutableStateFlow(false)
    val saveState: StateFlow<Boolean> = _saveState.asStateFlow()

    // ── Sync State ─────────────────────────────────────────────────────

    /** Current state of the Readwise data sync operation. */
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // ── Filter Lists ───────────────────────────────────────────────────

    /** Available books for the book filter drop-down, loaded from the local database. */
    private val _books = MutableStateFlow<List<BookEntity>>(emptyList())
    val books: StateFlow<List<BookEntity>> = _books.asStateFlow()

    /** Available tags for the tag filter drop-down, loaded from the local database. */
    private val _tags = MutableStateFlow<List<TagEntity>>(emptyList())
    val tags: StateFlow<List<TagEntity>> = _tags.asStateFlow()

    // ── Highlight Count ────────────────────────────────────────────────

    /** Total number of highlights stored in the local database. */
    private val _highlightCount = MutableStateFlow(0)
    val highlightCount: StateFlow<Int> = _highlightCount.asStateFlow()

    init {
        // Pre-populate filter lists and the highlight count when the ViewModel is created
        loadFilters()
        loadHighlightCount()
    }

    // ── Actions ────────────────────────────────────────────────────────

    /** Persists a new Readwise API [token]. */
    fun setApiToken(token: String) {
        viewModelScope.launch { settings.setApiToken(token) }
    }

    /** Persists the background sync interval in [minutes]. */
    fun setRefreshIntervalMinutes(minutes: Int) {
        viewModelScope.launch { settings.setRefreshIntervalMinutes(minutes) }
    }

    /** Persists the book filter. Pass `null` to clear it. */
    fun setFilterBookId(bookId: Long?) {
        viewModelScope.launch { settings.setFilterBookId(bookId) }
    }

    /** Persists the tag filter. Pass `null` to clear it. */
    fun setFilterTagName(tagName: String?) {
        viewModelScope.launch { settings.setFilterTagName(tagName) }
    }

    /** Persists the widget font size in SP. */
    fun setFontSize(size: Float) {
        viewModelScope.launch { settings.setWidgetFontSize(size) }
    }

    /** Persists the widget font family identifier. */
    fun setFontFamily(family: String) {
        viewModelScope.launch { settings.setWidgetFontFamily(family) }
    }

    /** Persists the widget background color as a packed ARGB long. */
    fun setBackgroundColor(color: Long) {
        viewModelScope.launch { settings.setWidgetBackgroundColor(color) }
    }

    /** Persists the widget text color as a packed ARGB long. */
    fun setTextColor(color: Long) {
        viewModelScope.launch { settings.setWidgetTextColor(color) }
    }

    /** Persists the widget source attribution color as a packed ARGB long. */
    fun setSourceColor(color: Long) {
        viewModelScope.launch { settings.setWidgetSourceColor(color) }
    }

    /** Persists the widget corner radius in DP. */
    fun setCornerRadius(radius: Float) {
        viewModelScope.launch { settings.setWidgetCornerRadius(radius) }
    }

    /** Persists the widget border stroke width in DP. */
    fun setBorderWidth(width: Float) {
        viewModelScope.launch { settings.setWidgetBorderWidth(width) }
    }

    /** Persists the widget border color as a packed ARGB long. */
    fun setBorderColor(color: Long) {
        viewModelScope.launch { settings.setWidgetBorderColor(color) }
    }

    /** Persists the widget inner padding in DP. */
    fun setPadding(padding: Float) {
        viewModelScope.launch { settings.setWidgetPadding(padding) }
    }

    /** Persists the dynamic colors preference. */
    fun setUseDynamicColors(enabled: Boolean) {
        viewModelScope.launch { settings.setUseDynamicColors(enabled) }
    }

    /** Persists the maximum highlight character length filter. */
    fun setMaxHighlightLength(length: Int) {
        viewModelScope.launch { settings.setMaxHighlightLength(length) }
    }

    /**
     * Pushes the current settings to all active widget instances and briefly
     * sets [saveState] to `true` so the UI can show a confirmation indicator.
     */
    fun save() {
        viewModelScope.launch {
            HighlightWidget().updateAll(app)
            _saveState.value = true
            // Reset the save indicator after a short delay
            kotlinx.coroutines.delay(2000)
            _saveState.value = false
        }
    }

    /**
     * Triggers a full sync from the Readwise API, updating [syncState] throughout.
     *
     * On success, the highlight count and filter lists are refreshed so the UI
     * reflects the newly imported data. On failure, [SyncState.Error] is emitted
     * with the exception message.
     */
    fun sync() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                repository.syncHighlights()
                val count = repository.getHighlightCount()
                _highlightCount.value = count
                _syncState.value = SyncState.Success(count)
                // Refresh filter lists with data from the newly synced dataset
                loadFilters()
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Loads the available books and tags from the local database into [books]
     * and [tags]. Silently ignores errors (e.g. empty DB on first launch).
     */
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

    /**
     * Loads the total highlight count from the local database into [highlightCount].
     * Defaults to 0 on error.
     */
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
