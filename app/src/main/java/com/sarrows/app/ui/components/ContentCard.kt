package com.sarrows.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sarrows.app.ui.theme.*

@Composable
fun ContentCard(
    title: String,
    posterUrl: String?,
    rating: Double,
    year: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SarrowsDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            AsyncImage(
                model = posterUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
            )
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xE0000000))
                        )
                    )
            )
            // Rating badge
            if (rating > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = SarrowsGold,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "%.1f".format(rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = SarrowsWhite
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = SarrowsWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (year != null) {
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SarrowsWhite60
                )
            }
        }
    }
}

@Composable
fun WideContentCard(
    title: String,
    bannerUrl: String?,
    posterUrl: String?,
    rating: Double,
    year: Int?,
    genres: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SarrowsDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box {
            AsyncImage(
                model = bannerUrl ?: posterUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xF0000000))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = SarrowsWhite)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (year != null) {
                        Text("$year", style = MaterialTheme.typography.bodySmall, color = SarrowsWhite60)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (rating > 0) {
                        Icon(Icons.Filled.Star, null, tint = SarrowsGold, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("%.1f".format(rating), style = MaterialTheme.typography.bodySmall, color = SarrowsWhite60)
                    }
                }
            }
        }
    }
}
