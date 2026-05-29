package com.antigravity.mqttdash.mqtt

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAckReturnCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MqttConnectionManager"

/**
 * Manages a single HiveMQ MQTT 3 client connection and routes incoming
 * topic messages to per-topic [SharedFlow] streams for Compose UI consumption.
 *
 * One physical connection is kept for the active broker; all widget subscriptions
 * are multiplexed over this single connection.
 */
@Singleton
class MqttConnectionManager @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /** Per-topic message flows. Widgets collect from these. */
    private val topicFlows = ConcurrentHashMap<String, MutableSharedFlow<String>>()

    /** Pending publish calls buffered while disconnected */
    private data class PendingPublish(val topic: String, val payload: String, val qos: Int)
    private val publishQueue = ArrayDeque<PendingPublish>()

    private var client: Mqtt3AsyncClient? = null
    private var activeBrokerId: Long = -1L

    // ─── Connection ──────────────────────────────────────────────────────────

    fun connect(
        host: String,
        port: Int,
        clientId: String,
        username: String?,
        password: String?,
        useTls: Boolean,
        brokerId: Long
    ) {
        if (_connectionState.value is ConnectionState.Connecting ||
            _connectionState.value is ConnectionState.Connected
        ) {
            disconnect()
        }

        _connectionState.value = ConnectionState.Connecting(host)

        val builder = MqttClient.builder()
            .useMqttVersion3()
            .identifier(clientId)
            .serverHost(host)
            .serverPort(port)
            .automaticReconnectWithDefaultConfig()

        if (useTls) builder.sslWithDefaultConfig()

        val mqttClient = builder.buildAsync()

        val connectBuilder = mqttClient.connectWith()
            .keepAlive(60)
            .cleanSession(true)

        if (!username.isNullOrBlank()) {
            connectBuilder.simpleAuth()
                .username(username)
                .password(password?.toByteArray() ?: ByteArray(0))
                .applySimpleAuth()
        }

        connectBuilder.send()
            .whenComplete { connAck, throwable ->
                if (throwable != null || connAck.returnCode != Mqtt3ConnAckReturnCode.SUCCESS) {
                    val msg = throwable?.message ?: "Broker refused: ${connAck.returnCode}"
                    Log.e(TAG, "Connection failed: $msg")
                    _connectionState.value = ConnectionState.Error(msg)
                    return@whenComplete
                }

                Log.i(TAG, "Connected to $host:$port")
                client = mqttClient
                activeBrokerId = brokerId
                _connectionState.value = ConnectionState.Connected(host, brokerId)

                // Route all incoming messages through the topic flow map
                mqttClient.publishes(MqttGlobalPublishFilter.ALL) { publish ->
                    val topic = publish.topic.toString()
                    val payload = String(publish.payloadAsBytes)
                    scope.launch {
                        topicFlows.getOrPut(topic) {
                            MutableSharedFlow(replay = 1, extraBufferCapacity = 64)
                        }.emit(payload)
                    }
                }

                // Flush any buffered publish requests
                flushPublishQueue()
            }
    }

    fun disconnect() {
        client?.disconnect()
        client = null
        activeBrokerId = -1L
        topicFlows.clear()
        _connectionState.value = ConnectionState.Disconnected
    }

    // ─── Subscribe / Publish ─────────────────────────────────────────────────

    fun subscribe(topic: String, qos: Int = 1) {
        val mqttQos = MqttQos.fromCode(qos) ?: MqttQos.AT_LEAST_ONCE
        client?.subscribeWith()
            ?.topicFilter(topic)
            ?.qos(mqttQos)
            ?.send()
            ?.whenComplete { _, throwable ->
                if (throwable != null) Log.e(TAG, "Subscribe failed for $topic", throwable)
            }
    }

    fun unsubscribe(topic: String) {
        client?.unsubscribeWith()?.topicFilter(topic)?.send()
        topicFlows.remove(topic)
    }

    fun publish(topic: String, payload: String, qos: Int = 1, retained: Boolean = false) {
        val mqttClient = client
        if (mqttClient == null || _connectionState.value !is ConnectionState.Connected) {
            publishQueue.addLast(PendingPublish(topic, payload, qos))
            return
        }
        val mqttQos = MqttQos.fromCode(qos) ?: MqttQos.AT_LEAST_ONCE
        mqttClient.publishWith()
            .topic(topic)
            .payload(payload.toByteArray())
            .qos(mqttQos)
            .retain(retained)
            .send()
            .whenComplete { _, throwable ->
                if (throwable != null) Log.e(TAG, "Publish failed on $topic", throwable)
            }
    }

    /**
     * Returns a [SharedFlow] that emits payloads received on [topic].
     * Automatically subscribes if not already subscribed.
     */
    fun topicFlow(topic: String, qos: Int = 1): SharedFlow<String> {
        val flow = topicFlows.getOrPut(topic) {
            MutableSharedFlow(replay = 1, extraBufferCapacity = 64)
        }
        subscribe(topic, qos)
        return flow.asSharedFlow()
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private fun flushPublishQueue() {
        while (publishQueue.isNotEmpty()) {
            val pending = publishQueue.removeFirstOrNull() ?: break
            publish(pending.topic, pending.payload, pending.qos)
        }
    }
}
