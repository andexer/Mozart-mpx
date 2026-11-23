package com.mozartmpx.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    version = 2,
    exportSchema = false
)
abstract class MozartDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyAffinityDao(): HistoryAffinityDao
    abstract fun equalizerDao(): EqualizerDao

    companion object {
        @Volatile
        private var INSTANCE: MozartDatabase? = null

        fun getInstance(context: Context): MozartDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MozartDatabase::class.java,
                    "mozart_mpx_local.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
