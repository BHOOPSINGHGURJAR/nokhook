package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CatalogSeed
import com.example.model.BookCollection
import com.example.model.BookReview
import com.example.model.DiscussionPost
import com.example.model.DiscussionReply
import com.example.model.FollowingEdge
import com.example.model.RatingTier
import com.example.model.SeedDiscussion
import com.example.ui.components.BookCoverThumb
import com.example.ui.components.NokhookBrandMark
import com.example.ui.theme.AwardCyan
import com.example.ui.theme.ObsidianGlowTop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

enum class SocialSubTab(val label: String, val icon: ImageVector) {
    FEED("Feed", Icons.Filled.Home),
    BOOK_NEWS("News", Icons.Filled.Newspaper),
    DISCUSSIONS("Discussions", Icons.Filled.Forum),
    SUMMARIES("Summaries", Icons.Filled.PlayCircleFilled),
    REVIEWS("Reviews", Icons.Outlined.EditNote),
    COLLECTIONS("Collections", Icons.Filled.BookmarkBorder),
    WHATS_NEW("Whats New", Icons.Filled.AutoAwesome)
}

@Composable
fun SocialHubScreen(
    selectedTopic: String,
    isSignedIn: Boolean,
    following: List<FollowingEdge>,
    liveReviews: List<BookReview>,
    liveDiscussions: List<DiscussionPost>,
    publicCollections: List<BookCollection>,
    observeReplies: (String) -> Flow<List<DiscussionReply>>,
    onSelectTopic: (String) -> Unit,
    onRequireLogin: (String) -> Unit,
    onOpenBook: (String) -> Unit,
    onOpenCollection: (String) -> Unit,
    onOpenReaderProfile: (String) -> Unit,
    onToggleFollow: (String, String, String) -> Unit,
    onCreateDiscussion: (String, String, String) -> Unit,
    onPostReply: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableIntStateOf(0) }
    var sortTop by remember { mutableStateOf(true) }
    var showTopicsDrawer by remember { mutableStateOf(false) }
    var showSocialSearch by remember { mutableStateOf(false) }
    var socialQuery by remember { mutableStateOf("") }
    var showNewDiscussionDialog by remember { mutableStateOf(false) }
    var replyTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var seedReplyTarget by remember { mutableStateOf<SeedDiscussion?>(null) }
    var upvotedSeedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var localSeedReplies by remember { mutableStateOf<Map<String, List<com.example.model.SeedReply>>>(emptyMap()) }
    val context = LocalContext.current

    val subTabs = SocialSubTab.entries
    val currentTab = subTabs[activeSubTab]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Horizontal Sub-Tab Bar with crisp white indicator (Screenshots #7, #10, #11, #13b)
            ScrollableTabRow(
                selectedTabIndex = activeSubTab,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                        color = Color.White
                    )
                },
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) }
            ) {
                subTabs.forEachIndexed { index, tab ->
                    val isSelected = activeSubTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { activeSubTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.testTag("social_tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Topics [Count] Pill & Search Circle Bar matching Screenshots #7, #10, #11, #13b
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { showTopicsDrawer = !showTopicsDrawer },
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (showTopicsDrawer || selectedTopic != "All Topics") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedTopic == "All Topics") "Topics" else selectedTopic,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${CatalogSeed.allTopics.size - 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { showSocialSearch = !showSocialSearch },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Search, contentDescription = "Filter hub", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (showTopicsDrawer) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CatalogSeed.allTopics) { topic ->
                            FilterChip(
                                selected = selectedTopic == topic,
                                onClick = { onSelectTopic(topic) },
                                label = { Text(topic, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("topic_chip_$topic")
                            )
                        }
                    }
                }

                if (showSocialSearch) {
                    OutlinedTextField(
                        value = socialQuery,
                        onValueChange = { socialQuery = it },
                        placeholder = { Text("Search ${currentTab.label.lowercase()}...") },
                        trailingIcon = {
                            if (socialQuery.isNotEmpty()) {
                                androidx.compose.material3.IconButton(onClick = { socialQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (currentTab) {
                    SocialSubTab.FEED -> {
                        if (!isSignedIn) {
                            item {
                                MemberGateBanner(
                                    title = "Your Personal Reading Feed",
                                    subtitle = "Sign in to follow fellow nokhook readers and see their latest reviews, TBR saves, and ratings.",
                                    onSignIn = { onRequireLogin("Sign in to unlock your Social Feed") }
                                )
                            }
                        } else {
                            item {
                                Text(
                                    "Readers You Follow (${following.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val feedItems = CatalogSeed.seedFollowingActivity.filter { act ->
                                socialQuery.isBlank() ||
                                    act.userName.contains(socialQuery, true) ||
                                    act.username.contains(socialQuery, true) ||
                                    act.bookTitle.contains(socialQuery, true) ||
                                    act.actionLabel.contains(socialQuery, true)
                            }
                            items(feedItems, key = { it.id }) { act ->
                                val isFollowed = following.any { it.targetUserId == act.username }
                                Card(
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
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onOpenReaderProfile(act.username) }
                                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                                            ) {
                                                Text(act.userName, style = MaterialTheme.typography.titleSmall)
                                                Text("@${act.username} • ${act.timeAgo}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                            OutlinedButton(
                                                onClick = { onToggleFollow(act.username, act.username, act.userName) },
                                                shape = RoundedCornerShape(999.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                                            ) {
                                                Text(if (isFollowed) "Following" else "Follow", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        Text(
                                            text = "${act.actionLabel} \"${act.bookTitle}\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.clickable { onOpenBook(act.bookId) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SocialSubTab.BOOK_NEWS -> {
                        val filtered = CatalogSeed.bookNews.filter {
                            (selectedTopic == "All Topics" || it.topicTag.equals(selectedTopic, ignoreCase = true)) &&
                                (socialQuery.isBlank() || it.headline.contains(socialQuery, true) || it.summary.contains(socialQuery, true) || it.source.contains(socialQuery, true))
                        }
                        items(filtered, key = { it.id }) { news ->
                            val relatedBook = news.relatedBookId?.let { CatalogSeed.getBookById(it) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = relatedBook != null) {
                                        relatedBook?.let { onOpenBook(it.id) }
                                    },
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Hero News Image Banner matching Screenshot #11
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(188.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    Image(
                                        painter = painterResource(id = relatedBook?.coverRes ?: R.drawable.img_hero_editorial),
                                        contentDescription = news.headline,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, AwardCyan.copy(alpha = 0.65f))
                                    ) {
                                        Text(
                                            text = news.topicTag.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AwardCyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .clickable { activeSubTab = SocialSubTab.DISCUSSIONS.ordinal }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "Discuss", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Text(news.headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(news.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "By ${news.source} • ${news.timeAgo}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            }
                        }
                    }

                    SocialSubTab.DISCUSSIONS -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Community Debates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = sortTop, onClick = { sortTop = true }, label = { Text("Top") })
                                    FilterChip(selected = !sortTop, onClick = { sortTop = false }, label = { Text("Newest") })
                                }
                            }
                        }

                        val filteredLive = liveDiscussions.filter {
                            (selectedTopic == "All Topics" || it.topicTag.equals(selectedTopic, ignoreCase = true)) &&
                                (socialQuery.isBlank() || it.title.contains(socialQuery, true) || it.body.contains(socialQuery, true) || it.authorName.contains(socialQuery, true))
                        }
                        items(filteredLive, key = { "live_${it.id}" }) { post ->
                            LiveDiscussionCard(
                                post = post,
                                repliesFlow = if (isSignedIn) observeReplies(post.id) else flowOf(emptyList()),
                                onOpenReaderProfile = onOpenReaderProfile,
                                onReply = { parentId ->
                                    if (!isSignedIn) onRequireLogin("Sign in to reply to discussions")
                                    else replyTarget = post.id to parentId
                                }
                            )
                        }

                        val filteredSeed = CatalogSeed.seedDiscussions
                            .filter {
                                (selectedTopic == "All Topics" || it.topicTag.equals(selectedTopic, ignoreCase = true)) &&
                                    (socialQuery.isBlank() || it.title.contains(socialQuery, true) || it.body.contains(socialQuery, true) || it.authorName.contains(socialQuery, true))
                            }
                            .let { list -> if (sortTop) list.sortedByDescending { it.upvotes + if (it.id in upvotedSeedIds) 1 else 0 } else list }

                        items(filteredSeed, key = { it.id }) { disc ->
                            val isUpvoted = disc.id in upvotedSeedIds
                            val extraReplies = localSeedReplies[disc.id].orEmpty()
                            SeedDiscussionCard(
                                discussion = disc,
                                extraReplies = extraReplies,
                                isUpvoted = isUpvoted,
                                onToggleUpvote = {
                                    upvotedSeedIds = if (isUpvoted) upvotedSeedIds - disc.id else upvotedSeedIds + disc.id
                                },
                                onOpenBook = { onOpenBook(disc.bookId) },
                                onOpenReaderProfile = onOpenReaderProfile,
                                onReply = {
                                    if (!isSignedIn) onRequireLogin("Sign in to reply to book discussions")
                                    else seedReplyTarget = disc
                                }
                            )
                        }
                    }

                    SocialSubTab.SUMMARIES -> {
                        val filtered = CatalogSeed.bookSummaries.filter {
                            (selectedTopic == "All Topics" || it.topicTag.equals(selectedTopic, ignoreCase = true)) &&
                                (socialQuery.isBlank() || it.bookTitle.contains(socialQuery, true) || it.authorName.contains(socialQuery, true) || it.keyTakeaway.contains(socialQuery, true))
                        }
                        items(filtered, key = { it.id }) { sum ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 78.dp, height = 108.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onOpenBook(sum.bookId) }
                                        ) {
                                            Image(
                                                painter = painterResource(id = sum.coverRes),
                                                contentDescription = sum.bookTitle,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Icon(
                                                Icons.Filled.PlayCircleFilled,
                                                contentDescription = "Play summary",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(999.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "${sum.durationLabel} • ${sum.formatLabel}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Text(sum.bookTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text("by ${sum.authorName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(sum.keyTakeaway, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sum.youtubeUrl))
                                            runCatching { context.startActivity(intent) }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayCircleFilled, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Watch 2-Min Preview on YouTube")
                                    }
                                }
                            }
                        }
                    }

                    SocialSubTab.REVIEWS -> {
                        if (!isSignedIn) {
                            item {
                                MemberGateBanner(
                                    title = "Community Book Reviews",
                                    subtitle = "Sign in to read unfiltered reader reviews and see what friends marked Must Read.",
                                    onSignIn = { onRequireLogin("Sign in to read community book reviews") }
                                )
                            }
                        } else {
                            val filteredLive = liveReviews.filter {
                                (selectedTopic == "All Topics" || it.topicTag.equals(selectedTopic, ignoreCase = true)) &&
                                    (socialQuery.isBlank() || it.bookTitle.contains(socialQuery, true) || it.body.contains(socialQuery, true) || it.authorName.contains(socialQuery, true) || it.authorUsername.contains(socialQuery, true))
                            }
                            items(filteredLive, key = { "rev_live_${it.id}" }) { rev ->
                                val book = CatalogSeed.getBookById(rev.bookId)
                                val tier = RatingTier.fromKey(rev.ratingTier) ?: RatingTier.WORTH_READING
                                SplitPosterReviewCard(
                                    coverRes = book?.coverRes ?: R.drawable.img_book_cover_1,
                                    bookTitle = rev.bookTitle,
                                    reviewerName = rev.authorUsername,
                                    timeLabel = "Synced",
                                    tier = tier,
                                    body = rev.body,
                                    onClickBook = { onOpenBook(rev.bookId) },
                                    onClickReviewer = { onOpenReaderProfile(rev.authorUsername) }
                                )
                            }
                            val filteredSeed = CatalogSeed.seedReviews.filter {
                                (selectedTopic == "All Topics" || it.topicTag.equals(selectedTopic, ignoreCase = true)) &&
                                    (socialQuery.isBlank() || it.bookTitle.contains(socialQuery, true) || it.body.contains(socialQuery, true) || it.authorName.contains(socialQuery, true) || it.authorUsername.contains(socialQuery, true))
                            }
                            items(filteredSeed, key = { it.id }) { rev ->
                                SplitPosterReviewCard(
                                    coverRes = rev.coverRes,
                                    bookTitle = rev.bookTitle,
                                    reviewerName = rev.authorUsername,
                                    timeLabel = rev.timeAgo,
                                    tier = rev.ratingTier,
                                    body = rev.body,
                                    onClickBook = { onOpenBook(rev.bookId) },
                                    onClickReviewer = { onOpenReaderProfile(rev.authorUsername) }
                                )
                            }
                        }
                    }

                    SocialSubTab.COLLECTIONS -> {
                        val filteredPublic = publicCollections.filter {
                            (selectedTopic == "All Topics" || it.topicTag.equals(selectedTopic, ignoreCase = true)) &&
                                (socialQuery.isBlank() || it.title.contains(socialQuery, true) || it.description.contains(socialQuery, true) || it.creatorUsername.contains(socialQuery, true))
                        }
                        items(filteredPublic, key = { "col_live_${it.id}" }) { col ->
                            TwoUpCollectionShowcaseCard(
                                title = col.title,
                                curatorInitial = col.creatorName.take(1).uppercase(),
                                curatorUsername = col.creatorUsername,
                                bookIds = col.bookIds,
                                onVisit = { onOpenCollection(col.id) },
                                onOpenBook = onOpenBook,
                                onOpenCurator = { onOpenReaderProfile(col.creatorUsername) },
                                onOpenDiscuss = { activeSubTab = SocialSubTab.DISCUSSIONS.ordinal }
                            )
                        }
                        val filteredSeed = CatalogSeed.curatedCollections.filter {
                            (selectedTopic == "All Topics" || it.topicTag.equals(selectedTopic, ignoreCase = true)) &&
                                (socialQuery.isBlank() || it.title.contains(socialQuery, true) || it.description.contains(socialQuery, true) || it.curatorUsername.contains(socialQuery, true))
                        }
                        items(filteredSeed, key = { it.id }) { col ->
                            TwoUpCollectionShowcaseCard(
                                title = col.title,
                                curatorInitial = col.curatorName.take(1).uppercase(),
                                curatorUsername = col.curatorUsername,
                                bookIds = col.bookIds,
                                onVisit = { onOpenCollection(col.id) },
                                onOpenBook = onOpenBook,
                                onOpenCurator = { onOpenReaderProfile(col.curatorUsername) },
                                onOpenDiscuss = { activeSubTab = SocialSubTab.DISCUSSIONS.ordinal }
                            )
                        }
                    }

                    SocialSubTab.WHATS_NEW -> {
                        val filteredUpdates = CatalogSeed.whatsNew.filter {
                            socialQuery.isBlank() || it.title.contains(socialQuery, true) || it.description.contains(socialQuery, true) || it.versionTag.contains(socialQuery, true)
                        }
                        items(filteredUpdates, key = { it.id }) { item ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Violet Gradient Update Card matching Screenshot #7
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    ObsidianGlowTop,
                                                    Color(0xFF28124B),
                                                    Color(0xFF140B24)
                                                )
                                            )
                                        )
                                        .padding(18.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        NokhookBrandMark(tint = Color.White)
                                        Text(
                                            text = "UPDATE • ${item.versionTag}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFDE68A),
                                            textDecoration = TextDecoration.Underline
                                        )
                                        Text(
                                            text = item.title.uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .clickable { activeSubTab = SocialSubTab.DISCUSSIONS.ordinal }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "Discuss update", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("By nokhook Official • ${item.dateLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }

        if (currentTab == SocialSubTab.DISCUSSIONS) {
            FloatingActionButton(
                onClick = {
                    if (!isSignedIn) onRequireLogin("Sign in to start a community discussion")
                    else showNewDiscussionDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("new_discussion_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Start discussion")
            }
        }
    }

    if (showNewDiscussionDialog) {
        NewDiscussionDialog(
            onDismiss = { showNewDiscussionDialog = false },
            onSubmit = { topic, title, body ->
                onCreateDiscussion(topic, title, body)
                showNewDiscussionDialog = false
            }
        )
    }

    replyTarget?.let { (discId, parentId) ->
        ReplyDialog(
            onDismiss = { replyTarget = null },
            onSubmit = { body ->
                onPostReply(discId, parentId, body)
                replyTarget = null
            }
        )
    }

    seedReplyTarget?.let { targetDisc ->
        ReplyDialog(
            onDismiss = { seedReplyTarget = null },
            onSubmit = { body ->
                val newReply = com.example.model.SeedReply(
                    id = "local_${System.currentTimeMillis()}",
                    authorName = "You",
                    authorUsername = "reader",
                    timeAgo = "Just now",
                    body = body
                )
                val existing = localSeedReplies[targetDisc.id].orEmpty()
                localSeedReplies = localSeedReplies + (targetDisc.id to (existing + newReply))
                seedReplyTarget = null
            }
        )
    }
}

@Composable
private fun SplitPosterReviewCard(
    coverRes: Int,
    bookTitle: String,
    reviewerName: String,
    timeLabel: String,
    tier: RatingTier,
    body: String,
    onClickBook: () -> Unit,
    onClickReviewer: () -> Unit
) {
    // Side-by-side tall poster cover + dark review card matching Screenshot #10
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClickBook),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = coverRes),
                contentDescription = bookTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(0.46f)
                    .height(228.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Card(
                modifier = Modifier
                    .weight(0.54f)
                    .height(228.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onClickReviewer)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = reviewerName.take(1).uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "By @$reviewerName",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 7,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(
            text = bookTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Read the recent reviews",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    }
}

@Composable
private fun TwoUpCollectionShowcaseCard(
    title: String,
    curatorInitial: String,
    curatorUsername: String,
    bookIds: List<String>,
    onVisit: () -> Unit,
    onOpenBook: (String) -> Unit,
    onOpenCurator: () -> Unit,
    onOpenDiscuss: () -> Unit
) {
    // Two-up poster grid + "Visit Collection ->" matching Screenshot #13b
    val previewBooks = remember(bookIds) { bookIds.mapNotNull { CatalogSeed.getBookById(it) }.take(2) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (previewBooks.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                previewBooks.forEach { book ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenBook(book.id) },
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BookCoverThumb(
                            book = book,
                            showAppSelect = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(224.dp)
                        )
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${if (book.isSeriesOrManga) "Series" else "Book"} • ${book.year}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (previewBooks.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onOpenCurator)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = curatorInitial.ifBlank { "N" },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "@$curatorUsername",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onVisit)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Visit Collection",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.Underline
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Visit Collection",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenDiscuss)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "Discuss collection", modifier = Modifier.size(16.dp))
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    }
}

@Composable
private fun MemberGateBanner(
    title: String,
    subtitle: String,
    onSignIn: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onSignIn, shape = RoundedCornerShape(10.dp)) {
                Text("Login / Sign Up")
            }
        }
    }
}

@Composable
private fun LiveDiscussionCard(
    post: DiscussionPost,
    repliesFlow: Flow<List<DiscussionReply>>,
    onOpenReaderProfile: (String) -> Unit,
    onReply: (String) -> Unit
) {
    val replies by repliesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val topReplies = replies.filter { it.parentReplyId.isEmpty() }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${post.authorName} (@${post.authorUsername})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenReaderProfile(post.authorUsername) }
                )
                Text(post.topicTag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(post.title, style = MaterialTheme.typography.titleMedium)
            Text(post.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            TextButton(onClick = { onReply("") }, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reply (${replies.size})")
            }

            topReplies.forEach { reply ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${reply.authorName} (@${reply.authorUsername})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onOpenReaderProfile(reply.authorUsername) }
                    )
                    Text(reply.body, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { onReply(reply.id) }, contentPadding = PaddingValues(0.dp)) {
                        Text("Reply to thread", style = MaterialTheme.typography.labelSmall)
                    }
                    replies.filter { it.parentReplyId == reply.id }.forEach { nested ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 4.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "${nested.authorName} (@${nested.authorUsername})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onOpenReaderProfile(nested.authorUsername) }
                            )
                            Text(nested.body, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeedDiscussionCard(
    discussion: SeedDiscussion,
    extraReplies: List<com.example.model.SeedReply>,
    isUpvoted: Boolean,
    onToggleUpvote: () -> Unit,
    onOpenBook: () -> Unit,
    onOpenReaderProfile: (String) -> Unit,
    onReply: () -> Unit
) {
    val allReplies = remember(discussion.replies, extraReplies) { discussion.replies + extraReplies }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${discussion.authorName} • ${discussion.timeAgo}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenReaderProfile(discussion.authorUsername) }
                )
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(discussion.topicTag, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            Text(discussion.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.clickable(onClick = onOpenBook))
            Text(discussion.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleUpvote)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ThumbUp,
                        contentDescription = "Upvote discussion",
                        modifier = Modifier.size(15.dp),
                        tint = if (isUpvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${discussion.upvotes + if (isUpvoted) 1 else 0}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isUpvoted) FontWeight.Bold else FontWeight.Normal,
                        color = if (isUpvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onReply)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Forum, contentDescription = "Reply to discussion", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("${allReplies.size} replies • Reply", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            allReplies.filter { it.parentReplyId.isEmpty() }.forEach { reply ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("${reply.authorName} • ${reply.timeAgo}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(reply.body, style = MaterialTheme.typography.bodySmall)
                    allReplies.filter { it.parentReplyId == reply.id }.forEach { nested ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 4.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text("${nested.authorName} • ${nested.timeAgo}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(nested.body, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewDiscussionDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var topic by remember { mutableStateOf("Fiction") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start a Book Discussion") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CatalogSeed.allTopics.drop(1)) { t ->
                        FilterChip(selected = topic == t, onClick = { topic = t }, label = { Text(t) })
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Question or Topic Headline") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Share your take") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(topic, title.trim(), body.trim()) },
                enabled = title.trim().length >= 3 && body.trim().length >= 3
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ReplyDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var body by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post Reply") },
        text = {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Your reply") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(body.trim()) },
                enabled = body.trim().isNotEmpty()
            ) {
                Text("Reply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
