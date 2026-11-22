package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.repository.ExamRepository
import kotlinx.serialization.json.Json

class NismPrepApplication : Application() {
    val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(this)
    }

    val repository: ExamRepository by lazy {
        ExamRepository(
            context = this,
            dao = database.examDao(),
            prefs = preferencesManager,
            json = json
        )
    }
}
