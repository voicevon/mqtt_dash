package com.antigravity.mqttdash.ui.screen.dashboard

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.antigravity.mqttdash.data.db.entity.WidgetEntity
import com.antigravity.mqttdash.data.db.entity.WidgetType
import com.antigravity.mqttdash.ui.widget.ImageWidget
import com.antigravity.mqttdash.ui.widget.SliderWidget
import com.antigravity.mqttdash.ui.widget.SwitchWidget
import com.antigravity.mqttdash.ui.widget.TextWidget
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject

/**
 * Dispatches a [WidgetEntity] to its corresponding Composable and
 * overlays edit-mode controls (delete button, drag handle).
 */
@Composable
fun WidgetRenderer(
    widget: WidgetEntity,
    isEditMode: Boolean,
    topicFlow: (topic: String, qos: Int) -> SharedFlow<String>,
    onPublish: (topic: String, payload: String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = runCatching { JSONObject(widget.configJson) }.getOrDefault(JSONObject())
    val subFlow = widget.subTopic?.let { topicFlow(it, widget.qos) }
    val fontSize = config.optString("fontSize", "MEDIUM")
    val cardColorHex = config.optString("cardColor", "")

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onEdit() }
                )
            }
    ) {
        // ── Widget content ──────────────────────────────────────────────
        when (widget.type) {
            WidgetType.TEXT -> TextWidget(
                title = widget.title,
                unit = config.optString("unit", ""),
                jsonPath = config.optString("jsonPath", "").ifBlank { null },
                topicFlow = subFlow,
                fontSize = fontSize,
                cardColorHex = cardColorHex,
                modifier = Modifier.fillMaxSize()
            )

            WidgetType.SWITCH -> SwitchWidget(
                title = widget.title,
                iconName = config.optString("iconName", "lightbulb"),
                onPayload = config.optString("onPayload", "1"),
                offPayload = config.optString("offPayload", "0"),
                topicFlow = subFlow,
                onPublish = { payload ->
                    widget.pubTopic?.let { onPublish(it, payload) }
                },
                fontSize = fontSize,
                cardColorHex = cardColorHex,
                modifier = Modifier.fillMaxSize()
            )

            WidgetType.SLIDER -> SliderWidget(
                title = widget.title,
                unit = config.optString("unit", ""),
                min = config.optDouble("min", 0.0).toFloat(),
                max = config.optDouble("max", 100.0).toFloat(),
                step = config.optDouble("step", 1.0).toFloat(),
                topicFlow = subFlow,
                onPublish = { payload ->
                    widget.pubTopic?.let { onPublish(it, payload) }
                },
                fontSize = fontSize,
                cardColorHex = cardColorHex,
                modifier = Modifier.fillMaxSize()
            )

            WidgetType.IMAGE -> ImageWidget(
                title = widget.title,
                maxFps = config.optInt("maxFps", 1),
                topicFlow = subFlow,
                fontSize = fontSize,
                cardColorHex = cardColorHex,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Edit mode overlay ───────────────────────────────────────────
        if (isEditMode) {
            // Edit button (top-left)
            SmallFloatingActionButton(
                onClick = onEdit,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(28.dp)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
            }

            // Delete button (top-right)
            SmallFloatingActionButton(
                onClick = onDelete,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Delete", modifier = Modifier.size(14.dp))
            }

            // Drag handle (bottom-right) — long press activates reorder via Reorderable library
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Drag",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(20.dp)
            )
        }
    }
}
