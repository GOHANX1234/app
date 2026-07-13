package com.sarrows.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sarrows.app.ui.components.*
import com.sarrows.app.ui.navigation.Routes
import com.sarrows.app.ui.theme.SarrowsBlack
import com.sarrows.app.ui.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onNavigateLogin: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = { SarrowsBottomNavBar(navController) },
        containerColor = SarrowsBlack
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        if (uiState.error != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.error ?: "Error loading content", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.load() }) { Text("Retry") }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SarrowsBlack),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Hero featured content
            if (uiState.trendingMovies.isNotEmpty()) {
                item {
                    val featured = uiState.trendingMovies.first()
                    WideContentCard(
                        title = featured.title,
                        bannerUrl = featured.bannerUrl,
                        posterUrl = featured.posterUrl,
                        rating = featured.rating,
                        year = featured.releaseYear,
                        genres = featured.genres.map { it.name }.take(3),
                        onClick = { navController.navigate(Routes.detail("movie", featured.id)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Trending Movies
            if (uiState.trendingMovies.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Trending Movies",
                        onSeeAllClick = { navController.navigate(Routes.browse("movies")) }
                    )
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.trendingMovies.take(10).forEach { movie ->
                            ContentCard(
                                title = movie.title,
                                posterUrl = movie.posterUrl,
                                rating = movie.rating,
                                year = movie.releaseYear,
                                onClick = { navController.navigate(Routes.detail("movie", movie.id)) }
                            )
                        }
                    }
                }
            }

            // Latest Movies
            if (uiState.latestMovies.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    SectionHeader(title = "New Releases")
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.latestMovies.take(10).forEach { movie ->
                            ContentCard(
                                title = movie.title,
                                posterUrl = movie.posterUrl,
                                rating = movie.rating,
                                year = movie.releaseYear,
                                onClick = { navController.navigate(Routes.detail("movie", movie.id)) }
                            )
                        }
                    }
                }
            }

            // Trending Anime
            if (uiState.trendingAnime.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    SectionHeader(
                        title = "Popular Anime",
                        onSeeAllClick = { navController.navigate(Routes.browse("anime")) }
                    )
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.trendingAnime.take(10).forEach { s ->
                            ContentCard(
                                title = s.title,
                                posterUrl = s.posterUrl,
                                rating = s.rating,
                                year = s.releaseYear,
                                onClick = { navController.navigate(Routes.detail("series", s.id)) }
                            )
                        }
                    }
                }
            }

            // Ongoing Anime
            if (uiState.ongoingAnime.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    SectionHeader(title = "Currently Airing")
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.ongoingAnime.take(10).forEach { s ->
                            ContentCard(
                                title = s.title,
                                posterUrl = s.posterUrl,
                                rating = s.rating,
                                year = s.releaseYear,
                                onClick = { navController.navigate(Routes.detail("series", s.id)) }
                            )
                        }
                    }
                }
            }

            // Trending Series
            if (uiState.trendingSeries.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    SectionHeader(
                        title = "Popular Series",
                        onSeeAllClick = { navController.navigate(Routes.browse("series")) }
                    )
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.trendingSeries.take(10).forEach { s ->
                            ContentCard(
                                title = s.title,
                                posterUrl = s.posterUrl,
                                rating = s.rating,
                                year = s.releaseYear,
                                onClick = { navController.navigate(Routes.detail("series", s.id)) }
                            )
                        }
                    }
                }
            }

            // Top Rated
            if (uiState.topRatedMovies.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    SectionHeader(title = "Top Rated Movies")
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.topRatedMovies.take(10).forEach { movie ->
                            ContentCard(
                                title = movie.title,
                                posterUrl = movie.posterUrl,
                                rating = movie.rating,
                                year = movie.releaseYear,
                                onClick = { navController.navigate(Routes.detail("movie", movie.id)) }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
