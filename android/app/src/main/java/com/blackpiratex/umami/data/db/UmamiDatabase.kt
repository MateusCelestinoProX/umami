package com.blackpiratex.umami.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.blackpiratex.umami.data.db.dao.MetricDao
import com.blackpiratex.umami.data.db.dao.PageviewDao
import com.blackpiratex.umami.data.db.dao.SessionDao
import com.blackpiratex.umami.data.db.dao.StatsDao
import com.blackpiratex.umami.data.db.dao.WebsiteDao
import com.blackpiratex.umami.data.db.entities.MetricEntity
import com.blackpiratex.umami.data.db.entities.PageviewPointEntity
import com.blackpiratex.umami.data.db.entities.SessionEntity
import com.blackpiratex.umami.data.db.entities.StatsEntity
import com.blackpiratex.umami.data.db.entities.WebsiteEntity

@Database(
    entities = [
        WebsiteEntity::class,
        StatsEntity::class,
        PageviewPointEntity::class,
        MetricEntity::class,
        SessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class UmamiDatabase : RoomDatabase() {
    abstract fun websiteDao(): WebsiteDao
    abstract fun statsDao(): StatsDao
    abstract fun pageviewDao(): PageviewDao
    abstract fun metricDao(): MetricDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: UmamiDatabase? = null

        fun getDatabase(context: Context): UmamiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UmamiDatabase::class.java,
                    "umami_analytics_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
