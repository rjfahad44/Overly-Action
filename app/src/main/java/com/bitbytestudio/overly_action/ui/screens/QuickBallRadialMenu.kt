package com.bitbytestudio.overly_action.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun QuickBallRadialMenu() {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(100f) }
    var offsetY by remember { mutableStateOf(400f) }
    var hidden by remember { mutableStateOf(false) }

    // auto-hide after 5 sec
    LaunchedEffect(expanded) {
        if (!expanded) {
            delay(5000)
            hidden = true
        }
    }

    val targetX by animateFloatAsState(
        targetValue = if (hidden) {
            if (offsetX < screenWidth.value / 2) 0f else screenWidth.value - 50f
        } else offsetX,
        animationSpec = tween(800)
    )

    val alpha by animateFloatAsState(
        targetValue = if (hidden) 0.5f else 1f,
        animationSpec = tween(800)
    )

    Box(
        Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // circular expanded icons
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandIn(),
            exit = fadeOut() + shrinkOut(),
            modifier = Modifier
                .offset(x = targetX.dp, y = offsetY.dp)
                .alpha(alpha)
        ) {
            RadialMenuButtons(
                onBrightnessUp = { changeBrightness(context, +0.1f) },
                onBrightnessDown = { changeBrightness(context, -0.1f) },
                onVolumeUp = { changeVolume(context, true) },
                onVolumeDown = { changeVolume(context, false) },
                onLock = { /* screen off/lock action */ }
            )
        }

        // main floating ball
        Box(
            modifier = Modifier
                .offset(x = targetX.dp, y = offsetY.dp)
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFF6200EE))
                .alpha(alpha)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        hidden = false
                        expanded = false
                        offsetX += drag.x / 2
                        offsetY += drag.y / 2
                    }
                }
                .clickable {
                    hidden = false
                    expanded = !expanded
                },
            contentAlignment = Alignment.Center
        ) {
            Text(if (expanded) "×" else "⚙️", color = Color.White)
        }
    }
}

@Composable
fun RadialMenuButtons(
    onBrightnessUp: () -> Unit,
    onBrightnessDown: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onLock: () -> Unit
) {
    val icons = listOf(
        "🔆" to onBrightnessUp,
        "🔅" to onBrightnessDown,
        "🔊" to onVolumeUp,
        "🔈" to onVolumeDown,
        "⏻" to onLock
    )
    val radius = 90.dp

    Box(
        modifier = Modifier.size(radius * 2),
        contentAlignment = Alignment.Center
    ) {
        icons.forEachIndexed { index, pair ->
            val angle = 72 * index - 90 // spread 5 buttons in circle
            val rad = Math.toRadians(angle.toDouble())
            val x = cos(rad) * radius.value
            val y = sin(rad) * radius.value

            Box(
                modifier = Modifier
                    .offset(x.dp, y.dp)
                    .size(45.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .clickable { pair.second() },
                contentAlignment = Alignment.Center
            ) {
                Text(pair.first, color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/** ---------- System Control Helpers ---------- **/

private fun changeVolume(context: Context, up: Boolean) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    am.adjustVolume(if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
}

private fun changeBrightness(context: Context, delta: Float) {
    try {
        val resolver = context.contentResolver
        val current = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
        val newVal = (current + delta * 255).toInt().coerceIn(20, 255)
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, newVal)
    } catch (_: Exception) { }
}
