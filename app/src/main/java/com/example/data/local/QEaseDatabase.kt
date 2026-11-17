package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SavedCentreEntity::class,
        UserTokenEntity::class,
        OfflineCrowdReportEntity::class,
        CentreReviewEntity::class,
        LiveQueueUpdateEntity::class,
        UserAccountEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class QEaseDatabase : RoomDatabase() {

    abstract fun qeaseDao(): QEaseDao

    companion object {
        @Volatile
        private var INSTANCE: QEaseDatabase? = null

        fun getDatabase(context: Context): QEaseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QEaseDatabase::class.java,
                    "qease_platform_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
