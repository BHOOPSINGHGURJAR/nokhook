package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.RatingMeterStats
import com.example.model.RatingTier
import com.example.model.VibeSlice
import com.example.ui.theme.VibeAmber
import com.example.ui.theme.VibePlum
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

enum class VibeChartDisplayMode(val label: String) {
    BAR("Bar Graph"),
    DONUT("Donut"),
    RADAR("Radar")
}

/**
 * Computes rating-weighted emotional vibe distribution ("Inspirational", "Dark", "Fast-paced",
 * "Educational") derived from a book's baseline mood tags and live community user ratings.
 */
fun computeRatingWeightedEmotionalVibes(
    baseVibes: List<VibeSlice>,
    ratingStats: RatingMeterStats?,
    userSelectedTier: RatingTier?
): List<VibeSlice> {
    if (baseVibes.isEmpty()) return emptyList()

    val effectiveStats = ratingStats?.withUserVote(userSelectedTier)
    val totalVotes = (effectiveStats?.totalVotes ?: 100).coerceAtLeast(1)
    val mustRatio = (effectiveStats?.mustReadVotes ?: 50).toFloat() / totalVotes
    val worthRatio = (effectiveStats?.worthReadingVotes ?: 30).toFloat() / totalVotes
    val lightRatio = (effectiveStats?.lightReadVotes ?: 15).toFloat() / totalVotes
    val skipRatio = (effectiveStats?.skipVotes ?: 5).toFloat() / totalVotes

    val rawScores = linkedMapOf(
        "Inspirational" to 12f + (mustRatio * 34f),
        "Dark" to 10f + (worthRatio * 20f) + (skipRatio * 18f),
        "Fast-paced" to 12f + (lightRatio * 38f) + (mustRatio * 12f),
        "Educational" to 10f + (worthRatio * 28f) + (mustRatio * 14f)
    )

    baseVibes.forEach { slice ->
        val p = slice.percentage.toFloat()
        when (slice.name.lowercase()) {
            "suspense", "mystery", "satire" -> {
                rawScores["Dark"] = (rawScores["Dark"] ?: 0f) + p * 0.70f
                rawScores["Fast-paced"] = (rawScores["Fast-paced"] ?: 0f) + p * 0.55f
            }
            "adventure" -> {
                rawScores["Fast-paced"] = (rawScores["Fast-paced"] ?: 0f) + p * 0.78f
                rawScores["Inspirational"] = (rawScores["Inspirational"] ?: 0f) + p * 0.35f
            }
            "psychology", "business", "clarity", "culture" -> {
                rawScores["Educational"] = (rawScores["Educational"] ?: 0f) + p * 0.80f
                rawScores["Inspirational"] = (rawScores["Inspirational"] ?: 0f) + p * 0.45f
            }
            "drama", "nostalgia" -> {
                rawScores["Inspirational"] = (rawScores["Inspirational"] ?: 0f) + p * 0.55f
                rawScores["Dark"] = (rawScores["Dark"] ?: 0f) + p * 0.40f
            }
            "romance", "humor" -> {
                rawScores["Fast-paced"] = (rawScores["Fast-paced"] ?: 0f) + p * 0.60f
                rawScores["Inspirational"] = (rawScores["Inspirational"] ?: 0f) + p * 0.50f
            }
            else -> {
                rawScores["Inspirational"] = (rawScores["Inspirational"] ?: 0f) + p * 0.40f
                rawScores["Educational"] = (rawScores["Educational"] ?: 0f) + p * 0.30f
            }
        }
    }

    val sum = rawScores.values.sum().coerceAtLeast(1f)
    val palette = mapOf(
        "Inspirational" to 0xFFF59E0B,
        "Dark" to 0xFF6366F1,
        "Fast-paced" to 0xFFEF4444,
        "Educational" to 0xFF10B981
    )

    val ordered = rawScores.entries.sortedByDescending { it.value }
    val rounded = ordered.map { (name, score) ->
        name to ((score / sum) * 100f).roundToInt().coerceAtLeast(4)
    }.toMutableList()

    val diff = 100 - rounded.sumOf { it.second }
    if (rounded.isNotEmpty() && diff != 0) {
        rounded[0] = rounded[0].first to (rounded[0].second + diff).coerceAtLeast(4)
    }

    return rounded.map { (name, pct) ->
        VibeSlice(
            name = name,
            percentage = pct,
            colorHex = palette[name] ?: 0xFF8B5CF6
        )
    }
}

/**
 * Generates a concise reader-rating driver label for the active emotional tag tooltip.
 */
fun computeEmotionalTagDriverSummary(
    tagName: String,
    ratingStats: RatingMeterStats?,
    userSelectedTier: RatingTier?
): String {
    val effective = ratingStats?.withUserVote(userSelectedTier) ?: return "Primary emotional tag"
    val mustPct = effective.percentageFor(RatingTier.MUST_READ)
    val worthPct = effective.percentageFor(RatingTier.WORTH_READING)
    val lightPct = effective.percentageFor(RatingTier.LIGHT_READ)
    return when (tagName) {
        "Inspirational" -> "Boosted by $mustPct% Must Read ratings"
        "Dark" -> "Shaped by $worthPct% Worth Reading & tone depth"
        "Fast-paced" -> "Driven by $lightPct% Light Read & page-turner votes"
        "Educational" -> "Backed by $worthPct% Worth Reading & $mustPct% Must Read"
        else -> "Weighted across ${formatCompactCount(effective.totalVotes)} ratings"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VibeChartCard(
    vibes: List<VibeSlice>,
    onFindSimilarVibes: () -> Unit,
    modifier: Modifier = Modifier,
    ratingStats: RatingMeterStats? = null,
    userSelectedTier: RatingTier? = null
) {
    if (vibes.isEmpty()) {
        VibeChartPlaceholder(
            onFindSimilarVibes = onFindSimilarVibes,
            modifier = modifier
        )
        return
    }

    val chartSlices = remember(vibes, ratingStats, userSelectedTier) {
        computeRatingWeightedEmotionalVibes(
            baseVibes = vibes,
            ratingStats = ratingStats,
            userSelectedTier = userSelectedTier
        )
    }
    val effectiveTotalRatings = remember(ratingStats, userSelectedTier) {
        ratingStats?.withUserVote(userSelectedTier)?.totalVotes ?: 0
    }

    var displayMode by remember { mutableStateOf(VibeChartDisplayMode.BAR) }
    var animated by remember(chartSlices, displayMode) { mutableStateOf(false) }
    LaunchedEffect(chartSlices, displayMode) { animated = true }
    val progress by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "vibe_chart_anim"
    )

    var selectedIndex by remember(chartSlices) { mutableIntStateOf(0) }
    val safeIndex = selectedIndex.coerceIn(0, (chartSlices.size - 1).coerceAtLeast(0))
    val activeVibe = chartSlices.getOrNull(safeIndex) ?: chartSlices.firstOrNull()
    val estimatedTagRatings = if (effectiveTotalRatings > 0 && activeVibe != null) {
        ((effectiveTotalRatings * activeVibe.percentage) / 100f).roundToInt()
    } else 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vibe_chart_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Vibe Chart",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (effectiveTotalRatings > 0) {
                        "Emotional tags from ${formatCompactCount(effectiveTotalRatings)} user ratings"
                    } else {
                        "Emotional tags weighted by reader ratings"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Chart Type Toggle (Bar Graph / Donut / Radar)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.testTag("vibe_chart_mode_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    VibeChartDisplayMode.entries.forEach { mode ->
                        val isModeSelected = mode == displayMode
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .clickable { displayMode = mode }
                                .testTag("vibe_mode_${mode.name.lowercase()}"),
                            shape = RoundedCornerShape(7.dp),
                            color = if (isModeSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (mode) {
                                        VibeChartDisplayMode.BAR -> Icons.Filled.BarChart
                                        VibeChartDisplayMode.DONUT -> Icons.Filled.DonutLarge
                                        VibeChartDisplayMode.RADAR -> Icons.Filled.Hexagon
                                    },
                                    contentDescription = "${mode.label} view",
                                    tint = if (isModeSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Interactive Tooltip Banner (detail callout synced with selected bar/slice/axis)
        if (activeVibe != null) {
            val driverHint = remember(activeVibe.name, ratingStats, userSelectedTier) {
                computeEmotionalTagDriverSummary(activeVibe.name, ratingStats, userSelectedTier)
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vibe_active_tooltip"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Color(activeVibe.colorHex).copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(activeVibe.colorHex))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = activeVibe.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (effectiveTotalRatings > 0) {
                                    "~${formatCompactCount(estimatedTagRatings)} ratings · $driverHint"
                                } else {
                                    driverHint
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(activeVibe.colorHex).copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${activeVibe.percentage}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(activeVibe.colorHex),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        when (displayMode) {
            VibeChartDisplayMode.BAR -> {
                VibeCartesianBarChart(
                    chartSlices = chartSlices,
                    safeIndex = safeIndex,
                    progress = progress,
                    onSelectIndex = { selectedIndex = it }
                )
            }
            VibeChartDisplayMode.DONUT -> {
                VibeInteractiveDonutChart(
                    chartSlices = chartSlices,
                    safeIndex = safeIndex,
                    activeVibe = activeVibe,
                    progress = progress,
                    onSelectIndex = { selectedIndex = it }
                )
            }
            VibeChartDisplayMode.RADAR -> {
                VibeEmotionalRadarChart(
                    chartSlices = chartSlices,
                    safeIndex = safeIndex,
                    progress = progress,
                    onSelectIndex = { selectedIndex = it }
                )
            }
        }

        // Interactive Emotional Tag Legend Pills (Inspirational, Dark, Fast-paced, Educational)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chartSlices.forEachIndexed { index, slice ->
                val isSelected = index == safeIndex
                Surface(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { selectedIndex = index }
                        .testTag("vibe_pill_${slice.name.lowercase()}"),
                    shape = RoundedCornerShape(999.dp),
                    color = if (isSelected) Color(slice.colorHex).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(slice.colorHex) else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(slice.colorHex))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${slice.name} (${slice.percentage}%)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (userSelectedTier != null) {
            Text(
                text = "Live-weighted with your \"${userSelectedTier.label}\" rating",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        FindSimilarVibesButton(onClick = onFindSimilarVibes)
    }
}

@Composable
private fun VibeCartesianBarChart(
    chartSlices: List<VibeSlice>,
    safeIndex: Int,
    progress: Float,
    onSelectIndex: (Int) -> Unit
) {
    val maxRaw = chartSlices.maxOfOrNull { it.percentage } ?: 50
    val maxChartPct = (((maxRaw + 15) / 20) * 20).coerceIn(40, 100)
    val yTicks = listOf(maxChartPct, (maxChartPct * 3) / 4, maxChartPct / 2, maxChartPct / 4, 0)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vibe_bar_chart")
            .semantics { contentDescription = "Emotional vibe bar chart" },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(174.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Y-Axis Percentage Labels
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(34.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    yTicks.forEach { tick ->
                        Text(
                            text = "$tick%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Plot Area with Dashed Cartesian Gridlines + Gradient Bars
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val steps = 4
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        for (i in 0..steps) {
                            val y = size.height * (i.toFloat() / steps)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (i == steps) null else dashEffect
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        chartSlices.forEachIndexed { index, slice ->
                            val isSelected = index == safeIndex
                            val sliceColor = Color(slice.colorHex)
                            val fraction = ((slice.percentage.toFloat() / maxChartPct) * progress)
                                .coerceIn(0.06f, 1f)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectIndex(index) }
                                    .testTag("vibe_bar_${slice.name.lowercase()}"),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) sliceColor.copy(alpha = 0.16f) else Color.Transparent
                                ) {
                                    Text(
                                        text = "${slice.percentage}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) sliceColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.74f)
                                        .fillMaxHeight(fraction * 0.78f)
                                        .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    if (isSelected) sliceColor else sliceColor.copy(alpha = 0.78f),
                                                    if (isSelected) sliceColor.copy(alpha = 0.72f) else sliceColor.copy(alpha = 0.42f)
                                                )
                                            )
                                        )
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(3.dp)
                                                .align(Alignment.TopCenter)
                                                .background(Color.White.copy(alpha = 0.75f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // X-Axis Labels aligned with Plot Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 42.dp, end = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                chartSlices.forEachIndexed { index, slice ->
                    val isSelected = index == safeIndex
                    Text(
                        text = slice.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectIndex(index) }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VibeInteractiveDonutChart(
    chartSlices: List<VibeSlice>,
    safeIndex: Int,
    activeVibe: VibeSlice?,
    progress: Float,
    onSelectIndex: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vibe_donut_chart")
            .semantics { contentDescription = "Emotional vibe donut chart" },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(204.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                val total = chartSlices.sumOf { it.percentage }.coerceAtLeast(1)
                Canvas(
                    modifier = Modifier
                        .size(192.dp)
                        .pointerInput(chartSlices) {
                            detectTapGestures { offset ->
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val dx = offset.x - centerX
                                val dy = offset.y - centerY
                                val dist = hypot(dx, dy)
                                val outerR = size.width / 2f
                                val innerR = outerR - 44.dp.toPx()
                                if (dist in innerR..outerR) {
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                    if (angle < 0f) angle += 360f
                                    var acc = 0f
                                    chartSlices.forEachIndexed { idx, slice ->
                                        val sweep = (slice.percentage.toFloat() / total) * 360f
                                        if (angle >= acc && angle <= acc + sweep) {
                                            onSelectIndex(idx)
                                            return@detectTapGestures
                                        }
                                        acc += sweep
                                    }
                                }
                            }
                        }
                ) {
                    val strokeWidth = 24.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    var currentStart = -90f
                    chartSlices.forEachIndexed { index, slice ->
                        val isSelected = index == safeIndex
                        val sweep = (slice.percentage.toFloat() / total) * 360f * progress
                        val activeWidth = if (isSelected) 30.dp.toPx() else strokeWidth
                        val sliceColor = Color(slice.colorHex)
                        drawArc(
                            color = if (isSelected) sliceColor else sliceColor.copy(alpha = 0.78f),
                            startAngle = currentStart + 2.5f,
                            sweepAngle = (sweep - 5f).coerceAtLeast(1f),
                            useCenter = false,
                            style = Stroke(width = activeWidth, cap = StrokeCap.Butt)
                        )
                        currentStart += (slice.percentage.toFloat() / total) * 360f
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = activeVibe?.name ?: "Mood",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${activeVibe?.percentage ?: 0}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = activeVibe?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun VibeEmotionalRadarChart(
    chartSlices: List<VibeSlice>,
    safeIndex: Int,
    progress: Float,
    onSelectIndex: (Int) -> Unit
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val fillPrimary = MaterialTheme.colorScheme.primary
    val topSlice = chartSlices.getOrNull(0)
    val rightSlice = chartSlices.getOrNull(1)
    val bottomSlice = chartSlices.getOrNull(2)
    val leftSlice = chartSlices.getOrNull(3)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vibe_radar_chart")
            .semantics { contentDescription = "Emotional vibe radar chart" },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (topSlice != null) {
                RadarAxisLabel(
                    slice = topSlice,
                    isSelected = safeIndex == 0,
                    onClick = { onSelectIndex(0) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (leftSlice != null) {
                    RadarAxisLabel(
                        slice = leftSlice,
                        isSelected = safeIndex == 3,
                        onClick = { onSelectIndex(3) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Canvas(
                    modifier = Modifier
                        .size(168.dp)
                        .pointerInput(chartSlices) {
                            detectTapGestures { offset ->
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val dx = offset.x - cx
                                val dy = offset.y - cy
                                val idx = if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
                                    if (dx >= 0f) 1 else 3
                                } else {
                                    if (dy <= 0f) 0 else 2
                                }
                                if (idx < chartSlices.size) onSelectIndex(idx)
                            }
                        }
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = (size.minDimension / 2f) - 12.dp.toPx()
                    val count = chartSlices.size.coerceAtLeast(3)
                    val maxPct = (chartSlices.maxOfOrNull { it.percentage } ?: 50).coerceAtLeast(50).toFloat()

                    // Concentric polygon grid rings (25%, 50%, 75%, 100%)
                    listOf(0.25f, 0.50f, 0.75f, 1.0f).forEach { scale ->
                        val ringPath = Path()
                        for (i in 0 until count) {
                            val angleRad = (-PI / 2.0) + (2.0 * PI * i / count)
                            val x = cx + (radius * scale * cos(angleRad)).toFloat()
                            val y = cy + (radius * scale * sin(angleRad)).toFloat()
                            if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                        }
                        ringPath.close()
                        drawPath(
                            path = ringPath,
                            color = gridColor,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Axis spokes
                    for (i in 0 until count) {
                        val angleRad = (-PI / 2.0) + (2.0 * PI * i / count)
                        val x = cx + (radius * cos(angleRad)).toFloat()
                        val y = cy + (radius * sin(angleRad)).toFloat()
                        drawLine(
                            color = gridColor,
                            start = Offset(cx, cy),
                            end = Offset(x, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Data polygon
                    val dataPoints = chartSlices.mapIndexed { i, slice ->
                        val angleRad = (-PI / 2.0) + (2.0 * PI * i / count)
                        val norm = ((slice.percentage / maxPct) * progress).coerceIn(0.15f, 1f)
                        Offset(
                            x = cx + (radius * norm * cos(angleRad)).toFloat(),
                            y = cy + (radius * norm * sin(angleRad)).toFloat()
                        )
                    }

                    val dataPath = Path()
                    dataPoints.forEachIndexed { idx, pt ->
                        if (idx == 0) dataPath.moveTo(pt.x, pt.y) else dataPath.lineTo(pt.x, pt.y)
                    }
                    dataPath.close()

                    drawPath(
                        path = dataPath,
                        color = fillPrimary.copy(alpha = 0.24f)
                    )
                    drawPath(
                        path = dataPath,
                        color = fillPrimary,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // Vertex markers
                    dataPoints.forEachIndexed { idx, pt ->
                        val sliceColor = Color(chartSlices[idx].colorHex)
                        val isSelected = idx == safeIndex
                        drawCircle(
                            color = sliceColor,
                            radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
                            center = pt
                        )
                        if (isSelected) {
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }

                if (rightSlice != null) {
                    RadarAxisLabel(
                        slice = rightSlice,
                        isSelected = safeIndex == 1,
                        onClick = { onSelectIndex(1) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            if (bottomSlice != null) {
                RadarAxisLabel(
                    slice = bottomSlice,
                    isSelected = safeIndex == 2,
                    onClick = { onSelectIndex(2) }
                )
            }
        }
    }
}

@Composable
private fun RadarAxisLabel(
    slice: VibeSlice,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(slice.colorHex).copy(alpha = 0.16f) else Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = slice.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(slice.colorHex) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${slice.percentage}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(slice.colorHex) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Compact horizontal multi-segment vibe bar + top 2 emotional tags for book cards and match lists.
 */
@Composable
fun CompactEmotionalVibeStrip(
    vibes: List<VibeSlice>,
    ratingStats: RatingMeterStats?,
    modifier: Modifier = Modifier
) {
    val weighted = remember(vibes, ratingStats) {
        computeRatingWeightedEmotionalVibes(
            baseVibes = vibes,
            ratingStats = ratingStats,
            userSelectedTier = null
        )
    }
    if (weighted.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            weighted.forEach { slice ->
                Box(
                    modifier = Modifier
                        .weight(slice.percentage.toFloat().coerceAtLeast(1f))
                        .fillMaxHeight()
                        .background(Color(slice.colorHex))
                )
            }
        }
        Text(
            text = weighted.take(3).joinToString(" · ") { "${it.name} ${it.percentage}%" },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun VibeChartPlaceholder(
    onFindSimilarVibes: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vibe_chart_placeholder"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Vibe Chart",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "Awaiting Vibes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(164.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val ringTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    val trackTint = MaterialTheme.colorScheme.surfaceVariant
                    Canvas(modifier = Modifier.size(156.dp)) {
                        val strokeWidth = 20.dp.toPx()
                        drawArc(
                            color = trackTint,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                        drawArc(
                            color = ringTint,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f)
                            )
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.DonutLarge,
                            contentDescription = "Vibe Chart placeholder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No Vibes Yet",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = "Community mood breakdown will appear here once readers tag Inspirational, Dark, Fast-paced, or Educational vibes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (onFindSimilarVibes != null) {
            FindSimilarVibesButton(onClick = onFindSimilarVibes)
        }
    }
}

@Composable
private fun FindSimilarVibesButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("find_similar_vibes_button"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.width(24.dp).height(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(VibePlum)
                        .align(Alignment.CenterStart)
                )
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .offset(x = 8.dp)
                        .clip(CircleShape)
                        .background(VibeAmber)
                        .align(Alignment.CenterStart)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Find Similar Vibes",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Find similar vibes",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
