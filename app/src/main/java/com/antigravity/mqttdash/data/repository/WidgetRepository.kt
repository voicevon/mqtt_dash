package com.antigravity.mqttdash.data.repository

import com.antigravity.mqttdash.data.db.dao.WidgetDao
import com.antigravity.mqttdash.data.db.entity.WidgetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRepository @Inject constructor(
    private val widgetDao: WidgetDao
) {
    fun getWidgetsForDashboard(dashboardId: Long): Flow<List<WidgetEntity>> =
        widgetDao.getWidgetsForDashboard(dashboardId)

    suspend fun getWidgetById(id: Long): WidgetEntity? = widgetDao.getWidgetById(id)

    suspend fun saveWidget(widget: WidgetEntity): Long = widgetDao.insertWidget(widget)

    suspend fun updateWidget(widget: WidgetEntity) = widgetDao.updateWidget(widget)

    suspend fun deleteWidget(widget: WidgetEntity) = widgetDao.deleteWidget(widget)

    /**
     * Persist drag-and-drop reorder result.
     * Reassigns sortOrder based on the list position.
     */
    suspend fun reorder(widgets: List<WidgetEntity>) {
        val reordered = widgets.mapIndexed { index, widget ->
            widget.copy(sortOrder = index)
        }
        widgetDao.updateSortOrders(reordered)
    }
}
