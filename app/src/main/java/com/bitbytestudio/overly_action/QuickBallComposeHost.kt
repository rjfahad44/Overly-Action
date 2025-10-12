package com.bitbytestudio.overly_action


import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


@Composable
fun QuickBallComposeHost(
    overlayView: View,
    params: WindowManager.LayoutParams,
    windowManager: WindowManager,
    onRequestClose: () -> Unit
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    val screenWidth = with(density) { displayMetrics.widthPixels.toFloat() }
    val screenHeight = with(density) { displayMetrics.heightPixels.toFloat() }
    val statusBarHeight = WindowInsets.statusBars.getTop(density)
    val navBarHeight = WindowInsets.navigationBars.getBottom(density)

    var posX by remember { mutableFloatStateOf(params.x.toFloat()) }
    var posY by remember { mutableFloatStateOf(params.y.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }

    val ballSize = 44f
    val edgePadding = 8f
    val menuSize = 200


    val minX = edgePadding
    val maxX = screenWidth - ballSize - edgePadding - 50f

    val minY = statusBarHeight + edgePadding
    val maxY = screenHeight - navBarHeight - ballSize - edgePadding

    var hideJob by remember { mutableStateOf<Job?>(null) }

    fun restartHideTimer() {
        hideJob?.cancel()
        hideJob = coroutineScope.launch {
            delay(5000)
            if (!expanded && !isDragging) hidden = true
        }
    }

    fun updateOverlayPosition(x: Float, y: Float) {
        if (!overlayView.isAttachedToWindow) return
        params.x = x.roundToInt()
        params.y = y.roundToInt()
        try {
            windowManager.updateViewLayout(overlayView, params)
        } catch (_: Exception) {}
    }

    fun snapToEdge() {
        coroutineScope.launch {
            val targetX = if (posX + ballSize / 2f < screenWidth / 2f) {
                edgePadding // Left side fully visible
            } else {
                screenWidth - ballSize - edgePadding - 80f // Right side fully visible
            }

            val animSpec = spring<Float>(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )

            animate(
                initialValue = posX,
                targetValue = targetX,
                animationSpec = animSpec
            ) { value, _ ->
                posX = value
                updateOverlayPosition(posX, posY)
            }
        }
    }

    LaunchedEffect(Unit) { restartHideTimer() }

    val alpha by animateFloatAsState(
        targetValue = if (hidden) 0.4f else 1f,
        animationSpec = tween(300)
    )

    Box(
        modifier = Modifier
            .wrapContentSize()
            .alpha(alpha)
    ) {

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .size(menuSize.dp)
                    .background(color = Color.Green)
                    .offset {
                        val offsetX = if (posX + ballSize / 2f < screenWidth / 2f) {
                            ballSize.toInt()   // Expand to the right
                        } else {
                            -menuSize  // Expand to the left
                        }
                        val offsetY = 0
                        IntOffset(offsetX, offsetY)
                    }
            ) {
                RadialMenu(
                    onBrightnessUp = { changeBrightness(ctx, +0.1f); restartHideTimer() },
                    onBrightnessDown = { changeBrightness(ctx, -0.1f); restartHideTimer() },
                    onVolumeUp = { changeVolume(ctx, true); restartHideTimer() },
                    onVolumeDown = { changeVolume(ctx, false); restartHideTimer() },
                    onLock = { lockScreen(ctx); restartHideTimer() },
                    onClose = { expanded = false; onRequestClose() }
                )
            }
        }


        Box(
            modifier = Modifier
                .size(ballSize.dp)
                .clip(CircleShape)
                .background(if (expanded) Color(0xFF1976D2) else Color(0xFF2196F3))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            hidden = false
                            expanded = false
                            hideJob?.cancel()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            posX = (posX + dragAmount.x).coerceIn(minX, maxX)
                            posY = (posY + dragAmount.y).coerceIn(minY, maxY)
                            updateOverlayPosition(posX, posY)
                        },
                        onDragEnd = {
                            isDragging = false
                            snapToEdge()
                            restartHideTimer()
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        hidden = false
                        expanded = !expanded
                        restartHideTimer()
                    }),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Settings,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}





@Composable
fun RadialMenu(
    onBrightnessUp: () -> Unit,
    onBrightnessDown: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onLock: () -> Unit,
    onClose: () -> Unit
) {
    val items = listOf(
        MenuButton("Brightness+", Icons.Default.LightMode, Color(0xFFFFC107), onBrightnessUp),
        MenuButton("Brightness-", Icons.Default.DarkMode, Color(0xFFFF9800), onBrightnessDown),
        MenuButton("Volume+", Icons.Default.VolumeUp, Color(0xFF66BB6A), onVolumeUp),
        MenuButton("Volume-", Icons.Default.VolumeDown, Color(0xFF42A5F5), onVolumeDown),
        MenuButton("Lock", Icons.Default.Lock, Color(0xFF8BC34A), onLock),
    )

    val radius = 60f

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Radial buttons
        items.forEachIndexed { index, button ->
            val angle = (360f / items.size) * index - 90f
            val rad = Math.toRadians(angle.toDouble())
            val x = cos(rad) * radius
            val y = sin(rad) * radius

            Box(
                modifier = Modifier
                    .offset(x = x.dp, y = y.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(button.color)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            button.onClick()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = button.icon,
                    contentDescription = button.label,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Center close button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935))
                .pointerInput(Unit) {
                    detectTapGestures { onClose() }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

data class MenuButton(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

// Helper functions
private fun changeVolume(context: Context, up: Boolean) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    am.adjustStreamVolume(
        android.media.AudioManager.STREAM_MUSIC,
        if (up) android.media.AudioManager.ADJUST_RAISE else android.media.AudioManager.ADJUST_LOWER,
        android.media.AudioManager.FLAG_SHOW_UI
    )
}

private fun changeBrightness(context: Context, delta: Float) {
    try {
        val resolver = context.contentResolver
        val current = android.provider.Settings.System.getInt(
            resolver,
            android.provider.Settings.System.SCREEN_BRIGHTNESS
        )
        val newVal = (current + delta * 255).toInt().coerceIn(10, 255)
        android.provider.Settings.System.putInt(
            resolver,
            android.provider.Settings.System.SCREEN_BRIGHTNESS,
            newVal
        )
        android.widget.Toast.makeText(
            context,
            "Brightness: ${(newVal * 100 / 255)}%",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "Grant WRITE_SETTINGS permission",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

private fun lockScreen(context: Context) {
    try {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                as android.app.admin.DevicePolicyManager
        val compName = android.content.ComponentName(
            context,
            DeviceAdminReceiverImpl::class.java
        )
        if (dpm.isAdminActive(compName)) {
            dpm.lockNow()
        } else {
            android.widget.Toast.makeText(
                context,
                "Enable Device Admin first",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "Lock failed: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
