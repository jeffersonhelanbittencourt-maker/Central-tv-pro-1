package com.example.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.model.ContentType
import com.example.data.model.MediaItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AspectRatioMode(val label: String, val resizeMode: Int) {
    FIT("Ajustar (Original)", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    ZOOM("Preencher Tela", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    STRETCH("Esticar", AspectRatioFrameLayout.RESIZE_MODE_FILL)
}

data class TrackInfo(
    val id: String,
    val name: String,
    val language: String?,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
class TvPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var currentItem: MediaItemEntity? = null
    private var lastRequestedPosition: Long = 0L

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isLive = MutableStateFlow(false)
    val isLive: StateFlow<Boolean> = _isLive.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<TrackInfo>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val subtitleTracks: StateFlow<List<TrackInfo>> = _subtitleTracks.asStateFlow()

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    fun getPlayer(): ExoPlayer {
        if (exoPlayer == null) {
            val player = ExoPlayer.Builder(context)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .build()

            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onPlaybackStateChanged(state: Int) {
                    _playbackState.value = state
                    when (state) {
                        Player.STATE_READY -> {
                            _errorMessage.value = null
                            val dur = player.duration
                            _duration.value = if (dur == C.TIME_UNSET || dur < 0) 0L else dur
                            _isLive.value = player.isCurrentMediaItemLive || (currentItem?.type == ContentType.LIVE_TV)
                        }
                        Player.STATE_ENDED -> {
                            _isPlaying.value = false
                        }
                        Player.STATE_IDLE -> {}
                        Player.STATE_BUFFERING -> {}
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("TvPlayerManager", "Player error: ${error.errorCodeName}", error)
                    _isPlaying.value = false
                    val friendlyMsg = when {
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                            "Falha de conexão com a transmissão. Verifique a internet e tente novamente."
                        error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ->
                            "Formato de vídeo não suportado pelo decodificador do dispositivo."
                        else -> "Transmissão temporariamente indisponível (${error.localizedMessage ?: "Erro de rede"})."
                    }
                    _errorMessage.value = friendlyMsg
                }

                override fun onTracksChanged(tracks: Tracks) {
                    updateTrackLists(tracks)
                }
            })

            exoPlayer = player
            startProgressTracking()
        }
        return exoPlayer!!
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.playbackState == Player.STATE_READY) {
                        val pos = player.currentPosition.coerceAtLeast(0L)
                        _currentPosition.value = pos
                        val dur = player.duration
                        if (dur != C.TIME_UNSET && dur > 0) {
                            _duration.value = dur
                        }
                    }
                }
                delay(500)
            }
        }
    }

    fun playMedia(item: MediaItemEntity, startPositionMs: Long = 0L) {
        currentItem = item
        lastRequestedPosition = startPositionMs
        _errorMessage.value = null
        _isLive.value = item.type == ContentType.LIVE_TV

        val player = getPlayer()
        player.stop()
        player.clearMediaItems()

        val uri = Uri.parse(item.streamUrl)
        val mediaSource = buildMediaSource(uri)

        player.setMediaSource(mediaSource)
        player.prepare()

        if (startPositionMs > 0 && item.type != ContentType.LIVE_TV) {
            player.seekTo(startPositionMs)
        }

        player.playWhenReady = true
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("CentralTVPro/1.0 (Linux; Android TV; ExoPlayer)")
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(25000)
            .setAllowCrossProtocolRedirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaItem = MediaItem.fromUri(uri)

        val urlClean = uri.toString().split("?")[0].lowercase()
        return if (urlClean.endsWith(".mp4") || urlClean.endsWith(".m4v") || urlClean.endsWith(".webm") || urlClean.endsWith(".mkv") || urlClean.endsWith(".mov")) {
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
        } else if (urlClean.endsWith(".m3u8") || urlClean.contains("/hls/") || urlClean.contains(".m3u8")) {
            HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.let {
            if (!_isLive.value) {
                it.seekTo(positionMs)
                _currentPosition.value = positionMs
            }
        }
    }

    fun seekForward(deltaMs: Long = 10000L) {
        exoPlayer?.let {
            if (!_isLive.value) {
                val newPos = (it.currentPosition + deltaMs).coerceAtMost(it.duration)
                it.seekTo(newPos)
                _currentPosition.value = newPos
            }
        }
    }

    fun seekBackward(deltaMs: Long = 10000L) {
        exoPlayer?.let {
            if (!_isLive.value) {
                val newPos = (it.currentPosition - deltaMs).coerceAtLeast(0L)
                it.seekTo(newPos)
                _currentPosition.value = newPos
            }
        }
    }

    fun cycleAspectRatio(): AspectRatioMode {
        val modes = AspectRatioMode.values()
        val nextIndex = (_aspectRatioMode.value.ordinal + 1) % modes.size
        val newMode = modes[nextIndex]
        _aspectRatioMode.value = newMode
        return newMode
    }

    fun retry() {
        currentItem?.let {
            playMedia(it, _currentPosition.value)
        }
    }

    private fun updateTrackLists(tracks: Tracks) {
        val audios = mutableListOf<TrackInfo>()
        val subs = mutableListOf<TrackInfo>()

        for (group in tracks.groups) {
            val trackType = group.type
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val isSelected = group.isTrackSelected(i)
                val trackName = format.label ?: format.language ?: "Faixa ${i + 1}"
                val track = TrackInfo(
                    id = format.id ?: "$trackType-$i",
                    name = trackName,
                    language = format.language,
                    isSelected = isSelected
                )

                if (trackType == C.TRACK_TYPE_AUDIO) {
                    audios.add(track)
                } else if (trackType == C.TRACK_TYPE_TEXT) {
                    subs.add(track)
                }
            }
        }

        _audioTracks.value = audios
        _subtitleTracks.value = subs
    }

    fun release() {
        progressJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}
