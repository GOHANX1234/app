package com.sarrows.app.ui.screens.detail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.sarrows.app.data.models.*
import com.sarrows.app.ui.components.*
import com.sarrows.app.ui.player.PlayerActivity
import com.sarrows.app.ui.theme.*
import com.sarrows.app.ui.viewmodels.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    contentType: String,
    contentId: String,
    navController: NavController,
    detailViewModel: DetailViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by detailViewModel.uiState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current

    var showReviewSheet by remember { mutableStateOf(false) }
    var reviewRating by remember { mutableStateOf(7) }
    var reviewComment by remember { mutableStateOf("") }
    var expandDescription by remember { mutableStateOf(false) }

    LaunchedEffect(contentId, contentType) {
        when (contentType) {
            "movie" -> detailViewModel.loadMovie(contentId, currentUser?.id)
            // No dedicated /api/anime/:id endpoint â€” look up the full Series object via browse API.
            else    -> detailViewModel.loadSeriesById(contentId, currentUser?.id)
        }
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize().background(SarrowsBlack), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SarrowsRed)
        }
        return
    }

    val movie  = uiState.movie
    val series = uiState.series
    val title  = movie?.title ?: series?.title ?: "Content"
    val banner = movie?.bannerUrl ?: series?.bannerUrl
    val poster = movie?.posterUrl ?: series?.posterUrl
    val desc   = movie?.description ?: series?.description ?: ""
    val rating = movie?.rating ?: series?.rating ?: 0.0
    val year   = movie?.releaseYear ?: series?.releaseYear
    val genres = movie?.genres ?: series?.genres ?: emptyList()
    val cast   = movie?.cast ?: series?.cast ?: emptyList()
    val targetType = if (movie != null) "Movie" else "Series"
    val targetId   = movie?.id ?: series?.id ?: contentId

    LaunchedEffect(targetId) {
        detailViewModel.recordView(targetType, targetId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = SarrowsWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = SarrowsBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SarrowsBlack),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Banner / Hero
            item {
                Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    AsyncImage(
                        model = banner ?: poster,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, SarrowsBlack),
                                startY = 0.3f * 260 * 3.0f
                            )
                        )
                    )
                    // Play button overlay
                    Box(
                        modifier = Modifier.align(Alignment.Center).background(
                            SarrowsRed.copy(alpha = 0.9f), CircleShape
                        ).clip(CircleShape)
                    ) {
                        IconButton(
                            onClick = {
                                val intent = Intent(context, PlayerActivity::class.java).apply {
                                    putExtra("contentType", if (movie != null) "movie" else "series")
                                    putExtra("contentId", targetId)
                                    putExtra("title", title)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, "Play", tint = SarrowsWhite, modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }

            // Title & Meta
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, color = SarrowsWhite)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (year != null) Text("$year", color = SarrowsWhite60, style = MaterialTheme.typography.bodySmall)
                        if (rating > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = SarrowsGold, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("%.1f".format(rating), color = SarrowsGold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        movie?.duration?.let { Text("${it}m", color = SarrowsWhite60, style = MaterialTheme.typography.bodySmall) }
                        series?.let { StatusBadge(it.status) }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Genre chips
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        genres.forEach { GenreBadge(it.name) }
                    }
                }
            }

            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Play
                    Button(
                        onClick = {
                            val intent = Intent(context, PlayerActivity::class.java).apply {
                                putExtra("contentType", if (movie != null) "movie" else "series")
                                putExtra("contentId", targetId)
                                putExtra("title", title)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SarrowsRed)
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Watch")
                    }
                    // Watchlist
                    OutlinedButton(
                        onClick = {
                            if (currentUser != null) detailViewModel.toggleWatchlist(targetType, targetId)
                        },
                        modifier = Modifier.height(48.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(
                                if (uiState.inWatchlist) SarrowsRed else SarrowsDarkBorder,
                                if (uiState.inWatchlist) SarrowsRed else SarrowsDarkBorder
                            ))
                        )
                    ) {
                        Icon(
                            if (uiState.inWatchlist) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                            null,
                            tint = if (uiState.inWatchlist) SarrowsRed else SarrowsWhite60
                        )
                    }
                    // Review
                    if (currentUser != null) {
                        OutlinedButton(
                            onClick = {
                                uiState.userReview?.let {
                                    reviewRating  = it.rating
                                    reviewComment = it.comment ?: ""
                                }
                                showReviewSheet = true
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.RateReview, null, tint = SarrowsWhite60)
                        }
                    }
                }
            }

            // Description
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = if (expandDescription) desc else desc.take(200) + if (desc.length > 200) "â€¦" else "",
                        style = MaterialTheme.typography.bodyMedium, color = SarrowsWhite87
                    )
                    if (desc.length > 200) {
                        TextButton(onClick = { expandDescription = !expandDescription }) {
                            Text(if (expandDescription) "Less" else "More", color = SarrowsRed)
                        }
                    }
                }
            }

            // Cast
            if (cast.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(title = "Cast")
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        cast.take(8).forEach { member ->
                            CastCard(member)
                        }
                    }
                }
            }

            // Episodes (for series)
            series?.episodes?.let { episodes ->
                if (episodes.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionHeader(title = "Episodes")
                    }
                    items(episodes.take(20)) { ep ->
                        EpisodeItem(ep = ep, onPlay = {
                            val intent = Intent(context, PlayerActivity::class.java).apply {
                                putExtra("contentType", "episode")
                                putExtra("contentId", ep.id)
                                putExtra("title", ep.title.ifEmpty { "S${ep.season} E${ep.episodeNumber}" })
                            }
                            context.startActivity(intent)
                        })
                    }
                }
            }

            // Reviews
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(title = "Reviews (${uiState.reviews.size})")
            }
            if (uiState.reviews.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No reviews yet. Be the first!", color = SarrowsWhite60)
                    }
                }
            } else {
                items(uiState.reviews.take(10)) { review ->
                    ReviewItem(review = review, isOwnReview = review.user.id == currentUser?.id,
                        onDelete = { detailViewModel.deleteReview(review.id) })
                }
            }
        }

        // Review bottom sheet
        if (showReviewSheet) {
            ModalBottomSheet(
                onDismissRequest = { showReviewSheet = false },
                containerColor = SarrowsDarkCard
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Rate & Review", style = MaterialTheme.typography.titleLarge, color = SarrowsWhite)
                    Spacer(Modifier.height(16.dp))
                    Text("Rating: $reviewRating / 10", color = SarrowsWhite60)
                    Slider(
                        value = reviewRating.toFloat(),
                        onValueChange = { reviewRating = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = SarrowsRed, activeTrackColor = SarrowsRed)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reviewComment, onValueChange = { reviewComment = it },
                        label = { Text("Comment (optional)") },
                        modifier = Modifier.fillMaxWidth(), maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SarrowsRed, focusedLabelColor = SarrowsRed,
                            focusedTextColor = SarrowsWhite, unfocusedTextColor = SarrowsWhite,
                            unfocusedBorderColor = SarrowsDarkBorder, unfocusedLabelColor = SarrowsWhite60
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            detailViewModel.submitReview(targetType, targetId, reviewRating, reviewComment.takeIf { it.isNotBlank() })
                            showReviewSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SarrowsRed)
                    ) { Text("Submit Review") }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun CastCard(member: CastMember) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = member.image,
            contentDescription = member.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp).clip(CircleShape).background(SarrowsDarkCard)
        )
        Spacer(Modifier.height(4.dp))
        Text(member.name, style = MaterialTheme.typography.labelSmall, color = SarrowsWhite87, maxLines = 1)
        Text(member.character ?: "", style = MaterialTheme.typography.labelSmall, color = SarrowsWhite60, maxLines = 1)
    }
}

@Composable
fun EpisodeItem(ep: com.sarrows.app.data.models.Episode, onPlay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onPlay,
        colors = CardDefaults.cardColors(containerColor = SarrowsDarkCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayCircleOutline, null, tint = SarrowsRed, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("S${ep.season} E${ep.episodeNumber}", style = MaterialTheme.typography.labelMedium, color = SarrowsRed)
                Text(ep.title.ifEmpty { "Episode ${ep.episodeNumber}" }, style = MaterialTheme.typography.bodyMedium, color = SarrowsWhite)
            }
            Icon(Icons.Default.ChevronRight, null, tint = SarrowsWhite38)
        }
    }
}

@Composable
fun ReviewItem(review: Review, isOwnReview: Boolean, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SarrowsDarkCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).background(SarrowsRed.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        review.user.nickname.first().uppercaseChar().toString(),
                        color = SarrowsRed, style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(review.user.nickname, style = MaterialTheme.typography.labelMedium, color = SarrowsWhite)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = SarrowsGold, modifier = Modifier.size(14.dp))
                    Text("${review.rating}/10", style = MaterialTheme.typography.labelMedium, color = SarrowsGold)
                }
                if (isOwnReview) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, null, tint = SarrowsWhite38, modifier = Modifier.size(16.dp))
                    }
                }
            }
            review.comment?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = SarrowsWhite87)
            }
        }
    }
}
