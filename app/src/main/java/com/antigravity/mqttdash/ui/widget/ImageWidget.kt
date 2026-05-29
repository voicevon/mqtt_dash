package com.antigravity.mqttdash.ui.widget

import android.util.Base64
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import kotlinx.coroutines.flow.SharedFlow
import androidx.compose.ui.unit.sp

/**
 * Image widget.
 * Accepts a payload that is either:
 *  - A URL string (starts with http/https)
 *  - A Base64-encoded JPEG/PNG image
 *
 * Applies a rate limit of [maxFps] to avoid excessive recompositions on fast streams.
 */
@Composable
fun ImageWidget(
    title: String,
    maxFps: Int,
    topicFlow: SharedFlow<String>?,
    modifier: Modifier = Modifier,
    onClickFullScreen: (Any) -> Unit = {},
    fontSize: String = "MEDIUM",
    cardColorHex: String = ""
) {
    var imageSource by remember { mutableStateOf<Any?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val minIntervalMs = if (maxFps > 0) (1000L / maxFps) else 1000L

    val titleSize = when (fontSize) {
        "SMALL" -> 10.sp
        "LARGE" -> 14.sp
        else -> 12.sp
    }
    val containerColor = if (cardColorHex.isNotBlank()) {
        try { Color(android.graphics.Color.parseColor(cardColorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.surface }
    } else {
        MaterialTheme.colorScheme.surface
    }
    var lastUpdateMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(topicFlow) {
        topicFlow?.collect { payload ->
            val now = System.currentTimeMillis()
            if (now - lastUpdateMs < minIntervalMs) return@collect
            lastUpdateMs = now

            imageSource = when {
                payload.startsWith("http://") || payload.startsWith("https://") -> {
                    payload.trim()
                }
                else -> {
                    // Try decode as Base64
                    try {
                        Base64.decode(payload.trim(), Base64.DEFAULT)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            isLoading = false
        }
    }

    WidgetCard(
        containerColor = containerColor,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { imageSource?.let(onClickFullScreen) }
        ) {
            if (isLoading || imageSource == null) {
                ShimmerBox(modifier = Modifier.fillMaxSize())
            } else {
                AsyncImage(
                    model = imageSource,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { isLoading = false }
                )
            }
            // Title overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = titleSize),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ShimmerBox(modifier: Modifier = Modifier) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )
    Box(modifier = modifier.background(brush))
}
