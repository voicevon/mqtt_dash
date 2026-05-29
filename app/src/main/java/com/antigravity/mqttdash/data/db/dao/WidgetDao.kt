package com.antigravity.mqttdash.data.db.dao

import androidx.room.*
import com.antigravity.mqttdash.data.db.entity.WidgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetDao {

    @Query("SELECT * FROM widgets WHERE dashboardId = :dashboardId ORDER BY sortOrder ASC")
    fun getWidgetsForDashboard(dashboardId: Long): Flow<List<WidgetEntity>>

    @Query("SELECT * FROM widgets WHERE id = :id")
    suspend fun getWidgetById(id: Long): WidgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidget(widget: WidgetEntity): Long

    @Update
    suspend fun updateWidget(widget: WidgetEntity)

    @Delete
    suspend fun deleteWidget(widget: WidgetEntity)

    /** Bulk update sort orders after drag-and-drop reorder */
    @Transaction
    suspend fun updateSortOrders(widgets: List<WidgetEntity>) {
        widgets.forEach { updateWidget(it) }
    }
}
