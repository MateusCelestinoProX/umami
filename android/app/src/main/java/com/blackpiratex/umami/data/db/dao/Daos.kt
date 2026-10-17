package com.blackpiratex.umami.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.blackpiratex.umami.data.db.entities.MetricEntity
import com.blackpiratex.umami.data.db.entities.PageviewPointEntity
import com.blackpiratex.umami.data.db.entities.SessionEntity
import com.blackpiratex.umami.data.db.entities.StatsEntity
import com.blackpiratex.umami.data.db.entities.WebsiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WebsiteDao {
    @Query("SELECT * FROM websites")
    fun getAllWebsites(): Flow<List<WebsiteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebsites(websites: List<WebsiteEntity>)

    @Query("DELETE FROM websites")
    suspend fun clearAll()
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM stats WHERE websiteId = :websiteId AND timeRangeKey = :timeRangeKey AND filterKey = :filterKey LIMIT 1")
    fun getStats(websiteId: String, timeRangeKey: String, filterKey: String): Flow<StatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: StatsEntity)
}

@Dao
interface PageviewDao {
    @Query("SELECT * FROM pageview_points WHERE websiteId = :websiteId AND timeRangeKey = :timeRangeKey AND filterKey = :filterKey ORDER BY timestampX ASC")
    fun getPageviewPoints(websiteId: String, timeRangeKey: String, filterKey: String): Flow<List<PageviewPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageviewPoints(points: List<PageviewPointEntity>)

    @Query("DELETE FROM pageview_points WHERE websiteId = :websiteId AND timeRangeKey = :timeRangeKey AND filterKey = :filterKey")
    suspend fun deletePageviewPoints(websiteId: String, timeRangeKey: String, filterKey: String)
}

@Dao
interface MetricDao {
    @Query("SELECT * FROM metrics WHERE websiteId = :websiteId AND timeRangeKey = :timeRangeKey AND filterKey = :filterKey AND metricType = :metricType ORDER BY countY DESC")
    fun getMetrics(websiteId: String, timeRangeKey: String, filterKey: String, metricType: String): Flow<List<MetricEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetrics(metrics: List<MetricEntity>)

    @Query("DELETE FROM metrics WHERE websiteId = :websiteId AND timeRangeKey = :timeRangeKey AND filterKey = :filterKey AND metricType = :metricType")
    suspend fun deleteMetrics(websiteId: String, timeRangeKey: String, filterKey: String, metricType: String)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE websiteId = :websiteId AND timeRangeKey = :timeRangeKey AND filterKey = :filterKey ORDER BY lastAt DESC")
    fun getSessions(websiteId: String, timeRangeKey: String, filterKey: String): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Query("DELETE FROM sessions WHERE websiteId = :websiteId AND timeRangeKey = :timeRangeKey AND filterKey = :filterKey")
    suspend fun deleteSessions(websiteId: String, timeRangeKey: String, filterKey: String)
}
