package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.AdminAccessPolicy
import com.example.data.BackendConfig
import com.example.data.CatalogOverrideStore
import com.example.data.CatalogSeed
import com.example.data.EditableAppSettings
import com.example.data.NokhookBackendClient
import com.example.model.Book
import com.example.model.BookContribution
import com.example.model.RatingMeterStats
import com.example.model.VibeSlice
import com.example.ui.components.BookCoverThumb
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SelectGold
import com.example.ui.theme.TierLightRead
import com.example.ui.theme.TierMustRead
import com.example.ui.theme.TierSkip
import com.example.ui.theme.TierWorthReading
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    currentUserEmail: String?,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    if (!AdminAccessPolicy.isAdminEmail(currentUserEmail)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Restricted Access") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = TierSkip, modifier = Modifier.size(36.dp))
                        Text("Admin Authorization Required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "This workspace is restricted strictly to authorized editorial administrators.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onBack) { Text("Return") }
                    }
                }
            }
        }
        return
    }

    val scope = rememberCoroutineScope()
    val backendClient = remember {
        val firebaseAuth = runCatching { Firebase.auth }.getOrNull()
        NokhookBackendClient(firebaseAuth)
    }

    val appSettings by CatalogOverrideStore.appSettings.collectAsStateWithLifecycle()
    val catalogVersion by CatalogOverrideStore.catalogVersion.collectAsStateWithLifecycle()
    val overridesMap by CatalogOverrideStore.bookOverrides.collectAsStateWithLifecycle()
    val moderationQueue by CatalogOverrideStore.moderationQueue.collectAsStateWithLifecycle()

    val books = remember(catalogVersion) { CatalogSeed.books }

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingBook by remember { mutableStateOf<Book?>(null) }
    var isCreatingNewBook by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("admin_dashboard_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Admin Editorial Studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Authorized: ${currentUserEmail.orEmpty()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val tabLabels = listOf("Overview", "Books & Vibes", "App & Banners", "Moderation")
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabLabels.forEachIndexed { index, title ->
                    val badgeCount = if (index == 3) moderationQueue.count { it.status == "pending" } else 0
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = if (badgeCount > 0) "$title ($badgeCount)" else title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("admin_tab_$index")
                    )
                }
            }

            when (selectedTab) {
                0 -> AdminOverviewTab(
                    books = books,
                    overridesCount = overridesMap.size + CatalogOverrideStore.customBooks.collectAsStateWithLifecycle().value.size,
                    moderationQueue = moderationQueue,
                    appSettings = appSettings,
                    onJumpToBooks = { selectedTab = 1 },
                    onJumpToSettings = { selectedTab = 2 }
                )

                1 -> AdminBooksTab(
                    books = books,
                    overridesMap = overridesMap,
                    onAddBook = {
                        isCreatingNewBook = true
                        editingBook = createBlankBookTemplate()
                    },
                    onEditBook = { book ->
                        isCreatingNewBook = false
                        editingBook = book
                    },
                    onResetSeedBook = { bookId ->
                        CatalogOverrideStore.resetSeedBookOverride(bookId)
                        onShowMessage("Restored seed values for book")
                    }
                )

                2 -> AdminSettingsTab(
                    currentSettings = appSettings,
                    onSaveSettings = { updated ->
                        CatalogOverrideStore.updateAppSettings(updated)
                        if (backendClient.isConfigured()) {
                            scope.launch {
                                backendClient.saveRemoteSettings(updated)
                                    .onSuccess { onShowMessage("Settings saved locally & synced to NeonDB") }
                                    .onFailure { err -> onShowMessage("Saved locally (Cloud sync pending: ${err.message})") }
                            }
                        } else {
                            onShowMessage("App banners & configuration updated live")
                        }
                    },
                    onResetSettings = {
                        CatalogOverrideStore.resetAppSettings()
                        onShowMessage("App settings restored to defaults")
                    }
                )

                3 -> AdminModerationTab(
                    queue = moderationQueue,
                    onApprove = { item ->
                        CatalogOverrideStore.moderateContribution(item.id, "approved")
                        if (backendClient.isConfigured()) {
                            scope.launch { backendClient.moderateRemoteContribution(item.id, "approved") }
                        }
                        onShowMessage("Approved & published \"${item.bookTitle}\" to catalog")
                    },
                    onReject = { item ->
                        CatalogOverrideStore.moderateContribution(item.id, "rejected")
                        if (backendClient.isConfigured()) {
                            scope.launch { backendClient.moderateRemoteContribution(item.id, "rejected") }
                        }
                        onShowMessage("Marked \"${item.bookTitle}\" as rejected")
                    }
                )
            }
        }
    }

    editingBook?.let { target ->
        AdminBookEditorDialog(
            book = target,
            isNew = isCreatingNewBook,
            onDismiss = { editingBook = null },
            onSave = { updatedBook ->
                CatalogOverrideStore.upsertBook(updatedBook)
                editingBook = null
                if (backendClient.isConfigured()) {
                    scope.launch {
                        backendClient.upsertRemoteBook(updatedBook, isNew = isCreatingNewBook)
                            .onSuccess { onShowMessage("\"${updatedBook.title}\" synced to NeonDB") }
                            .onFailure { err -> onShowMessage("Updated locally (Cloud sync pending: ${err.message})") }
                    }
                } else {
                    onShowMessage("Saved \"${updatedBook.title}\" — live across Home, Explore & Vibe Charts")
                }
            },
            onDelete = if (!isCreatingNewBook && !CatalogOverrideStore.isSeedBook(target.id)) {
                {
                    CatalogOverrideStore.deleteBook(target.id)
                    editingBook = null
                    onShowMessage("Deleted \"${target.title}\"")
                }
            } else {
                null
            }
        )
    }
}

@Composable
private fun AdminOverviewTab(
    books: List<Book>,
    overridesCount: Int,
    moderationQueue: List<BookContribution>,
    appSettings: EditableAppSettings,
    onJumpToBooks: () -> Unit,
    onJumpToSettings: () -> Unit
) {
    val totalVotes = remember(books) { books.sumOf { it.ratingStats.totalVotes } }
    val mustReadTotal = remember(books) { books.sumOf { it.ratingStats.mustReadVotes } }
    val worthTotal = remember(books) { books.sumOf { it.ratingStats.worthReadingVotes } }
    val lightTotal = remember(books) { books.sumOf { it.ratingStats.lightReadVotes } }
    val skipTotal = remember(books) { books.sumOf { it.ratingStats.skipVotes } }
    val featuredCount = remember(books) { books.count { it.isAppSelect || it.isEditorsPick } }
    val pendingCount = remember(moderationQueue) { moderationQueue.count { it.status == "pending" } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminKpiCard(
                    title = "Catalog Books",
                    value = books.size.toString(),
                    subtitle = "$overridesCount admin edit(s)",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    title = "Total Votes",
                    value = formatCompactNumber(totalVotes),
                    subtitle = "Across 4 tiers",
                    accent = EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminKpiCard(
                    title = "Featured Picks",
                    value = featuredCount.toString(),
                    subtitle = "Select & Editor's",
                    accent = SelectGold,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    title = "Pending Queue",
                    value = pendingCount.toString(),
                    subtitle = "Submissions",
                    accent = TierWorthReading,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Community Rating Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    val safeTotal = totalVotes.coerceAtLeast(1)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(999.dp))
                    ) {
                        val parts = listOf(
                            mustReadTotal to TierMustRead,
                            worthTotal to TierWorthReading,
                            lightTotal to TierLightRead,
                            skipTotal to TierSkip
                        )
                        parts.forEach { (count, color) ->
                            val w = (count.toFloat() / safeTotal).coerceAtLeast(0.02f)
                            Box(
                                modifier = Modifier
                                    .weight(w)
                                    .fillMaxSize()
                                    .background(color)
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TierCountLabel("Must Read", mustReadTotal, TierMustRead)
                        TierCountLabel("Worth", worthTotal, TierWorthReading)
                        TierCountLabel("Light", lightTotal, TierLightRead)
                        TierCountLabel("Skip", skipTotal, TierSkip)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CloudDone, contentDescription = null, tint = EmeraldAccent)
                        Text("Production Cloud Stack & RBAC", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    StackStatusRow("Frontend Web", "Vercel (`/frontend`)")
                    StackStatusRow("Backend APIs", "Vercel Functions (`/backend/api`)")
                    StackStatusRow("Primary Database", "NeonDB PostgreSQL (`schema.sql`)")
                    StackStatusRow("Image Storage", "Cloudinary (${BackendConfig.CLOUDINARY_CLOUD_NAME})")
                    StackStatusRow("Authentication", "Firebase Auth + Admin RBAC")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = SelectGold, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Authorized Admins: ${AdminAccessPolicy.ADMIN_EMAILS.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (BackendConfig.isConfigured()) {
                            "Remote API Endpoint Connected: ${BackendConfig.API_BASE_URL}"
                        } else {
                            "Local Editorial Sync Active — connect Vercel Backend URL in BackendConfig once NeonDB & Cloudinary env vars are set."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (BackendConfig.isConfigured()) EmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (appSettings.maintenanceMode) {
                        Text(
                            text = "Notice: Maintenance Mode flag is currently ON.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TierSkip,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onJumpToBooks,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Catalog")
                }
                OutlinedButton(
                    onClick = onJumpToSettings,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Banners")
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = accent)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TierCountLabel(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        Text(formatCompactNumber(count), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StackStatusRow(layer: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(layer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AdminBooksTab(
    books: List<Book>,
    overridesMap: Map<String, Book>,
    onAddBook: () -> Unit,
    onEditBook: (Book) -> Unit,
    onResetSeedBook: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(books, query) {
        val q = query.trim()
        if (q.isEmpty()) books
        else books.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.authorName.contains(q, ignoreCase = true) ||
                it.genre.contains(q, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search catalog books...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onAddBook,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("admin_add_book_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Book", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        items(filtered, key = { it.id }) { book ->
            val hasOverride = overridesMap.containsKey(book.id) || !CatalogOverrideStore.isSeedBook(book.id)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (hasOverride) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BookCoverThumb(
                        book = book,
                        showAppSelect = false,
                        modifier = Modifier.size(width = 56.dp, height = 80.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            book.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${book.authorName} • ${book.genre} (${book.language})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (book.isAppSelect) MiniAdminPill("Select", SelectGold)
                            if (book.isEditorsPick) MiniAdminPill("Editor's", MaterialTheme.colorScheme.primary)
                            if (book.isTalkOfTown) MiniAdminPill("Talk", EmeraldAccent)
                            if (hasOverride) MiniAdminPill("Edited", Color(0xFF38BDF8))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (overridesMap.containsKey(book.id)) {
                            IconButton(onClick = { onResetSeedBook(book.id) }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Reset book override", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedButton(
                            onClick = { onEditBook(book) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("admin_edit_book_${book.id}")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniAdminPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun AdminSettingsTab(
    currentSettings: EditableAppSettings,
    onSaveSettings: (EditableAppSettings) -> Unit,
    onResetSettings: () -> Unit
) {
    var appName by remember(currentSettings) { mutableStateOf(currentSettings.appName) }
    var tagline by remember(currentSettings) { mutableStateOf(currentSettings.tagline) }
    var announcementBanner by remember(currentSettings) { mutableStateOf(currentSettings.announcementBanner) }
    var spotlightTitle by remember(currentSettings) { mutableStateOf(currentSettings.spotlightTitle) }
    var spotlightSubtitle by remember(currentSettings) { mutableStateOf(currentSettings.spotlightSubtitle) }
    var requireLoginForReviews by remember(currentSettings) { mutableStateOf(currentSettings.requireLoginForReviews) }
    var allowContributions by remember(currentSettings) { mutableStateOf(currentSettings.allowContributions) }
    var maintenanceMode by remember(currentSettings) { mutableStateOf(currentSettings.maintenanceMode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Live App Branding & Home Banners", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("Editorial Tagline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_settings_tagline")
                )
                OutlinedTextField(
                    value = announcementBanner,
                    onValueChange = { announcementBanner = it },
                    label = { Text("Home Announcement Ticker (leave blank to hide)") },
                    placeholder = { Text("e.g., Monsoon Reading Week — Live Community Polls Open") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("admin_settings_announcement")
                )
                OutlinedTextField(
                    value = spotlightTitle,
                    onValueChange = { spotlightTitle = it },
                    label = { Text("Home Spotlight Banner Title Override") },
                    placeholder = { Text("Leave blank to auto-use top Editor's Pick") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_settings_spotlight_title")
                )
                OutlinedTextField(
                    value = spotlightSubtitle,
                    onValueChange = { spotlightSubtitle = it },
                    label = { Text("Home Spotlight Subtitle Override") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Community & Editorial Switches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AdminSwitchRow("Require Sign-In for Reviews", requireLoginForReviews) { requireLoginForReviews = it }
                AdminSwitchRow("Allow Book Contributions", allowContributions) { allowContributions = it }
                AdminSwitchRow("Maintenance Banner Mode", maintenanceMode) { maintenanceMode = it }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onResetSettings,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reset Defaults")
            }
            Button(
                onClick = {
                    onSaveSettings(
                        EditableAppSettings(
                            appName = appName.trim().ifBlank { "nokhook" },
                            tagline = tagline.trim().ifBlank { "Read, Rate & Discuss" },
                            announcementBanner = announcementBanner.trim(),
                            spotlightTitle = spotlightTitle.trim(),
                            spotlightSubtitle = spotlightSubtitle.trim(),
                            requireLoginForReviews = requireLoginForReviews,
                            allowContributions = allowContributions,
                            maintenanceMode = maintenanceMode
                        )
                    )
                },
                modifier = Modifier.weight(1f).testTag("admin_save_settings_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
private fun AdminSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AdminModerationTab(
    queue: List<BookContribution>,
    onApprove: (BookContribution) -> Unit,
    onReject: (BookContribution) -> Unit
) {
    if (queue.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No community book submissions in queue.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(queue, key = { it.id }) { item ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.bookTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${item.authorName} • ${item.language} • ${item.genre}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val statusColor = when (item.status) {
                            "approved" -> EmeraldAccent
                            "rejected" -> TierSkip
                            else -> SelectGold
                        }
                        MiniAdminPill(item.status.replaceFirstChar { it.uppercase() }, statusColor)
                    }
                    if (item.notes.isNotBlank()) {
                        Text(item.notes, style = MaterialTheme.typography.bodySmall)
                    }
                    if (item.status == "pending") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onApprove(item) },
                                modifier = Modifier.weight(1f).testTag("admin_approve_${item.id}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve & Publish")
                            }
                            OutlinedButton(
                                onClick = { onReject(item) },
                                modifier = Modifier.weight(1f).testTag("admin_reject_${item.id}"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminBookEditorDialog(
    book: Book,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Book) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(book.title) }
    var subtitle by remember { mutableStateOf(book.subtitle) }
    var authorName by remember { mutableStateOf(book.authorName) }
    var publisherName by remember { mutableStateOf(book.publisherName) }
    var genre by remember { mutableStateOf(book.genre) }
    var language by remember { mutableStateOf(book.language) }
    var yearStr by remember { mutableStateOf(book.year.toString()) }
    var coverImageUrl by remember { mutableStateOf(book.coverImageUrl) }
    var overview by remember { mutableStateOf(book.overview) }

    var isAppSelect by remember { mutableStateOf(book.isAppSelect) }
    var isEditorsPick by remember { mutableStateOf(book.isEditorsPick) }
    var isTalkOfTown by remember { mutableStateOf(book.isTalkOfTown) }
    var isUpcoming by remember { mutableStateOf(book.isUpcoming) }

    var mustVotesStr by remember { mutableStateOf(book.ratingStats.mustReadVotes.toString()) }
    var worthVotesStr by remember { mutableStateOf(book.ratingStats.worthReadingVotes.toString()) }
    var lightVotesStr by remember { mutableStateOf(book.ratingStats.lightReadVotes.toString()) }
    var skipVotesStr by remember { mutableStateOf(book.ratingStats.skipVotes.toString()) }

    fun findVibe(name: String, fallback: Int): Float =
        (book.vibes.firstOrNull { it.name.equals(name, ignoreCase = true) }?.percentage ?: fallback).toFloat()

    var inspSlider by remember { mutableFloatStateOf(findVibe("Inspirational", 35)) }
    var darkSlider by remember { mutableFloatStateOf(findVibe("Dark", 20)) }
    var fastSlider by remember { mutableFloatStateOf(findVibe("Fast-paced", 25)) }
    var eduSlider by remember { mutableFloatStateOf(findVibe("Educational", 20)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add Catalog Book" else "Edit Book & Vibe Profile") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Book Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_book_title_input")
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = authorName,
                    onValueChange = { authorName = it },
                    label = { Text("Author Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_book_author_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        label = { Text("Genre") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = language,
                        onValueChange = { language = it },
                        label = { Text("Language") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = publisherName,
                        onValueChange = { publisherName = it },
                        label = { Text("Publisher") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it.filter(Char::isDigit).take(4) },
                        label = { Text("Year") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(92.dp)
                    )
                }
                OutlinedTextField(
                    value = coverImageUrl,
                    onValueChange = { coverImageUrl = it },
                    label = { Text("Cover Image URL (Cloudinary)") },
                    placeholder = { Text("https://res.cloudinary.com/${BackendConfig.CLOUDINARY_CLOUD_NAME}/image/upload/...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = overview,
                    onValueChange = { overview = it },
                    label = { Text("Editorial Overview") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()
                Text("Discovery Placements", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                AdminSwitchRow("nokhook Select", isAppSelect) { isAppSelect = it }
                AdminSwitchRow("Editor's Pick", isEditorsPick) { isEditorsPick = it }
                AdminSwitchRow("Talk Of The Town", isTalkOfTown) { isTalkOfTown = it }
                AdminSwitchRow("Upcoming Release", isUpcoming) { isUpcoming = it }

                HorizontalDivider()
                Text("4-Tier Community Votes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mustVotesStr,
                        onValueChange = { mustVotesStr = it.filter(Char::isDigit) },
                        label = { Text("Must Read") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = worthVotesStr,
                        onValueChange = { worthVotesStr = it.filter(Char::isDigit) },
                        label = { Text("Worth") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lightVotesStr,
                        onValueChange = { lightVotesStr = it.filter(Char::isDigit) },
                        label = { Text("Light") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = skipVotesStr,
                        onValueChange = { skipVotesStr = it.filter(Char::isDigit) },
                        label = { Text("Skip") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()
                Text("Emotional Vibe Profile (auto-normalized to 100%)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                VibeSliderRow("Inspirational", inspSlider) { inspSlider = it }
                VibeSliderRow("Dark", darkSlider) { darkSlider = it }
                VibeSliderRow("Fast-paced", fastSlider) { fastSlider = it }
                VibeSliderRow("Educational", eduSlider) { eduSlider = it }

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = TierSkip)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Custom Book")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.trim().isNotEmpty() && authorName.trim().isNotEmpty(),
                onClick = {
                    val raw = listOf(inspSlider, darkSlider, fastSlider, eduSlider)
                    val sum = raw.sum().coerceAtLeast(1f)
                    val p0 = ((inspSlider / sum) * 100f).roundToInt()
                    val p1 = ((darkSlider / sum) * 100f).roundToInt()
                    val p2 = ((fastSlider / sum) * 100f).roundToInt()
                    val p3 = (100 - p0 - p1 - p2).coerceAtLeast(0)

                    val finalId = if (isNew && book.id.startsWith("custom_")) {
                        "custom_" + title.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_') + "_" + (System.currentTimeMillis() % 10000)
                    } else book.id

                    onSave(
                        book.copy(
                            id = finalId,
                            title = title.trim(),
                            subtitle = subtitle.trim(),
                            authorName = authorName.trim(),
                            publisherName = publisherName.trim().ifBlank { "Independent Press" },
                            genre = genre.trim().ifBlank { "Fiction" },
                            language = language.trim().ifBlank { "English" },
                            year = yearStr.toIntOrNull() ?: 2026,
                            coverImageUrl = coverImageUrl.trim(),
                            overview = overview.trim(),
                            isAppSelect = isAppSelect,
                            isEditorsPick = isEditorsPick,
                            isTalkOfTown = isTalkOfTown,
                            isUpcoming = isUpcoming,
                            ratingStats = RatingMeterStats(
                                mustReadVotes = mustVotesStr.toIntOrNull() ?: book.ratingStats.mustReadVotes,
                                worthReadingVotes = worthVotesStr.toIntOrNull() ?: book.ratingStats.worthReadingVotes,
                                lightReadVotes = lightVotesStr.toIntOrNull() ?: book.ratingStats.lightReadVotes,
                                skipVotes = skipVotesStr.toIntOrNull() ?: book.ratingStats.skipVotes
                            ),
                            vibes = listOf(
                                VibeSlice("Inspirational", p0, 0xFF00C288),
                                VibeSlice("Dark", p1, 0xFFE53935),
                                VibeSlice("Fast-paced", p2, 0xFF7B61FF),
                                VibeSlice("Educational", p3, 0xFFF5A623)
                            )
                        )
                    )
                },
                modifier = Modifier.testTag("admin_save_book_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun VibeSliderRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("${value.roundToInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f
        )
    }
}

private fun createBlankBookTemplate(): Book = Book(
    id = "custom_${System.currentTimeMillis()}",
    title = "",
    subtitle = "",
    authorId = "auth_community",
    authorName = "",
    publisherId = "pub_penguin",
    publisherName = "Penguin Literary",
    year = 2026,
    pages = 320,
    estimatedHours = 5,
    language = "English",
    country = "India",
    ageRating = "13+",
    genre = "Fiction",
    subGenre = "Contemporary",
    awards = emptyList(),
    overview = "",
    vibes = listOf(
        VibeSlice("Inspirational", 35, 0xFF00C288),
        VibeSlice("Dark", 20, 0xFFE53935),
        VibeSlice("Fast-paced", 25, 0xFF7B61FF),
        VibeSlice("Educational", 20, 0xFFF5A623)
    ),
    ratingStats = RatingMeterStats(mustReadVotes = 25, worthReadingVotes = 15, lightReadVotes = 5, skipVotes = 1),
    coverRes = R.drawable.img_book_cover_1,
    accentColorHex = 0xFF6D28D9,
    isTalkOfTown = true
)

private fun formatCompactNumber(value: Int): String = when {
    value >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", value / 1_000_000f)
    value >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", value / 1_000f)
    else -> value.toString()
}
