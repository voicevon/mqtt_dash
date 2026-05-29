package com.antigravity.mqttdash.ui.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.SharedFlow

/** Mapping from icon name string to Material icon vector */
val switchIconMap: Map<String, ImageVector> = mapOf(
    "lightbulb" to Icons.Filled.Lightbulb,
    "power" to Icons.Filled.Power,
    "ac_unit" to Icons.Filled.AcUnit,
    "thermostat" to Icons.Filled.Thermostat,
    "outlet" to Icons.Filled.Outlet,
    "fan" to Icons.Filled.Propane, // proxy
    "lock" to Icons.Filled.Lock,
    "door" to Icons.Filled.MeetingRoom,
    "blind" to Icons.Filled.Blinds,
    "camera" to Icons.Filled.Camera,
    "speaker" to Icons.Filled.Speaker,
    "tv" to Icons.Filled.Tv,
    "default" to Icons.Filled.ToggleOn
)

/**
 * Switch widget.
 * Subscribes to [topicFlow] to track on/off state; publishes [onPayload]/[offPayload]
 * on toggle with optimistic UI update.
 */
@Composable
fun SwitchWidget(
    title: String,
    iconName: String,
    onPayload: String,
    offPayload: String,
    topicFlow: SharedFlow<String>?,
    onPublish: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isOn by remember { mutableStateOf(false) }

    // Subscribe to state topic
    LaunchedEffect(topicFlow) {
        topicFlow?.collect { payload ->
            isOn = payload.trim() == onPayload.trim()
        }
    }

    val bgColor by animateColorAsState(
        targetValue = if (isOn)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300),
        label = "switchBg"
    )

    val icon = switchIconMap[iconName] ?: Icons.Filled.ToggleOn

    WidgetCard(
        containerColor = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isOn) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = isOn,
                onCheckedChange = { newState ->
                    isOn = newState  // optimistic update
                    onPublish(if (newState) onPayload else offPayload)
                }
            )
        }
    }
}
