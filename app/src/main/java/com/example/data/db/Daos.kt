package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllFavorites(userId: String): Flow<List<FavoriteAnimeEntity>>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND category = :category ORDER BY updatedAt DESC")
    fun getFavoritesByCategory(userId: String, category: FavoriteCategory): Flow<List<FavoriteAnimeEntity>>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND id = :id LIMIT 1")
    fun getFavoriteById(userId: String, id: Long): Flow<FavoriteAnimeEntity?>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getFavoriteDirect(userId: String, id: Long): FavoriteAnimeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteAnimeEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND id = :id")
    suspend fun deleteFavoriteById(userId: String, id: Long)

    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun clearFavoritesForUser(userId: String)

    @Query("SELECT * FROM favorites ORDER BY updatedAt DESC")
    fun getAllFavoritesAnyUser(): Flow<List<FavoriteAnimeEntity>>

    @Query("UPDATE favorites SET userId = :newUserId WHERE userId = 'guest_user' OR userId = 'guest_default' OR userId = 'local_user_default'")
    suspend fun migrateGuestFavorites(newUserId: String)
}

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history WHERE userId = :userId ORDER BY lastWatchedTimestamp DESC")
    fun getAllHistory(userId: String): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedTimestamp DESC")
    fun getAllHistoryAnyUser(): Flow<List<WatchHistoryEntity>>

    @Query("UPDATE watch_history SET userId = :newUserId WHERE userId = 'guest_user' OR userId = 'guest_default' OR userId = 'local_user_default'")
    suspend fun migrateGuestHistory(newUserId: String)

    @Query("SELECT * FROM watch_history WHERE userId = :userId AND animeId = :animeId LIMIT 1")
    fun getHistoryForAnime(userId: String, animeId: Long): Flow<WatchHistoryEntity?>

    @Query("SELECT * FROM watch_history WHERE userId = :userId AND animeId = :animeId LIMIT 1")
    suspend fun getHistoryDirect(userId: String, animeId: Long): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHistory(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE userId = :userId AND animeId = :animeId")
    suspend fun deleteHistoryForAnime(userId: String, animeId: Long)

    @Query("DELETE FROM watch_history WHERE userId = :userId")
    suspend fun clearHistory(userId: String)
}

@Dao
interface SocialDao {
    @Query("SELECT * FROM friends WHERE ownerUserId = :ownerUserId AND isPending = 0 ORDER BY lastActive DESC")
    fun getFriends(ownerUserId: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE ownerUserId = :ownerUserId AND isPending = 0")
    suspend fun getFriendsDirect(ownerUserId: String): List<FriendEntity>

    @Query("SELECT * FROM friends WHERE ownerUserId = :ownerUserId")
    suspend fun getAllFriendsDirect(ownerUserId: String): List<FriendEntity>

    @Query("SELECT * FROM friends WHERE ownerUserId = :ownerUserId AND isPending = 1 ORDER BY lastActive DESC")
    fun getPendingRequests(ownerUserId: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE ownerUserId = :ownerUserId AND isPending = 1 AND isIncoming = 1 ORDER BY lastActive DESC")
    fun getIncomingRequests(ownerUserId: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE ownerUserId = :ownerUserId AND isPending = 1 AND isIncoming = 0 ORDER BY lastActive DESC")
    fun getOutgoingRequests(ownerUserId: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE ownerUserId = :ownerUserId AND userId = :userId LIMIT 1")
    suspend fun getFriendById(ownerUserId: String, userId: String): FriendEntity?

    @Query("SELECT * FROM friends WHERE ownerUserId = :ownerUserId AND userId = :userId LIMIT 1")
    fun getFriendFlow(ownerUserId: String, userId: String): Flow<FriendEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Update
    suspend fun updateFriend(friend: FriendEntity)

    @Query("DELETE FROM friends WHERE ownerUserId = :ownerUserId AND userId = :userId")
    suspend fun deleteFriend(ownerUserId: String, userId: String)

    @Query("SELECT * FROM chat_messages WHERE ownerUserId = :ownerUserId AND friendId = :friendId ORDER BY timestamp ASC")
    fun getMessagesForFriend(ownerUserId: String, friendId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE ownerUserId = :ownerUserId AND friendId = :friendId AND timestamp = :timestamp AND text = :text")
    suspend fun hasMessage(ownerUserId: String, friendId: String, timestamp: Long, text: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM player_settings WHERE id = 1 LIMIT 1")
    fun getPlayerSettings(): Flow<PlayerSettingsEntity?>

    @Query("SELECT * FROM player_settings WHERE id = 1 LIMIT 1")
    suspend fun getPlayerSettingsDirect(): PlayerSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlayerSettings(settings: PlayerSettingsEntity)
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE isCurrentActive = 1 LIMIT 1")
    fun getActiveUser(): Flow<UserAccountEntity?>

    @Query("SELECT * FROM user_accounts WHERE isCurrentActive = 1 LIMIT 1")
    suspend fun getActiveUserDirect(): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<UserAccountEntity>>

    @Query("SELECT * FROM user_accounts")
    suspend fun getAllAccountsDirect(): List<UserAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserAccountEntity)

    @Query("UPDATE user_accounts SET isCurrentActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE user_accounts SET isCurrentActive = 1 WHERE userId = :userId")
    suspend fun activateUser(userId: String)

    @Query("DELETE FROM user_accounts WHERE userId = :userId")
    suspend fun deleteUser(userId: String)

    @Query("DELETE FROM user_accounts WHERE userId != :keepUserId")
    suspend fun deleteOtherAccounts(keepUserId: String)

    @Query("DELETE FROM user_accounts")
    suspend fun deleteAllAccounts()
}
