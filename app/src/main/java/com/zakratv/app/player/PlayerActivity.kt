package com.zakratv.app.player

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.zakratv.app.data.remote.HttpClientFactory

/**
 * Stable ExoPlayer for Fire Stick / Android TV + Real-Debrid direct links.
 * - Keep screen on
 * - Auto-resume after rebuffer when user did not pause
 * - Spanish error message instead of silent freeze
 */
@UnstableApi
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var errorView: TextView? = null
    private var userPaused: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Whole body guarded: a movie must NEVER close the whole app. On any failure we
        // show a Spanish message inside the player instead of crashing the process.
        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            val root = FrameLayout(this).apply {
                setBackgroundColor(Color.BLACK)
                keepScreenOn = true
            }

            playerView = PlayerView(this).apply {
                keepScreenOn = true
                useController = true
                controllerShowTimeoutMs = 4000
                controllerHideOnTouch = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                setBackgroundColor(Color.BLACK)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
            }

            errorView = TextView(this).apply {
                visibility = View.GONE
                setTextColor(Color.WHITE)
                setBackgroundColor(0xCC000000.toInt())
                setPadding(48, 32, 48, 32)
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                )
            }

            root.addView(playerView)
            root.addView(errorView)
            setContentView(root)

            val url = intent.getStringExtra(EXTRA_URL).orEmpty()
            val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
            if (url.isBlank()) {
                showError("No hay enlace de reproducción.")
                return
            }
            if (!url.startsWith("http", ignoreCase = true)) {
                showError("Este enlace no es reproducible directo. Vuelve atrás y elige otro (⚡ RD).")
                return
            }
            initPlayer(url, title)
        } catch (t: Throwable) {
            runCatching {
                showError("No se pudo abrir el reproductor.\n(${t.javaClass.simpleName}: ${t.message})\nVuelve atrás e inténtalo con otro enlace.")
            }
        }
    }

    private fun initPlayer(url: String, title: String) {
      try {
        Log.i(TAG, "initPlayer len=${url.length} title=$title")
        val httpFactory = OkHttpDataSource.Factory(HttpClientFactory.mediaClient)
            .setUserAgent("ZakraTV/1.5 (Android TV; ExoPlayer)")

        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)

        // Buffer capped by BYTES to stay safe on low-RAM Fire Sticks (largeHeap=false).
        // Time-based 180s of 4K could OOM the process; a byte cap prevents the crash.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs */ 25_000,
                /* maxBufferMs */ 60_000,
                /* bufferForPlaybackMs */ 2_500,
                /* bufferForPlaybackAfterRebufferMs */ 5_000,
            )
            .setTargetBufferBytes(48 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()

        val trackSelector = DefaultTrackSelector(this).apply {
            // Prefer Spanish audio (Latino) across the common ISO codes for multi-audio files.
            parameters = buildUponParameters()
                .setForceHighestSupportedBitrate(false)
                .setMaxVideoSize(3840, 2160)
                .setPreferredAudioLanguages("es", "spa", "es-419", "es-MX", "lat")
                .setPreferredTextLanguages("es", "spa", "es-419")
                .build()
        }

        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)
            .setEnableAudioFloatOutput(false)

        player = ExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .also { exo ->
                playerView?.player = exo
                exo.setMediaItem(
                    MediaItem.Builder()
                        .setUri(url)
                        .setMediaId(url)
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(title.ifBlank { "Zakra TV" })
                                .build(),
                        )
                        .build(),
                )
                exo.playWhenReady = true
                exo.prepare()
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.i(TAG, "state=$playbackState (1=IDLE 2=BUFFERING 3=READY 4=ENDED)")
                        when (playbackState) {
                            Player.STATE_READY -> {
                                hideError()
                                // Resume after rebuffer if user didn't pause
                                if (!userPaused && !exo.playWhenReady) {
                                    exo.playWhenReady = true
                                }
                                if (!userPaused) {
                                    exo.play()
                                }
                            }
                            Player.STATE_BUFFERING -> {
                                // keep playWhenReady so playback continues after buffer fills
                                if (!userPaused) {
                                    exo.playWhenReady = true
                                }
                            }
                            Player.STATE_ENDED -> {
                                // stay ended; user can seek back
                            }
                            Player.STATE_IDLE -> {
                                if (!userPaused) {
                                    // try recover from idle stalls
                                    runCatching {
                                        exo.prepare()
                                        exo.playWhenReady = true
                                    }
                                }
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.w(TAG, "playerError code=${error.errorCode} ${error.errorCodeName}", error)
                        val msg = when (error.errorCode) {
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                            -> "Error de red. Comprueba el Wi‑Fi e inténtalo de nuevo (OK = reintentar)."
                            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                            -> "El enlace ya no está disponible. Vuelve atrás y elige otro."
                            PlaybackException.ERROR_CODE_DECODING_FAILED,
                            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                            -> "Este dispositivo no puede decodificar el vídeo. Prueba otra calidad."
                            else -> "Error al reproducir. Pulsa OK para reintentar o atrás para elegir otro enlace."
                        }
                        showError(msg)
                        // One automatic recover attempt for transient IO
                        if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                        ) {
                            runCatching {
                                exo.prepare()
                                if (!userPaused) exo.playWhenReady = true
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        Log.i(TAG, "isPlaying=$isPlaying")
                        if (isPlaying) hideError()
                    }
                })
            }
      } catch (t: Throwable) {
          Log.e(TAG, "initPlayer failed", t)
          showError("No se pudo iniciar el vídeo.\n(${t.javaClass.simpleName}: ${t.message})\nVuelve atrás y prueba otro enlace (⚡ RD).")
      }
    }

    private fun showError(message: String) {
        errorView?.text = message
        errorView?.visibility = View.VISIBLE
        playerView?.showController()
    }

    private fun hideError() {
        errorView?.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (!userPaused) {
            player?.playWhenReady = true
            player?.play()
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (!userPaused) {
            player?.playWhenReady = true
        }
    }

    override fun onPause() {
        // Do not force-pause on brief lifecycle blips if still resumed soon;
        // onStop will handle leaving the activity.
        super.onPause()
    }

    override fun onStop() {
        // Pause only when leaving the screen so we don't burn bandwidth
        player?.playWhenReady = false
        super.onStop()
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    private fun releasePlayer() {
        playerView?.player = null
        player?.release()
        player = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val p = player ?: return super.onKeyDown(keyCode, event)
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            -> {
                if (errorView?.visibility == View.VISIBLE) {
                    hideError()
                    userPaused = false
                    p.prepare()
                    p.playWhenReady = true
                    p.play()
                    true
                } else {
                    if (p.isPlaying) {
                        userPaused = true
                        p.pause()
                    } else {
                        userPaused = false
                        p.playWhenReady = true
                        p.play()
                    }
                    playerView?.showController()
                    true
                }
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                userPaused = false
                p.playWhenReady = true
                p.play()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                userPaused = true
                p.pause()
                true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                p.seekTo((p.currentPosition + 10_000).coerceAtMost(p.duration.coerceAtLeast(0)))
                true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                p.seekTo((p.currentPosition - 10_000).coerceAtLeast(0))
                true
            }
            // Don't steal DPAD left/right for seek when controller is for navigation —
            // only media FF/RW. Prevents accidental pause/seek storms.
            KeyEvent.KEYCODE_BACK -> {
                if (playerView?.isControllerFullyVisible == true) {
                    playerView?.hideController()
                    true
                } else {
                    finish()
                    true
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    companion object {
        private const val TAG = "ZakraPlayer"
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_QUALITY = "quality"
    }
}
