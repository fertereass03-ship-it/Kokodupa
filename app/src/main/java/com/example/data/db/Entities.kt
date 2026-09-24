package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FavoriteCategory(val displayName: String, val ukDisplayName: String = displayName) {
    WATCHING("Смотрю", "Дивлюся"),
    PLAN_TO_WATCH("Планирую", "У планах"),
    DROPPED("Брошено", "Кинуто"),
    COMPLETED("Просмотрено", "Переглянуто")
}

@Entity(
    tableName = "favorites",
    primaryKeys = ["userId", "id"]
)
data class FavoriteAnimeEntity(
    val userId: String,
    val id: Long,
    val name: String,
    val russianName: String,
    val posterUrl: String,
    val score: String,
    val kind: String,
    val episodesCount: Int,
    val year: String,
    val category: FavoriteCategory = FavoriteCategory.WATCHING,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "watch_history",
    primaryKeys = ["userId", "animeId"]
)
data class WatchHistoryEntity(
    val userId: String,
    val animeId: Long,
    val name: String,
    val russianName: String,
    val posterUrl: String,
    val episodeNumber: Int,
    val episodeTitle: String,
    val voiceName: String,
    val streamUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val quality: String,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
) {
    val progressPercent: Int
        get() = if (durationMs > 0) ((positionMs.toDouble() / durationMs) * 100).toInt().coerceIn(0, 100) else 0
}

@Entity(
    tableName = "friends",
    primaryKeys = ["ownerUserId", "userId"]
)
data class FriendEntity(
    val ownerUserId: String = "",
    val userId: String,
    val name: String,
    val avatarUrl: String,
    val status: String,
    val isPending: Boolean = false,
    val isIncoming: Boolean = false,
    val lastActive: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: String = "",
    val friendId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMine: Boolean,
    val status: String = "SENT" // SENDING, SENT, READ
)

@Entity(tableName = "player_settings")
data class PlayerSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val seekStepSeconds: Int = 90,
    val autoSkip: Boolean = true,
    val defaultSpeed: Float = 1.0f,
    val defaultQuality: String = "1080p",
    val subtitlesEnabled: Boolean = false,
    val hardwareAcceleration: Boolean = true
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val userId: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String,
    val authProvider: String, // "GOOGLE" or "EMAIL"
    val passwordHash: String? = null,
    val isCurrentActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
