package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.audio.AudioPlayerManager
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.WordRepository
import com.example.notification.NotificationHelper
import com.example.notification.SrsNotificationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class QuranApp : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this, applicationScope)
    }

    val wordRepository: WordRepository by lazy {
        WordRepository(database.wordDao())
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    val audioPlayerManager: AudioPlayerManager by lazy {
        AudioPlayerManager(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        try {
            SrsNotificationWorker.scheduleDailyReminder(this)
        } catch (e: Exception) {
            Log.w("QuranApp", "WorkManager initialization skipped in test environment", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        audioPlayerManager.release()
    }
}
