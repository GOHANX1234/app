package com.sarrows.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.sarrows.app.ui.components.SarrowsBottomNavBar
import com.sarrows.app.ui.navigation.Routes
import com.sarrows.app.ui.theme.*
import com.sarrows.app.ui.viewmodels.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        bottomBar = { SarrowsBottomNavBar(navController) },
        topBar = {
            SearchBar(
                query = uiState.query,
                onQueryChange = { viewModel.onQueryChange(it) },
                onSearch = {},
                active = false,
                onActiveChange = {},
                placeholder = { Text("Search movies, anime, series…", color = SarrowsWhite60) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = SarrowsWhite60) },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Close, null, tint = SarrowsWhite60)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                colors = SearchBarDefaults.colors(
                    containerColor     = SarrowsDarkCard,
                    inputFieldColors   = TextFieldDefaults.colors(
                        focusedTextColor   = SarrowsWhite,
                        unfocusedTextColor = SarrowsWhite
                    )
                )
            ) {}
        },
        containerColor = SarrowsBlack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(SarrowsBlack)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.query.length >= 2 && uiState.movies.isEmpty() && uiState.series.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SearchOff, null, tint = SarrowsWhite38, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No results for \"${uiState.query}\"", color = SarrowsWhite60)
                    }
                }
                uiState.query.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Search, null, tint = SarrowsWhite38, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Search for movies, anime & series", color = SarrowsWhite60)
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.movies.isNotEmpty()) {
                            item {
                                Text("Movies", style = MaterialTheme.typography.titleMedium, color = SarrowsWhite)
                                Spacer(Modifier.height(8.dp))
                            }
                            items(uiState.movies) { movie ->
                                SearchResultItem(
                                    title = movie.title,
                                    subtitle = "${movie.releaseYear ?: ""} · ${movie.genres.take(2).joinToString(", ") { it.name }}",
                                    posterUrl = movie.posterUrl,
                                    rating = movie.rating,
                                    onClick = { navController.navigate(Routes.detail("movie", movie.id)) }
                                )
                            }
                        }
                        if (uiState.series.isNotEmpty()) {
                            item {
                                if (uiState.movies.isNotEmpty()) Spacer(Modifier.height(8.dp))
                                Text("Anime & Series", style = MaterialTheme.typography.titleMedium, color = SarrowsWhite)
                                Spacer(Modifier.height(8.dp))
                            }
                            items(uiState.series) { s ->
                                SearchResultItem(
                                    title = s.title,
                                    subtitle = "${s.releaseYear ?: ""} · ${s.type.replaceFirstChar { it.uppercase() }} · ${s.genres.take(2).joinToString(", ") { it.name }}",
                                    posterUrl = s.posterUrl,
                                    rating = s.rating,
                                    onClick = { navController.navigate(Routes.detail("series", s.id)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    title: String,
    subtitle: String,
    posterUrl: String?,
    rating: Double,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SarrowsDarkCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = posterUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 52.dp, height = 74.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = SarrowsWhite, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SarrowsWhite60, maxLines = 1)
                if (rating > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = SarrowsGold, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("%.1f".format(rating), style = MaterialTheme.typography.labelSmall, color = SarrowsGold)
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = SarrowsWhite38)
        }
    }
}
