package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.ChapterDefinition
import com.example.domain.model.ChapterPerformanceStat
import com.example.domain.model.ExamDefinition
import com.example.ui.theme.NismColors
import com.example.ui.theme.TabularMonoStyle

private val QUESTION_COUNT_OPTIONS = listOf(5, 10, 15, 20, 25)

private enum class ChapterFilterMode(val label: String) {
    ALL("All Chapters"),
    READY("Ready to Practise"),
    PRACTISED("Practised")
}

private enum class ChapterSortMode(val label: String) {
    SYLLABUS_ORDER("Ch. 1–12"),
    READY_FIRST("Available First"),
    WEIGHTAGE_DESC("Weightage ↓"),
    LOWEST_ACCURACY("Lowest Accuracy")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChapterPracticeScreen(
    exam: ExamDefinition,
    chapterStats: List<ChapterPerformanceStat>,
    isDarkMode: Boolean,
    onStartChapterPractice: (chapterNumber: Int, requestedCount: Int) -> Unit,
    onNavigateQuestionBank: () -> Unit
) {
    val countsByChapter = remember { mutableStateMapOf<Int, Int>() }
    var filterMode by remember { mutableStateOf(ChapterFilterMode.ALL) }
    var sortMode by remember { mutableStateOf(ChapterSortMode.SYLLABUS_ORDER) }

    val statsByChapter = remember(chapterStats) {
        chapterStats.associateBy { it.chapterNumber }
    }

    val readyCount = remember(exam.chapters, statsByChapter) {
        exam.chapters.count { (statsByChapter[it.chapterNumber]?.availableQuestions ?: 0) > 0 }
    }
    val practisedCount = remember(exam.chapters, statsByChapter) {
        exam.chapters.count { (statsByChapter[it.chapterNumber]?.questionsSeen ?: 0) > 0 }
    }

    val displayedChapters = remember(exam.chapters, statsByChapter, filterMode, sortMode) {
        val filtered = exam.chapters.filter { ch ->
            val stat = statsByChapter[ch.chapterNumber]
            when (filterMode) {
                ChapterFilterMode.ALL -> true
                ChapterFilterMode.READY -> (stat?.availableQuestions ?: 0) > 0
                ChapterFilterMode.PRACTISED -> (stat?.questionsSeen ?: 0) > 0
            }
        }
        when (sortMode) {
            ChapterSortMode.SYLLABUS_ORDER -> filtered.sortedBy { it.chapterNumber }
            ChapterSortMode.READY_FIRST -> filtered.sortedWith(
                compareByDescending<ChapterDefinition> {
                    (statsByChapter[it.chapterNumber]?.availableQuestions ?: 0) > 0
                }.thenByDescending {
                    statsByChapter[it.chapterNumber]?.availableQuestions ?: 0
                }.thenBy { it.chapterNumber }
            )
            ChapterSortMode.WEIGHTAGE_DESC -> filtered.sortedWith(
                compareByDescending<ChapterDefinition> { it.weightage }
                    .thenBy { it.chapterNumber }
            )
            ChapterSortMode.LOWEST_ACCURACY -> filtered.sortedWith(
                compareBy<ChapterDefinition> {
                    statsByChapter[it.chapterNumber]?.accuracy ?: Double.MAX_VALUE
                }.thenByDescending {
                    (statsByChapter[it.chapterNumber]?.availableQuestions ?: 0) > 0
                }.thenBy { it.chapterNumber }
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "chapters_header") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
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
                            Text(
                                text = "${exam.code} · Chapter Drills",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "60s / Q · Max 25",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Syllabus Chapters",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose any chapter, select how many questions you want to practise, and start an adaptive 60-second timed drill.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Filter pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ChapterFilterMode.entries.forEach { mode ->
                                val selected = filterMode == mode
                                val countLabel = when (mode) {
                                    ChapterFilterMode.ALL -> "${mode.label} (${exam.chapters.size})"
                                    ChapterFilterMode.READY -> "${mode.label} ($readyCount)"
                                    ChapterFilterMode.PRACTISED -> "${mode.label} ($practisedCount)"
                                }
                                Surface(
                                    onClick = { filterMode = mode },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 38.dp)
                                        .testTag("chapters_filter_${mode.name.lowercase()}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = countLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sort selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort chapters",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Sort:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ChapterSortMode.entries.forEach { sMode ->
                                val active = sortMode == sMode
                                Surface(
                                    onClick = { sortMode = sMode },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (active) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.testTag("chapters_sort_${sMode.name.lowercase()}")
                                ) {
                                    Text(
                                        text = sMode.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        color = if (active) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (displayedChapters.isEmpty()) {
                item(key = "chapters_empty_filter") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No chapters match this filter",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Switch back to All Chapters or import questions into the Question Bank.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = { filterMode = ChapterFilterMode.ALL },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Show All Chapters")
                            }
                        }
                    }
                }
            } else {
                items(displayedChapters, key = { it.chapterNumber }) { chapter ->
                    val stat = statsByChapter[chapter.chapterNumber]
                    val available = stat?.availableQuestions ?: 0
                    val defaultCount = if (available >= 10) 10 else minOf(available, 25).coerceAtLeast(5)
                    val requestedCount = countsByChapter[chapter.chapterNumber] ?: defaultCount
                    val effectiveCount = minOf(requestedCount, available, 25)

                    ChapterCardItem(
                        chapter = chapter,
                        stat = stat,
                        available = available,
                        requestedCount = requestedCount,
                        effectiveCount = effectiveCount,
                        isDarkMode = isDarkMode,
                        onSelectCount = { count -> countsByChapter[chapter.chapterNumber] = count },
                        onStartPractice = { onStartChapterPractice(chapter.chapterNumber, requestedCount) },
                        onNavigateQuestionBank = onNavigateQuestionBank
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterCardItem(
    chapter: ChapterDefinition,
    stat: ChapterPerformanceStat?,
    available: Int,
    requestedCount: Int,
    effectiveCount: Int,
    isDarkMode: Boolean,
    onSelectCount: (Int) -> Unit,
    onStartPractice: () -> Unit,
    onNavigateQuestionBank: () -> Unit
) {
    val hasQuestions = available > 0
    val emeraldTint = if (isDarkMode) NismColors.EmeraldLight else NismColors.Emerald
    val emeraldBg = if (isDarkMode) NismColors.EmeraldBgDark else NismColors.EmeraldBgLight
    val accuracyColor = when {
        stat?.accuracy == null -> MaterialTheme.colorScheme.onSurfaceVariant
        stat.accuracy >= 70.0 -> emeraldTint
        stat.accuracy >= 50.0 -> if (isDarkMode) NismColors.AmberLight else NismColors.Amber
        else -> if (isDarkMode) NismColors.RoseLight else NismColors.Rose
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chapter_card_${chapter.chapterNumber}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top badge row: Chapter number pill + Availability pill + Weightage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "CH ${chapter.chapterNumber.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (hasQuestions) emeraldBg else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (hasQuestions) emeraldTint.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (hasQuestions) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = emeraldTint,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (hasQuestions) "$available Qs Ready" else "0 Qs in Bank",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasQuestions) emeraldTint else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = "~${chapter.weightage}% weight",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chapter.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chapter.keyTopics.forEach { topic ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Compact 3-column chapter progress summary box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Drills",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${stat?.chapterPracticeAttempts ?: 0}",
                                style = MaterialTheme.typography.titleSmall.merge(TabularMonoStyle),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Accuracy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stat?.accuracy?.let { "$it%" } ?: "—",
                                style = MaterialTheme.typography.titleSmall.merge(TabularMonoStyle),
                                fontWeight = FontWeight.Bold,
                                color = accuracyColor
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Attempted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${stat?.questionsAttempted ?: 0}",
                                style = MaterialTheme.typography.titleSmall.merge(TabularMonoStyle),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (stat?.accuracy != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (stat.accuracy / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = accuracyColor,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(12.dp))

            if (!hasQuestions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Import questions for Chapter ${chapter.chapterNumber} in Question Bank to unlock drills.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = onNavigateQuestionBank,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("chapter_${chapter.chapterNumber}_import_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Qs", maxLines = 1)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Number of Questions",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (requestedCount > available) {
                            "Using all $available Qs"
                        } else {
                            "$effectiveCount Qs · ${effectiveCount} min max"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val sideBySide = maxWidth >= 560.dp
                    if (sideBySide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QuestionCountSegmentedBar(
                                chapterNumber = chapter.chapterNumber,
                                available = available,
                                selectedCount = requestedCount,
                                onSelectCount = onSelectCount,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = onStartPractice,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .widthIn(min = 176.dp)
                                    .testTag("start_chapter_${chapter.chapterNumber}_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Start ($effectiveCount Qs)",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuestionCountSegmentedBar(
                                chapterNumber = chapter.chapterNumber,
                                available = available,
                                selectedCount = requestedCount,
                                onSelectCount = onSelectCount,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = onStartPractice,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("start_chapter_${chapter.chapterNumber}_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Start Chapter Drill ($effectiveCount Qs)",
                                    style = MaterialTheme.typography.labelLarge,
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
}

@Composable
private fun QuestionCountSegmentedBar(
    chapterNumber: Int,
    available: Int,
    selectedCount: Int,
    onSelectCount: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            QUESTION_COUNT_OPTIONS.forEach { opt ->
                val active = selectedCount == opt
                val exceedsAvailable = opt > available
                Surface(
                    onClick = { onSelectCount(opt) },
                    shape = RoundedCornerShape(7.dp),
                    color = if (active) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = if (active) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    } else {
                        null
                    },
                    shadowElevation = if (active) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .testTag("chapter_${chapterNumber}_count_$opt")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$opt",
                            style = MaterialTheme.typography.labelLarge.merge(TabularMonoStyle),
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                active -> MaterialTheme.colorScheme.primary
                                exceedsAvailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
