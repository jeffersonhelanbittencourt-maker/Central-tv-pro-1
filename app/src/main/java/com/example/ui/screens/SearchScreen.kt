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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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
import com.example.data.model.ContentType
import com.example.ui.components.TvMediaCard
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaNavy
import com.example.ui.theme.CinemaSlate
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TvViewModel

@Composable
fun SearchScreen(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val rawResults by viewModel.searchResults.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("Tudo") }

    val filteredResults = remember(rawResults, selectedFilter) {
        when (selectedFilter) {
            "TV ao vivo" -> rawResults.filter { it.type == ContentType.LIVE_TV }
            "Filmes" -> rawResults.filter { it.type == ContentType.MOVIE }
            "Séries" -> rawResults.filter { it.type == ContentType.SERIES }
            else -> rawResults
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .padding(top = 8.dp)
            .testTag("search_screen")
    ) {
        // Search Input
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Pesquisar canais, filmes, séries, gêneros...", color = TextSecondary) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
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
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("search_input_field")
        )

        // Type Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Tudo", "TV ao vivo", "Filmes", "Séries").forEach { tab ->
                val isSelected = tab == selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = tab },
                    label = { Text(tab, fontSize = 12.sp) },
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
                    )
                )
            }
        }

        Text(
            text = if (query.isBlank()) "Destaques e sugestões (${filteredResults.size})" else "Resultados para \"$query\" (${filteredResults.size})",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (filteredResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
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
                        text = "Verifique a digitação ou adicione mais listas nas Listas de reprodução.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 145.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredResults, key = { it.id }) { item ->
                    TvMediaCard(
                        item = item,
                        cardWidth = 145.dp,
                        cardHeight = 220.dp,
                        onClick = { viewModel.playMedia(item) },
                        testTagSuffix = "search_grid"
                    )
                }
            }
        }
    }
}
