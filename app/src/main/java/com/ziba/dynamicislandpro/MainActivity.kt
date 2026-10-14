package com.ziba.dynamicislandpro

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ziba.dynamicislandpro.service.IslandOverlayService
import com.ziba.dynamicislandpro.service.TimerForegroundService
import com.ziba.dynamicislandpro.ui.theme.DynamicIslandTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DynamicIslandTheme {
                Surface(Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }

    @Composable
    private fun MainScreen() {
        var overlayRunning by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Text("Dynamic Island Pro", style = MaterialTheme.typography.headlineMedium)

            Button(onClick = { requestOverlay() }) { Text(stringResource(R.string.perm_overlay)) }
            Button(onClick = { requestNotificationListener() }) { Text(stringResource(R.string.perm_notification)) }
            Button(onClick = { requestBattery() }) { Text(stringResource(R.string.perm_battery)) }
            Button(onClick = { openAccessibilitySettings() }) { Text(stringResource(R.string.perm_accessibility)) }

            Button(onClick = {
                overlayRunning = true
                startOverlayService()
            }) { Text(stringResource(R.string.start_overlay)) }

            OutlinedButton(onClick = {
                overlayRunning = false
                stopService(Intent(this@MainActivity, IslandOverlayService::class.java))
            }) { Text(stringResource(R.string.stop_overlay)) }

            Button(onClick = {
                TimerForegroundService.start(this@MainActivity)
            }) { Text(stringResource(R.string.start_timer)) }
            Button(onClick = { TimerForegroundService.pause(this@MainActivity) }) {
                Text(stringResource(R.string.pause_timer))
            }
            Button(onClick = { TimerForegroundService.reset(this@MainActivity) }) {
                Text(stringResource(R.string.reset_timer))
            }
        }
    }

    private fun requestOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
    }

    private fun requestNotificationListener() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun requestBattery() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName"))
            )
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun startOverlayService() {
        val intent = Intent(this, IslandOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
    }
}
