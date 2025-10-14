package com.bitbytestudio.overly_action

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.net.toUri
import com.bitbytestudio.overly_action.ui.screens.QuickBallService

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
    ) {
        checkAndRequestPermissions()
    }

    private val writeSettingsPermissionLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
    ) {
        checkAndRequestPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            MaterialTheme {
                Surface {
                    Text(text = "QuickBall — open app to grant permissions")
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        when {
            !Settings.canDrawOverlays(this) -> {
                requestOverlayPermission()
            }
            !Settings.System.canWrite(this) -> {
                requestWriteSettingsPermission()
            }
            else -> {
                startServiceAndFinish()
            }
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri()
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun requestWriteSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = "package:$packageName".toUri()
        }
        writeSettingsPermissionLauncher.launch(intent)
    }

    private fun startServiceAndFinish() {
        val svcIntent = Intent(this, QuickBallService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svcIntent)
        } else {
            startService(svcIntent)
        }
        finish()
    }
}
