package com.antigravity.mqttdash.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for MQTT layer.
 * MqttConnectionManager is auto-provided via @Inject constructor;
 * this module is a placeholder for future MQTT-layer bindings (e.g. MqttService).
 */
@Module
@InstallIn(SingletonComponent::class)
object MqttModule

