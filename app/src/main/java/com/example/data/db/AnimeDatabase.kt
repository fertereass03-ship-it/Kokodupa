package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FavoriteAnimeEntity::class,
        WatchHistoryEntity::class,
        FriendEntity::class,
        ChatMessageEntity::class,
        PlayerSettingsEntity::class,
        UserAccountEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AnimeDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun socialDao(): SocialDao
    abstract fun settingsDao(): SettingsDao
    abstract fun userAccountDao(): UserAccountDao

    companion object {
        @Volatile
        private var INSTANCE: AnimeDatabase? = null

        fun getDatabase(context: Context): AnimeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnimeDatabase::class.java,
                    "aniwerti_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate ONLY default player settings (no fake accounts, no fake friends)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.settingsDao()?.savePlayerSettings(
                                    PlayerSettingsEntity(
                                        id = 1,
                                        seekStepSeconds = 90,
                                        autoSkip = true,
                                        defaultSpeed = 1.0f,
                                        defaultQuality = "1080p",
                                        subtitlesEnabled = false
                                    )
                                )
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
