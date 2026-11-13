package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CatalogSeed
import com.example.model.Book
import com.example.model.BookCollection
import com.example.model.BookReview
import com.example.model.RatingTier
import com.example.model.ReadingStatus
import com.example.model.TimeFilter
import com.example.model.UserBookState
import com.example.ui.components.BookCoverThumb
import com.example.ui.components.RatingMeterCard
import com.example.ui.components.VibeChartCard
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.AwardCyan
import com.example.ui.theme.SelectGold
import com.example.ui.theme.WarmFlameEnd
import com.example.ui.theme.WarmFlameStart

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookDetailScreen(
    book: Book,
    userBookState: UserBookState?,
    isSignedIn: Boolean,
    userCollections: List<BookCollection>,
    publicCollections: List<BookCollection>,
    liveReviews: List<BookReview>,
    onBack: () -> Unit,
    onRequireLogin: (String) -> Unit,
    onToggleStatus: (ReadingStatus) -> Unit,
    onVoteRatingTier: (RatingTier) -> Unit,
    onFindSimilarVibes: () -> Unit,
    onOpenAuthor: (String) -> Unit,
    onOpenPublisher: (String) -> Unit,
    onOpenCollection: (String) -> Unit,
    onOpenReaderProfile: (String) -> Unit,
    onAddBookToCollection: (BookCollection) -> Unit,
    onCreateCollectionWithBook: (String, String, Boolean) -> Unit,
    onPostReview: (RatingTier, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var overviewExpanded by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }

    val currentStatus = ReadingStatus.fromKey(userBookState?.status)
    val userTier = RatingTier.fromKey(userBookState?.ratingTier)
    val author = remember(book.authorId) { CatalogSeed.getAuthorById(book.authorId) }
    val recommendations = remember(book.id) { CatalogSeed.getRecommendationsForBook(book) }

    val matchingCuratedCollections = remember(book.id) {
        CatalogSeed.curatedCollections.filter { book.id in it.bookIds }
    }
    val matchingPublicUserCollections = remember(book.id, publicCollections) {
        publicCollections.filter { book.id in it.bookIds }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Atmospheric Header Banner + Poster Cover + 4-Grid Metadata (Screenshots #12, #13)
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp)
                ) {
                    if (book.coverImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = book.coverImageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = book.coverRes),
                            error = painterResource(id = book.coverRes),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = book.coverRes),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                    // Subtle Preview Play Circle on banner when external preview/buy link exists
                    if (book.buyLinks.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable {
                                    val firstLink = book.buyLinks.firstOrNull()?.url
                                    if (!firstLink.isNullOrBlank()) {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(firstLink))) }
                                    }
                                }
                                .testTag("book_detail_preview_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Open book preview link",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.52f))
                                .testTag("book_detail_back")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { shareBook(context, book) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.52f))
                                .testTag("book_detail_share")
                        ) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "Share ${book.title}",
                                tint = Color.White
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 132.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    BookCoverThumb(
                        book = book,
                        showAppSelect = false,
                        modifier = Modifier.size(width = 114.dp, height = 172.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("book_detail_header_metadata"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (book.isUpcoming) "Upcoming • ${book.releaseDateLabel}"
                            else "Book • ${book.year} • ${book.pages}p • ~${book.estimatedHours}h",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (book.subtitle.isNotBlank()) {
                            Text(
                                text = book.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "${book.genre} • ${book.subGenre}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetadataCell(
                                label = "Written By",
                                value = book.authorName,
                                modifier = Modifier.weight(1f),
                                isInteractive = true,
                                avatarRes = author?.avatarRes ?: 0,
                                onClick = { onOpenAuthor(book.authorId) }
                            )
                            MetadataCell(label = "Country", value = book.country, modifier = Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetadataCell(label = "Language", value = book.language, modifier = Modifier.weight(1f))
                            MetadataCell(label = "Age Rating", value = book.ageRating, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 2. Primary Action Buttons + Upcoming Release Card (Screenshots #12, #13)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!book.isUpcoming) {
                    val isRead = currentStatus == ReadingStatus.READ
                    val isTbr = currentStatus == ReadingStatus.TBR
                    Button(
                        onClick = {
                            if (!isSignedIn) onRequireLogin("Sign in to mark books as Read")
                            else onToggleStatus(ReadingStatus.READ)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("action_mark_read"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRead) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (isRead) Icons.Filled.CheckCircle else Icons.Filled.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRead) "Marked as Read" else "Mark as Read",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (!isSignedIn) onRequireLogin("Sign in to organize Collections")
                                else showCollectionDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("action_collections"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Filled.BookmarkBorder, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Collections")
                        }
                        OutlinedButton(
                            onClick = {
                                if (!isSignedIn) onRequireLogin("Sign in to save books to Read Later (TBR)")
                                else onToggleStatus(ReadingStatus.TBR)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("action_tbr"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isTbr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isTbr) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isTbr) "Saved in TBR" else "Read Later")
                        }
                    }
                } else {
                    val isInterested = currentStatus == ReadingStatus.INTERESTED
                    val interestedCount = (book.interestedByFilter[TimeFilter.ALL_TIME] ?: 11200) +
                        if (isInterested) 1 else 0

                    // Warm Coral-Orange Gradient "Mark as Interested" CTA matching Screenshot #12
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isInterested) Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondary)
                                ) else Brush.horizontalGradient(
                                    listOf(WarmFlameStart, WarmFlameEnd)
                                )
                            )
                            .clickable {
                                if (!isSignedIn) onRequireLogin("Sign in to mark Interested & receive release alerts")
                                else onToggleStatus(ReadingStatus.INTERESTED)
                            }
                            .testTag("action_mark_interested"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isInterested) "Interested • Alert On" else "Mark as Interested",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (!isSignedIn) onRequireLogin("Sign in to add to a Collection")
                            else showCollectionDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("action_add_to_collection"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Filled.BookmarkBorder, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Collection")
                    }

                    // Release Box Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Publishing Soon",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = book.releaseDateLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = WarmFlameStart,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${formatCompactCount(interestedCount)} interested",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WarmFlameStart
                                    )
                                    Text(
                                        text = "  •  ",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { shareBook(context, book) }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                            .testTag("action_share_upcoming"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = "Share book",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Share",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(WarmFlameStart, WarmFlameEnd)))
                                    .clickable {
                                        if (!isSignedIn) onRequireLogin("Sign in to mark Interested")
                                        else onToggleStatus(ReadingStatus.INTERESTED)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = "Interested",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Side-by-side Outlined Badges (Awards + nokhook Select) matching Screenshot #13
                if (book.awards.isNotEmpty() || book.isAppSelect) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (book.awards.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Filled.EmojiEvents,
                                        contentDescription = null,
                                        tint = AwardCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = book.awards.joinToString(", "),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AwardCyan,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (book.isAppSelect) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Verified,
                                        contentDescription = null,
                                        tint = SelectGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "nokhook Select",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SelectGold,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Overview with Inline Read More
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = book.overview,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (overviewExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(
                    onClick = { overviewExpanded = !overviewExpanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (overviewExpanded) "Show Less" else "Read More")
                }
            }
        }

        // 4. Read / Buy On External Platforms (Screenshot #9 "Tickets On" style)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                Text(
                    text = "Read / Buy On",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    book.buyLinks.forEach { link ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                    runCatching { context.startActivity(intent) }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${link.platformName} · ${link.actionLabel}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Vibe Chart (Interactive Bar & Donut + Rating-Weighted Emotional Tags)
        item {
            VibeChartCard(
                vibes = book.vibes,
                onFindSimilarVibes = onFindSimilarVibes,
                ratingStats = book.ratingStats,
                userSelectedTier = userTier,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 6. Author Image + Recommendations By (Name, Image, Profession)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                Text(
                    text = "Author & Contributors",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(114.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenAuthor(book.authorId) }
                            .padding(vertical = 4.dp)
                            .testTag("author_profile_card")
                    ) {
                        val avatarRes = author?.avatarRes ?: 0
                        Surface(
                            modifier = Modifier.size(86.dp),
                            shape = CircleShape,
                            color = Color(author?.accentColorHex ?: 0xFF7C3AED),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        ) {
                            if (avatarRes != 0) {
                                Image(
                                    painter = painterResource(id = avatarRes),
                                    contentDescription = "${book.authorName} portrait",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = "${book.authorName} portrait",
                                        tint = Color.White,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = book.authorName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Author",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Recommendations By",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Notable readers & critics endorsing this book",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recommendations_by_section"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    recommendations.forEach { person ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = person.username.isNotBlank()) {
                                    onOpenReaderProfile(person.username)
                                }
                                .testTag("recommendation_card_${person.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(56.dp),
                                    shape = CircleShape,
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                                ) {
                                    Image(
                                        painter = painterResource(id = person.imageRes),
                                        contentDescription = "${person.name} portrait",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = person.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = person.profession,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (person.quote.isNotBlank()) {
                                        Text(
                                            text = "\"${person.quote}\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. nokhook 4-Tier Rating Meter
        item {
            RatingMeterCard(
                stats = book.ratingStats,
                userSelectedTier = userTier,
                onSelectTier = { tier ->
                    if (!isSignedIn) onRequireLogin("Sign in to vote on the nokhook Meter")
                    else onVoteRatingTier(tier)
                },
                onShare = { shareBook(context, book) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 8. Users' Collections
        if (matchingCuratedCollections.isNotEmpty() || matchingPublicUserCollections.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(
                        text = "Users' Collections",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(matchingCuratedCollections, key = { it.id }) { col ->
                            Card(
                                modifier = Modifier
                                    .width(232.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenCollection(col.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(col.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = "by ${col.curatorName} (@${col.curatorUsername})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { onOpenReaderProfile(col.curatorUsername) }
                                    )
                                    Text("${col.bookIds.size} books • ${col.topicTag}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        items(matchingPublicUserCollections, key = { it.id }) { col ->
                            Card(
                                modifier = Modifier
                                    .width(232.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenCollection(col.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(col.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = "by @${col.creatorUsername}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { onOpenReaderProfile(col.creatorUsername) }
                                    )
                                    Text("${col.bookIds.size} books", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 9. User Reviews Section (Blurred Teaser Gate in Guest Mode, matching Screenshot #3!)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "User Reviews",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = {
                            if (!isSignedIn) onRequireLogin("Sign in to write a review for ${book.title}")
                            else showReviewDialog = true
                        },
                        modifier = Modifier.testTag("write_review_button")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Write Review")
                    }
                }

                if (!isSignedIn) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onRequireLogin("Sign in to read community reviews & share yours") }
                            .testTag("reviews_login_gate_card")
                    ) {
                        // Blurred background review cards matching Screenshot #3 teaser
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .blur(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReviewCardItem(
                                authorName = "Aarav Verma",
                                username = "aarav_reads",
                                timeLabel = "2 hrs ago",
                                tier = RatingTier.MUST_READ,
                                body = "An extraordinary read with unforgettable prose and emotional depth across every chapter."
                            )
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = "Login required for reviews",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = "Sign In to Unlock User Reviews",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Guests can browse Vibe Charts & nokhook Meter freely. Join to read & publish reviews.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { onRequireLogin("Sign in to read & write book reviews") },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Login / Sign Up")
                                }
                            }
                        }
                    }
                } else {
                    val bookLiveReviews = liveReviews.filter { it.bookId == book.id }
                    val seedBookReviews = CatalogSeed.seedReviews.filter { it.bookId == book.id }
                    if (bookLiveReviews.isEmpty() && seedBookReviews.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "No reviews yet — be the first nokhook reader to share your verdict!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        bookLiveReviews.forEach { rev ->
                            val tier = RatingTier.fromKey(rev.ratingTier) ?: RatingTier.WORTH_READING
                            ReviewCardItem(
                                authorName = rev.authorName,
                                username = rev.authorUsername,
                                timeLabel = "Synced",
                                tier = tier,
                                body = rev.body,
                                onClickAuthor = { onOpenReaderProfile(rev.authorUsername) }
                            )
                        }
                        seedBookReviews.forEach { rev ->
                            ReviewCardItem(
                                authorName = rev.authorName,
                                username = rev.authorUsername,
                                timeLabel = rev.timeAgo,
                                tier = rev.ratingTier,
                                body = rev.body,
                                onClickAuthor = { onOpenReaderProfile(rev.authorUsername) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCollectionDialog) {
        AddToCollectionDialog(
            bookTitle = book.title,
            userCollections = userCollections,
            onDismiss = { showCollectionDialog = false },
            onSelectExisting = { col ->
                onAddBookToCollection(col)
                showCollectionDialog = false
            },
            onCreateNew = { title, desc, isPub ->
                onCreateCollectionWithBook(title, desc, isPub)
                showCollectionDialog = false
            }
        )
    }

    if (showReviewDialog) {
        WriteReviewDialog(
            bookTitle = book.title,
            initialTier = userTier ?: RatingTier.MUST_READ,
            onDismiss = { showReviewDialog = false },
            onSubmit = { tier, body ->
                onPostReview(tier, body)
                showReviewDialog = false
            }
        )
    }
}

private fun shareBook(context: android.content.Context, book: Book) {
    val shareText = buildString {
        append("${book.title} by ${book.authorName}")
        if (book.isUpcoming && book.releaseDateLabel.isNotBlank()) {
            append(" — releasing ${book.releaseDateLabel}")
        }
        append(" on nokhook")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, book.title)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    runCatching {
        context.startActivity(Intent.createChooser(sendIntent, "Share book"))
    }
}

@Composable
private fun MetadataCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = false,
    avatarRes: Int = 0,
    onClick: (() -> Unit)? = null
) {
    val cellModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    } else {
        modifier
    }
    Column(modifier = cellModifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (avatarRes != 0) {
                Image(
                    painter = painterResource(id = avatarRes),
                    contentDescription = "$value portrait",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isInteractive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReviewCardItem(
    authorName: String,
    username: String,
    timeLabel: String,
    tier: RatingTier,
    body: String,
    onClickAuthor: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = if (onClickAuthor != null) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onClickAuthor)
                            .padding(vertical = 2.dp, horizontal = 2.dp)
                    } else Modifier
                ) {
                    Text(authorName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "@$username • $timeLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (onClickAuthor != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = tier.color.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = tier.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tier.color,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun AddToCollectionDialog(
    bookTitle: String,
    userCollections: List<BookCollection>,
    onDismiss: () -> Unit,
    onSelectExisting: (BookCollection) -> Unit,
    onCreateNew: (String, String, Boolean) -> Unit
) {
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save \"$bookTitle\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (userCollections.isNotEmpty()) {
                    Text("Your Collections:", style = MaterialTheme.typography.labelLarge)
                    userCollections.forEach { col ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectExisting(col) },
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(col.title, style = MaterialTheme.typography.bodyMedium)
                                Text("${col.bookIds.size} books", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Text("Or Create New Collection:", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Collection Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newDesc,
                    onValueChange = { newDesc = it },
                    label = { Text("Short Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isPublic = !isPublic }
                ) {
                    Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                    Text("Make collection public", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newTitle.trim().isNotEmpty()) {
                        onCreateNew(newTitle.trim(), newDesc.trim(), isPublic)
                    }
                },
                enabled = newTitle.trim().isNotEmpty()
            ) {
                Text("Create & Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WriteReviewDialog(
    bookTitle: String,
    initialTier: RatingTier,
    onDismiss: () -> Unit,
    onSubmit: (RatingTier, String) -> Unit
) {
    var selectedTier by remember { mutableStateOf(initialTier) }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review $bookTitle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select your 4-Tier Verdict:", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RatingTier.entries.forEach { tier ->
                        FilterChip(
                            selected = selectedTier == tier,
                            onClick = { selectedTier = tier },
                            label = { Text(tier.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Your thoughts (min 3 chars)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedTier, body.trim()) },
                enabled = body.trim().length >= 3
            ) {
                Text("Publish Review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
