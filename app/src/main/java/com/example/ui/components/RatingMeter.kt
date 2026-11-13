package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.RatingMeterStats
import com.example.model.RatingTier

@Composable
fun RatingMeterCard(
    stats: RatingMeterStats,
    userSelectedTier: RatingTier?,
    onSelectTier: (RatingTier) -> Unit,
    onShare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val combinedStats = remember(stats, userSelectedTier) {
        stats.withUserVote(userSelectedTier)
    }
    val dominantTier = remember(combinedStats) { combinedStats.dominantTier() }
    val dominantPct = remember(combinedStats, dominantTier) {
        combinedStats.percentageFor(dominantTier)
    }

    var animated by remember(combinedStats) { mutableStateOf(false) }
    LaunchedEffect(combinedStats) { animated = true }
    val sweepAnim by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = tween(durationMillis = 750),
        label = "rating_meter_sweep"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "nokhook Meter",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share nokhook Meter",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Semi-Circular Meter (Thick Segmented Arc + Centered Bold % + Vote Tally)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val trackBg = MaterialTheme.colorScheme.surfaceVariant
            Canvas(
                modifier = Modifier.size(width = 256.dp, height = 136.dp)
            ) {
                val strokePx = 24.dp.toPx()
                val arcSize = Size(size.width - strokePx, (size.height * 2f) - strokePx)
                val topLeft = Offset(strokePx / 2f, strokePx / 2f)

                drawArc(
                    color = trackBg,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                val total = combinedStats.totalVotes.coerceAtLeast(1)
                val orderedTiers = listOf(
                    RatingTier.WORTH_READING to combinedStats.worthReadingVotes,
                    RatingTier.MUST_READ to combinedStats.mustReadVotes,
                    RatingTier.LIGHT_READ to combinedStats.lightReadVotes,
                    RatingTier.SKIP to combinedStats.skipVotes
                ).filter { it.second > 0 }

                var start = 180f
                val gapDegrees = if (orderedTiers.size > 1) 3.5f else 0f
                orderedTiers.forEach { (tier, votes) ->
                    val rawSweep = (votes.toFloat() / total) * 180f
                    val drawSweep = ((rawSweep - gapDegrees) * sweepAnim).coerceAtLeast(1f)
                    if (rawSweep >= 3f) {
                        drawArc(
                            color = tier.color,
                            startAngle = start + (gapDegrees / 2f),
                            sweepAngle = drawSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                    start += rawSweep
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Text(
                    text = "$dominantPct%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = dominantTier.color
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${combinedStats.mustReadVotes + combinedStats.worthReadingVotes}/${combinedStats.totalVotes} Votes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Clean Minimalist 4-Row Breakdown (Tap any tier to vote)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RatingTier.entries.forEach { tier ->
                val pct = combinedStats.percentageFor(tier)
                val isSelected = userSelectedTier == tier
                val pctDisplay = if (pct == 0) "<1%" else "$pct%"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) tier.color.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.background
                        )
                        .clickable { onSelectTier(tier) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("rating_tier_${tier.key}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(tier.color)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = if (isSelected) "${tier.label} (Your Vote)" else tier.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = pctDisplay,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) tier.color else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
