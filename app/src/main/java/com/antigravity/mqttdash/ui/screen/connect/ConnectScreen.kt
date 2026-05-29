package com.antigravity.mqttdash.ui.screen.connect

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antigravity.mqttdash.data.db.entity.BrokerEntity

@Composable
fun ConnectScreen(
    onConnected: (brokerId: Long, dashboardId: Long) -> Unit,
    viewModel: ConnectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentBrokers by viewModel.recentBrokers.collectAsState()
    var brokerToRename by remember { mutableStateOf<BrokerEntity?>(null) }
    var brokerToDelete by remember { mutableStateOf<BrokerEntity?>(null) }
    val focusManager = LocalFocusManager.current

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 48.dp)
            ) {
                // ── Logo & Headline ─────────────────────────────────────────
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SignalWifi4Bar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "MQTT Dash 仪表盘",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "连接到您的 MQTT 物联网服务器",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Quick Connect Card ──────────────────────────────────────
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                "快速连接",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = uiState.name,
                                onValueChange = viewModel::onNameChange,
                                label = { Text("连接别名 / 名称 (可选)") },
                                placeholder = { Text("例如: 我的智能家居服务器") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                )
                            )
                            Spacer(Modifier.height(8.dp))

                            // Host : Port row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = uiState.host,
                                    onValueChange = viewModel::onHostChange,
                                    label = { Text("服务器地址 (Host)") },
                                    placeholder = { Text("例如: broker.emqx.io") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                    )
                                )
                                OutlinedTextField(
                                    value = uiState.port,
                                    onValueChange = viewModel::onPortChange,
                                    label = { Text("端口") },
                                    singleLine = true,
                                    modifier = Modifier.width(90.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    )
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // Advanced options toggle
                            TextButton(
                                onClick = viewModel::toggleAdvanced,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(
                                    if (uiState.showAdvanced) Icons.Outlined.ExpandLess
                                    else Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("高级连接选项")
                            }

                            // Animated advanced section
                            AnimatedVisibility(
                                visible = uiState.showAdvanced,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = uiState.username,
                                        onValueChange = viewModel::onUsernameChange,
                                        label = { Text("用户名 (可选)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        leadingIcon = { Icon(Icons.Filled.Lock, null, modifier = Modifier.size(18.dp)) }
                                    )
                                    OutlinedTextField(
                                        value = uiState.password,
                                        onValueChange = viewModel::onPasswordChange,
                                        label = { Text("密码 (可选)") },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = uiState.clientId,
                                        onValueChange = viewModel::onClientIdChange,
                                        label = { Text("客户端 ID (Client ID)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "启用安全连接 (TLS / SSL)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = uiState.useTls,
                                            onCheckedChange = viewModel::onTlsChange
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Connect button
                            Button(
                                onClick = { viewModel.connect(onConnected) },
                                enabled = !uiState.isConnecting && uiState.host.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (uiState.isConnecting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("正在连接...")
                                } else {
                                    Icon(Icons.Filled.Wifi, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("连接", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }

                // ── Recent Brokers ──────────────────────────────────────────
                if (recentBrokers.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "最近的连接",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(recentBrokers) { broker ->
                        BrokerHistoryItem(
                            broker = broker,
                            onClick = { viewModel.loadBroker(broker) },
                            onRename = { brokerToRename = broker },
                            onDelete = { brokerToDelete = broker }
                        )
                    }
                }
            }

            // Dialog: rename connection
            brokerToRename?.let { broker ->
                var name by remember { mutableStateOf(broker.name) }
                AlertDialog(
                    onDismissRequest = { brokerToRename = null },
                    title = { Text("重命名连接配置") },
                    text = {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("连接别名 / 名称") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.updateBrokerName(broker, name)
                                brokerToRename = null
                            }
                        ) { Text("保存") }
                    },
                    dismissButton = {
                        TextButton(onClick = { brokerToRename = null }) { Text("取消") }
                    }
                )
            }

            // Dialog: delete connection
            brokerToDelete?.let { broker ->
                AlertDialog(
                    onDismissRequest = { brokerToDelete = null },
                    title = { Text("删除保存的连接") },
                    text = { Text("您确定要删除与 '${broker.name}' 的保存连接配置吗？") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteBroker(broker)
                                brokerToDelete = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("删除") }
                    },
                    dismissButton = {
                        TextButton(onClick = { brokerToDelete = null }) { Text("取消") }
                    }
                )
            }
        }
    }
}

@Composable
private fun BrokerHistoryItem(
    broker: BrokerEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(broker.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${broker.host}:${broker.port}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onRename,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Rename Connection",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete Connection",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
