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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.TvMediaCard
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaNavy
import com.example.ui.theme.CinemaSlate
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TvViewModel

@Composable
fun MoviesScreen(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.allMovies.collectAsStateWithLifecycle()
    val genres by viewModel.movieCategories.collectAsStateWithLifecycle()
    var selectedGenre by remember { mutableStateOf("Todos") }
    var filterText by remember { mutableStateOf("") }

    val filteredMovies = remember(movies, selectedGenre, filterText) {
        movies.filter { movie ->
            val matchGenre = selectedGenre == "Todos" || movie.groupTitle.equals(selectedGenre, ignoreCase = true)
            val matchText = filterText.isBlank() ||
                    movie.title.contains(filterText, ignoreCase = true) ||
                    movie.groupTitle.contains(filterText, ignoreCase = true)
            matchGenre && matchText
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .padding(top = 8.dp)
            .testTag("movies_screen")
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Filmes",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${filteredMovies.size} filmes no catálogo",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Search bar
        OutlinedTextField(
            value = filterText,
            onValueChange = { filterText = it },
            placeholder = { Text("Buscar filmes por título ou gênero...", color = TextSecondary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            trailingIcon = {
                if (filterText.isNotEmpty()) {
                    IconButton(onClick = { filterText = "" }) {
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
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("movie_filter_input")
        )

        // Genre Filter Chips
        val chipGenres = remember(genres) { listOf("Todos") + genres }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chipGenres) { genre ->
                val isSelected = genre == selectedGenre
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedGenre = genre },
                    label = { Text(genre, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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
                    modifier = Modifier.testTag("genre_chip_$genre")
                )
            }
        }

        // Movie Poster Grid
        if (filteredMovies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhum filme encontrado",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Adicione listas VOD ou sincronize as fontes nas configurações.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredMovies, key = { it.id }) { movie ->
                    TvMediaCard(
                        item = movie,
                        cardWidth = 145.dp,
                        cardHeight = 220.dp,
                        onClick = { viewModel.playMedia(movie) },
                        testTagSuffix = "grid_movie"
                    )
                }
            }
        }
    }
}
