package com.antigravity.mqttdash.ui.screen.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antigravity.mqttdash.data.db.entity.WidgetEntity
import com.antigravity.mqttdash.data.db.entity.WidgetType
import com.antigravity.mqttdash.mqtt.ConnectionState
import com.antigravity.mqttdash.ui.screen.dashboard.sheet.AddWidgetSheet
import com.antigravity.mqttdash.ui.screen.dashboard.sheet.EditWidgetSheet
import com.antigravity.mqttdash.ui.widget.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    brokerId: Long,
    dashboardId: Long,
    onDisconnect: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val broker by viewModel.broker.collectAsState()
    val dashboards by viewModel.dashboards.collectAsState()
    val widgets by viewModel.widgets.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val currentDashboardId by viewModel.currentDashboard.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var editingWidget by remember { mutableStateOf<WidgetEntity?>(null) }
    var showNewDashboardDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Reorderable grid state
    val lazyGridState = rememberLazyGridState()
    val reorderableState = rememberReorderableLazyGridState(
        lazyGridState = lazyGridState,
        onMove = { from, to ->
            val mutable = widgets.toMutableList()
            mutable.add(to.index, mutable.removeAt(from.index))
            viewModel.reorderWidgets(mutable)
        }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DashboardDrawer(
                dashboards = dashboards,
                currentDashboardId = currentDashboardId,
                onSelectDashboard = { dashboardId ->
                    viewModel.switchDashboard(dashboardId)
                    scope.launch { drawerState.close() }
                },
                onAddDashboard = { showNewDashboardDialog = true },
                onDisconnect = {
                    viewModel.disconnect()
                    onDisconnect()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                DashboardAppBar(
                    brokerName = broker?.name ?: "Dashboard",
                    isConnected = connectionState is ConnectionState.Connected,
                    isEditMode = isEditMode,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onEditToggle = viewModel::toggleEditMode
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = !isEditMode,
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = { showAddSheet = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add widget")
                    }
                }
            }
        ) { padding ->
            if (widgets.isEmpty()) {
                EmptyDashboardHint(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = lazyGridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = widgets,
                        key = { it.id },
                        span = { widget ->
                            GridItemSpan(widget.colSpan.coerceIn(1, 4))
                        }
                    ) { widget ->
                        val itemHeight = if (widget.rowSpan >= 2) 200.dp else 110.dp
                        ReorderableItem(
                            state = reorderableState,
                            key = widget.id,
                            enabled = isEditMode
                        ) {
                            Box(modifier = Modifier.height(itemHeight)) {
                                WidgetRenderer(
                                    widget = widget,
                                    isEditMode = isEditMode,
                                    topicFlow = { topic, qos ->
                                        viewModel.topicFlow(topic, qos)
                                    },
                                    onPublish = { topic, payload ->
                                        viewModel.publish(topic, payload)
                                    },
                                    onEdit = { editingWidget = widget },
                                    onDelete = { viewModel.deleteWidget(widget) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet: add new widget
    if (showAddSheet) {
        AddWidgetSheet(
            dashboardId = currentDashboardId,
            onDismiss = { showAddSheet = false },
            onAdd = { newWidget ->
                viewModel.addWidget(newWidget)
                showAddSheet = false
            }
        )
    }

    // Bottom sheet: edit existing widget
    editingWidget?.let { widget ->
        EditWidgetSheet(
            widget = widget,
            onDismiss = { editingWidget = null },
            onSave = { updated ->
                viewModel.updateWidget(updated)
                editingWidget = null
            }
        )
    }

    // Dialog: new dashboard
    if (showNewDashboardDialog) {
        NewDashboardDialog(
            onDismiss = { showNewDashboardDialog = false },
            onCreate = { name ->
                viewModel.addDashboard(name)
                showNewDashboardDialog = false
            }
        )
    }
}

// ─── Connection indicator dot ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardAppBar(
    brokerName: String,
    isConnected: Boolean,
    isEditMode: Boolean,
    onMenuClick: () -> Unit,
    onEditToggle: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900), repeatMode = RepeatMode.Reverse
        ), label = "pulseAlpha"
    )

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConnected) Color(0xFF4CAF50).copy(alpha = alpha)
                            else MaterialTheme.colorScheme.error
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(brokerName, style = MaterialTheme.typography.titleMedium)
            }
        },
        actions = {
            TextButton(onClick = onEditToggle) {
                Text(if (isEditMode) "Done" else "Edit")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (isEditMode)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun EmptyDashboardHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Dashboard,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No widgets yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Tap + to add your first widget",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun NewDashboardDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Dashboard") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Dashboard name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name) },
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
