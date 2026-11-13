package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.auth.AdminAccessPolicy
import com.example.data.CatalogOverrideStore
import com.example.data.CatalogSeed
import com.example.data.EditableAppSettings
import com.example.model.Book
import com.example.model.RatingMeterStats
import com.example.model.UserProfile
import com.example.model.VibeSlice
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ProfileAndMenuScreen
import com.example.ui.theme.NokhookTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AdminAccessAndSettingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        CatalogOverrideStore.resetAllForTesting()
    }

    @After
    fun tearDown() {
        CatalogOverrideStore.resetAllForTesting()
    }

    @Test
    fun adminAllowlist_grantsOnlySpecifiedTwoEmails() {
        assertTrue(AdminAccessPolicy.isAdminEmail("bhoopsinghgurjar001@gmail.com"))
        assertTrue(AdminAccessPolicy.isAdminEmail("  BHOOPSINGHGURJAR001@gmail.com "))
        assertTrue(AdminAccessPolicy.isAdminEmail("bs9396684@gmail.com"))
        assertTrue(AdminAccessPolicy.isAdminEmail("BS9396684@GMAIL.COM"))

        assertFalse(AdminAccessPolicy.isAdminEmail(null))
        assertFalse(AdminAccessPolicy.isAdminEmail(""))
        assertFalse(AdminAccessPolicy.isAdminEmail("reader@gmail.com"))
        assertFalse(AdminAccessPolicy.isAdminEmail("bhoopsinghgurjar002@gmail.com"))
    }

    @Test
    fun settingsDialog_hidesAdminPanelForNonAdminUsers() {
        composeRule.setContent {
            NokhookTheme(darkTheme = true) {
                ProfileAndMenuScreen(
                    isSignedIn = false,
                    userProfile = null,
                    bookStates = emptyMap(),
                    userReviews = emptyList(),
                    userCollections = emptyList(),
                    following = emptyList(),
                    darkModeOverride = true,
                    showMenuOverlay = false,
                    onDismissMenuOverlay = {},
                    onToggleDarkMode = {},
                    onRequestSignIn = {},
                    onSaveProfile = { _, _, _, _, _, _, _ -> },
                    onOpenBook = {},
                    onOpenCollection = {},
                    onOpenContributions = {},
                    onLogout = {},
                    currentUserEmail = "reader@gmail.com",
                    onOpenAdminPanel = {}
                )
            }
        }

        composeRule.onNodeWithTag("profile_settings_button").performClick()
        composeRule.onNodeWithTag("settings_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_admin_panel_button").assertDoesNotExist()
    }

    @Test
    fun settingsDialog_showsAndLaunchesAdminPanelForAuthorizedAdmin() {
        var openedAdmin = false
        val profile = UserProfile(
            id = "admin1",
            userId = "admin1",
            username = "bhoop_editor",
            firstName = "Bhoop",
            lastName = "Gurjar"
        )

        composeRule.setContent {
            NokhookTheme(darkTheme = true) {
                ProfileAndMenuScreen(
                    isSignedIn = true,
                    userProfile = profile,
                    bookStates = emptyMap(),
                    userReviews = emptyList(),
                    userCollections = emptyList(),
                    following = emptyList(),
                    darkModeOverride = true,
                    showMenuOverlay = true,
                    onDismissMenuOverlay = {},
                    onToggleDarkMode = {},
                    onRequestSignIn = {},
                    onSaveProfile = { _, _, _, _, _, _, _ -> },
                    onOpenBook = {},
                    onOpenCollection = {},
                    onOpenContributions = {},
                    onLogout = {},
                    currentUserEmail = "bs9396684@gmail.com",
                    onOpenAdminPanel = { openedAdmin = true }
                )
            }
        }

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithTag("settings_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_admin_panel_button").assertIsDisplayed().performClick()
        assertTrue(openedAdmin)
    }

    @Test
    fun adminDashboardScreen_blocksUnauthorizedEmail() {
        composeRule.setContent {
            NokhookTheme(darkTheme = true) {
                AdminDashboardScreen(
                    currentUserEmail = "unauthorized@gmail.com",
                    onBack = {},
                    onShowMessage = {}
                )
            }
        }

        composeRule.onNodeWithText("Admin Authorization Required").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_dashboard_screen").assertDoesNotExist()
    }

    @Test
    fun adminDashboardScreen_rendersTabsForAuthorizedAdmin() {
        composeRule.setContent {
            NokhookTheme(darkTheme = true) {
                AdminDashboardScreen(
                    currentUserEmail = "bhoopsinghgurjar001@gmail.com",
                    onBack = {},
                    onShowMessage = {}
                )
            }
        }

        composeRule.onNodeWithTag("admin_dashboard_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Admin Editorial Studio").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_tab_1").performClick()
        composeRule.onNodeWithTag("admin_add_book_button").assertIsDisplayed()
    }

    @Test
    fun catalogOverrideStore_appliesLiveSettingsAndBookEdits() {
        CatalogOverrideStore.updateAppSettings(
            EditableAppSettings(
                announcementBanner = "Monsoon Festival Live!",
                spotlightTitle = "Custom Spotlight Title"
            )
        )
        assertEquals("Monsoon Festival Live!", CatalogOverrideStore.appSettings.value.announcementBanner)
        assertEquals("Custom Spotlight Title", CatalogOverrideStore.appSettings.value.spotlightTitle)

        val customBook = Book(
            id = "custom_test_editorial",
            title = "Cloud Architecture Chronicles",
            subtitle = "Neon & Cloudinary",
            authorId = "auth_test",
            authorName = "Bhoop Gurjar",
            publisherId = "pub_test",
            publisherName = "nokhook Press",
            year = 2026,
            pages = 240,
            estimatedHours = 4,
            language = "English",
            country = "India",
            ageRating = "13+",
            genre = "Business",
            subGenre = "Engineering",
            awards = emptyList(),
            overview = "Editorial test addition",
            vibes = listOf(
                VibeSlice("Inspirational", 40, 0xFF00C288),
                VibeSlice("Dark", 10, 0xFFE53935),
                VibeSlice("Fast-paced", 25, 0xFF7B61FF),
                VibeSlice("Educational", 25, 0xFFF5A623)
            ),
            ratingStats = RatingMeterStats(50, 20, 5, 1),
            coverRes = R.drawable.img_book_cover_1,
            accentColorHex = 0xFF6D28D9,
            coverImageUrl = "https://res.cloudinary.com/demo/image/upload/sample.jpg"
        )
        CatalogOverrideStore.upsertBook(customBook)

        val resolved = CatalogSeed.getBookById("custom_test_editorial")
        assertEquals("Cloud Architecture Chronicles", resolved?.title)
        assertEquals("https://res.cloudinary.com/demo/image/upload/sample.jpg", resolved?.coverImageUrl)
    }
}
