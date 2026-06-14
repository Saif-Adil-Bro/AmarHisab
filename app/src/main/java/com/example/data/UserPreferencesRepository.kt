package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

/**
 * Repository class that abstracts access to DataStore Preferences for user settings.
 */
class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val DEFAULT_PROFILE_ID = longPreferencesKey("default_profile_id")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    /**
     * Flow tracking user's default load Profile ID.
     * Returns null if no default is explicitly saved.
     */
    val defaultProfileIdFlow: Flow<Long?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val id = preferences[PreferencesKeys.DEFAULT_PROFILE_ID]
            if (id == null || id == -1L) null else id
        }

    /**
     * Flow tracking user's default selected currency. Default is "৳".
     */
    val defaultCurrencyFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.DEFAULT_CURRENCY] ?: "৳"
        }

    /**
     * Flow tracking theme selection: "System" (default), "Light", "Dark".
     */
    val themePreferenceFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.THEME_PREFERENCE] ?: "System"
        }

    /**
     * Flow tracking language selection: "Bangla" (default) or "English".
     */
    val appLanguageFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] ?: "Bangla"
        }

    /**
     * Saves selected default profile ID. Pass null to clear.
     */
    suspend fun saveDefaultProfileId(profileId: Long?) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_PROFILE_ID] = profileId ?: -1L
        }
    }

    /**
     * Saves preferred currency format symbol.
     */
    suspend fun saveDefaultCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_CURRENCY] = currency
        }
    }

    /**
     * Saves user theme preference.
     */
    suspend fun saveThemePreference(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_PREFERENCE] = theme
        }
    }

    /**
     * Saves preferred locale translation language.
     */
    suspend fun saveAppLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = language
        }
    }
}
