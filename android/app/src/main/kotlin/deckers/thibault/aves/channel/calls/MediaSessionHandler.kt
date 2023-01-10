package deckers.thibault.aves.channel.calls

import android.content.ComponentName
import android.content.Context
import android.media.session.PlaybackState
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import deckers.thibault.aves.channel.calls.Coresult.Companion.safe
import deckers.thibault.aves.channel.calls.Coresult.Companion.safeSuspend
import deckers.thibault.aves.channel.streams.MediaCommandStreamHandler
import deckers.thibault.aves.utils.FlutterUtils
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MediaSessionHandler(private val context: Context, private val mediaCommandHandler: MediaCommandStreamHandler) : MethodCallHandler {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var session: MediaSessionCompat? = null

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "update" -> ioScope.launch { safeSuspend(call, result, ::update) }
            "release" -> ioScope.launch { safe(call, result, ::release) }
            else -> result.notImplemented()
        }
    }

    private suspend fun update(call: MethodCall, result: MethodChannel.Result) {
        val uri = call.argument<String>("uri")?.let { Uri.parse(it) }
        val title = call.argument<String>("title")
        val durationMillis = call.argument<Number>("durationMillis")?.toLong()
        val stateString = call.argument<String>("state")
        val positionMillis = call.argument<Number>("positionMillis")?.toLong()
        val playbackSpeed = call.argument<Number>("playbackSpeed")?.toFloat()

        if (uri == null || title == null || durationMillis == null || stateString == null || positionMillis == null || playbackSpeed == null) {
            result.error("update-args", "missing arguments", null)
            return
        }

        val state = when (stateString) {
            STATE_STOPPED -> PlaybackStateCompat.STATE_STOPPED
            STATE_PAUSED -> PlaybackStateCompat.STATE_PAUSED
            STATE_PLAYING -> PlaybackStateCompat.STATE_PLAYING
            else -> {
                result.error("update-state", "unknown state=$stateString", null)
                return
            }
        }

        var actions = PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SEEK_TO
        actions = if (state == PlaybackState.STATE_PLAYING) {
            actions or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP
        } else {
            actions or PlaybackStateCompat.ACTION_PLAY
        }

        val playbackState = PlaybackStateCompat.Builder()
            .setState(
                state,
                positionMillis,
                playbackSpeed,
                System.currentTimeMillis()
            )
            .setActions(actions)
            .build()

        FlutterUtils.runOnUiThread {
            if (session == null) {
                val mbrIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE)
                val mbrName = ComponentName(context, MediaButtonReceiver::class.java)
                session = MediaSessionCompat(context, "aves", mbrName, mbrIntent).apply {
                    setCallback(mediaCommandHandler)
                }
            }
            session!!.apply {
                val metadata = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMillis)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, uri.toString())
                    .build()
                setMetadata(metadata)
                setPlaybackState(playbackState)
                if (!isActive) {
                    isActive = true
                }
            }
        }

        result.success(null)
    }

    private fun release(@Suppress("unused_parameter") call: MethodCall, result: MethodChannel.Result) {
        session?.let {
            it.release()
            session = null
        }
        result.success(null)
    }

    companion object {
        const val CHANNEL = "deckers.thibault/aves/media_session"

        const val STATE_STOPPED = "stopped"
        const val STATE_PAUSED = "paused"
        const val STATE_PLAYING = "playing"
    }
}