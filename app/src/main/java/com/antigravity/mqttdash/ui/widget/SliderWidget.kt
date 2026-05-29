package com.antigravity.mqttdash.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.roundToInt

/**
 * Slider widget.
 * Displays a range slider mapped to [min]..[max] with [step].
 * Subscribes to state; publishes only on release to avoid MQTT flooding.
 */
@Composable
fun SliderWidget(
    title: String,
    unit: String,
    min: Float,
    max: Float,
    step: Float,
    topicFlow: SharedFlow<String>?,
    onPublish: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: String = "MEDIUM",
    cardColorHex: String = ""
) {
    var sliderValue by remember { mutableFloatStateOf(min) }
    var isDragging by remember { mutableStateOf(false) }

    val titleSize = when (fontSize) {
        "SMALL" -> 10.sp
        "LARGE" -> 14.sp
        else -> 12.sp
    }
    val valueSize = when (fontSize) {
        "SMALL" -> 14.sp
        "LARGE" -> 22.sp
        else -> 18.sp
    }
    val containerColor = if (cardColorHex.isNotBlank()) {
        try { Color(android.graphics.Color.parseColor(cardColorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.surface }
    } else {
        MaterialTheme.colorScheme.surface
    }

    // Receive state from broker (only update when not dragging to avoid jitter)
    LaunchedEffect(topicFlow) {
        topicFlow?.collect { payload ->
            if (!isDragging) {
                payload.toFloatOrNull()?.let { remote ->
                    sliderValue = remote.coerceIn(min, max)
                }
            }
        }
    }

    val steps = if (step > 0f) ((max - min) / step).roundToInt() - 1 else 0

    WidgetCard(
        containerColor = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = titleSize),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${formatValue(sliderValue, step)} $unit".trim(),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = valueSize),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = sliderValue,
                onValueChange = { newVal ->
                    isDragging = true
                    sliderValue = newVal
                },
                onValueChangeFinished = {
                    isDragging = false
                    onPublish(formatValue(sliderValue, step))
                },
                valueRange = min..max,
                steps = if (steps > 0) steps else 0,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${formatValue(min, step)} $unit".trim(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${formatValue(max, step)} $unit".trim(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatValue(value: Float, step: Float): String {
    return if (step >= 1f) value.roundToInt().toString()
    else "%.${decimalPlaces(step)}f".format(value)
}

private fun decimalPlaces(step: Float): Int {
    val s = step.toString()
    return if (s.contains('.')) s.substringAfter('.').trimEnd('0').length else 0
}
