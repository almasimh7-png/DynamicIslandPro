package com.ziba.dynamicislandpro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ziba.dynamicislandpro.R
import com.ziba.dynamicislandpro.core.AppBus
import com.ziba.dynamicislandpro.ui.theme.DynamicIslandTheme
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

/**
 * Foreground service that draws the Dynamic Island overlay on top of everything
 * using WindowManager + ComposeView, with expanded/collapsed animated states.
 */
class IslandOverlayService : Service(), LifecycleOwner {

    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    private lateinit var wm: WindowManager
    private var composeView: ComposeView? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        registry.currentState = Lifecycle.State.CREATED
        startForegroundNotification()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        handler.post { addOverlayView() }
    }

    private fun startForegroundNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val id = "island_overlay"
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(id, getString(R.string.notif_channel_overlay),
                    NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification: Notification =
            if (Build.VERSION.SDK_INT >= 26) {
                Notification.Builder(this, id)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(getString(R.string.app_name))
                    .build()
            } else {
                Notification.Builder(this).setSmallIcon(android.R.drawable.ic_dialog_info).build()
            }
        startForeground(1001, notification)
    }

    private fun addOverlayView() {
        if (composeView != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 40
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@IslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@IslandOverlayService)
            setContent { IslandOverlayContent() }
        }
        wm.addView(view, params)
        composeView = view
        registry.currentState = Lifecycle.State.RESUMED
    }

    @Composable
    private fun IslandOverlayContent() {
        val state by AppBus.islandState.collectAsState()
        val notif by AppBus.notifications.collectAsState()
        val media by AppBus.media.collectAsState()
        val timer by AppBus.timer.collectAsState()
        val callState by AppBus.callState.collectAsState()

        val expanded = state == AppBus.IslandState.EXPANDED
        val scale by animateFloatAsState(if (expanded) 1f else 0.6f, tween(300), label = "scale")
        val alpha by animateFloatAsState(if (expanded) 1f else 0.55f, tween(300), label = "alpha")

        // Call state changes animate the island color/size
        val callColor = when (callState) {
            AppBus.CallState.INCOMING -> Color(0xFF1B5E20)
            AppBus.CallState.ACTIVE -> Color(0xFF00695C)
            AppBus.CallState.ENDED -> Color(0xFFB71C1C)
            else -> Color.Black
        }

        DynamicIslandTheme {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { AppBus.toggle() }
                    .verticalDragToggle()
            ) {
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .alpha(alpha)
                        .background(callColor, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (expanded) {
                        Column(
                            modifier = Modifier.width(240.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            notif?.let { n ->
                                Text("🔔 ${n.title}", color = Color.White, fontSize = 14.sp)
                                Text(n.text, color = Color(0xFFBDBDBD), fontSize = 12.sp,
                                    maxLines = 2)
                            }
                            if (media.title.isNotEmpty()) {
                                Text("🎵 ${media.title}", color = Color.White, fontSize = 14.sp)
                                Text(media.artist, color = Color(0xFFBDBDBD), fontSize = 12.sp)
                            }
                            if (timer.running || timer.remainingMs > 0) {
                                Text(fmt(timer.remainingMs), color = Color.White, fontSize = 16.sp)
                                LinearProgressIndicator(
                                    progress = {
                                        if (timer.totalMs > 0)
                                            (timer.totalMs - timer.remainingMs).toFloat() / timer.totalMs
                                        else 0f
                                    },
                                    modifier = Modifier.fillMaxWidth().height(4.dp)
                                )
                            }
                            when (callState) {
                                AppBus.CallState.INCOMING ->
                                    Text("📞 " + getString(R.string.incoming_call),
                                        color = Color.White, fontSize = 14.sp)
                                AppBus.CallState.ENDED ->
                                    Text(getString(R.string.call_ended),
                                        color = Color.White, fontSize = 14.sp)
                                else -> {}
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (timer.running) {
                                Text(fmt(timer.remainingMs), color = Color.White, fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                            }
                            if (media.playing) Text("🎵", fontSize = 12.sp)
                            when (callState) {
                                AppBus.CallState.INCOMING -> Text("📞", fontSize = 12.sp)
                                AppBus.CallState.ACTIVE -> Text("☎", fontSize = 12.sp)
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Modifier.verticalDragToggle(): Modifier = this.then(
        Modifier.clickable(enabled = false) {}
    )

    private fun fmt(ms: Long): String {
        val totalSec = ms / 1000
        return String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
    }

    override fun onDestroy() {
        composeView?.let { runCatching { wm.removeView(it) } }
        composeView = null
        registry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }
}
