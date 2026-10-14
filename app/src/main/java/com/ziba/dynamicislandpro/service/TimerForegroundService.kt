package com.ziba.dynamicislandpro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ziba.dynamicislandpro.R
import com.ziba.dynamicislandpro.core.AppBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground timer/stopwatch using Kotlin coroutines.
 * Start: 10-second countdown demo (change DURATION_MS for custom duration).
 * Actions: ACTION_PAUSE / ACTION_RESUME / ACTION_RESET.
 */
class TimerForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private var totalMs = DURATION_MS
    private var remainingMs = DURATION_MS
    private var paused = false
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> paused = true
            ACTION_RESUME -> paused = false
            ACTION_RESET -> reset()
            else -> startTimer()
        }
        startForegroundNotification()
        return START_STICKY
    }

    private fun startTimer() {
        if (running) { paused = false; return }
        running = true
        paused = false
        job = scope.launch {
            while (isActive && remainingMs > 0) {
                if (!paused) {
                    remainingMs -= TICK_MS
                    publish(running = true)
                    if (remainingMs <= 0) {
                        remainingMs = 0
                        publish(running = false)
                        stopSelf()
                        break
                    }
                }
                delay(TICK_MS)
            }
        }
    }

    private fun reset() {
        job?.cancel()
        running = false
        paused = false
        remainingMs = totalMs
        publish(running = false)
    }

    private fun publish(runningNow: Boolean) {
        AppBus.pushTimer(AppBus.TimerInfo(runningNow, paused, remainingMs, totalMs))
    }

    private fun startForegroundNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val id = "island_timer"
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(id, getString(R.string.notif_channel_timer),
                    NotificationManager.IMPORTANCE_LOW)
            )
        }
        val builder: NotificationCompat.Builder =
            if (Build.VERSION.SDK_INT >= 26) NotificationCompat.Builder(this, id)
            else NotificationCompat.Builder(this)
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentTitle(getString(R.string.timer_running))
            .setOngoing(true)
            .build()
        startForeground(1002, notification)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val DURATION_MS = 10_000L
        const val TICK_MS = 100L
        const val ACTION_PAUSE = "com.ziba.dynamicislandpro.TIMER_PAUSE"
        const val ACTION_RESUME = "com.ziba.dynamicislandpro.TIMER_RESUME"
        const val ACTION_RESET = "com.ziba.dynamicislandpro.TIMER_RESET"

        fun start(ctx: Context) = ctx.startForegroundService(intent(ctx))
        fun pause(ctx: Context) = ctx.startService(actionIntent(ctx, ACTION_PAUSE))
        fun reset(ctx: Context) = ctx.startService(actionIntent(ctx, ACTION_RESET))
        private fun intent(ctx: Context) = Intent(ctx, TimerForegroundService::class.java)
        private fun actionIntent(ctx: Context, action: String) =
            intent(ctx).setAction(action)
    }
}
