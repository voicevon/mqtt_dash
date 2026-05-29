package com.antigravity.mqttdash.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayway.jsonpath.JsonPath
import kotlinx.coroutines.flow.SharedFlow

/**
 * Text / Value widget.
 * Subscribes to [topicFlow], optionally applies [jsonPath], and displays the result.
 */
@Composable
fun TextWidget(
    title: String,
    unit: String,
    jsonPath: String?,
    topicFlow: SharedFlow<String>?,
    modifier: Modifier = Modifier
) {
    var rawValue by remember { mutableStateOf("—") }

    LaunchedEffect(topicFlow) {
        topicFlow?.collect { payload ->
            rawValue = if (!jsonPath.isNullOrBlank()) {
                try {
                    JsonPath.read<Any>(payload, jsonPath).toString()
                } catch (e: Exception) {
                    payload
                }
            } else {
                payload
            }
        }
    }

    WidgetCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = rawValue,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (unit.isNotBlank()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
