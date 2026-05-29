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
 * Edit sheet shown when user taps a widget in edit mode.
 * Renders type-specific configuration fields.
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
                "Configure Widget",
                style = MaterialTheme.typography.titleLarge
            )

            // ── Common fields ─────────────────────────────
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = subTopic,
                onValueChange = { subTopic = it },
                label = { Text("Subscribe topic") },
                placeholder = { Text("home/sensor/temp") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (widget.type != WidgetType.TEXT && widget.type != WidgetType.IMAGE) {
                OutlinedTextField(
                    value = pubTopic,
                    onValueChange = { pubTopic = it },
                    label = { Text("Publish topic") },
                    placeholder = { Text("home/device/set") },
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

            HorizontalDivider()

            // ── Type-specific fields ──────────────────────
            when (widget.type) {
                WidgetType.TEXT -> {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit suffix (e.g. °C)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = jsonPath,
                        onValueChange = { jsonPath = it },
                        label = { Text("JSONPath (optional, e.g. $.temp)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WidgetType.SWITCH -> {
                    OutlinedTextField(
                        value = onPayload,
                        onValueChange = { onPayload = it },
                        label = { Text("ON payload") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = offPayload,
                        onValueChange = { offPayload = it },
                        label = { Text("OFF payload") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WidgetType.SLIDER -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sliderMin,
                            onValueChange = { sliderMin = it },
                            label = { Text("Min") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sliderMax,
                            onValueChange = { sliderMax = it },
                            label = { Text("Max") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sliderStep,
                            onValueChange = { sliderStep = it },
                            label = { Text("Step") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = sliderUnit,
                        onValueChange = { sliderUnit = it },
                        label = { Text("Unit (e.g. %)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WidgetType.IMAGE -> {
                    OutlinedTextField(
                        value = maxFps,
                        onValueChange = { maxFps = it },
                        label = { Text("Max FPS (refresh rate limit)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val newConfig = when (widget.type) {
                        WidgetType.TEXT   -> """{"unit":"$unit","jsonPath":"$jsonPath"}"""
                        WidgetType.SWITCH -> """{"onPayload":"$onPayload","offPayload":"$offPayload","iconName":"$iconName"}"""
                        WidgetType.SLIDER -> """{"min":${sliderMin.toDoubleOrNull()?:0.0},"max":${sliderMax.toDoubleOrNull()?:100.0},"step":${sliderStep.toDoubleOrNull()?:1.0},"unit":"$sliderUnit"}"""
                        WidgetType.IMAGE  -> """{"maxFps":${maxFps.toIntOrNull()?:1}}"""
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
                Text("Save", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SizePicker(
    colSpan: Int, rowSpan: Int,
    onColSpanChange: (Int) -> Unit,
    onRowSpanChange: (Int) -> Unit
) {
    Column {
        Text("Width", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "Narrow", 2 to "Wide", 4 to "Full").forEach { (span, label) ->
                FilterChip(
                    selected = colSpan == span,
                    onClick = { onColSpanChange(span) },
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Height", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "Short", 2 to "Tall").forEach { (span, label) ->
                FilterChip(
                    selected = rowSpan == span,
                    onClick = { onRowSpanChange(span) },
                    label = { Text(label) }
                )
            }
        }
    }
}
