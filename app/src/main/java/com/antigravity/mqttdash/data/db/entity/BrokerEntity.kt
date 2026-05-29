package com.antigravity.mqttdash.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a saved MQTT broker connection configuration.
 */
@Entity(tableName = "brokers")
data class BrokerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 1883,
    val username: String? = null,
    val password: String? = null,
    val useTls: Boolean = false,
    val clientId: String,
    val keepAliveSeconds: Int = 60,
    val cleanSession: Boolean = true,
    /** Unix timestamp (ms) of the last successful connection */
    val lastConnectedAt: Long = 0L
)
