package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CatalogSeed
import com.example.model.Book
import com.example.model.ExploreFilterType
import com.example.ui.components.BookCoverThumb
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SelectGold

private data class BrowseTileSpec(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val filterType: ExploreFilterType,
    val subFilter: String = "All"
)

@Composable
fun ExploreBrowseScreen(
    searchQuery: String,
    activeFilter: ExploreFilterType?,
    activeSubFilter: String,
    filteredBooks: List<Book>,
    isSignedIn: Boolean,
    onSearchChange: (String) -> Unit,
    onSelectFilter: (ExploreFilterType?, String) -> Unit,
    onOpenBook: (String) -> Unit,
    onOpenReaderProfile: (String) -> Unit,
    onRequireLogin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search titles, authors, Hindi, manga...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search books") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_field")
            )
        }

        if (activeFilter == null && searchQuery.isBlank()) {
            item {
                Text(
                    text = "Browse By",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Row 1: Following Activity + Today Trending (2-Column Highlight Row)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(104.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (!isSignedIn) {
                                    onRequireLogin("Sign in to follow readers and see their activity")
                                } else {
                                    onSelectFilter(ExploreFilterType.FOLLOWING, "All")
                                }
                            }
                            .testTag("browse_card_following"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MonitorHeart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Following Activity",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(104.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectFilter(ExploreFilterType.TODAY_TRENDING, "All") }
                            .testTag("browse_card_today_trending"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, EmeraldAccent.copy(alpha = 0.75f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldAccent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Today Trending",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Row 2: 2-Column Row (Monthly Ranking | Top 100) matching Screenshot #4
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrowseTileCard(
                        spec = BrowseTileSpec(
                            id = "monthly_ranking",
                            label = "Monthly Ranking",
                            icon = Icons.Filled.CalendarMonth,
                            filterType = ExploreFilterType.TOP_100,
                            subFilter = "Monthly"
                        ),
                        height = 104,
                        onClick = { onSelectFilter(ExploreFilterType.TOP_100, "Monthly") },
                        modifier = Modifier.weight(1f)
                    )
                    BrowseTileCard(
                        spec = BrowseTileSpec(
                            id = "top_100",
                            label = "Top 100",
                            icon = Icons.Filled.WorkspacePremium,
                            filterType = ExploreFilterType.TOP_100,
                            subFilter = "All"
                        ),
                        height = 104,
                        onClick = { onSelectFilter(ExploreFilterType.TOP_100, "All") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Rows 3-5: 3-Column Square Grid (9 Browse Cards) matching Screenshot #4
            val gridSpecs = listOf(
                listOf(
                    BrowseTileSpec("category", "Category", Icons.Filled.Category, ExploreFilterType.GENRE, "All"),
                    BrowseTileSpec("genre", "Genre", Icons.Filled.TheaterComedy, ExploreFilterType.GENRE, "Fiction"),
                    BrowseTileSpec("country", "Country", Icons.Filled.Public, ExploreFilterType.LANGUAGE_COUNTRY, "India")
                ),
                listOf(
                    BrowseTileSpec("language", "Language", Icons.Filled.Translate, ExploreFilterType.LANGUAGE_COUNTRY, "Hindi"),
                    BrowseTileSpec("beginner_ya", "Young Adult", Icons.AutoMirrored.Filled.MenuBook, ExploreFilterType.BEGINNER_YA, "All"),
                    BrowseTileSpec("awards", "Award Winners", Icons.Filled.EmojiEvents, ExploreFilterType.AWARDS, "All")
                ),
                listOf(
                    BrowseTileSpec("app_select", "nokhook Select", Icons.Filled.Verified, ExploreFilterType.TOP_100, "Select"),
                    BrowseTileSpec("manga", "Manga & Comics", Icons.Filled.AutoStories, ExploreFilterType.SERIES_MANGA, "Manga"),
                    BrowseTileSpec("series", "Book Series", Icons.Filled.AutoStories, ExploreFilterType.SERIES_MANGA, "All")
                )
            )
            items(gridSpecs) { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTiles.forEach { tile ->
                        BrowseTileCard(
                            spec = tile,
                            height = 114,
                            onClick = { onSelectFilter(tile.filterType, tile.subFilter) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else {
            // Active Filter Header + Sub-filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (activeFilter != null) {
                                IconButton(
                                    onClick = { onSelectFilter(null, "All") },
                                    modifier = Modifier.testTag("explore_back_grid")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Browse By")
                                }
                            }
                            Text(
                                text = when {
                                    activeSubFilter == "Select" -> "nokhook Select"
                                    activeSubFilter == "Monthly" -> "Monthly Ranking"
                                    activeFilter != null -> activeFilter.title
                                    else -> "Search Results"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        if (activeFilter != null || searchQuery.isNotBlank()) {
                            TextButton(onClick = {
                                onSelectFilter(null, "All")
                                onSearchChange("")
                            }) {
                                Text("Reset")
                            }
                        }
                    }

                    val chips = when (activeFilter) {
                        ExploreFilterType.TODAY_TRENDING -> listOf("All", "Books", "Series & Manga", "Upcoming")
                        ExploreFilterType.GENRE -> listOf("All", "Fiction", "Non-Fiction", "Self-Help", "Thriller", "Romance", "Business", "Manga")
                        ExploreFilterType.LANGUAGE_COUNTRY -> listOf("All", "English", "Hindi", "Marathi", "Japanese", "India", "UK", "USA", "Japan")
                        else -> emptyList()
                    }
                    if (chips.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(chips) { chip ->
                                FilterChip(
                                    selected = activeSubFilter == chip,
                                    onClick = { onSelectFilter(activeFilter, chip) },
                                    label = { Text(chip) }
                                )
                            }
                        }
                    }
                }
            }

            if (activeFilter == ExploreFilterType.FOLLOWING) {
                items(CatalogSeed.seedFollowingActivity, key = { it.id }) { activity ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenBook(activity.bookId) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { onOpenReaderProfile(activity.username) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activity.userName.take(1),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${activity.userName} ${activity.actionLabel} ${activity.bookTitle}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "@${activity.username} • ${activity.timeAgo}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onOpenReaderProfile(activity.username) }
                                )
                            }
                        }
                    }
                }
            } else {
                val displayBooks = when (activeSubFilter) {
                    "Select" -> filteredBooks.filter { it.isAppSelect }
                    "Manga" -> filteredBooks.filter { it.genre.equals("Manga", ignoreCase = true) || it.seriesLabel.contains("Manga", ignoreCase = true) }
                    else -> filteredBooks
                }
                if (displayBooks.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No matching books found",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try another genre, language, or submit a missing book via Menu > My Contributions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(displayBooks, key = { it.id }) { book ->
                        ExploreBookListRow(
                            book = book,
                            rank = if (activeFilter == ExploreFilterType.TOP_100 && activeSubFilter != "Select") book.monthlyRank else null,
                            onClick = { onOpenBook(book.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseTileCard(
    spec: BrowseTileSpec,
    height: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("browse_card_${spec.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = if (spec.id == "app_select") SelectGold else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = spec.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExploreBookListRow(
    book: Book,
    rank: Int?,
    onClick: () -> Unit
) {
    val dominant = book.ratingStats.dominantTier()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (rank != null && rank < 99) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(34.dp)
                )
            }
            BookCoverThumb(
                book = book,
                showAppSelect = false,
                modifier = Modifier.size(width = 62.dp, height = 90.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${book.authorName} • ${book.language}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = book.genre,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = dominant.color.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = dominant.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = dominant.color,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
