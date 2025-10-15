package com.bitbytestudio.overly_action.ui.screens

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


data class QuickBallUiState(
    val posX: Float = 0f,
    val posY: Float = 0f,
    val isDragging: Boolean = false,
    val expanded: Boolean = false,
    val hidden: Boolean = false,
    val ballAlignment: Alignment = Alignment.CenterEnd,
    val alpha: Float = 1f,
    val isRightSide: Boolean = true,
    val shouldSnapToEdge: Boolean = false,
    val targetSnapX: Float = 0f
)

data class ScreenConfig(
    val screenWidthPx: Float,
    val screenHeightPx: Float,
    val statusBarHeightPx: Float,
    val navBarHeightPx: Float,
    val ballPx: Float,
    val menuPx: Float,
    val edgePadding: Float
)

class QuickBallViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuickBallUiState())
    val uiState: StateFlow<QuickBallUiState> = _uiState.asStateFlow()

    private var hideJob: Job? = null
    private var screenConfig: ScreenConfig? = null

    val minX: Float get() = screenConfig?.edgePadding ?: 0f
    val maxX: Float get() = screenConfig?.let {
        it.screenWidthPx - it.ballPx - it.edgePadding
    } ?: 0f
    val minY: Float get() = screenConfig?.let {
        it.statusBarHeightPx + it.edgePadding
    } ?: 0f
    val maxY: Float get() = screenConfig?.let {
        it.screenHeightPx - it.navBarHeightPx - it.ballPx - it.edgePadding
    } ?: 0f

    fun initialize(
        screenWidthPx: Float,
        screenHeightPx: Float,
        statusBarHeightPx: Float,
        navBarHeightPx: Float,
        ballPx: Float,
        menuPx: Float,
        edgePadding: Float,
        initialX: Float,
        initialY: Float
    ) {
        screenConfig = ScreenConfig(
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            statusBarHeightPx = statusBarHeightPx,
            navBarHeightPx = navBarHeightPx,
            ballPx = ballPx,
            menuPx = menuPx,
            edgePadding = edgePadding,
        )

        val coercedX = initialX.coerceIn(minX, maxX)
        val coercedY = initialY.coerceIn(minY, maxY)

        _uiState.value = _uiState.value.copy(
            posX = coercedX,
            posY = coercedY
        )

        startHideTimer()
    }

    fun onBallClick() {
        val config = screenConfig ?: return
        val newExpanded = !_uiState.value.expanded
        val isRight = isRightSide()
        _uiState.value = _uiState.value.copy(
            hidden = false,
            expanded = newExpanded,
            isRightSide = isRight,
        )
        restartHideTimer()
    }


    fun onDragStart() {
        _uiState.value = _uiState.value.copy(
            isDragging = true,
            hidden = false,
            shouldSnapToEdge = false
        )

        if (_uiState.value.expanded) {
            _uiState.value = _uiState.value.copy(expanded = false)
        }

        hideJob?.cancel()
    }

    fun updatePosition(x: Float, y: Float) {
        _uiState.value = _uiState.value.copy(
            posX = x,
            posY = y,
        )
    }

    fun onDragEnd() {
        val config = screenConfig ?: return

        val currentX = _uiState.value.posX
        val targetX = if (currentX + config.ballPx / 2f < config.screenWidthPx / 2f) {
            config.edgePadding
        } else {
            config.screenWidthPx - config.ballPx - config.edgePadding
        }

        val isRight = isRightSide()

        _uiState.value = _uiState.value.copy(
            isDragging = false,
            shouldSnapToEdge = true,
            targetSnapX = targetX,
            isRightSide = isRight,
            ballAlignment = if (isRight) {
                Alignment.CenterEnd
            } else {
                Alignment.CenterStart
            },
        )

        restartHideTimer()
    }

    fun onSnapComplete() {
        _uiState.value = _uiState.value.copy(shouldSnapToEdge = false)
    }

    private fun startHideTimer() {
        hideJob?.cancel()
        hideJob = viewModelScope.launch {
            delay(5000)
            if (!_uiState.value.expanded && !_uiState.value.isDragging) {
                _uiState.value = _uiState.value.copy(
                    hidden = true,
                    alpha = 0.2f
                )
            }
        }
    }


    fun restartHideTimer() {
        _uiState.value = _uiState.value.copy(
            hidden = false,
            alpha = 1f
        )
        startHideTimer()
    }

    private fun isRightSide(): Boolean {
        val config = screenConfig ?: return false
        return _uiState.value.posX + config.ballPx / 2f >= config.screenWidthPx / 2f
    }

    fun getMenuPosition(): Pair<Int, Int> {
        val config = screenConfig ?: return Pair(0, 0)
        val midX = _uiState.value.posX + config.ballPx / 2f
        val midY = _uiState.value.posY + config.ballPx / 2f

        val menuOffsetX = (midX - config.menuPx / 2f).coerceIn(
            config.edgePadding,
            config.screenWidthPx - config.menuPx - config.edgePadding
        )
        val menuOffsetY = (midY - config.menuPx / 2f).coerceIn(
            config.statusBarHeightPx,
            config.screenHeightPx - config.navBarHeightPx - config.menuPx
        )

        return Pair(menuOffsetX.roundToInt(), menuOffsetY.roundToInt())
    }

    fun getBallPosition(): Pair<Int, Int> {
        return Pair(
            _uiState.value.posX.roundToInt(),
            _uiState.value.posY.roundToInt()
        )
    }

    fun getMenuSize(): Int = screenConfig?.menuPx?.roundToInt() ?: 0
    fun getBallSize(): Int = screenConfig?.ballPx?.roundToInt() ?: 0

    // Actions
    fun onBrightnessUp(context: Context) {
        changeBrightness(context, +0.1f)
        restartHideTimer()
    }

    fun onBrightnessDown(context: Context) {
        changeBrightness(context, -0.1f)
        restartHideTimer()
    }

    fun onVolumeUp(context: Context) {
        changeVolume(context, true)
        restartHideTimer()
    }

    fun onVolumeDown(context: Context) {
        changeVolume(context, false)
        restartHideTimer()
    }

    fun onClose() {
        _uiState.value = _uiState.value.copy(expanded = false)
    }

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

    override fun onCleared() {
        super.onCleared()
        hideJob?.cancel()
    }
}