package com.bitbytestudio.overly_action


import android.R.attr.maxHeight
import android.R.attr.maxWidth
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


//@Composable
//fun QuickBallComposeHost(
//    overlayView: View,
//    params: WindowManager.LayoutParams,
//    windowManager: WindowManager,
//    matrices: DisplayMetrics,
//    ballSize: Float,
//    edgePadding: Float,
//    onRequestClose: () -> Unit,
//) {
//    val ctx = LocalContext.current
//    val density = LocalDensity.current
//    val coroutineScope = rememberCoroutineScope()
//
//    val screenWidth = with(density) { matrices.widthPixels.toFloat() }
//    val screenHeight = with(density) { matrices.heightPixels.toFloat() }
//    val statusBarHeight = WindowInsets.statusBars.getTop(density)
//    val navBarHeight = WindowInsets.navigationBars.getBottom(density)
//
//    var ballPosX by remember { mutableFloatStateOf(params.x.toFloat() + edgePadding) }
//    var ballPosY by remember { mutableFloatStateOf(params.y.toFloat()) }
//    var isDragging by remember { mutableStateOf(false) }
//    var expanded by remember { mutableStateOf(false) }
//    var hidden by remember { mutableStateOf(false) }
//    var ballAlignment by remember { mutableStateOf(Alignment.CenterStart) }
//
//    val menuSize = 200f
//
//    val minX = edgePadding
//    val maxX = screenWidth - ballSize - edgePadding
//
//    val minY = statusBarHeight + edgePadding
//    val maxY = screenHeight - navBarHeight - ballSize - edgePadding
//
//    var hideJob by remember { mutableStateOf<Job?>(null) }
//
//    fun restartHideTimer() {
//        hideJob?.cancel()
//        hideJob = coroutineScope.launch {
//            delay(5000)
//            if (!expanded && !isDragging) hidden = true
//        }
//    }
//
//    fun updateWindowLayout() {
//        if (!overlayView.isAttachedToWindow) return
//
//        try {
//            val menuPx = with(density) { menuSize.dp.toPx() }
//            val ballPx = with(density) { ballSize.dp.toPx() }
//
//            if (expanded) {
//                val menuOffsetX = (ballPosX + ballPx / 2f - menuPx / 2f)
//                    .coerceIn(edgePadding, screenWidth - menuPx - edgePadding)
//
//                val menuOffsetY = (ballPosY + ballPx / 2f - menuPx / 2f)
//                    .coerceIn(
//                        statusBarHeight.toFloat(),
//                        screenHeight - navBarHeight - menuPx
//                    )
//
//                params.x = menuOffsetX.roundToInt()
//                params.y = menuOffsetY.roundToInt()
//                params.width = menuPx.roundToInt()
//                params.height = menuPx.roundToInt()
//            } else {
//                params.x = ballPosX.roundToInt()
//                params.y = ballPosY.roundToInt()
//                params.width = ballPx.roundToInt()
//                params.height = ballPx.roundToInt()
//            }
//
//            windowManager.updateViewLayout(overlayView, params)
//        } catch (e: Exception) {
//            Log.e("QuickBall", "Error updating layout", e)
//        }
//    }
//
//
//
//
//
//    fun snapToEdge() {
//        coroutineScope.launch {
//            val targetX = if (ballPosX + ballSize / 2f < screenWidth / 2f) {
//                edgePadding
//            } else {
//                screenWidth - ballSize - edgePadding - 70f
//            }
//
//            animate(
//                initialValue = ballPosX,
//                targetValue = targetX,
//                animationSpec = spring(
//                    dampingRatio = Spring.DampingRatioHighBouncy,
//                    stiffness = Spring.StiffnessMedium
//                )
//            ) { value, _ ->
//                ballPosX = value
//                updateWindowLayout()
//            }
//        }
//    }
//
//    LaunchedEffect(expanded) {
//        updateWindowLayout()
//    }
//
//    LaunchedEffect(Unit) {
//        updateWindowLayout()
//        restartHideTimer()
//    }
//
//    val alpha by animateFloatAsState(
//        targetValue = if (hidden) 0.2f else 1f,
//        animationSpec = tween(300)
//    )
//
//    val scale by animateFloatAsState(
//        targetValue = if (hidden) 0.5f else 1f,
//        animationSpec = spring(
//            dampingRatio = Spring.DampingRatioHighBouncy,
//            stiffness = Spring.StiffnessMedium
//        )
//    )
//
//    val animatedMenuSize by animateDpAsState(
//        targetValue = if (expanded) menuSize.dp else 0.dp,
//        animationSpec = spring(
//                dampingRatio = Spring.DampingRatioNoBouncy,
//                stiffness = Spring.StiffnessLow
//            )
//    )
//
//    Box(
//        modifier = Modifier
//            .wrapContentSize()
//            .alpha(alpha),
//        contentAlignment = Alignment.Center
//    ) {
//
//        AnimatedVisibility(
//            visible = expanded,
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(menuSize.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                RadialMenu(
//                    isRightSide = ballPosX + ballSize / 2f >= screenWidth / 2f,
//                    onBrightnessUp = { changeBrightness(ctx, +0.1f); restartHideTimer() },
//                    onBrightnessDown = { changeBrightness(ctx, -0.1f); restartHideTimer() },
//                    onVolumeUp = { changeVolume(ctx, true); restartHideTimer() },
//                    onVolumeDown = { changeVolume(ctx, false); restartHideTimer() },
//                    onLock = { lockScreen(ctx); restartHideTimer() },
//                    onClose = {
//                        expanded = false
//                        onRequestClose()
//                    }
//                )
//            }
//        }
//
//
//        Box(
//            modifier = Modifier
//                .size(ballSize.dp)
//                .align(ballAlignment)
//                .clip(CircleShape)
//                .background(if (expanded) Color(0xFF0069BB) else Color(0xFF2196F3))
//                .pointerInput(Unit) {
//                    detectDragGestures(
//                        onDragStart = {
//                            isDragging = true
//                            hidden = false
//                            if (expanded) {
//                                expanded = false
//                                coroutineScope.launch { delay(100) }
//                            }
//                            hideJob?.cancel()
//                        },
//                        onDrag = { change, dragAmount ->
//                            change.consume()
//                            ballPosX = (ballPosX + dragAmount.x).coerceIn(minX, maxX)
//                            ballPosY = (ballPosY + dragAmount.y).coerceIn(minY, maxY)
//                            updateWindowLayout()
//                        },
//                        onDragEnd = {
//                            isDragging = false
//                            snapToEdge()
//                            restartHideTimer()
//                        }
//                    )
//                }
//                .clickable(
//                    interactionSource = remember { MutableInteractionSource() },
//                    indication = null,
//                    onClick = {
//                        hidden = false
//                        expanded = !expanded
//                        ballAlignment = if (expanded && (ballPosX + ballSize / 2f >= screenWidth / 2f)) Alignment.CenterEnd else Alignment.CenterStart
//                        restartHideTimer()
//                    }
//                ),
//            contentAlignment = Alignment.Center
//        ) {
//            Icon(
//                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Settings,
//                contentDescription = null,
//                tint = Color.White,
//                modifier = Modifier.size(24.dp)
//            )
//        }
//    }
//}





@Composable
fun QuickBallComposeHost(
    overlayView: View,
    params: WindowManager.LayoutParams,
    windowManager: WindowManager,
    matrices: DisplayMetrics,
    ballSize: Float,
    edgePadding: Float,
    onRequestClose: () -> Unit,
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val ballPx = remember(density, ballSize) { with(density) { ballSize.dp.toPx() } }
    val menuSize = 200f
    val menuPx by remember(density, menuSize) { mutableFloatStateOf(with(density) { menuSize.dp.toPx() }) }

    val screenWidthPx = remember(matrices, density) { matrices.widthPixels.toFloat() }
    val screenHeightPx = remember(matrices, density) { matrices.heightPixels.toFloat() }
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val navBarHeightPx = WindowInsets.navigationBars.getBottom(density).toFloat()

    val minX = edgePadding
    val maxX = remember(screenWidthPx, ballPx, edgePadding) { screenWidthPx - ballPx - edgePadding }
    val minY = statusBarHeightPx + edgePadding
    val maxY = remember(screenHeightPx, navBarHeightPx, ballPx, edgePadding) {
        screenHeightPx - navBarHeightPx - ballPx - edgePadding
    }

    val overlayViewState by rememberUpdatedState(overlayView)
    val paramsState by rememberUpdatedState(params)
    val windowManagerState by rememberUpdatedState(windowManager)
    val onRequestCloseState by rememberUpdatedState(onRequestClose)

    val initialX = remember { params.x.toFloat() + edgePadding }
    val initialY = remember { params.y.toFloat() }

    val state = remember { QuickBallState(initialX.coerceIn(minX, maxX), initialY.coerceIn(minY, maxY)) }

    var hideJob by remember { mutableStateOf<Job?>(null) }
    val restartHideTimer by rememberUpdatedState(
        newValue = {
            hideJob?.cancel()
            hideJob = coroutineScope.launch {
                delay(5000)
                if (!state.expanded && !state.isDragging) {
                    state.hidden = true
                }
            }
        }
    )

    val updateWindowLayout by rememberUpdatedState(
        newValue = {
            if (!overlayViewState.isAttachedToWindow) return@rememberUpdatedState
            try {
                val p = paramsState
                if (state.expanded) {
                    val midX = state.posX.value + ballPx / 2f
                    val midY = state.posY.value + ballPx / 2f

                    val menuOffsetX = (midX - menuPx / 2f).coerceIn(edgePadding, screenWidthPx - menuPx - edgePadding)
                    val menuOffsetY = (midY - menuPx / 2f).coerceIn(
                        statusBarHeightPx,
                        screenHeightPx - navBarHeightPx - menuPx
                    )

                    p.x = menuOffsetX.roundToInt()
                    p.y = menuOffsetY.roundToInt()
                    p.width = menuPx.roundToInt()
                    p.height = menuPx.roundToInt()
                } else {
                    p.x = state.posX.value.roundToInt()
                    p.y = state.posY.value.roundToInt()
                    p.width = ballPx.roundToInt()
                    p.height = ballPx.roundToInt()
                }
                windowManagerState.updateViewLayout(overlayViewState, p)
            } catch (e: Exception) {
                Log.e("QuickBall", "Error updating layout", e)
            }
        }
    )

    LaunchedEffect(state.expanded) {
        updateWindowLayout()
    }

    LaunchedEffect(Unit) {
        updateWindowLayout()
        restartHideTimer()
    }


    LaunchedEffect(Unit) {
        launch {
            snapshotFlow { Pair(state.posX.value, state.posY.value) }
                .collect { _ -> updateWindowLayout() }
        }
        snapshotFlow { state.expanded }.collect { updateWindowLayout() }
    }

    val alpha by animateFloatAsState(
        targetValue = if (state.hidden) 0.2f else 1f,
        animationSpec = tween(300)
    )

    val animatedMenuSize by animateDpAsState(
        targetValue = if (state.expanded) menuSize.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    fun snapToEdge() {
        coroutineScope.launch {
            val currentX = state.posX.value
            val targetX = if (currentX + ballPx / 2f < screenWidthPx / 2f) {
                edgePadding
            } else {
                screenWidthPx - ballPx - edgePadding
            }

            state.posX.animateTo(
                targetX,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            updateWindowLayout()
        }
    }


    fun onBallClick() {
        state.hidden = false
        state.expanded = !state.expanded
        state.ballAlignment = if (state.expanded && (state.posX.value + ballPx / 2f >= screenWidthPx / 2f)) {
            Alignment.CenterEnd
        } else {
            Alignment.CenterStart
        }
        restartHideTimer()
    }

    suspend fun onDragChange(dragAmountX: Float, dragAmountY: Float) {
        val newX = (state.posX.value + dragAmountX).coerceIn(minX, maxX)
        val newY = (state.posY.value + dragAmountY).coerceIn(minY, maxY)
        state.posX.snapTo(newX)
        state.posY.snapTo(newY)
    }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = state.expanded) {
            Box(
                modifier = Modifier
                    .size(menuSize.dp),
                contentAlignment = Alignment.Center
            ) {
                RadialMenu(
                    isRightSide = state.posX.value + ballPx / 2f >= screenWidthPx / 2f,
                    onBrightnessUp = { changeBrightness(ctx, +0.1f); restartHideTimer() },
                    onBrightnessDown = { changeBrightness(ctx, -0.1f); restartHideTimer() },
                    onVolumeUp = { changeVolume(ctx, true); restartHideTimer() },
                    onVolumeDown = { changeVolume(ctx, false); restartHideTimer() },
                    onLock = { lockScreen(ctx); restartHideTimer() },
                    onClose = {
                        state.expanded = false
                        onRequestCloseState()
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .size(ballSize.dp)
                .align(state.ballAlignment)
                .clip(CircleShape)
                .background(if (state.expanded) Color(0xFF0069BB) else Color(0xFF2196F3))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            state.isDragging = true
                            state.hidden = false
                            if (state.expanded) {
                                state.expanded = false
                                coroutineScope.launch { delay(100) }
                            }
                            hideJob?.cancel()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                onDragChange(dragAmount.x, dragAmount.y)
                            }
                        },
                        onDragEnd = {
                            state.isDragging = false
                            snapToEdge()
                            restartHideTimer()
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onBallClick() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (state.expanded) Icons.Default.Close else Icons.Default.Settings,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


class QuickBallState(
    startX: Float,
    startY: Float,
) {
    val posX = Animatable(startX)
    val posY = Animatable(startY)
    var isDragging by mutableStateOf(false)
    var expanded by mutableStateOf(false)
    var hidden by mutableStateOf(false)

    var ballAlignment by mutableStateOf(Alignment.CenterStart)
}


@Composable
fun RadialMenu(
    isRightSide: Boolean,
    radius: Float = 50f,
    onBrightnessUp: () -> Unit,
    onBrightnessDown: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onLock: () -> Unit,
    onClose: () -> Unit
) {
    val items = remember {
        listOf(
            MenuButton("Brightness+", Icons.Default.LightMode, Color(0xFFFFA726), onBrightnessUp),
            MenuButton("Brightness-", Icons.Default.DarkMode, Color(0xFFEF5350), onBrightnessDown),
            MenuButton("Volume+", Icons.Default.VolumeUp, Color(0xFF66BB6A), onVolumeUp),
            MenuButton("Volume-", Icons.Default.VolumeDown, Color(0xFF42A5F5), onVolumeDown),
            MenuButton("Lock", Icons.Default.Lock, Color(0xFFAB47BC), onLock),
        )
    }

    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var lastAngle by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        items.forEachIndexed { index, button ->
            val anglePerItem = 360f / items.size
            val baseAngle = anglePerItem * index - if (isRightSide) 35f else -145f
            val totalAngle = baseAngle + rotationAngle
            val rad = Math.toRadians(totalAngle.toDouble())
            val x = cos(rad) * radius
            val y = sin(rad) * radius

            Box(
                modifier = Modifier
                    .absoluteOffset(x = x.dp, y = y.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(button.color)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val center = Offset(radius / 2f, radius / 2f)
                                lastAngle = (offset - center).angle()
                            },
                            onDrag = { change, _ ->
                                val center = Offset(radius / 2f, radius / 2f)
                                val currentAngle = (change.position - center).angle()
                                val delta = currentAngle - lastAngle
                                rotationAngle += delta
                                lastAngle = currentAngle
                            }
                        )
                    }
                    .clickable { button.onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = button.icon,
                    contentDescription = button.label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935))
                .clickable(onClick = onClose),
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

private fun Offset.angle(): Float {
    return Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
}



data class MenuButton(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)


private fun changeVolume(context: Context, up: Boolean) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    am.adjustStreamVolume(
        AudioManager.STREAM_MUSIC,
        if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
        AudioManager.FLAG_SHOW_UI
    )
}

private fun changeBrightness(context: Context, delta: Float) {
    try {
        val resolver = context.contentResolver
        val current = Settings.System.getInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS
        )
        val newVal = (current + delta * 255).toInt().coerceIn(10, 255)
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS,
            newVal
        )
        Toast.makeText(
            context,
            "☀️ ${(newVal * 100 / 255)}%",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Grant WRITE_SETTINGS permission",
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun lockScreen(context: Context) {
    try {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                as DevicePolicyManager
        val compName = ComponentName(
            context,
            DeviceAdminReceiverImpl::class.java
        )
        if (dpm.isAdminActive(compName)) {
            dpm.lockNow()
        } else {
            Toast.makeText(
                context,
                "Enable Device Admin in Settings → Security",
                Toast.LENGTH_LONG
            ).show()
        }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Lock failed: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
    }
}



/*    fun updateWindowLayout() {
        if (!overlayView.isAttachedToWindow) return

        try {
            val isRight = ballPosX + ballSize / 2f >= screenWidth / 2f

            val menuPx = with(density) { menuSize.dp.toPx().roundToInt() }
            val ballPx = with(density) { ballSize.dp.toPx().roundToInt() }

            val targetX: Int
            val targetY = ballPosY.roundToInt()
            val targetW: Int
            val targetH: Int

            if (expanded) {
                if (isRight) {
                    targetX = (ballPosX - menuSize * density.density).roundToInt() + 60
                    targetW = menuPx + ballPx
                } else {
                    targetX = ballPosX.roundToInt() - 60
                    val rightSpace = screenWidth - targetX
                    targetW = min(menuPx + ballPx, rightSpace.roundToInt())
                }
                targetH = menuPx
            } else {
                targetX = ballPosX.roundToInt()
                targetW = ballPx
                targetH = ballPx
            }

            params.x = targetX
            params.y = targetY
            params.width = targetW
            params.height = targetH
            windowManager.updateViewLayout(overlayView, params)

        } catch (e: Exception) {
            Log.e("QuickBall", "Error updating layout", e)
        }
    }*/
