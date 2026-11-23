package com.mozartmpx.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TrackEntity::class,
        HiddenTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlayHistoryEntity::class,
        TasteAffinityEntity::class,
        CustomEqPresetEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class MozartDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyAffinityDao(): HistoryAffinityDao
    abstract fun equalizerDao(): EqualizerDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_history_trackId` ON `play_history` (`trackId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_history_playedAt` ON `play_history` (`playedAt`)")
                val cutoff = System.currentTimeMillis() - 90L * 24L * 60L * 60L * 1000L
                db.execSQL("DELETE FROM `play_history` WHERE `playedAt` < $cutoff")
            }
        }

        @Volatile
        private var INSTANCE: MozartDatabase? = null

        fun getInstance(context: Context): MozartDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MozartDatabase::class.java,
                    "mozart_mpx_local.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
