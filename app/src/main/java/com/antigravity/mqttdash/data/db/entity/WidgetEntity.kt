package com.antigravity.mqttdash.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Widget types supported in MVP.
 */
enum class WidgetType {
    TEXT,
    SWITCH,
    SLIDER,
    IMAGE
}

/**
 * A single dashboard widget card.
 *
 * The [configJson] field stores widget-type-specific settings as a JSON string.
 * This allows adding new widget types without altering the table schema.
 *
 * Text:   { "unit": "°C", "jsonPath": "$.temp", "color": "#FFFFFF" }
 * Switch: { "onPayload": "1", "offPayload": "0", "iconName": "lightbulb" }
 * Slider: { "min": 0.0, "max": 100.0, "step": 1.0, "unit": "%" }
 * Image:  { "maxFps": 1 }
 */
@Entity(
    tableName = "widgets",
    foreignKeys = [
        ForeignKey(
            entity = DashboardEntity::class,
            parentColumns = ["id"],
            childColumns = ["dashboardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dashboardId")]
)
data class WidgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dashboardId: Long,
    val type: WidgetType,
    val title: String,
    /** Topic to subscribe for receiving state updates */
    val subTopic: String? = null,
    /** Topic to publish commands to */
    val pubTopic: String? = null,
    val qos: Int = 1,
    /** Grid column span: 1 or 2 (out of a 4-column grid) */
    val colSpan: Int = 1,
    /** Grid row span: 1 or 2 */
    val rowSpan: Int = 1,
    /** Sort order within the dashboard */
    val sortOrder: Int = 0,
    /** Widget-type-specific configuration as JSON */
    val configJson: String = "{}"
)
