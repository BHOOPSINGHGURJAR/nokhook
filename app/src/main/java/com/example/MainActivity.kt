package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.AdminAccessPolicy
import com.example.auth.AuthGateBottomSheet
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOutUser
import com.example.data.CatalogOverrideStore
import com.example.data.CatalogSeed
import com.example.data.NokhookRepository
import com.example.model.BookCollection
import com.example.model.BookContribution
import com.example.model.BookReview
import com.example.model.DiscussionPost
import com.example.model.DiscussionReply
import com.example.model.ExploreFilterType
import com.example.model.FollowingEdge
import com.example.model.UserBookState
import com.example.model.UserProfile
import com.example.ui.components.NokhookTopBar
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthorOrPublisherScreen
import com.example.ui.screens.BookDetailScreen
import com.example.ui.screens.CollectionDetailScreen
import com.example.ui.screens.ContributionsScreen
import com.example.ui.screens.ExploreBrowseScreen
import com.example.ui.screens.HomeDiscoveryScreen
import com.example.ui.screens.LibraryShelfScreen
import com.example.ui.screens.ProfileAndMenuScreen
import com.example.ui.screens.PublicReaderProfileScreen
import com.example.ui.screens.SimilarVibesScreen
import com.example.ui.screens.SocialHubScreen
import com.example.ui.theme.NokhookTheme
import com.example.viewmodel.AuthenticatedViewModel
import com.example.viewmodel.GuestCatalogViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_EXPLORE = "explore"
private const val ROUTE_SOCIAL = "social"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_BOOK_DETAIL = "book_detail/{bookId}"
private const val ROUTE_SIMILAR_VIBES = "similar_vibes/{bookId}"
private const val ROUTE_AUTHOR = "author/{authorId}"
private const val ROUTE_PUBLISHER = "publisher/{publisherId}"
private const val ROUTE_COLLECTION = "collection/{collectionId}"
private const val ROUTE_CONTRIBUTIONS = "contributions"
private const val ROUTE_READER_PROFILE = "reader/{username}"
private const val ROUTE_ADMIN_DASHBOARD = "admin_dashboard"

private data class BottomNavDest(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavDest(ROUTE_EXPLORE, "Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    BottomNavDest(ROUTE_HOME, "Home", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday),
    BottomNavDest(ROUTE_SOCIAL, "Social", Icons.Filled.LocalCafe, Icons.Outlined.LocalCafe),
    BottomNavDest(ROUTE_LIBRARY, "Library", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    BottomNavDest(ROUTE_PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            var darkTheme by remember { mutableStateOf(true) }
            NokhookTheme(darkTheme = darkTheme) {
                NokhookAppRoot(
                    darkTheme = darkTheme,
                    onToggleDarkTheme = { darkTheme = it }
                )
            }
        }
    }
}

private data class AuthPromptRequest(
    val reason: String,
    val openSignUp: Boolean = false
)

@Composable
fun NokhookAppRoot(
    darkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val guestVm: GuestCatalogViewModel = viewModel()
    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        CatalogOverrideStore.initialize(context)
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    var authPrompt by remember { mutableStateOf<AuthPromptRequest?>(null) }

    val user = currentUser
    if (user == null) {
        NokhookMainShell(
            isSignedIn = false,
            guestVm = guestVm,
            authVm = null,
            userProfile = null,
            bookStates = emptyMap(),
            following = emptyList(),
            userCollections = emptyList(),
            publicCollections = emptyList(),
            publicReviews = emptyList(),
            publicDiscussions = emptyList(),
            contributions = emptyList(),
            observeReplies = { flowOf(emptyList()) },
            darkTheme = darkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
            onRequireLogin = { reason, signUp -> authPrompt = AuthPromptRequest(reason, signUp) },
            onLogout = {},
            currentUserEmail = null
        )
    } else {
        androidx.compose.runtime.key(user.uid) {
            val authVm: AuthenticatedViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        NokhookRepository(db, Firebase.auth).let { repo ->
                            AuthenticatedViewModel(repo, user.uid)
                        }
                    }
                }
            )

            val pendingDraft by guestVm.pendingDraft.collectAsStateWithLifecycle()
            LaunchedEffect(user.uid, pendingDraft) {
                authVm.ensureDefaultProfileIfMissing(pendingDraft, user.displayName)
                if (pendingDraft != null) {
                    guestVm.savePendingDraft(null)
                }
            }

            val profileState by authVm.userProfile.collectAsStateWithLifecycle()
            val bookStates by authVm.bookStates.collectAsStateWithLifecycle()
            val following by authVm.following.collectAsStateWithLifecycle()
            val userCollections by authVm.userCollections.collectAsStateWithLifecycle()
            val publicCollections by authVm.publicCollections.collectAsStateWithLifecycle()
            val publicReviews by authVm.publicReviews.collectAsStateWithLifecycle()
            val publicDiscussions by authVm.publicDiscussions.collectAsStateWithLifecycle()
            val contributions by authVm.contributions.collectAsStateWithLifecycle()

            NokhookMainShell(
                isSignedIn = true,
                guestVm = guestVm,
                authVm = authVm,
                userProfile = (profileState as? UiState.Success)?.data,
                bookStates = bookStates,
                following = following,
                userCollections = userCollections,
                publicCollections = publicCollections,
                publicReviews = publicReviews,
                publicDiscussions = publicDiscussions,
                contributions = contributions,
                observeReplies = { discId -> authVm.observeDiscussionReplies(discId) },
                darkTheme = darkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
                onRequireLogin = { _, _ -> },
                onLogout = {
                    signOutUser(credentialManager, scope)
                },
                currentUserEmail = user.email
            )
        }
    }

    authPrompt?.let { prompt ->
        AuthGateBottomSheet(
            actionReason = prompt.reason,
            initialSignUpMode = prompt.openSignUp,
            onDismiss = { authPrompt = null },
            onSaveDraft = { draft -> guestVm.savePendingDraft(draft) },
            onAuthSuccess = { authPrompt = null }
        )
    }
}

@Composable
private fun NokhookMainShell(
    isSignedIn: Boolean,
    guestVm: GuestCatalogViewModel,
    authVm: AuthenticatedViewModel?,
    userProfile: UserProfile?,
    bookStates: Map<String, UserBookState>,
    following: List<FollowingEdge>,
    userCollections: List<BookCollection>,
    publicCollections: List<BookCollection>,
    publicReviews: List<BookReview>,
    publicDiscussions: List<DiscussionPost>,
    contributions: List<BookContribution>,
    observeReplies: (String) -> Flow<List<DiscussionReply>>,
    darkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onRequireLogin: (String, Boolean) -> Unit,
    onLogout: () -> Unit,
    currentUserEmail: String?
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val uiScope = rememberCoroutineScope()

    val selectedTimeFilter by guestVm.selectedTimeFilter.collectAsStateWithLifecycle()
    val searchQuery by guestVm.searchQuery.collectAsStateWithLifecycle()
    val selectedTopic by guestVm.selectedTopic.collectAsStateWithLifecycle()
    val activeExploreFilter by guestVm.activeExploreFilter.collectAsStateWithLifecycle()
    val activeExploreSubFilter by guestVm.activeExploreSubFilter.collectAsStateWithLifecycle()
    val appSettings by CatalogOverrideStore.appSettings.collectAsStateWithLifecycle()
    val catalogVersion by CatalogOverrideStore.catalogVersion.collectAsStateWithLifecycle()

    var showMenuOverlay by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val actionMessage by (authVm?.actionMessage?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) })
    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            authVm?.clearMessage()
        }
    }

    val topLevelRoutes = setOf(ROUTE_HOME, ROUTE_EXPLORE, ROUTE_SOCIAL, ROUTE_LIBRARY, ROUTE_PROFILE)
    val showBottomBar = currentRoute in topLevelRoutes || currentRoute == ROUTE_BOOK_DETAIL
    val showTopBrandBar = currentRoute != ROUTE_SIMILAR_VIBES &&
        currentRoute != ROUTE_AUTHOR &&
        currentRoute != ROUTE_PUBLISHER &&
        currentRoute != ROUTE_COLLECTION &&
        currentRoute != ROUTE_CONTRIBUTIONS &&
        currentRoute != ROUTE_READER_PROFILE &&
        currentRoute != ROUTE_ADMIN_DASHBOARD

    fun navigateTopTab(route: String) {
        showMenuOverlay = false
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showTopBrandBar) {
                NokhookTopBar(
                    isSignedIn = isSignedIn,
                    onLogoClick = { navigateTopTab(ROUTE_HOME) },
                    onLoginClick = { onRequireLogin("Sign in to your nokhook reading account", false) },
                    onSignUpClick = { onRequireLogin("Create your @username on nokhook", true) },
                    onSearchClick = {
                        guestVm.selectExploreFilter(null, "All")
                        navigateTopTab(ROUTE_EXPLORE)
                    },
                    onNotificationsClick = { showNotificationsDialog = true },
                    onGridClick = {
                        guestVm.selectExploreFilter(null, "All")
                        navigateTopTab(ROUTE_EXPLORE)
                    },
                    onMenuClick = {
                        if (currentRoute == ROUTE_PROFILE && showMenuOverlay) {
                            showMenuOverlay = false
                        } else {
                            navigateTopTab(ROUTE_PROFILE)
                            showMenuOverlay = true
                        }
                    },
                    isMenuOpen = currentRoute == ROUTE_PROFILE && showMenuOverlay
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                alwaysShowLabel = false,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = ROUTE_HOME
            ) {
                composable(ROUTE_HOME) {
                    val leaderboard = remember(selectedTimeFilter, catalogVersion) {
                        guestVm.getUpcomingLeaderboard(selectedTimeFilter)
                    }
                    val todayTrending = remember(catalogVersion) { guestVm.getTodayTrending() }
                    HomeDiscoveryScreen(
                        selectedTimeFilter = selectedTimeFilter,
                        leaderboard = leaderboard,
                        todayTrending = todayTrending,
                        talkOfTheTown = remember(catalogVersion) { guestVm.getTalkOfTheTown() },
                        editorsPicks = remember(catalogVersion) { guestVm.getEditorsPicks() },
                        appSelectBooks = remember(catalogVersion) { guestVm.getAppSelectBooks() },
                        userBookStates = bookStates,
                        onSelectTimeFilter = { guestVm.setTimeFilter(it) },
                        onSeeAllTrending = {
                            guestVm.selectExploreFilter(ExploreFilterType.TODAY_TRENDING, "All")
                            navigateTopTab(ROUTE_EXPLORE)
                        },
                        onOpenBook = { bookId -> navController.navigate("book_detail/$bookId") },
                        appSettings = appSettings
                    )
                }

                composable(ROUTE_EXPLORE) {
                    val filtered = remember(activeExploreFilter, activeExploreSubFilter, searchQuery, catalogVersion) {
                        guestVm.getFilteredExploreBooks(activeExploreFilter, activeExploreSubFilter, searchQuery)
                    }
                    ExploreBrowseScreen(
                        searchQuery = searchQuery,
                        activeFilter = activeExploreFilter,
                        activeSubFilter = activeExploreSubFilter,
                        filteredBooks = filtered,
                        isSignedIn = isSignedIn,
                        onSearchChange = { guestVm.setSearchQuery(it) },
                        onSelectFilter = { filter, sub -> guestVm.selectExploreFilter(filter, sub) },
                        onOpenBook = { bookId -> navController.navigate("book_detail/$bookId") },
                        onOpenReaderProfile = { uname -> navController.navigate("reader/${android.net.Uri.encode(uname)}") },
                        onRequireLogin = { msg -> onRequireLogin(msg, false) }
                    )
                }

                composable(ROUTE_SOCIAL) {
                    SocialHubScreen(
                        selectedTopic = selectedTopic,
                        isSignedIn = isSignedIn,
                        following = following,
                        liveReviews = publicReviews,
                        liveDiscussions = publicDiscussions,
                        publicCollections = publicCollections,
                        observeReplies = observeReplies,
                        onSelectTopic = { guestVm.setSelectedTopic(it) },
                        onRequireLogin = { msg -> onRequireLogin(msg, false) },
                        onOpenBook = { bookId -> navController.navigate("book_detail/$bookId") },
                        onOpenCollection = { colId -> navController.navigate("collection/$colId") },
                        onOpenReaderProfile = { uname -> navController.navigate("reader/${android.net.Uri.encode(uname)}") },
                        onToggleFollow = { tid, uname, dname ->
                            authVm?.toggleFollow(tid, uname, dname)
                        },
                        onCreateDiscussion = { topic, title, body ->
                            authVm?.createDiscussion(topic, "", title, body)
                        },
                        onPostReply = { discId, parentId, body ->
                            authVm?.postReply(discId, parentId, body)
                        }
                    )
                }

                composable(ROUTE_LIBRARY) {
                    LibraryShelfScreen(
                        isSignedIn = isSignedIn,
                        bookStates = bookStates,
                        onRequestSignIn = { onRequireLogin("Sign in to access your Read, TBR & Interested shelves", false) },
                        onOpenBook = { bookId -> navController.navigate("book_detail/$bookId") }
                    )
                }

                composable(ROUTE_PROFILE) {
                    if (showMenuOverlay) {
                        BackHandler { showMenuOverlay = false }
                    }
                    val myReviews = remember(publicReviews, authVm?.currentUserId) {
                        val uid = authVm?.currentUserId
                        if (uid == null) emptyList() else publicReviews.filter { it.userId == uid }
                    }
                    ProfileAndMenuScreen(
                        isSignedIn = isSignedIn,
                        userProfile = userProfile,
                        bookStates = bookStates,
                        userReviews = myReviews,
                        userCollections = userCollections,
                        following = following,
                        darkModeOverride = darkTheme,
                        showMenuOverlay = showMenuOverlay,
                        onDismissMenuOverlay = { showMenuOverlay = false },
                        onToggleDarkMode = onToggleDarkTheme,
                        onRequestSignIn = { onRequireLogin("Sign in or register your @username on nokhook", false) },
                        onSaveProfile = { uname, fname, lname, cc, phone, verified, bio ->
                            authVm?.saveUserProfile(uname, fname, lname, cc, phone, verified, bio)
                        },
                        onOpenBook = { bookId -> navController.navigate("book_detail/$bookId") },
                        onOpenCollection = { colId -> navController.navigate("collection/$colId") },
                        onOpenContributions = { navController.navigate(ROUTE_CONTRIBUTIONS) },
                        onLogout = onLogout,
                        currentUserEmail = currentUserEmail,
                        onOpenAdminPanel = {
                            if (AdminAccessPolicy.isAdminEmail(currentUserEmail)) {
                                navController.navigate(ROUTE_ADMIN_DASHBOARD)
                            }
                        }
                    )
                }

                composable(ROUTE_ADMIN_DASHBOARD) {
                    AdminDashboardScreen(
                        currentUserEmail = currentUserEmail,
                        onBack = { navController.popBackStack() },
                        onShowMessage = { msg ->
                            uiScope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }

                composable(ROUTE_BOOK_DETAIL) { entry ->
                    val bookId = entry.arguments?.getString("bookId").orEmpty()
                    val book = remember(bookId, catalogVersion) { CatalogSeed.getBookById(bookId) }
                    if (book != null) {
                        BookDetailScreen(
                            book = book,
                            userBookState = bookStates[book.id],
                            isSignedIn = isSignedIn,
                            userCollections = userCollections,
                            publicCollections = publicCollections,
                            liveReviews = publicReviews,
                            onBack = { navController.popBackStack() },
                            onRequireLogin = { msg -> onRequireLogin(msg, false) },
                            onToggleStatus = { status -> authVm?.toggleBookStatus(book.id, status) },
                            onVoteRatingTier = { tier -> authVm?.voteRatingTier(book.id, tier) },
                            onFindSimilarVibes = { navController.navigate("similar_vibes/${book.id}") },
                            onOpenAuthor = { authorId -> navController.navigate("author/$authorId") },
                            onOpenPublisher = { pubId -> navController.navigate("publisher/$pubId") },
                            onOpenCollection = { colId -> navController.navigate("collection/$colId") },
                            onOpenReaderProfile = { uname -> navController.navigate("reader/${android.net.Uri.encode(uname)}") },
                            onAddBookToCollection = { col -> authVm?.addBookToCollection(col, book.id) },
                            onCreateCollectionWithBook = { title, desc, isPub ->
                                authVm?.createCollection(title, desc, isPub, listOf(book.id), book.genre)
                            },
                            onPostReview = { tier, body ->
                                authVm?.postReview(book.id, book.title, tier, book.genre, body)
                            }
                        )
                    }
                }

                composable(ROUTE_SIMILAR_VIBES) { entry ->
                    val bookId = entry.arguments?.getString("bookId").orEmpty()
                    val book = CatalogSeed.getBookById(bookId)
                    if (book != null) {
                        SimilarVibesScreen(
                            sourceBook = book,
                            onBack = { navController.popBackStack() },
                            onOpenBook = { targetId -> navController.navigate("book_detail/$targetId") }
                        )
                    }
                }

                composable(ROUTE_AUTHOR) { entry ->
                    val authorId = entry.arguments?.getString("authorId").orEmpty()
                    AuthorOrPublisherScreen(
                        isAuthor = true,
                        entityId = authorId,
                        onBack = { navController.popBackStack() },
                        onOpenBook = { bookId -> navController.navigate("book_detail/$bookId") }
                    )
                }

                composable(ROUTE_PUBLISHER) { entry ->
                    val pubId = entry.arguments?.getString("publisherId").orEmpty()
                    AuthorOrPublisherScreen(
                        isAuthor = false,
                        entityId = pubId,
                        onBack = { navController.popBackStack() },
                        onOpenBook = { bookId -> navController.navigate("book_detail/$bookId") }
                    )
                }

                composable(ROUTE_COLLECTION) { entry ->
                    val colId = entry.arguments?.getString("collectionId").orEmpty()
                    val customCol = (userCollections + publicCollections).firstOrNull { it.id == colId }
                    CollectionDetailScreen(
                        collectionId = colId,
                        customBookIds = customCol?.bookIds,
                        customTitle = customCol?.title,
                        customCurator = customCol?.let { "${it.creatorName} (@${it.creatorUsername})" },
                        customCuratorUsername = customCol?.creatorUsername,
                        customDescription = customCol?.description,
                        onBack = { navController.popBackStack() },
                        onOpenBook = { bookId -> navController.navigate("book_detail/$bookId") },
                        onOpenReaderProfile = { uname -> navController.navigate("reader/${android.net.Uri.encode(uname)}") }
                    )
                }

                composable(ROUTE_CONTRIBUTIONS) {
                    BackHandler { navController.popBackStack() }
                    ContributionsScreen(
                        contributions = contributions,
                        onBack = { navController.popBackStack() },
                        onSubmitContribution = { title, author, pub, lang, genre, notes ->
                            authVm?.submitContribution(title, author, pub, lang, genre, notes)
                        }
                    )
                }

                composable(ROUTE_READER_PROFILE) { entry ->
                    val username = android.net.Uri.decode(entry.arguments?.getString("username").orEmpty())
                    PublicReaderProfileScreen(
                        usernameOrName = username,
                        isSignedIn = isSignedIn,
                        following = following,
                        liveReviews = publicReviews,
                        publicCollections = publicCollections,
                        onBack = { navController.popBackStack() },
                        onRequireLogin = { msg -> onRequireLogin(msg, false) },
                        onToggleFollow = { tid, uname, dname ->
                            authVm?.toggleFollow(tid, uname, dname)
                        },
                        onOpenBook = { bookId -> navController.navigate("book_detail/$bookId") },
                        onOpenCollection = { colId -> navController.navigate("collection/$colId") }
                    )
                }
            }
        }
    }

    if (showNotificationsDialog) {
        val interestedBooks = remember(bookStates) {
            bookStates.values
                .filter { it.status == "interested" }
                .mapNotNull { CatalogSeed.getBookById(it.bookId) }
        }
        val topTrendingBook = remember { guestVm.getTodayTrending().firstOrNull()?.first }
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("Release & Trending Alerts") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (interestedBooks.isNotEmpty()) {
                        Text(
                            text = "Tracking ${interestedBooks.size} upcoming release(s):",
                            style = MaterialTheme.typography.labelLarge
                        )
                        interestedBooks.take(3).forEach { book ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        showNotificationsDialog = false
                                        navController.navigate("book_detail/${book.id}")
                                    },
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "Releasing ${book.releaseDateLabel} • Tap to view",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Mark any upcoming release as Interested to track launch dates here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (topTrendingBook != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showNotificationsDialog = false
                                    navController.navigate("book_detail/${topTrendingBook.id}")
                                },
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "#1 Today Trending: ${topTrendingBook.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "by ${topTrendingBook.authorName} • Tap to open",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
