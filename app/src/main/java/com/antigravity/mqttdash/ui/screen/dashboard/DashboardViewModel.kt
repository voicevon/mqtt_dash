package com.antigravity.mqttdash.ui.screen.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.mqttdash.data.db.entity.BrokerEntity
import com.antigravity.mqttdash.data.db.entity.DashboardEntity
import com.antigravity.mqttdash.data.db.entity.WidgetEntity
import com.antigravity.mqttdash.data.repository.BrokerRepository
import com.antigravity.mqttdash.data.repository.DashboardRepository
import com.antigravity.mqttdash.data.repository.WidgetRepository
import com.antigravity.mqttdash.mqtt.ConnectionState
import com.antigravity.mqttdash.mqtt.MqttConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mqttManager: MqttConnectionManager,
    private val brokerRepository: BrokerRepository,
    private val dashboardRepository: DashboardRepository,
    private val widgetRepository: WidgetRepository
) : ViewModel() {

    private val brokerId: Long = checkNotNull(savedStateHandle["brokerId"])
    private val _currentDashboardId = MutableStateFlow<Long>(checkNotNull(savedStateHandle["dashboardId"]))

    val connectionState: StateFlow<ConnectionState> = mqttManager.connectionState
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.Disconnected)

    val broker: StateFlow<BrokerEntity?> = flow {
        emit(brokerRepository.getBrokerById(brokerId))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val dashboards: StateFlow<List<DashboardEntity>> =
        dashboardRepository.getDashboardsForBroker(brokerId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentDashboard: StateFlow<Long> = _currentDashboardId.asStateFlow()

    val widgets: StateFlow<List<WidgetEntity>> = _currentDashboardId
        .flatMapLatest { dashboardId ->
            widgetRepository.getWidgetsForDashboard(dashboardId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    fun switchDashboard(dashboardId: Long) {
        _currentDashboardId.value = dashboardId
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun addWidget(widget: WidgetEntity) {
        viewModelScope.launch {
            widgetRepository.saveWidget(widget.copy(dashboardId = _currentDashboardId.value))
        }
    }

    fun updateWidget(widget: WidgetEntity) {
        viewModelScope.launch { widgetRepository.updateWidget(widget) }
    }

    fun deleteWidget(widget: WidgetEntity) {
        viewModelScope.launch { widgetRepository.deleteWidget(widget) }
    }

    fun reorderWidgets(reordered: List<WidgetEntity>) {
        viewModelScope.launch { widgetRepository.reorder(reordered) }
    }

    fun publish(topic: String, payload: String, qos: Int = 1) {
        mqttManager.publish(topic, payload, qos)
    }

    fun topicFlow(topic: String, qos: Int = 1) = mqttManager.topicFlow(topic, qos)

    fun disconnect() {
        mqttManager.disconnect()
    }

    fun addDashboard(name: String) {
        viewModelScope.launch {
            dashboardRepository.saveDashboard(
                DashboardEntity(
                    brokerId = brokerId,
                    name = name,
                    position = dashboards.value.size
                )
            )
        }
    }

    fun renameDashboard(dashboard: DashboardEntity, newName: String) {
        viewModelScope.launch {
            dashboardRepository.saveDashboard(dashboard.copy(name = newName))
        }
    }

    fun deleteDashboard(dashboard: DashboardEntity) {
        viewModelScope.launch {
            if (_currentDashboardId.value == dashboard.id) {
                val other = dashboards.value.firstOrNull { it.id != dashboard.id }
                if (other != null) {
                    _currentDashboardId.value = other.id
                }
            }
            dashboardRepository.deleteDashboard(dashboard)
        }
    }
}
