package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.ChapterPerformanceStat
import com.example.domain.model.ExamAnalyticsSummary
import com.example.domain.model.ExamDefinition
import com.example.domain.model.TrendPoint
import com.example.ui.components.MetricCard
import com.example.ui.theme.NismColors
import com.example.ui.theme.TabularMonoStyle
import kotlin.math.abs

@Composable
fun PerformanceScreen(
    exam: ExamDefinition,
    analytics: ExamAnalyticsSummary,
    totalAttempts: Int,
    isDarkMode: Boolean,
    onStartChapterPractice: (chapterNumber: Int, requestedCount: Int) -> Unit,
    onViewHistory: () -> Unit
) {
    var weakPracticeCount by remember { mutableIntStateOf(10) }

    val weakOrUnpractisedChapters = remember(analytics) {
        val practisedLow = analytics.chapterStats
            .filter { it.accuracy != null && it.accuracy < 75.0 }
            .sortedBy { it.accuracy ?: 0.0 }
        val unpractised = analytics.chapterStats.filter { it.questionsSeen == 0 }
        if (practisedLow.isNotEmpty()) {
            practisedLow + unpractised.take(maxOf(0, 4 - practisedLow.size))
        } else {
            (analytics.weakestChapters + unpractised).take(4)
        }
    }

    var selectedWeakChapterNum by remember(weakOrUnpractisedChapters) {
        mutableStateOf(
            weakOrUnpractisedChapters.firstOrNull()?.chapterNumber
                ?: exam.chapters.firstOrNull()?.chapterNumber
        )
    }

    val activeWeakStat = analytics.chapterStats.find { it.chapterNumber == selectedWeakChapterNum }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "analytics_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${exam.code} · Analytics & Insights",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Performance Dashboard",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onViewHistory,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("analytics_view_history_button")
                    ) {
                        Text("History ($totalAttempts)")
                    }
                }
            }

            item(key = "analytics_kpis") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "Overall Accuracy",
                            value = analytics.overallAccuracy?.let { "$it%" } ?: "—",
                            subtitle = "Attempted correct",
                            valueColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Tests Attempted",
                            value = "${analytics.totalTestsAttempted}",
                            subtitle = "${analytics.fullMockCount} Full · ${analytics.chapterPracticeCount} Chapter",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "Average Marks",
                            value = analytics.averageMarks?.toString() ?: "—",
                            subtitle = analytics.averageScorePercent?.let { "Avg $it%" } ?: "No tests yet",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Highest Marks",
                            value = analytics.highestMarksLabel ?: "—",
                            subtitle = analytics.highestScorePercent?.let { "Peak $it%" } ?: "No tests yet",
                            valueColor = if (isDarkMode) NismColors.EmeraldLight else NismColors.Emerald,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    MetricCard(
                        label = "Avg Time / Question",
                        value = analytics.avgTimePerQuestionSeconds?.let { "${it}s" } ?: "—",
                        subtitle = "Target pace <= 60s",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item(key = "trend_chart") {
                ImprovementChartCard(
                    trend = analytics.chronologicalTrend,
                    passingPercentage = exam.passingPercentage,
                    isDarkMode = isDarkMode
                )
            }

            item(key = "weak_chapters_widget") {
                PractiseWeakChaptersCard(
                    chapters = weakOrUnpractisedChapters,
                    selectedChapterNum = selectedWeakChapterNum,
                    activeWeakStat = activeWeakStat,
                    practiceCount = weakPracticeCount,
                    isDarkMode = isDarkMode,
                    onSelectChapter = { selectedWeakChapterNum = it },
                    onSelectCount = { weakPracticeCount = it },
                    onStartPractice = { chNum -> onStartChapterPractice(chNum, weakPracticeCount) }
                )
            }

            item(key = "strong_vs_weak") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChapterRankingCard(
                        title = "Strongest Chapters",
                        emptyText = "Attempt tests to identify your top-performing chapters.",
                        chapters = analytics.strongestChapters,
                        isStrong = true,
                        isDarkMode = isDarkMode
                    )
                    ChapterRankingCard(
                        title = "Weakest Chapters",
                        emptyText = "Attempt tests to highlight chapters needing targeted revision.",
                        chapters = analytics.weakestChapters,
                        isStrong = false,
                        isDarkMode = isDarkMode
                    )
                }
            }

            item(key = "full_breakdown_header") {
                Text(
                    text = "Complete Chapter-wise Performance",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(analytics.chapterStats, key = { it.chapterNumber }) { stat ->
                ChapterPerformanceRowCard(
                    stat = stat,
                    isDarkMode = isDarkMode,
                    onPractise = { onStartChapterPractice(stat.chapterNumber, 10) }
                )
            }
        }
    }
}

@Composable
private fun ImprovementChartCard(
    trend: List<TrendPoint>,
    passingPercentage: Int?,
    isDarkMode: Boolean
) {
    var selectedIdx by remember(trend) { mutableStateOf<Int?>(null) }

    val scoreLineColor = MaterialTheme.colorScheme.primary
    val accLineColor = if (isDarkMode) NismColors.EmeraldLight else NismColors.Emerald
    val gridColor = MaterialTheme.colorScheme.outline
    val passLineColor = if (isDarkMode) NismColors.AmberLight.copy(alpha = 0.7f) else NismColors.Amber.copy(alpha = 0.7f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = scoreLineColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Improvement Over Time", style = MaterialTheme.typography.titleMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendDot("Score %", scoreLineColor)
                    LegendDot("Accuracy %", accLineColor)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (trend.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Complete at least one test to plot your score and accuracy trajectory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(184.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .padding(end = 6.dp, top = 8.dp, bottom = 12.dp)
                            .height(164.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("100%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("50%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("0%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(184.dp)
                            .pointerInput(trend) {
                                detectTapGestures { offset ->
                                    val padLeft = 16f
                                    val padRight = 16f
                                    val innerW = (size.width - padLeft - padRight).coerceAtLeast(1f)
                                    if (trend.size == 1) {
                                        selectedIdx = 0
                                    } else {
                                        var closest = 0
                                        var minDist = Float.MAX_VALUE
                                        trend.indices.forEach { i ->
                                            val x = padLeft + (i.toFloat() / (trend.size - 1)) * innerW
                                            val d = abs(offset.x - x)
                                            if (d < minDist) {
                                                minDist = d
                                                closest = i
                                            }
                                        }
                                        selectedIdx = closest
                                    }
                                }
                            }
                    ) {
                        val padLeft = 10.dp.toPx()
                        val padRight = 10.dp.toPx()
                        val padTop = 12.dp.toPx()
                        val padBottom = 16.dp.toPx()
                        val innerW = (size.width - padLeft - padRight).coerceAtLeast(1f)
                        val innerH = (size.height - padTop - padBottom).coerceAtLeast(1f)

                        fun getX(idx: Int): Float =
                            if (trend.size <= 1) padLeft + innerW / 2f
                            else padLeft + (idx.toFloat() / (trend.size - 1)) * innerW

                        fun getY(pct: Double): Float =
                            padTop + innerH - (pct.coerceIn(0.0, 100.0).toFloat() / 100f) * innerH

                        listOf(0, 25, 50, 75, 100).forEach { pct ->
                            val y = getY(pct.toDouble())
                            drawLine(
                                color = gridColor,
                                start = Offset(padLeft, y),
                                end = Offset(size.width - padRight, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (pct == 0) null else PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                            )
                        }

                        if (passingPercentage != null) {
                            val passY = getY(passingPercentage.toDouble())
                            drawLine(
                                color = passLineColor,
                                start = Offset(padLeft, passY),
                                end = Offset(size.width - padRight, passY),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                            )
                        }

                        if (trend.size > 1) {
                            val accPath = Path()
                            val scorePath = Path()
                            val fillPath = Path()
                            val bottomY = getY(0.0)
                            trend.forEachIndexed { idx, pt ->
                                val x = getX(idx)
                                val yAcc = getY(pt.accuracy)
                                val yScore = getY(pt.scorePercent)
                                if (idx == 0) {
                                    accPath.moveTo(x, yAcc)
                                    scorePath.moveTo(x, yScore)
                                    fillPath.moveTo(x, bottomY)
                                    fillPath.lineTo(x, yScore)
                                } else {
                                    accPath.lineTo(x, yAcc)
                                    scorePath.lineTo(x, yScore)
                                    fillPath.lineTo(x, yScore)
                                }
                                if (idx == trend.lastIndex) {
                                    fillPath.lineTo(x, bottomY)
                                    fillPath.close()
                                }
                            }
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        scoreLineColor.copy(alpha = 0.22f),
                                        scoreLineColor.copy(alpha = 0.0f)
                                    ),
                                    startY = padTop,
                                    endY = bottomY
                                )
                            )
                            drawPath(
                                path = accPath,
                                color = accLineColor,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawPath(
                                path = scorePath,
                                color = scoreLineColor,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        trend.forEachIndexed { idx, pt ->
                            val x = getX(idx)
                            val isHighlighted = selectedIdx == idx
                            drawCircle(
                                color = accLineColor,
                                radius = if (isHighlighted) 5.5.dp.toPx() else 4.dp.toPx(),
                                center = Offset(x, getY(pt.accuracy))
                            )
                            drawCircle(
                                color = scoreLineColor,
                                radius = if (isHighlighted) 6.dp.toPx() else 4.5.dp.toPx(),
                                center = Offset(x, getY(pt.scorePercent))
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(10.dp))

                val target = selectedIdx?.let { trend.getOrNull(it) } ?: trend.last()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${target.label} (${target.dateShort})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Score: ${target.scorePercent}% · Accuracy: ${target.accuracy}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PractiseWeakChaptersCard(
    chapters: List<ChapterPerformanceStat>,
    selectedChapterNum: Int?,
    activeWeakStat: ChapterPerformanceStat?,
    practiceCount: Int,
    isDarkMode: Boolean,
    onSelectChapter: (Int) -> Unit,
    onSelectCount: (Int) -> Unit,
    onStartPractice: (Int) -> Unit
) {
    val amberTint = if (isDarkMode) NismColors.AmberLight else NismColors.Amber
    val roseTint = if (isDarkMode) NismColors.RoseLight else NismColors.Rose

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = amberTint, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Practise Weak Chapters", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select a low-accuracy or unpractised chapter below to launch an immediate 60s-per-question drill.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                chapters.forEach { ch ->
                    val isSelected = selectedChapterNum == ch.chapterNumber
                    Surface(
                        onClick = { onSelectChapter(ch.chapterNumber) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weak_chapter_select_${ch.chapterNumber}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ch. ${ch.chapterNumber}: ${ch.chapterTitle}",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${ch.availableQuestions} Qs available · ${ch.attemptsCount} attempts",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ch.accuracy?.let { "$it% acc" } ?: "Unpractised",
                                style = MaterialTheme.typography.labelSmall,
                                color = roseTint
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Number of Questions",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val sideBySide = maxWidth >= 560.dp
                val countSelector: @Composable (Modifier) -> Unit = { barModifier ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = barModifier
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(5, 10, 15, 20, 25).forEach { cnt ->
                                val active = practiceCount == cnt
                                Surface(
                                    onClick = { onSelectCount(cnt) },
                                    shape = RoundedCornerShape(7.dp),
                                    color = if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$cnt",
                                            style = MaterialTheme.typography.labelLarge.merge(TabularMonoStyle),
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val startButton: @Composable (Modifier) -> Unit = { btnModifier ->
                    if (activeWeakStat != null) {
                        val countLabel = minOf(practiceCount, activeWeakStat.availableQuestions)
                        Button(
                            onClick = { onStartPractice(activeWeakStat.chapterNumber) },
                            enabled = activeWeakStat.availableQuestions > 0,
                            shape = RoundedCornerShape(10.dp),
                            modifier = btnModifier
                                .height(48.dp)
                                .testTag("practise_weak_chapter_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeWeakStat.availableQuestions > 0) {
                                    "Practise Ch. ${activeWeakStat.chapterNumber} ($countLabel Qs)"
                                } else {
                                    "Ch. ${activeWeakStat.chapterNumber} (0 Qs)"
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (sideBySide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        countSelector(Modifier.weight(1f))
                        startButton(Modifier.widthIn(min = 190.dp))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        countSelector(Modifier.fillMaxWidth())
                        startButton(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterRankingCard(
    title: String,
    emptyText: String,
    chapters: List<ChapterPerformanceStat>,
    isStrong: Boolean,
    isDarkMode: Boolean
) {
    val accentColor = if (isStrong) {
        if (isDarkMode) NismColors.EmeraldLight else NismColors.Emerald
    } else {
        if (isDarkMode) NismColors.RoseLight else NismColors.Rose
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isStrong) Icons.Default.EmojiEvents else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (chapters.isEmpty()) {
                Text(emptyText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    chapters.forEach { ch ->
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ch. ${ch.chapterNumber}: ${ch.chapterTitle}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${ch.accuracy ?: 0.0}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            LinearProgressIndicator(
                                progress = { ((ch.accuracy ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = accentColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterPerformanceRowCard(
    stat: ChapterPerformanceStat,
    isDarkMode: Boolean,
    onPractise: () -> Unit
) {
    val barColor = when {
        stat.accuracy == null -> MaterialTheme.colorScheme.outline
        stat.accuracy >= 70.0 -> if (isDarkMode) NismColors.EmeraldLight else NismColors.Emerald
        stat.accuracy >= 50.0 -> if (isDarkMode) NismColors.AmberLight else NismColors.Amber
        else -> if (isDarkMode) NismColors.RoseLight else NismColors.Rose
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ch. ${stat.chapterNumber}: ${stat.chapterTitle}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Weightage ~${stat.weightage}% · ${stat.availableQuestions} Qs in bank",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onPractise,
                    enabled = stat.availableQuestions > 0,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("analytics_practise_ch_${stat.chapterNumber}")
                ) {
                    Text("Practise", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Drills", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${stat.chapterPracticeAttempts}", style = MaterialTheme.typography.labelLarge.merge(TabularMonoStyle))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Attempted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${stat.questionsAttempted}", style = MaterialTheme.typography.labelLarge.merge(TabularMonoStyle))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Correct", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${stat.correctCount}", style = MaterialTheme.typography.labelLarge.merge(TabularMonoStyle))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Avg Time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = stat.avgTimePerQuestion?.let { "${it}s" } ?: "—",
                        style = MaterialTheme.typography.labelLarge.merge(TabularMonoStyle)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            if (stat.accuracy != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { (stat.accuracy / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${stat.accuracy}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Text(
                    text = "Not attempted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
