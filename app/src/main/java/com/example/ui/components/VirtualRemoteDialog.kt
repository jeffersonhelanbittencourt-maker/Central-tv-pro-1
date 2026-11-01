package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CinemaNavy
import com.example.ui.theme.CinemaSlate
import com.example.ui.theme.CinemaSurfaceElevated
import com.example.ui.theme.FocusBorderCyan
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VirtualRemoteDialog(
    onDismiss: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onNextChannel: () -> Unit,
    onPrevChannel: () -> Unit,
    onAspectRatio: () -> Unit,
    isPlaying: Boolean
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CinemaNavy),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, FocusBorderCyan),
            modifier = Modifier
                .width(320.dp)
                .testTag("virtual_remote_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Controle Remoto TV",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Android TV & Box Simulator",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_remote_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar controle",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // D-Pad Controller Circle
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(CinemaSurfaceElevated)
                        .border(2.dp, CinemaSlate, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // UP Button
                    IconButton(
                        onClick = onUp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .size(48.dp)
                            .testTag("dpad_up")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Cima",
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // DOWN Button
                    IconButton(
                        onClick = onDown,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .size(48.dp)
                            .testTag("dpad_down")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Baixo",
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // LEFT Button
                    IconButton(
                        onClick = onLeft,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                            .size(48.dp)
                            .testTag("dpad_left")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Esquerda",
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // RIGHT Button
                    IconButton(
                        onClick = onRight,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .size(48.dp)
                            .testTag("dpad_right")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Direita",
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // OK / CENTER Button
                    FilledIconButton(
                        onClick = onOk,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("dpad_ok")
                    ) {
                        Text(
                            text = "OK",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom row actions: Back, Play/Pause, Aspect Ratio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedIconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("remote_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = TextPrimary
                        )
                    }

                    FilledIconButton(
                        onClick = onPlayPause,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = NeonAmber,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("remote_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    OutlinedIconButton(
                        onClick = onAspectRatio,
                        modifier = Modifier.testTag("remote_aspect_ratio")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Proporção de Tela",
                            tint = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Channel Up / Channel Down
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedIconButton(
                        onClick = onPrevChannel,
                        modifier = Modifier.testTag("remote_ch_prev")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Canal Anterior", tint = NeonCyan)
                        }
                    }

                    Text(
                        text = "CH - / +",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedIconButton(
                        onClick = onNextChannel,
                        modifier = Modifier.testTag("remote_ch_next")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Próximo Canal", tint = NeonCyan)
                        }
                    }
                }
            }
        }
    }
}
