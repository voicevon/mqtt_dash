package com.antigravity.mqttdash.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A named dashboard screen that belongs to a broker.
 * One broker can have multiple dashboards (e.g. "Living Room", "Kitchen").
 */
@Entity(
    tableName = "dashboards",
    foreignKeys = [
        ForeignKey(
            entity = BrokerEntity::class,
            parentColumns = ["id"],
            childColumns = ["brokerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("brokerId")]
)
data class DashboardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val brokerId: Long,
    val name: String,
    /** Display order among sibling dashboards */
    val position: Int = 0
)
