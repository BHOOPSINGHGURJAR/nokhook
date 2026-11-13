package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CatalogSeed
import com.example.model.Book
import com.example.model.BookCollection
import com.example.model.BookReview
import com.example.model.FollowingEdge
import com.example.model.RatingTier
import com.example.ui.components.BookCoverThumb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicReaderProfileScreen(
    usernameOrName: String,
    isSignedIn: Boolean,
    following: List<FollowingEdge>,
    liveReviews: List<BookReview>,
    publicCollections: List<BookCollection>,
    onBack: () -> Unit,
    onRequireLogin: (String) -> Unit,
    onToggleFollow: (String, String, String) -> Unit,
    onOpenBook: (String) -> Unit,
    onOpenCollection: (String) -> Unit
) {
    val profile = remember(usernameOrName) { CatalogSeed.getReaderProfile(usernameOrName) }
    val isFollowed = following.any { it.targetUserId.equals(profile.username, ignoreCase = true) }
    val context = LocalContext.current

    val readerReviews = remember(profile.username, liveReviews) {
        val live = liveReviews.filter {
            it.authorUsername.equals(profile.username, ignoreCase = true)
        }.map { rev ->
            val book = CatalogSeed.getBookById(rev.bookId)
            Triple(
                rev.bookId,
                rev.bookTitle,
                Triple(
                    RatingTier.fromKey(rev.ratingTier) ?: RatingTier.WORTH_READING,
                    rev.body,
                    book?.isSeriesOrManga ?: false
                )
            )
        }
        val seeded = CatalogSeed.seedReviews.filter {
            it.authorUsername.equals(profile.username, ignoreCase = true) ||
                it.authorName.equals(profile.displayName, ignoreCase = true)
        }.map { rev ->
            val book = CatalogSeed.getBookById(rev.bookId)
            Triple(
                rev.bookId,
                rev.bookTitle,
                Triple(rev.ratingTier, rev.body, book?.isSeriesOrManga ?: false)
            )
        }
        val combined = live + seeded
        if (combined.isNotEmpty()) combined
        else CatalogSeed.seedReviews.take(2).map { rev ->
            val book = CatalogSeed.getBookById(rev.bookId)
            Triple(rev.bookId, rev.bookTitle, Triple(rev.ratingTier, rev.body, book?.isSeriesOrManga ?: false))
        }
    }

    val readerCuratedCols = remember(profile.username) {
        CatalogSeed.curatedCollections.filter {
            it.curatorUsername.equals(profile.username, ignoreCase = true) ||
                it.curatorName.equals(profile.displayName, ignoreCase = true)
        }
    }
    val readerLiveCols = remember(profile.username, publicCollections) {
        publicCollections.filter { it.creatorUsername.equals(profile.username, ignoreCase = true) }
    }
    val interestedBooks = remember(profile.interestedBookIds) {
        profile.interestedBookIds.mapNotNull { CatalogSeed.getBookById(it) }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var reviewFilter by remember { mutableStateOf("All") }
    var typeFilter by remember { mutableStateOf("All") }
    var showReviewFilterMenu by remember { mutableStateOf(false) }
    var showTypeFilterMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var compactView by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "@${profile.username}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reader_profile_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val initials = profile.displayName.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifEmpty { "NR" }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Avatar + Stat Blocks + Bio + Verified Badge + Follow CTA
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(profile.accentColorHex),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatBlock(
                                value = "${maxOf(profile.reviewsCount, readerReviews.size)}",
                                label = "Reviews\nPosted"
                            )
                            ProfileStatBlock(
                                value = "${maxOf(profile.publicCollectionsCount, readerCuratedCols.size + readerLiveCols.size)}",
                                label = "Public\nCollections"
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (profile.isVerified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Verified reader",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Text(
                            text = "@${profile.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (profile.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = profile.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = profile.followersCountLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = profile.joinedLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (profile.socialUrlHint.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(profile.socialUrlHint))
                                        runCatching { context.startActivity(intent) }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Language,
                                        contentDescription = "Social link",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Check out ${profile.displayName} (@${profile.username}) on nokhook!")
                                    }
                                    runCatching { context.startActivity(Intent.createChooser(shareIntent, "Share Profile")) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share profile",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Primary Electric Violet Follow / Following CTA
                    Button(
                        onClick = {
                            if (!isSignedIn) {
                                onRequireLogin("Sign in to follow ${profile.displayName} (@${profile.username})")
                            } else {
                                onToggleFollow(profile.username, profile.username, profile.displayName)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("reader_follow_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowed) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary,
                            contentColor = if (isFollowed) MaterialTheme.colorScheme.onSurface else Color.White
                        ),
                        border = if (isFollowed) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                    ) {
                        Icon(
                            imageVector = if (isFollowed) Icons.Filled.Check else Icons.Filled.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFollowed) "Following" else "Follow",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // "Interested In" Horizontal Carousel matching screenshot
            if (interestedBooks.isNotEmpty()) {
                item {
                    InterestedInSection(
                        books = interestedBooks,
                        onOpenBook = onOpenBook
                    )
                }
            }

            // Icon Tabs (Reviews & Collections)
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color.White
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                Icons.Outlined.EditNote,
                                contentDescription = "Reviews",
                                tint = if (selectedTab == 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistPlay,
                                contentDescription = "Collections",
                                tint = if (selectedTab == 1) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Filter Pills & Search Row
            item {
                ProfileFilterControlsRow(
                    primaryLabel = if (selectedTab == 0) {
                        if (reviewFilter == "All") "All Reviews" else reviewFilter
                    } else "All Collections",
                    secondaryLabel = if (typeFilter == "All") "All Types" else typeFilter,
                    primaryExpanded = showReviewFilterMenu,
                    secondaryExpanded = showTypeFilterMenu,
                    onTogglePrimaryMenu = { showReviewFilterMenu = it },
                    onToggleSecondaryMenu = { showTypeFilterMenu = it },
                    primaryOptions = if (selectedTab == 0) listOf("All", "Must Read", "Worth Reading", "Light Read", "Skip") else listOf("All"),
                    secondaryOptions = listOf("All", "Books", "Series & Manga"),
                    onSelectPrimary = { reviewFilter = it },
                    onSelectSecondary = { typeFilter = it },
                    isCompactView = compactView,
                    onToggleCompactView = { compactView = !compactView },
                    showSearchBar = showSearchBar,
                    onToggleSearch = { showSearchBar = !showSearchBar },
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    searchPlaceholder = if (selectedTab == 0) "Search ${profile.displayName}'s reviews..." else "Search collections..."
                )
            }

            if (selectedTab == 0) {
                val filtered = readerReviews.filter { (bookId, bookTitle, triple) ->
                    val (tier, body, isSeries) = triple
                    val matchesQuery = searchText.isBlank() ||
                        bookTitle.contains(searchText, true) ||
                        body.contains(searchText, true)
                    val matchesTier = reviewFilter == "All" || tier.label.equals(reviewFilter, true)
                    val matchesType = when (typeFilter) {
                        "Books" -> !isSeries
                        "Series & Manga" -> isSeries
                        else -> true
                    }
                    matchesQuery && matchesTier && matchesType && bookId.isNotBlank()
                }
                items(filtered) { (bookId, bookTitle, triple) ->
                    val (tier, body, _) = triple
                    ProfileReviewCard(
                        bookId = bookId,
                        bookTitle = bookTitle,
                        tier = tier,
                        body = body,
                        compact = compactView,
                        onOpenBook = { onOpenBook(bookId) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                if (readerCuratedCols.isEmpty() && readerLiveCols.isEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${profile.displayName} hasn't published public collections yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(readerLiveCols, key = { "live_${it.id}" }) { col ->
                        ProfileCollectionRowCard(
                            title = col.title,
                            subtitle = "${col.bookIds.size} books • ${col.topicTag}",
                            badge = "Public",
                            onClick = { onOpenCollection(col.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    items(readerCuratedCols, key = { it.id }) { col ->
                        ProfileCollectionRowCard(
                            title = col.title,
                            subtitle = "${col.bookIds.size} books • ${col.topicTag}",
                            badge = "Curated",
                            onClick = { onOpenCollection(col.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun InterestedInSection(
    books: List<Book>,
    onOpenBook: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Interested In",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(books, key = { it.id }) { book ->
                Card(
                    modifier = Modifier
                        .width(276.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenBook(book.id) }
                        .testTag("interested_card_${book.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BookCoverThumb(
                            book = book,
                            showAppSelect = false,
                            modifier = Modifier.size(width = 50.dp, height = 70.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val dateLine = book.releaseDateLabel.ifBlank { "${book.year}" }
                            val formatTag = if (book.isSeriesOrManga) "Series" else "In Stores"
                            Text(
                                text = "$dateLine • $formatTag",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfileReviewCard(
    bookId: String,
    bookTitle: String,
    tier: RatingTier,
    body: String,
    onOpenBook: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val book = remember(bookId) { CatalogSeed.getBookById(bookId) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenBook),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Top
        ) {
            if (!compact && book != null) {
                BookCoverThumb(
                    book = book,
                    showAppSelect = false,
                    modifier = Modifier.size(width = 50.dp, height = 72.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bookTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = tier.color
                    ) {
                        Text(
                            text = tier.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (tier == RatingTier.LIGHT_READ) Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun ProfileCollectionRowCard(
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ProfileStatBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun ProfileFilterControlsRow(
    primaryLabel: String,
    secondaryLabel: String,
    primaryExpanded: Boolean,
    secondaryExpanded: Boolean,
    onTogglePrimaryMenu: (Boolean) -> Unit,
    onToggleSecondaryMenu: (Boolean) -> Unit,
    primaryOptions: List<String>,
    secondaryOptions: List<String>,
    onSelectPrimary: (String) -> Unit,
    onSelectSecondary: (String) -> Unit,
    isCompactView: Boolean = false,
    onToggleCompactView: () -> Unit = {},
    showSearchBar: Boolean,
    onToggleSearch: () -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    searchPlaceholder: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(
                        onClick = { onTogglePrimaryMenu(true) },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(primaryLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = primaryExpanded, onDismissRequest = { onTogglePrimaryMenu(false) }) {
                        primaryOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    onSelectPrimary(opt)
                                    onTogglePrimaryMenu(false)
                                }
                            )
                        }
                    }
                }

                Box {
                    OutlinedButton(
                        onClick = { onToggleSecondaryMenu(true) },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(secondaryLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = secondaryExpanded, onDismissRequest = { onToggleSecondaryMenu(false) }) {
                        secondaryOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    onSelectSecondary(opt)
                                    onToggleSecondaryMenu(false)
                                }
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = if (isCompactView) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isCompactView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleCompactView)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = "Toggle compact view",
                            tint = if (isCompactView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = if (showSearchBar) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (showSearchBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleSearch)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = if (showSearchBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (showSearchBar) {
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                placeholder = { Text(searchPlaceholder) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
