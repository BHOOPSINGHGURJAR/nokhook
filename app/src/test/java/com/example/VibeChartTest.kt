package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.model.RatingMeterStats
import com.example.model.RatingTier
import com.example.model.VibeSlice
import com.example.ui.components.VibeChartCard
import com.example.ui.components.computeEmotionalTagDriverSummary
import com.example.ui.components.computeRatingWeightedEmotionalVibes
import com.example.ui.theme.NokhookTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VibeChartTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleVibes = listOf(
        VibeSlice("Suspense", 45, 0xFF4F46E5),
        VibeSlice("Psychology", 35, 0xFFE05D44),
        VibeSlice("Mystery", 20, 0xFF0D9488)
    )

    private val sampleStats = RatingMeterStats(
        mustReadVotes = 420,
        worthReadingVotes = 210,
        lightReadVotes = 45,
        skipVotes = 18
    )

    @Test
    fun emotionalVibes_reweightWhenUserSelectsRatingTier() {
        val baseline = computeRatingWeightedEmotionalVibes(
            baseVibes = sampleVibes,
            ratingStats = RatingMeterStats(10, 10, 10, 10),
            userSelectedTier = null
        )
        val afterMustRead = computeRatingWeightedEmotionalVibes(
            baseVibes = sampleVibes,
            ratingStats = RatingMeterStats(100, 10, 10, 10),
            userSelectedTier = RatingTier.MUST_READ
        )

        assertEquals(4, baseline.size)
        assertEquals(100, baseline.sumOf { it.percentage })
        assertEquals(100, afterMustRead.sumOf { it.percentage })

        val inspBefore = baseline.first { it.name == "Inspirational" }.percentage
        val inspAfter = afterMustRead.first { it.name == "Inspirational" }.percentage
        assertTrue("Expected Inspirational share to increase with Must Read ratings", inspAfter > inspBefore)
    }

    @Test
    fun driverSummary_reflectsRatingBreakdown() {
        val summary = computeEmotionalTagDriverSummary(
            tagName = "Inspirational",
            ratingStats = sampleStats,
            userSelectedTier = RatingTier.MUST_READ
        )
        assertTrue(summary.contains("Must Read"))
        assertTrue(summary.contains("%"))
    }

    @Test
    fun vibeChartCard_switchesBetweenBarDonutAndRadarModes() {
        composeRule.setContent {
            NokhookTheme(darkTheme = true) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    VibeChartCard(
                        vibes = sampleVibes,
                        ratingStats = sampleStats,
                        userSelectedTier = RatingTier.MUST_READ,
                        onFindSimilarVibes = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag("vibe_chart_section").assertIsDisplayed()
        composeRule.onNodeWithTag("vibe_bar_chart").assertIsDisplayed()
        composeRule.onNodeWithTag("vibe_active_tooltip").assertIsDisplayed()

        // Switch to Donut mode
        composeRule.onNodeWithTag("vibe_mode_donut").performClick()
        composeRule.onNodeWithTag("vibe_donut_chart").assertIsDisplayed()

        // Switch to Radar mode
        composeRule.onNodeWithTag("vibe_mode_radar").performClick()
        composeRule.onNodeWithTag("vibe_radar_chart").assertIsDisplayed()

        // Scroll to pill, click it, then scroll back to tooltip and verify it updated
        composeRule.onNodeWithTag("vibe_pill_educational")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("vibe_active_tooltip")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNode(
            hasAnyAncestor(hasTestTag("vibe_active_tooltip")) and hasText("Educational")
        ).assertIsDisplayed()
    }
}
