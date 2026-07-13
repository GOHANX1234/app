package com.sarrows.app.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sarrows.app.ui.components.*
import com.sarrows.app.ui.navigation.Routes
import com.sarrows.app.ui.theme.*
import com.sarrows.app.ui.viewmodels.BrowseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    tab: String,
    navController: NavController,
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSort by remember { mutableStateOf("latest") }
    var selectedTab  by remember { mutableStateOf(tab) }

    LaunchedEffect(selectedTab, selectedSort) {
        when (selectedTab) {
            "movies" -> viewModel.loadMovies(sort = selectedSort)
            "anime"  -> viewModel.loadAnime(sort = selectedSort)
            "series" -> viewModel.loadSeries(sort = selectedSort)
        }
    }

    val tabs   = listOf("movies", "anime", "series")
    val labels = listOf("Movies", "Anime", "Series")
    val sorts  = listOf("latest", "views", "rating")
    val sortLabels = listOf("Latest", "Popular", "Top Rated")

    Scaffold(
        bottomBar = { SarrowsBottomNavBar(navController) },
        topBar = {
            Column(modifier = Modifier.background(SarrowsDark)) {
                // Tab row
                TabRow(
                    selectedTabIndex = tabs.indexOf(selectedTab),
                    containerColor   = SarrowsDark,
                    contentColor     = SarrowsRed,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(selectedTab)]),
                            color = SarrowsRed
                        )
                    }
                ) {
                    tabs.forEachIndexed { i, t ->
                        Tab(
                            selected = selectedTab == t,
                            onClick  = { selectedTab = t },
                            text = { Text(labels[i]) }
                        )
                    }
                }
                // Sort chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sorts.forEachIndexed { i, s ->
                        FilterChip(
                            selected = selectedSort == s,
                            onClick  = { selectedSort = s },
                            label    = { Text(sortLabels[i]) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor    = SarrowsRed,
                                selectedLabelColor        = SarrowsWhite,
                                containerColor            = SarrowsDarkCard,
                                labelColor                = SarrowsWhite60
                            )
                        )
                    }
                }
            }
        },
        containerColor = SarrowsBlack
    ) { padding ->
        val items = when (selectedTab) {
            "movies" -> uiState.movies
            "anime"  -> uiState.anime
            else     -> uiState.series
        }

        if (uiState.isLoading && items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(130.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                when {
                    selectedTab == "movies" -> {
                        val movie = uiState.movies.find { it.id == (item as? com.sarrows.app.data.models.Movie)?.id }
                        if (movie != null) {
                            ContentCard(
                                title = movie.title, posterUrl = movie.posterUrl,
                                rating = movie.rating, year = movie.releaseYear,
                                onClick = { navController.navigate(Routes.detail("movie", movie.id)) }
                            )
                        }
                    }
                    else -> {
                        val s = item as? com.sarrows.app.data.models.Series
                        if (s != null) {
                            ContentCard(
                                title = s.title, posterUrl = s.posterUrl,
                                rating = s.rating, year = s.releaseYear,
                                onClick = { navController.navigate(Routes.detail("series", s.id)) }
                            )
                        }
                    }
                }
            }

            // Load more
            if (uiState.currentPage < uiState.totalPages) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        } else {
                            TextButton(onClick = {
                                val nextPage = uiState.currentPage + 1
                                when (selectedTab) {
                                    "movies" -> viewModel.loadMovies(page = nextPage, sort = selectedSort)
                                    "anime"  -> viewModel.loadAnime(page = nextPage, sort = selectedSort)
                                    "series" -> viewModel.loadSeries(page = nextPage, sort = selectedSort)
                                }
                            }) { Text("Load more", color = SarrowsRed) }
                        }
                    }
                }
            }
        }
    }
}
