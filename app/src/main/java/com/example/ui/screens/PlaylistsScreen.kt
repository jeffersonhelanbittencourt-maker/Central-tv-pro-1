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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PlaylistSourceEntity
import com.example.data.model.SyncStats
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaylistsScreen(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier
) {
    val sources by viewModel.allSources.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncStatusMsg by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
    val lastSyncStats by viewModel.lastSyncStats.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var sourcePendingDelete by remember { mutableStateOf<PlaylistSourceEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .padding(top = 8.dp)
            .testTag("playlists_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lista de reprodução",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${sources.size} listas cadastradas",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.syncAllSources() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    enabled = !isSyncing,
                    modifier = Modifier.testTag("sync_all_sources_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Atualizar playlist", fontSize = 12.sp)
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("add_source_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adicionar playlist", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Detailed Sync Metrics Card
        val stats = lastSyncStats
        if (stats != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CinemaSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Resultado da Atualização",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total no Catálogo: ${stats.totalChannelsAfterSync}",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Já existiam:", color = TextSecondary, fontSize = 11.sp)
                            Text("${stats.existingChannelsBeforeSync}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Adicionados:", color = TextSecondary, fontSize = 11.sp)
                            Text("+${stats.addedChannels}", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Atualizados:", color = TextSecondary, fontSize = 11.sp)
                            Text("${stats.updatedChannels}", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Duplicados:", color = TextSecondary, fontSize = 11.sp)
                            Text("${stats.duplicateChannels}", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Status banner
        if (syncStatusMsg != null) {
            Surface(
                color = CinemaNavy,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = syncStatusMsg!!,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sources, key = { it.id }) { source ->
                PlaylistSourceCard(
                    source = source,
                    onSync = { viewModel.syncSingleSource(source) },
                    onDelete = { sourcePendingDelete = source }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (sourcePendingDelete != null) {
        AlertDialog(
            onDismissRequest = { sourcePendingDelete = null },
            title = { Text("Excluir lista de reprodução", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Deseja excluir esta lista de reprodução?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        sourcePendingDelete?.let { viewModel.deleteSource(it.id) }
                        sourcePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveBadgeRed, contentColor = Color.White)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { sourcePendingDelete = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = CinemaNavy,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Add source dialog
    if (showAddDialog) {
        AddSourceDialog(
            onDismiss = { showAddDialog = false },
            onAddUrl = { name, url ->
                viewModel.addCustomSource(name, url)
                showAddDialog = false
            },
            onAddRaw = { name, rawText ->
                viewModel.importRawM3u(name, rawText)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun PlaylistSourceCard(
    source: PlaylistSourceEntity,
    onSync: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CinemaNavy),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("source_card_${source.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val statusColor = when (source.status) {
                        "OK" -> NeonCyan
                        "ERROR" -> LiveBadgeRed
                        else -> NeonAmber
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = source.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSync,
                        modifier = Modifier.size(32.dp).testTag("sync_source_${source.id}")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar playlist", tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }

                    if (!source.isPreloaded) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp).testTag("delete_source_${source.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = source.url,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${source.itemCount} canais / títulos",
                    color = NeonAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                val dateStr = if (source.lastUpdated > 0) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
                    sdf.format(Date(source.lastUpdated))
                } else {
                    "Aguardando sincronização"
                }

                Text(
                    text = "Atualizado: $dateStr",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            if (source.status == "ERROR" && !source.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Erro: ${source.errorMessage}",
                    color = LiveBadgeRed,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AddSourceDialog(
    onDismiss: () -> Unit,
    onAddUrl: (String, String) -> Unit,
    onAddRaw: (String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var nameText by remember { mutableStateOf("") }
    var urlText by remember { mutableStateOf("") }
    var rawM3uText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CinemaNavy),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, FocusBorderCyan),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_source_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Adicionar playlist",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CinemaSlate,
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonCyan
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("URL Web", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Texto / Arquivo", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Nome da playlist", color = TextSecondary) },
                    placeholder = { Text("Ex: Meus Canais Favoritos", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CinemaSlate,
                        unfocusedContainerColor = CinemaSlate,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("source_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        label = { Text("URL da lista de reprodução (M3U / M3U8)", color = TextSecondary) },
                        placeholder = { Text("https://servidor.com/lista.m3u", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CinemaSlate,
                            unfocusedContainerColor = CinemaSlate,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("source_url_input")
                    )
                } else {
                    OutlinedTextField(
                        value = rawM3uText,
                        onValueChange = { rawM3uText = it },
                        label = { Text("Conteúdo da lista (M3U / M3U8)", color = TextSecondary) },
                        placeholder = { Text("#EXTM3U\n#EXTINF:-1,Canal...\nhttp://...", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CinemaSlate,
                            unfocusedContainerColor = CinemaSlate,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("source_raw_input")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (selectedTab == 0) {
                                if (urlText.isNotBlank()) {
                                    onAddUrl(nameText.ifBlank { "Lista de reprodução" }, urlText.trim())
                                }
                            } else {
                                if (rawM3uText.isNotBlank()) {
                                    onAddRaw(nameText.ifBlank { "Lista importada" }, rawM3uText.trim())
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        enabled = if (selectedTab == 0) urlText.isNotBlank() else rawM3uText.isNotBlank(),
                        modifier = Modifier.testTag("confirm_add_source_button")
                    ) {
                        Text("Importar playlist", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
