package com.antigravity.mqttdash.ui.screen.dashboard.sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.antigravity.mqttdash.data.db.entity.WidgetEntity
import com.antigravity.mqttdash.data.db.entity.WidgetType

data class WidgetTypeInfo(
    val type: WidgetType,
    val label: String,
    val description: String,
    val icon: ImageVector
)

private val widgetTypes = listOf(
    WidgetTypeInfo(WidgetType.TEXT, "文本数值", "展示传感器读数与文本", Icons.Filled.Numbers),
    WidgetTypeInfo(WidgetType.SWITCH, "开关", "双态控制开关", Icons.Filled.ToggleOn),
    WidgetTypeInfo(WidgetType.SLIDER, "滑动条", "连续范围数值控制 (0-100)", Icons.Filled.LinearScale),
    WidgetTypeInfo(WidgetType.IMAGE, "图像", "摄像头视频或图像流", Icons.Filled.Image),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWidgetSheet(
    dashboardId: Long,
    onDismiss: () -> Unit,
    onAdd: (WidgetEntity) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "添加控件",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            widgetTypes.forEach { info ->
                WidgetTypeCard(
                    info = info,
                    onClick = {
                        // Create widget with sensible defaults
                        val widget = WidgetEntity(
                            dashboardId = dashboardId,
                            type = info.type,
                            title = info.label,
                            colSpan = 2,
                            rowSpan = if (info.type == WidgetType.IMAGE) 2 else 1,
                            configJson = defaultConfigFor(info.type)
                        )
                        onAdd(widget)
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WidgetTypeCard(info: WidgetTypeInfo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = info.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(info.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    info.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

private fun defaultConfigFor(type: WidgetType): String = when (type) {
    WidgetType.TEXT   -> """{"unit":"","jsonPath":"","fontSize":"MEDIUM","cardColor":""}"""
    WidgetType.SWITCH -> """{"onPayload":"1","offPayload":"0","iconName":"lightbulb","fontSize":"MEDIUM","cardColor":""}"""
    WidgetType.SLIDER -> """{"min":0.0,"max":100.0,"step":1.0,"unit":"","fontSize":"MEDIUM","cardColor":""}"""
    WidgetType.IMAGE  -> """{"maxFps":1,"fontSize":"MEDIUM","cardColor":""}"""
}
