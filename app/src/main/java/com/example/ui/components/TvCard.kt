package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ContentType
import com.example.data.model.MediaItemEntity
import com.example.ui.theme.CinemaNavy
import com.example.ui.theme.CinemaSlate
import com.example.ui.theme.FocusBorderCyan
import com.example.ui.theme.LiveBadgeRed
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TvMediaCard(
    item: MediaItemEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 160.dp,
    cardHeight: Dp = 220.dp,
    progressFraction: Float? = null,
    testTagSuffix: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "card_scale"
    )

    Card(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .testTag("media_card_${item.id}_$testTagSuffix"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = CinemaNavy
        ),
        border = if (isFocused) {
            BorderStroke(2.5.dp, FocusBorderCyan)
        } else {
            BorderStroke(1.dp, CinemaSlate)
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 10.dp else 2.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Poster / Image / Channel Logo
            if (!item.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = if (item.type == ContentType.LIVE_TV) ContentScale.Fit else ContentScale.Crop
                )
            } else {
                // Placeholder gradient with icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(CinemaSlate, CinemaNavy)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.type) {
                            ContentType.LIVE_TV -> Icons.Default.LiveTv
                            ContentType.MOVIE -> Icons.Default.Movie
                            ContentType.SERIES -> Icons.Default.Tv
                        },
                        contentDescription = null,
                        tint = NeonCyan.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Dark gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.95f)
                            ),
                            startY = 60f
                        )
                    )
            )

            // Top Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.type == ContentType.LIVE_TV) {
                    if (item.isAvailable) {
                        Surface(
                            color = LiveBadgeRed,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AO VIVO",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = CinemaSlate,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "INDISPONÍVEL",
                                color = TextSecondary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else if (item.type == ContentType.SERIES) {
                    Surface(
                        color = NeonAmber,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (item.season != null && item.episode != null) "T${item.season} E${item.episode}" else "SÉRIE",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        color = NeonCyan,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "FILME",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.isMp4) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        color = Color(0xFFFF9800),
                        shape = RoundedCornerShape(4.dp)
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

                Spacer(modifier = Modifier.weight(1f))

                // Play icon highlight when focused
                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(FocusBorderCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproduzir",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Bottom title and info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val details = buildString {
                    append(item.groupTitle)
                    if (item.year != null) {
                        append(" • ")
                        append(item.year)
                    }
                    if (item.audioLanguage != null) {
                        append(" • ")
                        append(item.audioLanguage.uppercase())
                    }
                }

                Text(
                    text = details,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Continue watching progress bar
                if (progressFraction != null && progressFraction > 0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NeonCyan,
                        trackColor = CinemaSlate
                    )
                }
            }
        }
    }
}
