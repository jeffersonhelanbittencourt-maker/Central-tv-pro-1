package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.data.model.ContentType
import com.example.data.model.MediaItemEntity
import com.example.player.AspectRatioMode
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaNavy
import com.example.ui.theme.CinemaSlate
import com.example.ui.theme.FocusBorderCyan
import com.example.ui.theme.LiveBadgeRed
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TvViewModel
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier
) {
    val activeItem by viewModel.activeMediaItem.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
    val playbackState by viewModel.playerManager.playbackState.collectAsStateWithLifecycle()
    val currentPosition by viewModel.playerManager.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.playerManager.duration.collectAsStateWithLifecycle()
    val isLive by viewModel.playerManager.isLive.collectAsStateWithLifecycle()
    val errorMessage by viewModel.playerManager.errorMessage.collectAsStateWithLifecycle()
    val aspectMode by viewModel.playerManager.aspectRatioMode.collectAsStateWithLifecycle()
    val favorites by viewModel.allFavorites.collectAsStateWithLifecycle()
    val audioTracks by viewModel.playerManager.audioTracks.collectAsStateWithLifecycle()
    val subTracks by viewModel.playerManager.subtitleTracks.collectAsStateWithLifecycle()

    var showControls by remember { mutableStateOf(true) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }

    val isFav = remember(favorites, activeItem) {
        favorites.any { it.id == activeItem?.id }
    }

    // Auto-hide controls after 4 seconds of idle
    LaunchedEffect(showControls, isPlaying, isUserScrubbing) {
        if (showControls && isPlaying && !isUserScrubbing) {
            delay(4000)
            showControls = false
        }
    }

    // Intercept back button to save progress and pop navigation
    BackHandler {
        viewModel.saveCurrentProgress()
        viewModel.navigateBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
            .testTag("player_screen_root")
    ) {
        // Media3 ExoPlayer AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.playerManager.getPlayer()
                    useController = false
                    resizeMode = aspectMode.resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = viewModel.playerManager.getPlayer()
                playerView.resizeMode = aspectMode.resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering indicator
        if (playbackState == Player.STATE_BUFFERING && errorMessage == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .padding(20.dp)
                ) {
                    CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isLive) "Carregando transmissão ao vivo..." else "Carregando vídeo...",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Error overlay with retry
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(24.dp)
                        .background(CinemaNavy, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = LiveBadgeRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Não foi possível reproduzir",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "Falha na conexão.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.width(280.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.playerManager.retry() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("retry_stream_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tentar Novamente", fontWeight = FontWeight.Bold)
                        }

                        if (isLive) {
                            Button(
                                onClick = { viewModel.nextChannel() },
                                colors = ButtonDefaults.buttonColors(containerColor = CinemaSlate, contentColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Próximo Canal")
                            }
                        }
                    }
                }
            }
        }

        // Animated Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top gradient bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent)
                            )
                        )
                )

                // Bottom gradient bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                            )
                        )
                )

                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.saveCurrentProgress()
                                viewModel.navigateBack()
                            },
                            modifier = Modifier
                                .background(CinemaSlate.copy(alpha = 0.7f), CircleShape)
                                .testTag("player_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isLive) {
                                    Surface(
                                        color = LiveBadgeRed,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            text = "AO VIVO",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (activeItem?.isMp4 == true) {
                                    Surface(
                                        color = Color(0xFFFF9800),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            text = "MP4",
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = activeItem?.title ?: "Reproduzindo",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val detailsText = buildString {
                                append(activeItem?.groupTitle ?: "Geral")
                                activeItem?.year?.let {
                                    append(" • ")
                                    append(it)
                                }
                                activeItem?.audioLanguage?.let {
                                    append(" • Áudio: ")
                                    append(it.uppercase())
                                }
                                activeItem?.licenseStatus?.let {
                                    append(" • ")
                                    append(it)
                                }
                            }
                            Text(
                                text = detailsText,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Top Action Icons: Favorite, Aspect Ratio, Tracks
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        activeItem?.let { item ->
                            IconButton(
                                onClick = { viewModel.toggleFavorite(item) },
                                modifier = Modifier
                                    .background(CinemaSlate.copy(alpha = 0.7f), CircleShape)
                                    .testTag("player_favorite_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favoritar",
                                    tint = if (isFav) LiveBadgeRed else TextPrimary
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.playerManager.cycleAspectRatio() },
                            modifier = Modifier
                                .background(CinemaSlate.copy(alpha = 0.7f), CircleShape)
                                .testTag("player_aspect_ratio_toggle")
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Proporção de Tela", tint = NeonCyan)
                        }

                        if (audioTracks.isNotEmpty() || subTracks.isNotEmpty()) {
                            IconButton(
                                onClick = { showTrackDialog = true },
                                modifier = Modifier
                                    .background(CinemaSlate.copy(alpha = 0.7f), CircleShape)
                                    .testTag("player_tracks_toggle")
                            ) {
                                Icon(Icons.Default.ClosedCaption, contentDescription = "Áudio e Legendas", tint = TextPrimary)
                            }
                        }
                    }
                }

                // Center Playback Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLive) {
                        // Previous Channel
                        IconButton(
                            onClick = { viewModel.previousChannel() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(CinemaNavy.copy(alpha = 0.8f), CircleShape)
                                .testTag("player_prev_channel")
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Canal Anterior", tint = NeonCyan, modifier = Modifier.size(28.dp))
                        }
                    } else {
                        // Rewind 10s
                        IconButton(
                            onClick = { viewModel.playerManager.seekBackward(10000L) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(CinemaNavy.copy(alpha = 0.8f), CircleShape)
                                .testTag("player_rewind_10s")
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = "Voltar 10s", tint = TextPrimary, modifier = Modifier.size(28.dp))
                        }
                    }

                    // Main Play/Pause Button
                    FilledIconButton(
                        onClick = { viewModel.playerManager.togglePlayPause() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("player_center_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    if (isLive) {
                        // Next Channel
                        IconButton(
                            onClick = { viewModel.nextChannel() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(CinemaNavy.copy(alpha = 0.8f), CircleShape)
                                .testTag("player_next_channel")
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Próximo Canal", tint = NeonCyan, modifier = Modifier.size(28.dp))
                        }
                    } else {
                        // Forward 10s
                        IconButton(
                            onClick = { viewModel.playerManager.seekForward(10000L) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(CinemaNavy.copy(alpha = 0.8f), CircleShape)
                                .testTag("player_forward_10s")
                        ) {
                            Icon(Icons.Default.Forward10, contentDescription = "Avançar 10s", tint = TextPrimary, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                // Bottom Timeline Controls (VOD vs LIVE)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    if (isLive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(LiveBadgeRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Transmissão Ao Vivo Contínua",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Proporção: ${aspectMode.label}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        // VOD Seekbar
                        val activeDuration = duration.coerceAtLeast(1L)
                        val displayPosition = if (isUserScrubbing) scrubPosition.toLong() else currentPosition

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(displayPosition),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formatTime(activeDuration),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Slider(
                            value = (if (isUserScrubbing) scrubPosition else currentPosition.toFloat())
                                .coerceIn(0f, activeDuration.toFloat()),
                            onValueChange = { newValue ->
                                isUserScrubbing = true
                                scrubPosition = newValue
                            },
                            onValueChangeFinished = {
                                viewModel.playerManager.seekTo(scrubPosition.toLong())
                                isUserScrubbing = false
                            },
                            valueRange = 0f..activeDuration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = CinemaSlate
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_progress_slider")
                        )
                    }
                }
            }
        }
    }

    // Audio and Subtitle dialog
    if (showTrackDialog) {
        Dialog(onDismissRequest = { showTrackDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CinemaNavy),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Áudio e Legendas", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { showTrackDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Faixas de Áudio", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (audioTracks.isEmpty()) {
                        Text("Padrão do fluxo", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        LazyColumn(modifier = Modifier.height(80.dp)) {
                            items(audioTracks) { track ->
                                Text(
                                    text = track.name,
                                    color = if (track.isSelected) NeonCyan else TextPrimary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Legendas", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (subTracks.isEmpty()) {
                        Text("Nenhuma legenda embutida", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        LazyColumn(modifier = Modifier.height(80.dp)) {
                            items(subTracks) { track ->
                                Text(
                                    text = track.name,
                                    color = if (track.isSelected) NeonCyan else TextPrimary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    val remMinutes = minutes % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, remMinutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", remMinutes, seconds)
    }
}
