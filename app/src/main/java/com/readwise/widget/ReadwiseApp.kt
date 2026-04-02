package com.readwise.widget

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.readwise.widget.api.createReadwiseApi
import com.readwise.widget.data.AppDatabase
import com.readwise.widget.data.HighlightRepository
import com.readwise.widget.data.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// Extension property that creates a single DataStore instance scoped to the application
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Application subclass that acts as the manual dependency-injection root for the app.
 *
 * Lazily initialises the three application-wide singletons — [database],
 * [settingsDataStore], and [highlightRepository] — so that expensive resources
 * (e.g. database connections) are only created when first accessed.
 *
 * Components that need these dependencies cast [android.content.Context.getApplicationContext]
 * to [ReadwiseApp] and access the properties directly.
 */
class ReadwiseApp : Application() {

    /** Room database singleton for persisting highlights, books, and tags. */
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    /** DataStore wrapper that exposes all user settings as typed flows. */
    val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(dataStore, this)
    }

    override fun onCreate() {
        super.onCreate()
        // Migrate any plain-text API token to encrypted storage on first launch after update
        kotlinx.coroutines.MainScope().launch {
            settingsDataStore.migrateTokenIfNeeded()
        }
    }

    /**
     * Repository that coordinates API calls and local database operations.
     *
     * The [apiProvider] lambda reads the current API token synchronously (via
     * [runBlocking]) each time a sync is requested, so a freshly saved token
     * is always used without needing to recreate the repository.
     */
    val highlightRepository: HighlightRepository by lazy {
        HighlightRepository(
            db = database,
            apiProvider = {
                // Retrieve the stored token; return null if none is configured
                val token = runBlocking { settingsDataStore.apiToken.first() }
                if (token.isNotBlank()) createReadwiseApi(token) else null
            },
        )
    }
}
