package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.MediaItemEntity
import com.example.ui.components.TvMediaCard
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
fun HomeScreen(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier
) {
    val featured by viewModel.featuredMedia.collectAsStateWithLifecycle()
    val liveChannels by viewModel.allLiveTv.collectAsStateWithLifecycle()
    val movies by viewModel.allMovies.collectAsStateWithLifecycle()
    val series by viewModel.allSeries.collectAsStateWithLifecycle()
    val favorites by viewModel.allFavorites.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val statusMsg by viewModel.syncStatusMessage.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Hero Banner
        item {
            HeroBannerSection(
                onWatchLive = {
                    val firstLive = liveChannels.firstOrNull() ?: featured.firstOrNull()
                    firstLive?.let { viewModel.playMedia(it) }
                },
                onExploreMovies = {
                    viewModel.navigateTo(ScreenDestination.MOVIES)
                },
                onOpenRemote = {
                    viewModel.toggleVirtualRemote()
                }
            )
        }

        // Sync Status Bar
        if (statusMsg != null || isSyncing) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .background(CinemaNavy, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusMsg ?: "Sincronizando...",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section: Assistidos Recentemente / Continuar Assistindo
        if (continueWatching.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Assistidos recentemente",
                    subtitle = "Retome de onde você parou",
                    actionText = "Ver Histórico",
                    onAction = { viewModel.navigateTo(ScreenDestination.HISTORY) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(continueWatching, key = { it.mediaId }) { historyItem ->
                        val dummyMedia = MediaItemEntity(
                            id = historyItem.mediaId,
                            sourceId = "history",
                            title = historyItem.title,
                            streamUrl = historyItem.streamUrl,
                            logoUrl = historyItem.logoUrl,
                            groupTitle = historyItem.groupTitle,
                            contentType = historyItem.contentType
                        )
                        TvMediaCard(
                            item = dummyMedia,
                            progressFraction = historyItem.progressFraction,
                            onClick = { viewModel.playFromHistory(historyItem) },
                            testTagSuffix = "continue"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Section: TV Ao Vivo
        if (liveChannels.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "TV ao vivo em destaque",
                    subtitle = "Transmissões e canais em tempo real",
                    actionText = "Ver Todos (${liveChannels.size})",
                    onAction = { viewModel.navigateTo(ScreenDestination.LIVE_TV) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(liveChannels.take(15), key = { it.id }) { channel ->
                        TvMediaCard(
                            item = channel,
                            onClick = { viewModel.playMedia(channel) },
                            testTagSuffix = "live"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Section: Filmes
        if (movies.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Filmes Selecionados",
                    subtitle = "Longas-metragens e clássicos em alta qualidade",
                    actionText = "Ver Catálogo (${movies.size})",
                    onAction = { viewModel.navigateTo(ScreenDestination.MOVIES) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(movies.take(15), key = { it.id }) { movie ->
                        TvMediaCard(
                            item = movie,
                            onClick = { viewModel.playMedia(movie) },
                            testTagSuffix = "movie"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Section: Séries
        if (series.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Séries e Temporadas",
                    subtitle = "Episódios e produções completas",
                    actionText = "Ver Todas (${series.size})",
                    onAction = { viewModel.navigateTo(ScreenDestination.SERIES) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(series.take(15), key = { it.id }) { item ->
                        TvMediaCard(
                            item = item,
                            onClick = { viewModel.playMedia(item) },
                            testTagSuffix = "series"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Section: Favoritos
        if (favorites.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Favoritos",
                    subtitle = "Canais e títulos salvos",
                    actionText = "Ver Favoritos",
                    onAction = { viewModel.navigateTo(ScreenDestination.FAVORITES) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(favorites, key = { it.id }) { item ->
                        TvMediaCard(
                            item = item,
                            onClick = { viewModel.playMedia(item) },
                            testTagSuffix = "favorite"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
fun HeroBannerSection(
    onWatchLive: () -> Unit,
    onExploreMovies: () -> Unit,
    onOpenRemote: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(16.dp)
            .testTag("hero_banner_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CinemaNavy)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            Image(
                painter = painterResource(id = R.drawable.hero_streaming_banner),
                contentDescription = "Banner Central de TV Pro Ultimate",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Obsidian Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                CinemaBlack.copy(alpha = 0.95f),
                                CinemaBlack.copy(alpha = 0.75f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(NeonCyan, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "PRO ULTIMATE",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Android TV • Google TV • Mobile",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Central de TV Pro",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "TV Ao Vivo, Filmes, Séries e Listas M3U em uma interface cinematográfica rápida.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onWatchLive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("hero_watch_live_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Assistir TV", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onExploreMovies,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("hero_movies_button")
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Filmes", fontSize = 13.sp)
                    }

                    IconButton(
                        onClick = onOpenRemote,
                        modifier = Modifier
                            .background(CinemaSlate, RoundedCornerShape(10.dp))
                            .testTag("hero_remote_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsRemote,
                            contentDescription = "Controle Virtual",
                            tint = NeonAmber
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        if (actionText != null && onAction != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("section_action_${title.lowercase().replace(" ", "_")}")
            ) {
                Text(
                    text = actionText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
