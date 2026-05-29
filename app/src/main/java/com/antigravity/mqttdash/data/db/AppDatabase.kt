package com.antigravity.mqttdash.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.antigravity.mqttdash.data.db.dao.BrokerDao
import com.antigravity.mqttdash.data.db.dao.DashboardDao
import com.antigravity.mqttdash.data.db.dao.WidgetDao
import com.antigravity.mqttdash.data.db.entity.BrokerEntity
import com.antigravity.mqttdash.data.db.entity.DashboardEntity
import com.antigravity.mqttdash.data.db.entity.WidgetEntity

@Database(
    entities = [BrokerEntity::class, DashboardEntity::class, WidgetEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun brokerDao(): BrokerDao
    abstract fun dashboardDao(): DashboardDao
    abstract fun widgetDao(): WidgetDao
}
