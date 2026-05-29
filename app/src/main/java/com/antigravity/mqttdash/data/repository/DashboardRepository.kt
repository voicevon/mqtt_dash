package com.antigravity.mqttdash.data.repository

import com.antigravity.mqttdash.data.db.dao.DashboardDao
import com.antigravity.mqttdash.data.db.entity.DashboardEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val dashboardDao: DashboardDao
) {
    fun getDashboardsForBroker(brokerId: Long): Flow<List<DashboardEntity>> =
        dashboardDao.getDashboardsForBroker(brokerId)

    suspend fun getDashboardById(id: Long): DashboardEntity? =
        dashboardDao.getDashboardById(id)

    suspend fun saveDashboard(dashboard: DashboardEntity): Long =
        dashboardDao.insertDashboard(dashboard)

    suspend fun deleteDashboard(dashboard: DashboardEntity) =
        dashboardDao.deleteDashboard(dashboard)

    /** Create a default dashboard for a newly connected broker if none exist. */
    suspend fun ensureDefaultDashboard(brokerId: Long): Long {
        return saveDashboard(
            DashboardEntity(brokerId = brokerId, name = "Dashboard", position = 0)
        )
    }
}
