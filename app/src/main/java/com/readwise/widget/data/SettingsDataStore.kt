package com.readwise.widget.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Typed wrapper around Jetpack DataStore that exposes all user-configurable
 * settings as [Flow]s and provides suspend functions to update each value.
 *
 * The API token is stored separately in [EncryptedSharedPreferences] to protect
 * it from extraction on rooted devices or via backup.
 *
 * Every other setting is backed by a dedicated [Preferences] key defined in [Keys].
 * Callers observe settings reactively via the Flow properties and write changes
 * with the corresponding `set*` suspend function.
 *
 * @param dataStore The underlying [DataStore] instance provided by the application.
 * @param context Application context used to initialise encrypted storage.
 */
class SettingsDataStore(private val dataStore: DataStore<Preferences>, context: Context) {

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // ── Keys ────────────────────────────────────────────────────────────
    /** Typed preference keys for every stored setting. */
    private object Keys {
        val REFRESH_INTERVAL_MINUTES = intPreferencesKey("refresh_interval_minutes")
        val FILTER_BOOK_ID = longPreferencesKey("filter_book_id")
        val FILTER_TAG_NAME = stringPreferencesKey("filter_tag_name")
        val WIDGET_FONT_SIZE = floatPreferencesKey("widget_font_size")
        val WIDGET_FONT_FAMILY = stringPreferencesKey("widget_font_family")
        val WIDGET_BACKGROUND_COLOR = longPreferencesKey("widget_background_color")
        val WIDGET_TEXT_COLOR = longPreferencesKey("widget_text_color")
        val WIDGET_SOURCE_COLOR = longPreferencesKey("widget_source_color")
        val WIDGET_CORNER_RADIUS = floatPreferencesKey("widget_corner_radius")
        val WIDGET_BORDER_WIDTH = floatPreferencesKey("widget_border_width")
        val WIDGET_BORDER_COLOR = longPreferencesKey("widget_border_color")
        val WIDGET_PADDING = floatPreferencesKey("widget_padding")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        val MAX_HIGHLIGHT_LENGTH = intPreferencesKey("max_highlight_length")
    }

    // ── API Token (encrypted) ──────────────────────────────────────────

    private val _apiToken = MutableStateFlow(
        encryptedPrefs.getString("api_token", "") ?: ""
    )

    /** The Readwise API access token. Emits an empty string when unset. */
    val apiToken: Flow<String> = _apiToken

    /** Persists the Readwise API [token] in encrypted storage. */
    suspend fun setApiToken(token: String) {
        encryptedPrefs.edit().putString("api_token", token).apply()
        _apiToken.value = token
    }

    /**
     * Migrates the API token from plain-text DataStore to EncryptedSharedPreferences.
     * Called once on app startup; a no-op if the token was already migrated.
     */
    suspend fun migrateTokenIfNeeded() {
        dataStore.edit { prefs ->
            val oldToken = prefs[stringPreferencesKey("api_token")]
            if (!oldToken.isNullOrBlank()) {
                encryptedPrefs.edit().putString("api_token", oldToken).apply()
                _apiToken.value = oldToken
                prefs.remove(stringPreferencesKey("api_token"))
            }
        }
    }

    // ── Refresh Interval ────────────────────────────────────────────────

    /** How often (in minutes) the widget should automatically sync. Defaults to 30. */
    val refreshIntervalMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.REFRESH_INTERVAL_MINUTES] ?: 30
    }

    /** Persists the background sync interval in [minutes]. */
    suspend fun setRefreshIntervalMinutes(minutes: Int) {
        dataStore.edit { prefs -> prefs[Keys.REFRESH_INTERVAL_MINUTES] = minutes }
    }

    // ── Filter Book ID ──────────────────────────────────────────────────

    /**
     * Optional book ID used to restrict the widget to highlights from a single book.
     * Emits `null` when no filter is active.
     */
    val filterBookId: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[Keys.FILTER_BOOK_ID]
    }

    /**
     * Persists the book filter. Pass `null` to remove the filter and show highlights
     * from all books.
     */
    suspend fun setFilterBookId(bookId: Long?) {
        dataStore.edit { prefs ->
            if (bookId != null) {
                prefs[Keys.FILTER_BOOK_ID] = bookId
            } else {
                // Remove the key entirely so the flow emits null
                prefs.remove(Keys.FILTER_BOOK_ID)
            }
        }
    }

    // ── Filter Tag Name ─────────────────────────────────────────────────

    /**
     * Optional tag name used to restrict the widget to highlights with a specific tag.
     * Emits `null` when no filter is active.
     */
    val filterTagName: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.FILTER_TAG_NAME]
    }

    /**
     * Persists the tag filter. Pass `null` to remove the filter.
     */
    suspend fun setFilterTagName(tagName: String?) {
        dataStore.edit { prefs ->
            if (tagName != null) {
                prefs[Keys.FILTER_TAG_NAME] = tagName
            } else {
                // Remove the key entirely so the flow emits null
                prefs.remove(Keys.FILTER_TAG_NAME)
            }
        }
    }

    // ── Widget Font Size ────────────────────────────────────────────────

    /** Widget highlight text size in SP. Defaults to 16sp. */
    val widgetFontSize: Flow<Float> = dataStore.data.map { prefs ->
        prefs[Keys.WIDGET_FONT_SIZE] ?: 16f
    }

    /** Persists the widget font [size] in SP. */
    suspend fun setWidgetFontSize(size: Float) {
        dataStore.edit { prefs -> prefs[Keys.WIDGET_FONT_SIZE] = size }
    }

    // ── Widget Font Family ──────────────────────────────────────────────

    /** Font family identifier for the widget text. Defaults to "default" (system font). */
    val widgetFontFamily: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.WIDGET_FONT_FAMILY] ?: "default"
    }

    /** Persists the widget font [family] identifier. */
    suspend fun setWidgetFontFamily(family: String) {
        dataStore.edit { prefs -> prefs[Keys.WIDGET_FONT_FAMILY] = family }
    }

    // ── Widget Background Color ─────────────────────────────────────────

    /** Widget background color as a packed ARGB long. Defaults to opaque white. */
    val widgetBackgroundColor: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.WIDGET_BACKGROUND_COLOR] ?: 0xFFFFFFFFL
    }

    /** Persists the widget background [color] as a packed ARGB long. */
    suspend fun setWidgetBackgroundColor(color: Long) {
        dataStore.edit { prefs -> prefs[Keys.WIDGET_BACKGROUND_COLOR] = color }
    }

    // ── Widget Text Color ───────────────────────────────────────────────

    /** Widget highlight text color as a packed ARGB long. Defaults to near-black. */
    val widgetTextColor: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.WIDGET_TEXT_COLOR] ?: 0xFF1C1B1FL
    }

    /** Persists the widget text [color] as a packed ARGB long. */
    suspend fun setWidgetTextColor(color: Long) {
        dataStore.edit { prefs -> prefs[Keys.WIDGET_TEXT_COLOR] = color }
    }

    // ── Widget Source Color ─────────────────────────────────────────────

    /** Color used for the book/author attribution line. Defaults to medium grey. */
    val widgetSourceColor: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.WIDGET_SOURCE_COLOR] ?: 0xFF49454FL
    }

    /** Persists the widget source attribution [color] as a packed ARGB long. */
    suspend fun setWidgetSourceColor(color: Long) {
        dataStore.edit { prefs -> prefs[Keys.WIDGET_SOURCE_COLOR] = color }
    }

    // ── Widget Corner Radius ────────────────────────────────────────────

    /** Corner radius for the widget background in DP. Defaults to 16dp. */
    val widgetCornerRadius: Flow<Float> = dataStore.data.map { prefs ->
        prefs[Keys.WIDGET_CORNER_RADIUS] ?: 16f
    }

    /** Persists the widget corner [radius] in DP. */
    suspend fun setWidgetCornerRadius(radius: Float) {
        dataStore.edit { prefs -> prefs[Keys.WIDGET_CORNER_RADIUS] = radius }
    }

    // ── Widget Border Width ─────────────────────────────────────────────

    /** Border stroke width in DP. Defaults to 0 (no border). */
    val widgetBorderWidth: Flow<Float> = dataStore.data.map { prefs ->
        prefs[Keys.WIDGET_BORDER_WIDTH] ?: 0f
    }

    /** Persists the widget border stroke [width] in DP. */
    suspend fun setWidgetBorderWidth(width: Float) {
        dataStore.edit { prefs -> prefs[Keys.WIDGET_BORDER_WIDTH] = width }
    }

    // ── Widget Border Color ─────────────────────────────────────────────

    /** Widget border color as a packed ARGB long. Defaults to opaque black. */
    val widgetBorderColor: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.WIDGET_BORDER_COLOR] ?: 0xFF000000L
    }

    /** Persists the widget border [color] as a packed ARGB long. */
    suspend fun setWidgetBorderColor(color: Long) {
        dataStore.edit { prefs -> prefs[Keys.WIDGET_BORDER_COLOR] = color }
    }

    // ── Widget Padding ──────────────────────────────────────────────────

    /** Inner padding for the widget content area in DP. Defaults to 16dp. */
    val widgetPadding: Flow<Float> = dataStore.data.map { prefs ->
        prefs[Keys.WIDGET_PADDING] ?: 16f
    }

    /** Persists the widget inner [padding] in DP. */
    suspend fun setWidgetPadding(padding: Float) {
        dataStore.edit { prefs -> prefs[Keys.WIDGET_PADDING] = padding }
    }

    // ── Use Dynamic Colors ──────────────────────────────────────────────

    /**
     * Whether Material You dynamic colors should override the manual color settings.
     * Defaults to `true` on devices that support dynamic theming.
     */
    val useDynamicColors: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.USE_DYNAMIC_COLORS] ?: true
    }

    /** Persists the dynamic colors preference. */
    suspend fun setUseDynamicColors(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.USE_DYNAMIC_COLORS] = enabled }
    }

    // ── Max Highlight Length ────────────────────────────────────────────

    /**
     * Maximum character length for highlights shown in the widget. Highlights
     * longer than this value are excluded from random selection. Defaults to 300.
     */
    val maxHighlightLength: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.MAX_HIGHLIGHT_LENGTH] ?: 300
    }

    /** Persists the maximum highlight character [length]. */
    suspend fun setMaxHighlightLength(length: Int) {
        dataStore.edit { prefs -> prefs[Keys.MAX_HIGHLIGHT_LENGTH] = length }
    }
}
