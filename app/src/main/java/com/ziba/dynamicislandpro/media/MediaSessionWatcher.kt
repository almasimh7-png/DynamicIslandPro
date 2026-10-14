package com.ziba.dynamicislandpro.media

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.ziba.dynamicislandpro.core.AppBus

/**
 * Uses framework MediaSessionManager (requires NotificationListener access)
 * and MediaController callbacks to publish basic metadata / playback state
 * into AppBus for the overlay.
 */
class MediaSessionWatcher(context: Context) {

    private val appContext = context.applicationContext
    private val smm =
        appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val listenerComponent = ComponentName(appContext,
        "com.ziba.dynamicislandpro.service.IslandNotificationListener")

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        controllers?.forEach { attach(it) }
    }

    private var attached: MediaController? = null

    private val cb = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
            publish()
        }
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publish()
        }
    }

    fun start() {
        runCatching {
            smm.addOnActiveSessionsChangedListener(
                sessionsListener, listenerComponent, android.os.Process.myUserHandle()
            )
            smm.getActiveSessions(listenerComponent)?.forEach { attach(it) }
        }
    }

    private fun attach(controller: MediaController) {
        if (attached === controller) return
        attached?.unregisterCallback(cb)
        attached = controller
        controller.registerCallback(cb)
        publish()
    }

    private fun publish() {
        val c = attached ?: return
        val md = c.metadata
        val ps = c.playbackState
        AppBus.pushMedia(
            AppBus.MediaInfo(
                title = md?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "",
                artist = md?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "",
                playing = ps?.state == PlaybackState.STATE_PLAYING,
                positionMs = ps?.position ?: 0L,
                durationMs = md?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            )
        )
    }

    fun stop() {
        runCatching { smm.removeOnActiveSessionsChangedListener(sessionsListener) }
        attached?.unregisterCallback(cb)
        attached = null
    }
}
