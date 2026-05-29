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
import com.antigravity.mqttdash.data.db.entity.DashboardEntity
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
    val currentDashboardSize = dashboards.find { it.id == currentDashboardId }?.widgetSize ?: "MEDIUM"

    var showAddSheet by remember { mutableStateOf(false) }
    var editingWidget by remember { mutableStateOf<WidgetEntity?>(null) }
    var showNewDashboardDialog by remember { mutableStateOf(false) }
    var dashboardToRename by remember { mutableStateOf<DashboardEntity?>(null) }
    var dashboardToDelete by remember { mutableStateOf<DashboardEntity?>(null) }
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
                onRenameDashboard = { dashboardToRename = it },
                onDeleteDashboard = { dashboardToDelete = it },
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
                        val itemHeight = when (currentDashboardSize) {
                            "SMALL" -> if (widget.rowSpan >= 2) 150.dp else 85.dp
                            "LARGE" -> if (widget.rowSpan >= 2) 250.dp else 140.dp
                            else -> if (widget.rowSpan >= 2) 200.dp else 110.dp
                        }
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
            onCreate = { name, size ->
                viewModel.addDashboard(name, size)
                showNewDashboardDialog = false
            }
        )
    }

    // Dialog: rename dashboard
    dashboardToRename?.let { dashboard ->
        var name by remember { mutableStateOf(dashboard.name) }
        var widgetSize by remember { mutableStateOf(dashboard.widgetSize) }
        AlertDialog(
            onDismissRequest = { dashboardToRename = null },
            title = { Text("控制面板设置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("面板名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("全局 Widget 大小", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SMALL" to "小", "MEDIUM" to "中", "LARGE" to "大").forEach { (sz, label) ->
                            FilterChip(
                                selected = widgetSize == sz,
                                onClick = { widgetSize = sz },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.renameDashboard(dashboard, name, widgetSize)
                            dashboardToRename = null
                        }
                    },
                    enabled = name.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { dashboardToRename = null }) { Text("取消") }
            }
        )
    }

    // Dialog: delete dashboard
    dashboardToDelete?.let { dashboard ->
        AlertDialog(
            onDismissRequest = { dashboardToDelete = null },
            title = { Text("删除控制面板") },
            text = { Text("您确定要删除控制面板 '${dashboard.name}' 吗？这会同时删除它内部所有的控件。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDashboard(dashboard)
                        dashboardToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { dashboardToDelete = null }) { Text("取消") }
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
                Text(if (isEditMode) "完成" else "编辑")
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
            "暂无控件",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "点击右下角 + 按钮添加您的第一个控件",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewDashboardDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var widgetSize by remember { mutableStateOf("MEDIUM") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建控制面板") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("面板名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("全局 Widget 大小", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SMALL" to "小", "MEDIUM" to "中", "LARGE" to "大").forEach { (sz, label) ->
                        FilterChip(
                            selected = widgetSize == sz,
                            onClick = { widgetSize = sz },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name, widgetSize) },
                enabled = name.isNotBlank()
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
