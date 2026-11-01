package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.MediaItemEntity
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

@Composable
fun LiveTvScreen(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier
) {
    val allChannels by viewModel.allLiveTv.collectAsStateWithLifecycle()
    val categories by viewModel.liveCategories.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf("Todos") }
    var channelFilterText by remember { mutableStateOf("") }

    val filteredChannels = remember(allChannels, selectedCategory, channelFilterText) {
        allChannels.filter { channel ->
            val matchCategory = selectedCategory == "Todos" || channel.groupTitle.equals(selectedCategory, ignoreCase = true)
            val matchText = channelFilterText.isBlank() ||
                    channel.title.contains(channelFilterText, ignoreCase = true) ||
                    channel.groupTitle.contains(channelFilterText, ignoreCase = true)
            matchCategory && matchText
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .padding(top = 8.dp)
            .testTag("live_tv_screen")
    ) {
        // Header & Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TV ao vivo",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredChannels.size} canais disponíveis",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = { viewModel.syncAllSources() },
                modifier = Modifier
                    .background(CinemaNavy, RoundedCornerShape(8.dp))
                    .testTag("refresh_channels_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar playlist", tint = NeonCyan)
            }
        }

        // Quick Search Bar
        OutlinedTextField(
            value = channelFilterText,
            onValueChange = { channelFilterText = it },
            placeholder = { Text("Pesquisar canais por nome ou categoria...", color = TextSecondary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            trailingIcon = {
                if (channelFilterText.isNotEmpty()) {
                    IconButton(onClick = { channelFilterText = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = TextSecondary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CinemaNavy,
                unfocusedContainerColor = CinemaNavy,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CinemaSlate,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("channel_filter_input")
        )

        // Categories Chips
        val chipCategories = remember(categories) { listOf("Todos") + categories }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chipCategories) { cat ->
                val isSelected = cat == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = Color.Black,
                        containerColor = CinemaNavy,
                        labelColor = TextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = CinemaSlate,
                        selectedBorderColor = NeonCyan,
                        enabled = true,
                        selected = isSelected
                    ),
                    modifier = Modifier.testTag("category_chip_$cat")
                )
            }
        }

        // Channel Grid
        if (filteredChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhum canal encontrado",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tente outra categoria ou atualize as listas de reprodução cadastradas.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.syncAllSources() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Atualizar playlist")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(filteredChannels, key = { _, item -> item.id }) { index, channel ->
                    LiveChannelCard(
                        channelNumber = index + 1,
                        channel = channel,
                        onClick = { viewModel.playMedia(channel) }
                    )
                }
            }
        }
    }
}

@Composable
fun LiveChannelCard(
    channelNumber: Int,
    channel: MediaItemEntity,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(125.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .testTag("live_channel_${channel.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CinemaNavy),
        border = androidx.compose.foundation.BorderStroke(
            if (isFocused) 2.dp else 1.dp,
            if (isFocused) FocusBorderCyan else CinemaSlate
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Channel Number + LIVE Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%03d", channelNumber),
                    color = NeonAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (channel.isMp4) {
                        Surface(
                            color = Color(0xFFFF9800),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "MP4",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (channel.isAvailable) {
                        Surface(
                            color = LiveBadgeRed,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "AO VIVO",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = CinemaSlate,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "INDISPONÍVEL",
                                color = TextSecondary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Middle: Logo or Channel Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.title,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(2.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CinemaSlate),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = channel.groupTitle,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isFocused) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Sintonizar",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
