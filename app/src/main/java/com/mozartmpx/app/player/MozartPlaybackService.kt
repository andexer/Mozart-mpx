package com.mozartmpx.app.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mozartmpx.app.MainActivity
import com.mozartmpx.app.MozartApplication
import com.mozartmpx.app.R
import com.mozartmpx.app.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MozartPlaybackService : MediaSessionService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val engine = (application as MozartApplication).container.playbackEngine
        updateForegroundNotification(engine.playbackState.value.currentTrack, engine.playbackState.value.isPlaying)

        mediaSession = MediaSession.Builder(this, engine.mediaSessionPlayer)
            .setSessionActivity(openAppPendingIntent())
            .build()
        runCatching { onUpdateNotification(mediaSession!!, true) }

        scope.launch {
            engine.playbackState.collect { state ->
                updateForegroundNotification(state.currentTrack, state.isPlaying)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> engine().togglePlayPause()
            ACTION_NEXT -> engine().skipToNext(isManualSkip = true)
            ACTION_PREVIOUS -> engine().skipToPrevious()
            else -> super.onStartCommand(intent, flags, startId)
        }
        return START_STICKY
    }

    private fun engine() = (application as MozartApplication).container.playbackEngine

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_playback), NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun updateForegroundNotification(track: Track?, isPlaying: Boolean) {
        val title = track?.title ?: getString(R.string.app_name)
        val text = track?.let { "${it.artist} — ${it.album}" } ?: getString(R.string.notification_playing)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppPendingIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(action(ACTION_PREVIOUS, android.R.drawable.ic_media_previous, "Anterior"))
            .addAction(
                action(
                    ACTION_TOGGLE,
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (isPlaying) "Pausar" else "Reproducir"
                )
            )
            .addAction(action(ACTION_NEXT, android.R.drawable.ic_media_next, "Siguiente"))
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
    }

    private fun action(action: String, icon: Int, label: String): NotificationCompat.Action {
        val intent = Intent(this, MozartPlaybackService::class.java).setAction(action)
        val pendingIntent = PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action.Builder(icon, label, pendingIntent).build()
    }

    private fun openAppPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        scope.cancel()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL_ID = "mozart_playback"
        const val NOTIFICATION_ID = 1001
        const val ACTION_TOGGLE = "com.mozartmpx.app.action.TOGGLE"
        const val ACTION_NEXT = "com.mozartmpx.app.action.NEXT"
        const val ACTION_PREVIOUS = "com.mozartmpx.app.action.PREVIOUS"
    }
}
