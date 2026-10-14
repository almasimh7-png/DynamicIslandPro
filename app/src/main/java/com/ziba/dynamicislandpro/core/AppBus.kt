package com.ziba.dynamicislandpro.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Simple process-wide singleton state bus shared between services and the overlay UI.
 * (Alternative to LocalBroadcastManager, which is deprecated.)
 */
object AppBus {

    enum class IslandState { COLLAPSED, EXPANDED }

    data class NotificationEvent(
        val packageName: String,
        val title: String,
        val text: String,
        val isCallStyle: Boolean
    )

    data class MediaInfo(
        val title: String = "",
        val artist: String = "",
        val playing: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L
    )

    data class TimerInfo(
        val running: Boolean = false,
        val paused: Boolean = false,
        val remainingMs: Long = 0L,
        val totalMs: Long = 0L
    )

    enum class CallState { IDLE, INCOMING, ACTIVE, ENDED }

    private val _islandState = MutableStateFlow(IslandState.COLLAPSED)
    val islandState: StateFlow<IslandState> = _islandState

    private val _notifications = MutableStateFlow<NotificationEvent?>(null)
    val notifications: StateFlow<NotificationEvent?> = _notifications

    private val _media = MutableStateFlow(MediaInfo())
    val media: StateFlow<MediaInfo> = _media

    private val _timer = MutableStateFlow(TimerInfo())
    val timer: StateFlow<TimerInfo> = _timer

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState

    fun setExpanded(expanded: Boolean) {
        _islandState.value = if (expanded) IslandState.EXPANDED else IslandState.COLLAPSED
    }

    fun toggle() { setExpanded(_islandState.value == IslandState.COLLAPSED) }

    fun pushNotification(event: NotificationEvent) { _notifications.value = event }
    fun pushMedia(info: MediaInfo) { _media.value = info }
    fun pushTimer(info: TimerInfo) { _timer.value = info }
    fun pushCallState(state: CallState) { _callState.value = state }
}
