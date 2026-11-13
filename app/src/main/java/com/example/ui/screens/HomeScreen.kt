package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.EditableAppSettings
import com.example.model.Book
import com.example.model.ReadingStatus
import com.example.model.TimeFilter
import com.example.model.UserBookState
import com.example.ui.components.BookShelfCard
import com.example.ui.components.LeaderboardBookCard
import com.example.ui.components.TodayTrendingBookCard
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SelectGold
import com.example.ui.theme.WarmFlameStart

@Composable
fun HomeDiscoveryScreen(
    selectedTimeFilter: TimeFilter,
    leaderboard: List<Pair<Book, Int>>,
    todayTrending: List<Pair<Book, String>>,
    talkOfTheTown: List<Book>,
    editorsPicks: List<Book>,
    appSelectBooks: List<Book>,
    userBookStates: Map<String, UserBookState>,
    onSelectTimeFilter: (TimeFilter) -> Unit,
    onSeeAllTrending: () -> Unit,
    onOpenBook: (String) -> Unit,
    appSettings: EditableAppSettings = EditableAppSettings(),
    modifier: Modifier = Modifier
) {
    var filterExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_discovery_feed"),
        contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        if (appSettings.announcementBanner.isNotBlank()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("home_announcement_banner"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = appSettings.announcementBanner,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 1. Most Interested Leaderboard (Horizontal LazyRow + Time Filter Dropdown)
        item {
            Column(
                modifier = Modifier.testTag("section_most_interested_leaderboard"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeSectionHeader(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconTint = WarmFlameStart,
                        title = "Most Interested Leaderboard",
                        subtitle = "Upcoming books & release dates",
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        OutlinedButton(
                            onClick = { filterExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("leaderboard_time_filter")
                        ) {
                            Text(
                                text = selectedTimeFilter.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Select time filter",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = filterExpanded,
                            onDismissRequest = { filterExpanded = false }
                        ) {
                            TimeFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter.label) },
                                    onClick = {
                                        onSelectTimeFilter(filter)
                                        filterExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                LazyRow(
                    modifier = Modifier.testTag("row_most_interested_leaderboard"),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(leaderboard, key = { _, pair -> pair.first.id }) { index, (book, baseCount) ->
                        val extra = if (userBookStates[book.id]?.status == ReadingStatus.INTERESTED.key) 1 else 0
                        LeaderboardBookCard(
                            rank = index + 1,
                            book = book,
                            interestedCount = baseCount + extra,
                            onClick = { onOpenBook(book.id) }
                        )
                    }
                }
            }
        }

        // 2. Today Trending (Horizontal Scrollable Ranked Shelf + See All -> Explore)
        item {
            Column(
                modifier = Modifier.testTag("section_today_trending"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeSectionHeader(
                        icon = Icons.Filled.Bolt,
                        iconTint = EmeraldAccent,
                        title = "Today Trending",
                        subtitle = "Fastest-rising reads in the last 24h",
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onSeeAllTrending,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("see_all_today_trending")
                    ) {
                        Text("See All", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "See all trending books",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier.testTag("row_today_trending"),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(todayTrending, key = { _, pair -> "trend_${pair.first.id}" }) { index, (book, delta) ->
                        TodayTrendingBookCard(
                            rank = index + 1,
                            book = book,
                            trendDeltaLabel = delta,
                            onClick = { onOpenBook(book.id) }
                        )
                    }
                }
            }
        }

        // 3. Talk Of The Town (Horizontal Scrollable Poster Shelf with Cover + Title + Category)
        item {
            Column(
                modifier = Modifier.testTag("section_talk_of_the_town"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeSectionHeader(
                    icon = Icons.Filled.LocalFireDepartment,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Talk Of The Town",
                    subtitle = "Most discussed reads right now",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                LazyRow(
                    modifier = Modifier.testTag("row_talk_of_the_town"),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(talkOfTheTown, key = { it.id }) { book ->
                        BookShelfCard(
                            book = book,
                            showAppSelect = true,
                            showCategoryBadge = true,
                            subtitleOverride = "${book.authorName} • ${book.year}",
                            onClick = { onOpenBook(book.id) },
                            modifier = Modifier.width(154.dp)
                        )
                    }
                }
            }
        }

        // 3. Featured Editorial Spotlight Banner
        item {
            val spotlight = editorsPicks.firstOrNull()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = spotlight != null) {
                        spotlight?.let { onOpenBook(it.id) }
                    }
                    .testTag("home_hero_banner"),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(modifier = Modifier.height(176.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_editorial),
                        contentDescription = "Editor's reading corner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF09090D).copy(alpha = 0.88f),
                                        Color(0xFF09090D).copy(alpha = 0.35f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SPOTLIGHT PICK",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            text = appSettings.spotlightTitle.ifBlank {
                                spotlight?.title ?: "Discover Your Next Obsession"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = appSettings.spotlightSubtitle.ifBlank {
                                spotlight?.let { "${it.authorName} • ${it.genre} • View Vibe Chart" }
                                    ?: "Explore curated shelves & 4-tier ratings"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.84f)
                        )
                    }
                }
            }
        }

        // 4. Editor's Pick (Horizontal Scrollable Shelf)
        item {
            Column(
                modifier = Modifier.testTag("section_editors_pick"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeSectionHeader(
                    icon = Icons.Filled.Favorite,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Editor's Pick",
                    subtitle = "Weekly curator highlights",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                LazyRow(
                    modifier = Modifier.testTag("row_editors_pick"),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(editorsPicks, key = { it.id }) { book ->
                        BookShelfCard(
                            book = book,
                            showAppSelect = true,
                            showCategoryBadge = true,
                            subtitleOverride = "${book.authorName} • ${book.year}",
                            onClick = { onOpenBook(book.id) },
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
            }
        }

        // 5. nokhook Select Showcase (Horizontal Scrollable Shelf)
        item {
            Column(
                modifier = Modifier.testTag("section_nokhook_select"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeSectionHeader(
                    icon = Icons.Filled.Verified,
                    iconTint = SelectGold,
                    title = "nokhook Select",
                    subtitle = "Special curator editions",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                LazyRow(
                    modifier = Modifier.testTag("row_nokhook_select"),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(appSelectBooks, key = { it.id }) { book ->
                        BookShelfCard(
                            book = book,
                            showAppSelect = true,
                            onClick = { onOpenBook(book.id) },
                            modifier = Modifier.width(154.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
