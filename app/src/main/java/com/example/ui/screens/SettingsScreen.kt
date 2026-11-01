package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.player.AspectRatioMode
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaNavy
import com.example.ui.theme.CinemaSlate
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ScreenDestination
import com.example.ui.viewmodel.TvViewModel

@Composable
fun SettingsScreen(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier
) {
    val isTvMode by viewModel.isTvLayoutMode.collectAsStateWithLifecycle()
    val aspectMode by viewModel.playerManager.aspectRatioMode.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configurações",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Personalize a reprodução, navegação e fontes",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Section: TV & Navegação
        item {
            SettingsCategoryHeader(title = "Interface & Android TV")
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CinemaNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // TV Layout Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Modo Android TV / Google TV", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Adapta margens seguras e foco ampliado para TV Boxes e telas grandes", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = isTvMode,
                            onCheckedChange = { viewModel.toggleTvLayoutMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = CinemaSlate
                            ),
                            modifier = Modifier.testTag("tv_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Open Virtual Remote Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.SettingsRemote, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Controle Remoto Virtual", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Simule D-Pad de TV com setas, OK, voltar e canais", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = { viewModel.toggleVirtualRemote() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("open_virtual_remote_button")
                        ) {
                            Text("Abrir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section: Reprodução
        item {
            SettingsCategoryHeader(title = "Player de Vídeo (Media3 ExoPlayer)")
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CinemaNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.AspectRatio, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Proporção de Tela Padrão", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(aspectMode.label, color = NeonCyan, fontSize = 12.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.playerManager.cycleAspectRatio() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("change_aspect_ratio_button")
                        ) {
                            Text("Alternar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section: Listas e Dados
        item {
            SettingsCategoryHeader(title = "Gerenciamento de Dados")
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CinemaNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Listas M3U & Fontes", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Adicionar novas URLs, sincronizar e gerenciar", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = { viewModel.navigateTo(ScreenDestination.PLAYLISTS) },
                            colors = ButtonDefaults.buttonColors(containerColor = CinemaSlate, contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Gerenciar", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Limpar Histórico de Reprodução", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Remove posições salvas e registros recentes", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearAllHistory() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Limpar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section: Sobre o Projeto
        item {
            SettingsCategoryHeader(title = "Sobre o Aplicativo")
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CinemaNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Central de TV Pro Ultimate v1.0", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Arquitetura Nativa: Kotlin, Jetpack Compose, Room Database e AndroidX Media3 ExoPlayer. Desenvolvido para máxima compatibilidade em Celulares Android (Touch & Auto-Rotação), Android TV, Google TV, Chromecast com Google TV e TV Box.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Aviso legal: O aplicativo suporta exclusivamente transmissões abertas de TV, fontes comunitárias autorizadas (IPTV-org) e filmes sob domínio público ou licença aberta Creative Commons.",
                        color = NeonAmber.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        color = NeonCyan,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
