package com.antigravity.mqttdash.ui.screen.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.mqttdash.data.db.entity.BrokerEntity
import com.antigravity.mqttdash.data.repository.BrokerRepository
import com.antigravity.mqttdash.data.repository.DashboardRepository
import com.antigravity.mqttdash.mqtt.ConnectionState
import com.antigravity.mqttdash.mqtt.MqttConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ConnectUiState(
    val name: String = "",
    val host: String = "",
    val port: String = "1883",
    val username: String = "",
    val password: String = "",
    val useTls: Boolean = false,
    val clientId: String = "mqttdash-${UUID.randomUUID().toString().take(8)}",
    val showAdvanced: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val mqttManager: MqttConnectionManager,
    private val brokerRepository: BrokerRepository,
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = mqttManager.connectionState
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.Disconnected)

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    val recentBrokers = brokerRepository.allBrokers
        .map { list ->
            list.distinctBy {
                "${it.host}:${it.port}:${it.username ?: ""}:${it.password ?: ""}:${it.useTls}:${it.clientId}"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onNameChange(value: String) = _uiState.value.let { _uiState.value = it.copy(name = value) }
    fun onHostChange(value: String) = _uiState.value.let { _uiState.value = it.copy(host = value) }
    fun onPortChange(value: String) = _uiState.value.let { _uiState.value = it.copy(port = value) }
    fun onUsernameChange(v: String) = _uiState.value.let { _uiState.value = it.copy(username = v) }
    fun onPasswordChange(v: String) = _uiState.value.let { _uiState.value = it.copy(password = v) }
    fun onTlsChange(v: Boolean) = _uiState.value.let { _uiState.value = it.copy(useTls = v) }
    fun onClientIdChange(v: String) = _uiState.value.let { _uiState.value = it.copy(clientId = v) }
    fun toggleAdvanced() = _uiState.value.let { _uiState.value = it.copy(showAdvanced = !it.showAdvanced) }

    /** Load a saved broker into the form fields */
    fun loadBroker(broker: BrokerEntity) {
        _uiState.value = _uiState.value.copy(
            name = if (broker.name != broker.host) broker.name else "",
            host = broker.host,
            port = broker.port.toString(),
            username = broker.username ?: "",
            password = broker.password ?: "",
            useTls = broker.useTls,
            clientId = broker.clientId
        )
    }

    /**
     * Initiate MQTT connection.
     * On success, saves the broker to history and navigates to dashboard.
     */
    fun connect(onSuccess: (brokerId: Long, dashboardId: Long) -> Unit) {
        val state = _uiState.value
        val port = state.port.toIntOrNull() ?: 1883
        if (state.host.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter a broker address")
            return
        }

        _uiState.value = state.copy(isConnecting = true, errorMessage = null)

        mqttManager.connect(
            host = state.host.trim(),
            port = port,
            clientId = state.clientId,
            username = state.username.ifBlank { null },
            password = state.password.ifBlank { null },
            useTls = state.useTls,
            brokerId = -1L // will be set after save
        )

        viewModelScope.launch {
            mqttManager.connectionState.collect { connState ->
                when (connState) {
                    is ConnectionState.Connected -> {
                        // Check if broker already exists to de-duplicate
                        val existing = brokerRepository.findMatchingBroker(
                            host = state.host.trim(),
                            port = port,
                            username = state.username.ifBlank { null },
                            password = state.password.ifBlank { null },
                            useTls = state.useTls,
                            clientId = state.clientId
                        )
                        val brokerId = if (existing != null) {
                            val updated = existing.copy(
                                name = state.name.ifBlank { state.host.trim() }
                            )
                            brokerRepository.saveBroker(updated)
                            existing.id
                        } else {
                            val broker = BrokerEntity(
                                name = state.name.ifBlank { state.host.trim() },
                                host = state.host.trim(),
                                port = port,
                                username = state.username.ifBlank { null },
                                password = state.password.ifBlank { null },
                                useTls = state.useTls,
                                clientId = state.clientId
                            )
                            brokerRepository.saveBroker(broker)
                        }
                        brokerRepository.markConnected(brokerId)

                        // Ensure at least one dashboard exists
                        val dashboards = dashboardRepository.getDashboardsForBroker(brokerId)
                        // Collect first emission to get existing dashboards
                        var dashboardId: Long = -1L
                        dashboards.collect { list ->
                            dashboardId = if (list.isEmpty()) {
                                dashboardRepository.ensureDefaultDashboard(brokerId)
                            } else {
                                list.first().id
                            }
                            _uiState.value = _uiState.value.copy(isConnecting = false)
                            onSuccess(brokerId, dashboardId)
                            return@collect
                        }
                    }
                    is ConnectionState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isConnecting = false,
                            errorMessage = connState.message
                        )
                    }
                    else -> { /* waiting */ }
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun deleteBroker(broker: BrokerEntity) {
        viewModelScope.launch {
            brokerRepository.deleteBroker(broker)
        }
    }

    fun updateBrokerName(broker: BrokerEntity, newName: String) {
        viewModelScope.launch {
            val updated = broker.copy(name = newName.ifBlank { broker.host })
            brokerRepository.saveBroker(updated)
        }
    }
}
