package com.sarrows.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sarrows.app.ui.theme.*

@Composable
fun SectionHeader(
    title: String,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = SarrowsWhite
        )
        if (onSeeAllClick != null) {
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelMedium,
                    color = SarrowsRed
                )
            }
        }
    }
}

@Composable
fun GenreBadge(genre: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = SarrowsDarkSurface
    ) {
        Text(
            text = genre,
            style = MaterialTheme.typography.labelSmall,
            color = SarrowsWhite60,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (color, text) = when (status.lowercase()) {
        "ongoing"     -> SarrowsGreen to "Ongoing"
        "completed"   -> SarrowsBlue  to "Completed"
        "published"   -> SarrowsGreen to "Published"
        else          -> SarrowsWhite60 to status
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
