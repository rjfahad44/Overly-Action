package com.bitbytestudio.overly_action.ui.screens


import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bitbytestudio.overly_action.DeviceAdminReceiverImpl
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QuickBallComposeHost(
    overlayView: View,
    params: WindowManager.LayoutParams,
    windowManager: WindowManager,
    matrices: DisplayMetrics,
    overlyViewSize: Float,
    ballSize: Float,
    edgePadding: Float,
    viewModel: QuickBallViewModel,
    onRequestClose: () -> Unit,
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    val ballPx = remember(density, ballSize) { with(density) { ballSize.dp.toPx() } }
    val overlyViewSizePx = remember(density, overlyViewSize) { with(density) { overlyViewSize.dp.toPx() } }

    val screenWidthPx = remember(matrices) { matrices.widthPixels.toFloat() }
    val screenHeightPx = remember(matrices) { matrices.heightPixels.toFloat() }
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val navBarHeightPx = WindowInsets.navigationBars.getBottom(density).toFloat()

    val overlayViewState by rememberUpdatedState(overlayView)
    val paramsState by rememberUpdatedState(params)
    val windowManagerState by rememberUpdatedState(windowManager)
    val onRequestCloseState by rememberUpdatedState(onRequestClose)

    val initialX by remember { derivedStateOf { paramsState.x.toFloat() + edgePadding  } }
    val initialY by remember { derivedStateOf { paramsState.y.toFloat() } }

    val posXAnimatable by remember { derivedStateOf { Animatable(initialX) } }
    val posYAnimatable by remember { derivedStateOf { Animatable(initialY) } }

    LaunchedEffect(Unit) {
        viewModel.initialize(
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            statusBarHeightPx = statusBarHeightPx,
            navBarHeightPx = navBarHeightPx,
            ballPx = ballPx,
            menuPx = overlyViewSizePx,
            edgePadding = edgePadding,
            initialX = initialX,
            initialY = initialY
        )
    }

    LaunchedEffect(uiState.shouldSnapToEdge, uiState.targetSnapX) {
        if (uiState.shouldSnapToEdge) {
            posXAnimatable.animateTo(
                uiState.targetSnapX,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            viewModel.updatePosition(posXAnimatable.value, posYAnimatable.value)
            viewModel.onSnapComplete()
        }
    }

    val updateWindowLayout: () -> Unit = remember {
        {
            if (!overlayViewState.isAttachedToWindow) return@remember
            try {
                val p = paramsState
                if (uiState.expanded){
                    val (x, y) = viewModel.getMenuPosition()
                    val ballSize = viewModel.getMenuSize()
                    p.x = x
                    p.y = y
                    p.width = ballSize
                    p.height = ballSize
                }else{
                    p.x = posXAnimatable.value.roundToInt()
                    p.y = posYAnimatable.value.roundToInt()
                    val ballSize = viewModel.getBallSize()
                    p.width = ballSize
                    p.height = ballSize
                }
//                val (x, y) = viewModel.getMenuPosition()
//                val menuSize = viewModel.getMenuSize()
//                p.x = x
//                p.y = y
//                p.width = menuSize
//                p.height = menuSize
                windowManagerState.updateViewLayout(overlayViewState, p)
            } catch (e: Exception) {
                Log.e("QuickBall", "Error updating layout", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { Pair(posXAnimatable.value, posYAnimatable.value) }
            .collect { (x, y) ->
                viewModel.updatePosition(x, y)
                updateWindowLayout()
            }
    }

    LaunchedEffect(uiState.expanded) {
        updateWindowLayout()
    }

    val ballAlpha by animateFloatAsState(
        targetValue = if (uiState.hidden) 0.2f else 1f,
        animationSpec = tween(300),
        label = "ballAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize()
            //.background(color = Color.Green),
    ) {
        if (uiState.expanded){
            Box(
                modifier = Modifier
                    .size(overlyViewSize.dp)  // Menu size
                    .align(uiState.ballAlignment)  // Same alignment as ball
            ) {
                RadialMenu(
                    isRightSide = uiState.isRightSide,
                    ballSize = ballSize,
                    onBrightnessUp = { viewModel.onBrightnessUp(ctx) },
                    onBrightnessDown = { viewModel.onBrightnessDown(ctx) },
                    onVolumeUp = { viewModel.onVolumeUp(ctx) },
                    onVolumeDown = { viewModel.onVolumeDown(ctx) },
                    onLock = {
                        lockScreen(ctx)
                        viewModel.restartHideTimer()
                    },
                    onClose = {
                        viewModel.onClose()
                        onRequestCloseState()
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .size(ballSize.dp)
                .align(uiState.ballAlignment)
                .alpha(ballAlpha)
                .clip(CircleShape)
                .background(color = Color(0xFF2196F3))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            viewModel.onDragStart()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val newX = (posXAnimatable.value + dragAmount.x).coerceIn(viewModel.minX, viewModel.maxX)
                                val newY = (posYAnimatable.value + dragAmount.y).coerceIn(viewModel.minY, viewModel.maxY)
                                posXAnimatable.snapTo(newX)
                                posYAnimatable.snapTo(newY)
                            }
                        },
                        onDragEnd = {
                            viewModel.onDragEnd()
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = { viewModel.onBallClick() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (uiState.expanded) Icons.Default.Close else Icons.Default.Settings,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun RadialMenu(
    isRightSide: Boolean,
    ballSize: Float,
    onBrightnessUp: () -> Unit,
    onBrightnessDown: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onLock: () -> Unit,
    onClose: () -> Unit
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var lastAngle by remember { mutableFloatStateOf(0f) }

    val radius = 80f

    val items = remember {
        listOf(
            MenuButton("Brightness+", Icons.Default.LightMode, Color(0xFFFFA726), onBrightnessUp),
            MenuButton("Brightness-", Icons.Default.DarkMode, Color(0xFFEF5350), onBrightnessDown),
            MenuButton("Volume+", Icons.Default.VolumeUp, Color(0xFF66BB6A), onVolumeUp),
            MenuButton("Volume-", Icons.Default.VolumeDown, Color(0xFF42A5F5), onVolumeDown),
            MenuButton("Stop", Icons.Default.Stop, Color(0xFFFF1400), onClose),
            MenuButton("Lock", Icons.Default.Lock, Color(0xFFAB47BC), onLock),
        )
    }

    Box(
        modifier = Modifier
            .size((radius * 2.5f).dp)
            .offset(
                x = if(isRightSide) (radius * 1.25f).dp else (-radius * 1.25f).dp,
                y = 0.dp
            )
            .background(color = Color.White.copy(alpha = 0.3f), shape = CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        lastAngle = Offset(dx, dy).angle()
                    },
                    onDrag = { change, _ ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = change.position.x - centerX
                        val dy = change.position.y - centerY
                        val currentAngle = Offset(dx, dy).angle()
                        val delta = currentAngle - lastAngle
                        rotationAngle += delta
                        lastAngle = currentAngle
                        change.consume()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        items.forEachIndexed { index, button ->
            val anglePerItem = 360f / items.size
            val baseAngle = anglePerItem * index - if (isRightSide) 180f else 0f
            val totalAngle = baseAngle + rotationAngle
            val rad = Math.toRadians(totalAngle.toDouble())
            val x = cos(rad) * radius
            val y = sin(rad) * radius

            val scale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "itemScale$index"
            )

            val alpha by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 200,
                    delayMillis = index * 30
                ),
                label = "itemAlpha$index"
            )

            Box(
                modifier = Modifier
                    .offset(x = x.dp, y = y.dp)
                    .size(ballSize.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(button.color)
                    .clickable { button.onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = button.icon,
                    contentDescription = button.label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
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

private fun lockScreen(context: Context) {
    try {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val compName = ComponentName(context, DeviceAdminReceiverImpl::class.java)
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
        Toast.makeText(context, "Lock failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}