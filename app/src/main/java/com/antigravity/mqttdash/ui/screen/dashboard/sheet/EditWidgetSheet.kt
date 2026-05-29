package com.antigravity.mqttdash.ui.screen.dashboard.sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigravity.mqttdash.data.db.entity.WidgetEntity
import com.antigravity.mqttdash.data.db.entity.WidgetType
import org.json.JSONObject

/**
 * 编辑控件属性 Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWidgetSheet(
    widget: WidgetEntity,
    onDismiss: () -> Unit,
    onSave: (WidgetEntity) -> Unit
) {
    var title by remember { mutableStateOf(widget.title) }
    var subTopic by remember { mutableStateOf(widget.subTopic ?: "") }
    var pubTopic by remember { mutableStateOf(widget.pubTopic ?: "") }
    var colSpan by remember { mutableIntStateOf(widget.colSpan) }
    var rowSpan by remember { mutableIntStateOf(widget.rowSpan) }

    // Parse type-specific config
    val config = remember { runCatching { JSONObject(widget.configJson) }.getOrDefault(JSONObject()) }

    // TEXT fields
    var unit by remember { mutableStateOf(config.optString("unit", "")) }
    var jsonPath by remember { mutableStateOf(config.optString("jsonPath", "")) }

    // SWITCH fields
    var onPayload by remember { mutableStateOf(config.optString("onPayload", "1")) }
    var offPayload by remember { mutableStateOf(config.optString("offPayload", "0")) }
    var iconName by remember { mutableStateOf(config.optString("iconName", "lightbulb")) }

    // SLIDER fields
    var sliderMin by remember { mutableStateOf(config.optDouble("min", 0.0).toString()) }
    var sliderMax by remember { mutableStateOf(config.optDouble("max", 100.0).toString()) }
    var sliderStep by remember { mutableStateOf(config.optDouble("step", 1.0).toString()) }
    var sliderUnit by remember { mutableStateOf(config.optString("unit", "")) }

    // IMAGE fields
    var maxFps by remember { mutableStateOf(config.optInt("maxFps", 1).toString()) }

    // Size & Color & Font fields
    var fontSize by remember { mutableStateOf(config.optString("fontSize", "MEDIUM")) }
    var cardColor by remember { mutableStateOf(config.optString("cardColor", "")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "配置控件",
                style = MaterialTheme.typography.titleLarge
            )

            // ── 通用字段 ─────────────────────────────
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题 / 别名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = subTopic,
                onValueChange = { subTopic = it },
                label = { Text("订阅主题 (State Topic)") },
                placeholder = { Text("例如: home/sensor/temp") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (widget.type != WidgetType.TEXT && widget.type != WidgetType.IMAGE) {
                OutlinedTextField(
                    value = pubTopic,
                    onValueChange = { pubTopic = it },
                    label = { Text("发布主题 (Command Topic)") },
                    placeholder = { Text("例如: home/device/set") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Size picker
            SizePicker(
                colSpan = colSpan,
                rowSpan = rowSpan,
                onColSpanChange = { colSpan = it },
                onRowSpanChange = { rowSpan = it }
            )

            // Font & Color picker
            FontAndColorPicker(
                fontSize = fontSize,
                onFontSizeChange = { fontSize = it },
                cardColor = cardColor,
                onCardColorChange = { cardColor = it }
            )

            HorizontalDivider()

            // ── 类型特有字段 ──────────────────────
            when (widget.type) {
                WidgetType.TEXT -> {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("单位后缀 (例如：°C)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = jsonPath,
                        onValueChange = { jsonPath = it },
                        label = { Text("JSONPath (可选，例如：$.temp)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WidgetType.SWITCH -> {
                    OutlinedTextField(
                        value = onPayload,
                        onValueChange = { onPayload = it },
                        label = { Text("开启负载 (ON Payload)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = offPayload,
                        onValueChange = { offPayload = it },
                        label = { Text("关闭负载 (OFF Payload)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WidgetType.SLIDER -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sliderMin,
                            onValueChange = { sliderMin = it },
                            label = { Text("最小值") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sliderMax,
                            onValueChange = { sliderMax = it },
                            label = { Text("最大值") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sliderStep,
                            onValueChange = { sliderStep = it },
                            label = { Text("步长") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = sliderUnit,
                        onValueChange = { sliderUnit = it },
                        label = { Text("单位 (例如：%)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WidgetType.IMAGE -> {
                    OutlinedTextField(
                        value = maxFps,
                        onValueChange = { maxFps = it },
                        label = { Text("最高刷新率 (FPS 限制)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val newConfig = when (widget.type) {
                        WidgetType.TEXT   -> """{"unit":"$unit","jsonPath":"$jsonPath","fontSize":"$fontSize","cardColor":"$cardColor"}"""
                        WidgetType.SWITCH -> """{"onPayload":"$onPayload","offPayload":"$offPayload","iconName":"$iconName","fontSize":"$fontSize","cardColor":"$cardColor"}"""
                        WidgetType.SLIDER -> """{"min":${sliderMin.toDoubleOrNull()?:0.0},"max":${sliderMax.toDoubleOrNull()?:100.0},"step":${sliderStep.toDoubleOrNull()?:1.0},"unit":"$sliderUnit","fontSize":"$fontSize","cardColor":"$cardColor"}"""
                        WidgetType.IMAGE  -> """{"maxFps":${maxFps.toIntOrNull()?:1},"fontSize":"$fontSize","cardColor":"$cardColor"}"""
                    }
                    onSave(
                        widget.copy(
                            title = title.trim(),
                            subTopic = subTopic.trim().ifBlank { null },
                            pubTopic = pubTopic.trim().ifBlank { null },
                            colSpan = colSpan,
                            rowSpan = rowSpan,
                            configJson = newConfig
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("保存设置", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SizePicker(
    colSpan: Int, rowSpan: Int,
    onColSpanChange: (Int) -> Unit,
    onRowSpanChange: (Int) -> Unit
) {
    Column {
        Text("占用列数 (宽度)", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "窄卡 (1格)", 2 to "中卡 (2格)", 4 to "宽卡 (4格)").forEach { (span, label) ->
                FilterChip(
                    selected = colSpan == span,
                    onClick = { onColSpanChange(span) },
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("占用行数 (高度)", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "矮卡", 2 to "高卡").forEach { (span, label) ->
                FilterChip(
                    selected = rowSpan == span,
                    onClick = { onRowSpanChange(span) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontAndColorPicker(
    fontSize: String,
    onFontSizeChange: (String) -> Unit,
    cardColor: String,
    onCardColorChange: (String) -> Unit
) {
    Column {
        Spacer(Modifier.height(4.dp))
        Text("字号大小 (个体)", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("SMALL" to "小", "MEDIUM" to "中", "LARGE" to "大").forEach { (sz, label) ->
                FilterChip(
                    selected = fontSize == sz,
                    onClick = { onFontSizeChange(sz) },
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("卡片颜色 (个体)", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "" to "默认",
                "#E3F2FD" to "浅蓝",
                "#E8F5E9" to "浅绿",
                "#FFF3E0" to "浅橙",
                "#FCE4EC" to "浅粉",
                "#F3E5F5" to "浅紫"
            ).forEach { (colorHex, label) ->
                FilterChip(
                    selected = cardColor == colorHex,
                    onClick = { onCardColorChange(colorHex) },
                    label = { Text(label) }
                )
            }
        }
    }
}
