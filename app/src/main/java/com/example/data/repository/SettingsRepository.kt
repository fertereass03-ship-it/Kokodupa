package com.example.data.repository

import android.content.Context
import com.example.data.db.AnimeDatabase
import com.example.data.db.PlayerSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SettingsRepository(context: Context) {
    private val database = AnimeDatabase.getDatabase(context)
    private val settingsDao = database.settingsDao()

    fun getPlayerSettings(): Flow<PlayerSettingsEntity?> = settingsDao.getPlayerSettings()

    suspend fun getPlayerSettingsDirect(): PlayerSettingsEntity = withContext(Dispatchers.IO) {
        settingsDao.getPlayerSettingsDirect() ?: PlayerSettingsEntity(
            id = 1,
            seekStepSeconds = 90,
            autoSkip = true,
            defaultSpeed = 1.0f,
            defaultQuality = "1080p",
            subtitlesEnabled = false
        )
    }

    suspend fun updateSettings(settings: PlayerSettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.savePlayerSettings(settings)
    }

    suspend fun updateSeekStep(seconds: Int) = withContext(Dispatchers.IO) {
        val current = getPlayerSettingsDirect()
        settingsDao.savePlayerSettings(current.copy(seekStepSeconds = seconds))
    }

    suspend fun updateAutoSkip(enabled: Boolean) = withContext(Dispatchers.IO) {
        val current = getPlayerSettingsDirect()
        settingsDao.savePlayerSettings(current.copy(autoSkip = enabled))
    }

    suspend fun updateDefaultSpeed(speed: Float) = withContext(Dispatchers.IO) {
        val current = getPlayerSettingsDirect()
        settingsDao.savePlayerSettings(current.copy(defaultSpeed = speed))
    }

    suspend fun updateDefaultQuality(quality: String) = withContext(Dispatchers.IO) {
        val current = getPlayerSettingsDirect()
        settingsDao.savePlayerSettings(current.copy(defaultQuality = quality))
    }

    suspend fun updateSubtitles(enabled: Boolean) = withContext(Dispatchers.IO) {
        val current = getPlayerSettingsDirect()
        settingsDao.savePlayerSettings(current.copy(subtitlesEnabled = enabled))
    }
}
