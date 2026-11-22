package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nism_prep_preferences")

class PreferencesManager(private val context: Context) {
    private val keyDarkMode = booleanPreferencesKey("dark_mode")
    private val keyBankVersion = intPreferencesKey("builtin_bank_version")

    val isDarkModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[keyDarkMode] ?: false
    }

    suspend fun setDarkMode(darkMode: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[keyDarkMode] = darkMode
        }
    }

    suspend fun getBankVersion(): Int {
        return context.dataStore.data.first()[keyBankVersion] ?: 0
    }

    suspend fun setBankVersion(version: Int) {
        context.dataStore.edit { prefs ->
            prefs[keyBankVersion] = version
        }
    }
}
