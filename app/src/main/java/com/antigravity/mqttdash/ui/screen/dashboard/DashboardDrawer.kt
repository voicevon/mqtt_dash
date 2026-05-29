package com.antigravity.mqttdash.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigravity.mqttdash.data.db.entity.DashboardEntity

@Composable
fun DashboardDrawer(
    dashboards: List<DashboardEntity>,
    currentDashboardId: Long,
    onSelectDashboard: (Long) -> Unit,
    onAddDashboard: () -> Unit,
    onRenameDashboard: (DashboardEntity) -> Unit,
    onDeleteDashboard: (DashboardEntity) -> Unit,
    onDisconnect: () -> Unit
) {
    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            "控制面板列表",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(dashboards) { dashboard ->
                NavigationDrawerItem(
                    label = { Text(dashboard.name) },
                    selected = dashboard.id == currentDashboardId,
                    onClick = { onSelectDashboard(dashboard.id) },
                    icon = {
                        Icon(Icons.Filled.Dashboard, contentDescription = null)
                    },
                    badge = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onRenameDashboard(dashboard) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Rename Dashboard",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (dashboards.size > 1) {
                                IconButton(
                                    onClick = { onDeleteDashboard(dashboard) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete Dashboard",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            item {
                NavigationDrawerItem(
                    label = { Text("新建控制面板") },
                    selected = false,
                    onClick = onAddDashboard,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            label = { Text("断开连接", color = MaterialTheme.colorScheme.error) },
            selected = false,
            onClick = onDisconnect,
            icon = {
                Icon(
                    Icons.Filled.WifiOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(24.dp))
    }
}
