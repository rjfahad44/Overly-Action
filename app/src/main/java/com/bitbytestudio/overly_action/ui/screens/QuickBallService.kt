package com.bitbytestudio.overly_action.ui.screens

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.bitbytestudio.overly_action.MainActivity
import kotlin.math.roundToInt

class ComposeLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    fun onStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}


class QuickBallService : Service() {

    private lateinit var wm: WindowManager
    private var overlayView: ComposeView? = null
    private lateinit var params: WindowManager.LayoutParams
    private val lifecycleOwner = ComposeLifecycleOwner()

    private val viewModel: QuickBallViewModel by lazy {
        ViewModelProvider(lifecycleOwner)[QuickBallViewModel::class.java]
    }

    companion object {
        const val NOTIF_CHANNEL = "quickball_channel"
        const val NOTIF_ID = 101
        const val TAG = "QuickBallService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIF_ID, makeNotification())
        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()

        createOverlay()
        lifecycleOwner.onResume()
    }

    private fun createOverlay() {
        val overlyViewSizeDp = 200f
        val ballSizeDp = 40f
        val paddingDp = 0f

        val metrics = resources.displayMetrics
        val density = metrics.density
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val ballPx = (ballSizeDp * density).roundToInt()
        val overlyViewSizePx = (overlyViewSizeDp * density).roundToInt()
        val paddingPx = (paddingDp * density).roundToInt()
        val centerX = (screenWidth - ballPx - paddingPx)
        val centerY = (screenHeight - ballPx) / 2

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = centerX
            y = centerY
        }

        try {
            wm.addView(overlayView, params)
            overlayView?.post {
                overlayView?.setContent {
                    QuickBallComposeHost(
                        overlayView = overlayView!!,
                        params = params,
                        windowManager = wm,
                        matrices = metrics,
                        overlyViewSize = overlyViewSizeDp,
                        ballSize = ballSizeDp,
                        edgePadding = paddingDp,
                        viewModel = viewModel,
                        onRequestClose = {
                            stopSelf()
                        }
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error creating overlay", e)
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")

        try {
            lifecycleOwner.onPause()
            lifecycleOwner.onStop()
            lifecycleOwner.onDestroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying lifecycle", e)
        }

        overlayView?.let {
            try {
                wm.removeView(it)
                Log.d(TAG, "View removed from WindowManager")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing view", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun makeNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIF_CHANNEL)
            .setContentTitle("QuickBall Active")
            .setContentText("Floating control is running")
            .setSmallIcon(R.drawable.ic_media_play)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL,
                "QuickBall Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when QuickBall overlay is active"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
}