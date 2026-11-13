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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.EditNote
import com.example.auth.AdminAccessPolicy
import com.example.ui.theme.EmeraldAccent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.CatalogSeed
import com.example.model.BookCollection
import com.example.model.BookContribution
import com.example.model.BookReview
import com.example.model.FollowingEdge
import com.example.model.RatingTier
import com.example.model.ReadingStatus
import com.example.model.UserBookState
import com.example.model.UserProfile
import com.example.ui.components.BookCoverThumb
import com.example.ui.theme.LogoutTintBg
import com.example.ui.theme.LogoutTintBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAndMenuScreen(
    isSignedIn: Boolean,
    userProfile: UserProfile?,
    bookStates: Map<String, UserBookState>,
    userReviews: List<BookReview>,
    userCollections: List<BookCollection>,
    following: List<FollowingEdge>,
    darkModeOverride: Boolean,
    showMenuOverlay: Boolean,
    onDismissMenuOverlay: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onRequestSignIn: () -> Unit,
    onSaveProfile: (String, String, String, String, String, Boolean, String) -> Unit,
    onOpenBook: (String) -> Unit,
    onOpenCollection: (String) -> Unit,
    onOpenContributions: () -> Unit,
    onLogout: () -> Unit,
    currentUserEmail: String? = null,
    onOpenAdminPanel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSearchBar by remember { mutableStateOf(false) }
    var compactView by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("All") }
    var formatFilter by remember { mutableStateOf("All") }
    var showFilterDropdown by remember { mutableStateOf(false) }
    var showFormatDropdown by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    if (showMenuOverlay) {
        // Full-Screen Menu Overlay matching Screenshot #1
        NokhookMenuContent(
            isSignedIn = isSignedIn,
            onMyProfile = {
                onDismissMenuOverlay()
                selectedTab = 0
            },
            onMyReviews = {
                onDismissMenuOverlay()
                selectedTab = 0
            },
            onMyContributions = {
                onDismissMenuOverlay()
                if (!isSignedIn) onRequestSignIn() else onOpenContributions()
            },
            onSettings = {
                onDismissMenuOverlay()
                showSettingsDialog = true
            },
            onLogout = {
                onDismissMenuOverlay()
                onLogout()
            },
            onPrivacy = { showPrivacyDialog = true },
            onContact = { showContactDialog = true }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!isSignedIn) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF475569)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                            }
                            Text("Browsing as Guest", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Create your @username or sign in with Google to save your Read shelf, collections, reviews, and book contributions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onRequestSignIn,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("profile_signin_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Login / Sign Up")
                            }
                        }
                    }

                    MenuCardButton(Icons.Filled.Settings, "Settings", Modifier.testTag("profile_settings_button")) { showSettingsDialog = true }
                    MenuCardButton(Icons.Filled.Lock, "Privacy Policy") { showPrivacyDialog = true }
                    MenuCardButton(Icons.Filled.Person, "Contact Us") { showContactDialog = true }
                }
            } else {
                val displayName = listOfNotNull(userProfile?.firstName, userProfile?.lastName)
                    .joinToString(" ").ifBlank { "nokhook Reader" }
                val username = userProfile?.username?.ifBlank { null } ?: "reader"
                val initials = displayName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifEmpty { "NR" }
                val joinedDate = remember(userProfile?.createdAt) {
                    val date = userProfile?.createdAt?.toDate() ?: Date()
                    SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date)
                }
                val publicCollectionsCount = userCollections.count { it.isPublic }
                val interestedBooks = remember(bookStates) {
                    bookStates.values
                        .filter { it.status == ReadingStatus.INTERESTED.key }
                        .mapNotNull { CatalogSeed.getBookById(it.bookId) }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile Header matching reference profile design
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
                                        .background(Color(0xFF475569)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    ProfileStatBlock(value = "${userReviews.size}", label = "Reviews\nPosted")
                                    ProfileStatBlock(value = "$publicCollectionsCount", label = "Public\nCollections")
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (userProfile?.phoneVerified == true) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Filled.Verified,
                                            contentDescription = "Verified reader",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "@$username",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val bioText = userProfile?.bio.orEmpty()
                                if (bioText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = bioText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${userProfile?.followersCount ?: 0} Followers  •  ${following.size} Following",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Joined $joinedDate",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text("Edit Profile", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    // Interested In Horizontal Carousel for user's marked Interested books
                    if (interestedBooks.isNotEmpty()) {
                        item {
                            InterestedInSection(
                                books = interestedBooks,
                                onOpenBook = onOpenBook
                            )
                        }
                    }

                    // Icon Tabs matching reference design
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
                                onClick = { selectedTab = 0; typeFilter = "All"; formatFilter = "All" },
                                icon = {
                                    Icon(
                                        Icons.Outlined.EditNote,
                                        contentDescription = "My Reviews",
                                        tint = if (selectedTab == 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1; typeFilter = "All"; formatFilter = "All" },
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistPlay,
                                        contentDescription = "My Collections",
                                        tint = if (selectedTab == 1) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }

                    // Dual Filter Pill Row + Search
                    item {
                        ProfileFilterControlsRow(
                            primaryLabel = if (typeFilter == "All") {
                                if (selectedTab == 0) "All Reviews" else "All Collections"
                            } else typeFilter,
                            secondaryLabel = if (formatFilter == "All") "All Types" else formatFilter,
                            primaryExpanded = showFilterDropdown,
                            secondaryExpanded = showFormatDropdown,
                            onTogglePrimaryMenu = { showFilterDropdown = it },
                            onToggleSecondaryMenu = { showFormatDropdown = it },
                            primaryOptions = if (selectedTab == 0) listOf("All", "Must Read", "Worth Reading", "Light Read", "Skip") else listOf("All", "Public", "Private"),
                            secondaryOptions = listOf("All", "Books", "Series & Manga"),
                            onSelectPrimary = { typeFilter = it },
                            onSelectSecondary = { formatFilter = it },
                            isCompactView = compactView,
                            onToggleCompactView = { compactView = !compactView },
                            showSearchBar = showSearchBar,
                            onToggleSearch = { showSearchBar = !showSearchBar },
                            searchText = searchText,
                            onSearchTextChange = { searchText = it },
                            searchPlaceholder = if (selectedTab == 0) "Search your reviews..." else "Search your collections..."
                        )
                    }

                    if (selectedTab == 0) {
                        val filteredReviews = userReviews.filter { rev ->
                            val matchesQuery = searchText.isBlank() ||
                                rev.bookTitle.contains(searchText, ignoreCase = true) ||
                                rev.body.contains(searchText, ignoreCase = true)
                            val tierLabel = RatingTier.fromKey(rev.ratingTier)?.label ?: ""
                            val matchesFilter = typeFilter == "All" || tierLabel.equals(typeFilter, ignoreCase = true)
                            val isSeries = CatalogSeed.getBookById(rev.bookId)?.isSeriesOrManga == true
                            val matchesFormat = when (formatFilter) {
                                "Books" -> !isSeries
                                "Series & Manga" -> isSeries
                                else -> true
                            }
                            matchesQuery && matchesFilter && matchesFormat
                        }
                        if (filteredReviews.isEmpty()) {
                            item {
                                EmptyProfileBox(
                                    title = "You haven't posted any reviews yet",
                                    subtitle = "Start sharing your opinions on books & series",
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                            items(filteredReviews, key = { it.id }) { rev ->
                                val tier = RatingTier.fromKey(rev.ratingTier) ?: RatingTier.WORTH_READING
                                ProfileReviewCard(
                                    bookId = rev.bookId,
                                    bookTitle = rev.bookTitle,
                                    tier = tier,
                                    body = rev.body,
                                    compact = compactView,
                                    onOpenBook = { onOpenBook(rev.bookId) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else {
                        val filteredCols = userCollections.filter { col ->
                            val matchesQuery = searchText.isBlank() || col.title.contains(searchText, ignoreCase = true)
                            val matchesFilter = when (typeFilter) {
                                "Public" -> col.isPublic
                                "Private" -> !col.isPublic
                                else -> true
                            }
                            matchesQuery && matchesFilter
                        }
                        if (filteredCols.isEmpty()) {
                            item {
                                EmptyProfileBox(
                                    title = "No collections created yet",
                                    subtitle = "Save books from any Book Detail page to curate your shelves",
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                            items(filteredCols, key = { it.id }) { col ->
                                ProfileCollectionRowCard(
                                    title = col.title,
                                    subtitle = "${col.bookIds.size} books • ${col.topicTag}",
                                    badge = if (col.isPublic) "Public" else "Private",
                                    onClick = { onOpenCollection(col.id) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && userProfile != null) {
        EditProfileDialog(
            profile = userProfile,
            onDismiss = { showEditDialog = false },
            onSave = { uname, fname, lname, bio ->
                onSaveProfile(
                    uname,
                    fname,
                    lname,
                    userProfile.countryCode,
                    userProfile.phoneNumber,
                    userProfile.phoneVerified,
                    bio
                )
                showEditDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        val isAuthorizedAdmin = AdminAccessPolicy.isAdminEmail(currentUserEmail)
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Settings") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_dialog"),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DarkMode, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Obsidian Dark Theme")
                        }
                        Switch(checked = darkModeOverride, onCheckedChange = onToggleDarkMode)
                    }

                    if (isAuthorizedAdmin) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = "Admin Editorial Studio",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Authorized Admin • ${currentUserEmail.orEmpty()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldAccent
                                        )
                                    }
                                }
                                Text(
                                    text = "Edit catalog books, Vibe Charts, home banners, app configuration, and moderate submissions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        showSettingsDialog = false
                                        onOpenAdminPanel()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_admin_panel_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Admin Panel")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSettingsDialog = false }) { Text("Done") } }
        )
    }

    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("Contact nokhook") },
            text = { Text("Have feedback, press inquiries, or partnership requests? Reach our editorial team anytime at hello@nokhook.app.") },
            confirmButton = { TextButton(onClick = { showContactDialog = false }) { Text("Close") } }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = { Text("nokhook isolates personal profile & phone information under owner-only access rules. Public reviews and collections never expose your mobile number or private account details.") },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("Understood") } }
        )
    }
}

@Composable
private fun NokhookMenuContent(
    isSignedIn: Boolean,
    onMyProfile: () -> Unit,
    onMyReviews: () -> Unit,
    onMyContributions: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onPrivacy: () -> Unit,
    onContact: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MenuCardButton(Icons.Filled.Person, "My Profile", onClick = onMyProfile)
        MenuCardButton(Icons.Filled.RateReview, "My Reviews", onClick = onMyReviews)
        MenuCardButton(Icons.Filled.AddCircleOutline, "My Contributions", onClick = onMyContributions)
        MenuCardButton(Icons.Filled.Settings, "Settings", onClick = onSettings)

        if (isSignedIn) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onLogout)
                    .testTag("menu_logout_button"),
                shape = RoundedCornerShape(14.dp),
                color = LogoutTintBg,
                border = BorderStroke(1.dp, LogoutTintBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Logout",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onPrivacy)
            )
            Text(
                text = "Contact Us",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onContact)
            )
        }
        Text(
            text = "© 2026 nokhook Literary Media",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun MenuCardButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StatBlock(value: String, label: String) {
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
private fun EmptyProfileBox(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var username by remember { mutableStateOf(profile.username) }
    var firstName by remember { mutableStateOf(profile.firstName) }
    var lastName by remember { mutableStateOf(profile.lastName) }
    var bio by remember { mutableStateOf(profile.bio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Reader Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, prefix = { Text("@") })
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") })
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") })
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(username.trim(), firstName.trim(), lastName.trim(), bio.trim()) },
                enabled = username.trim().length >= 2 && firstName.trim().isNotEmpty()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionsScreen(
    contributions: List<BookContribution>,
    onBack: () -> Unit,
    onSubmitContribution: (String, String, String, String, String, String) -> Unit
) {
    var bookTitle by remember { mutableStateOf("") }
    var authorName by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("Hindi") }
    var genre by remember { mutableStateOf("Fiction") }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Contributions", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Suggest a Missing Book or Author", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Submitted entries go live after editorial approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(value = bookTitle, onValueChange = { bookTitle = it }, label = { Text("Book Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("contrib_title"))
                    OutlinedTextField(value = authorName, onValueChange = { authorName = it }, label = { Text("Author Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("contrib_author"))
                    OutlinedTextField(value = publisher, onValueChange = { publisher = it }, label = { Text("Publisher") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Language *") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre *") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ISBN / Awards / Notes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            onSubmitContribution(bookTitle, authorName, publisher, language, genre, notes)
                            bookTitle = ""
                            authorName = ""
                            publisher = ""
                            notes = ""
                        },
                        enabled = bookTitle.isNotBlank() && authorName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("contrib_submit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Submit for Approval")
                    }
                }
            }

            Text("Your Submissions (${contributions.size})", style = MaterialTheme.typography.titleMedium)
            contributions.forEach { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.bookTitle, style = MaterialTheme.typography.titleSmall)
                            Text("${item.authorName} • ${item.language} • ${item.genre}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = item.status.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryShelfScreen(
    isSignedIn: Boolean,
    bookStates: Map<String, UserBookState>,
    onRequestSignIn: () -> Unit,
    onOpenBook: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(ReadingStatus.READ) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("My Library", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        if (!isSignedIn) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Text("Track Read, TBR & Interested", style = MaterialTheme.typography.titleMedium)
                    Text("Sign in to sync your personal reading shelves across devices.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onRequestSignIn, shape = RoundedCornerShape(12.dp)) {
                        Text("Login / Sign Up")
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ReadingStatus.READ, ReadingStatus.TBR, ReadingStatus.INTERESTED).forEach { st ->
                    val count = bookStates.values.count { it.status == st.key }
                    FilterChip(
                        selected = activeFilter == st,
                        onClick = { activeFilter = st },
                        label = { Text("${st.label} ($count)") }
                    )
                }
            }

            val matchingBooks = bookStates.values
                .filter { it.status == activeFilter.key }
                .mapNotNull { CatalogSeed.getBookById(it.bookId) }

            if (matchingBooks.isEmpty()) {
                EmptyProfileBox(
                    title = "No books in ${activeFilter.label} yet",
                    subtitle = "Open any book and tap Read, Read Later, or Mark as Interested"
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(matchingBooks, key = { it.id }) { book ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenBook(book.id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                BookCoverThumb(book = book, showAppSelect = false, modifier = Modifier.size(width = 60.dp, height = 86.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(book.title, style = MaterialTheme.typography.titleSmall)
                                    Text("${book.authorName} • ${book.genre}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
