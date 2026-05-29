package com.antigravity.mqttdash.mqtt

/**
 * Represents the current state of the MQTT connection.
 */
sealed class ConnectionState {
    /** No active connection; user has not attempted to connect */
    data object Disconnected : ConnectionState()

    /** Actively trying to establish or re-establish a connection */
    data class Connecting(val host: String) : ConnectionState()

    /** Successfully connected to the broker */
    data class Connected(val host: String, val brokerId: Long) : ConnectionState()

    /** Connection attempt failed or dropped */
    data class Error(val message: String) : ConnectionState()
}
