package com.antigravity.mqttdash.data.db.dao

import androidx.room.*
import com.antigravity.mqttdash.data.db.entity.DashboardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {

    @Query("SELECT * FROM dashboards WHERE brokerId = :brokerId ORDER BY position ASC")
    fun getDashboardsForBroker(brokerId: Long): Flow<List<DashboardEntity>>

    @Query("SELECT * FROM dashboards WHERE id = :id")
    suspend fun getDashboardById(id: Long): DashboardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboard(dashboard: DashboardEntity): Long

    @Update
    suspend fun updateDashboard(dashboard: DashboardEntity)

    @Delete
    suspend fun deleteDashboard(dashboard: DashboardEntity)
}
