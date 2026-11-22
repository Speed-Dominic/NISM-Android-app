package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.ActiveTestSession
import com.example.domain.model.ExamAnalyticsSummary
import com.example.domain.model.ExamDefinition
import com.example.domain.model.TestAttempt
import com.example.domain.model.TestMode
import com.example.domain.util.AnalyticsEngine
import com.example.ui.components.MetricCard
import com.example.ui.components.PassFailChip
import com.example.ui.theme.NismColors
import com.example.ui.theme.TabularMonoStyle

@Composable
fun HomeDashboardScreen(
    exam: ExamDefinition,
    questionCount: Int,
    analytics: ExamAnalyticsSummary,
    recentAttempts: List<TestAttempt>,
    activeSession: ActiveTestSession?,
    isDarkMode: Boolean,
    onStartFullMock: () -> Unit,
    onNavigateChapters: () -> Unit,
    onNavigateAnalytics: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateQuestionBank: () -> Unit,
    onReviewAttempt: (TestAttempt) -> Unit,
    onResumeSession: () -> Unit,
    onDiscardSession: () -> Unit
) {
    val emeraldTint = if (isDarkMode) NismColors.EmeraldLight else NismColors.Emerald

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeSession != null) {
                item(key = "resume_banner") {
                    ResumeSessionBanner(
                        session = activeSession,
                        isDarkMode = isDarkMode,
                        onResume = onResumeSession,
                        onDiscard = onDiscardSession
                    )
                }
            }

            item(key = "exam_header") {
                ExamHeroHeaderCard(
                    exam = exam,
                    questionCount = questionCount
                )
            }

            item(key = "kpi_strip") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "Tests Attempted",
                            value = "${analytics.totalTestsAttempted}",
                            subtitle = "${analytics.fullMockCount} Mock · ${analytics.chapterPracticeCount} Drill",
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            iconTint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Highest Score",
                            value = analytics.highestScorePercent?.let { "$it%" } ?: "—",
                            subtitle = analytics.highestMarksLabel?.let { "Best: $it marks" } ?: "No attempts yet",
                            valueColor = if (analytics.highestScorePercent != null) emeraldTint else MaterialTheme.colorScheme.onSurface,
                            icon = Icons.Default.EmojiEvents,
                            iconTint = if (isDarkMode) NismColors.AmberLight else NismColors.Amber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "Average Score",
                            value = analytics.averageScorePercent?.let { "$it%" } ?: "—",
                            subtitle = analytics.averageMarks?.let { "Avg $it marks" } ?: "Completed tests",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Overall Accuracy",
                            value = analytics.overallAccuracy?.let { "$it%" } ?: "—",
                            subtitle = "Correct / attempted",
                            icon = Icons.Default.CheckCircle,
                            iconTint = emeraldTint,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Syllabus Coverage Progress Card
                    val syllabusRatio = if (analytics.totalChaptersCount > 0) {
                        (analytics.chaptersPractisedCount.toFloat() / analytics.totalChaptersCount.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    Surface(
                        shape = RoundedCornerShape(14.dp),
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
                                Column {
                                    Text(
                                        text = "Syllabus Coverage",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${analytics.chaptersPractisedCount} of ${analytics.totalChaptersCount} chapters practised",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "${(syllabusRatio * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium.merge(TabularMonoStyle),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { syllabusRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "modes_header") {
                Text(
                    text = "Examination & Practice Modes",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item(key = "mode_full_mock") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "100 Qs · 100 Marks · 120 Min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Full Mock Test",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Simulates the real NISM examination with 100 chapter-weighted MCQs across the full syllabus, a single 120-minute countdown timer, review palette, and zero negative marking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (questionCount == 0) {
                                    "Question bank empty"
                                } else {
                                    "Auto-submits at 00:00 · Pass ${exam.passingPercentage ?: 50}%"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            if (questionCount == 0) {
                                Button(
                                    onClick = onNavigateQuestionBank,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .heightIn(min = 44.dp)
                                        .testTag("dashboard_add_questions_button")
                                ) {
                                    Text("Add Questions", maxLines = 1)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Button(
                                    onClick = onStartFullMock,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .heightIn(min = 44.dp)
                                        .testTag("dashboard_start_full_mock_button")
                                ) {
                                    Text("Start Mock", maxLines = 1)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            item(key = "mode_chapter_practice") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmarks,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "5–25 Qs · 60s / Question",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Chapter-wise Practice",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Master individual syllabus chapters with 5 to 25 randomized MCQs exclusively from your chosen chapter and a focused 60-second countdown per question.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${exam.chapters.size} chapters ready for targeted drills",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = onNavigateChapters,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("dashboard_select_chapter_button")
                            ) {
                                Text("Browse Chapters", maxLines = 1)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            item(key = "quick_links") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = onNavigateAnalytics,
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_open_analytics_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(7.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Performance", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Accuracy trends & weak chapters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = onNavigateHistory,
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_open_history_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(7.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Test History", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Review completed attempts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "recent_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Test Results",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (recentAttempts.isNotEmpty()) {
                        TextButton(
                            onClick = onNavigateHistory,
                            modifier = Modifier.testTag("dashboard_view_all_attempts_button")
                        ) {
                            Text("View All")
                        }
                    }
                }
            }

            if (recentAttempts.isEmpty()) {
                item(key = "recent_empty") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(11.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No completed tests yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Launch a 100-question Full Mock Test or pick a chapter to record your first benchmark score.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (questionCount == 0) {
                                    Button(
                                        onClick = onNavigateQuestionBank,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("empty_open_bank_button")
                                    ) {
                                        Text("Open Question Bank")
                                    }
                                } else {
                                    Button(
                                        onClick = onStartFullMock,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("empty_start_mock_button")
                                    ) {
                                        Text("Start Full Mock")
                                    }
                                    OutlinedButton(
                                        onClick = onNavigateChapters,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("empty_practise_chapter_button")
                                    ) {
                                        Text("Chapters")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(recentAttempts.take(5), key = { it.id }) { attempt ->
                    RecentAttemptRowCard(
                        attempt = attempt,
                        isDarkMode = isDarkMode,
                        onReview = { onReviewAttempt(attempt) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExamHeroHeaderCard(
    exam: ExamDefinition,
    questionCount: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_exam_banner),
                    contentDescription = "NISM Certification Exam Study Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.08f),
                                    Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = exam.code.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = exam.shortName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = exam.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroSpecPill("${exam.chapters.size} Chapters")
                    HeroSpecPill("$questionCount MCQs in Bank")
                    HeroSpecPill("Pass ${exam.passingPercentage ?: 50}%")
                    HeroSpecPill("0 Negative Marking")
                }
            }
        }
    }
}

@Composable
private fun HeroSpecPill(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun ResumeSessionBanner(
    session: ActiveTestSession,
    isDarkMode: Boolean,
    onResume: () -> Unit,
    onDiscard: () -> Unit
) {
    val bgColor = if (isDarkMode) NismColors.AmberBgDark else NismColors.AmberBgLight
    val borderColor = if (isDarkMode) NismColors.Amber else NismColors.Amber.copy(alpha = 0.5f)
    val accentColor = if (isDarkMode) NismColors.AmberLight else NismColors.Amber

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unfinished Examination Session Saved",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val modeText = if (session.mode == TestMode.FULL_MOCK) {
                        "Full Mock Test"
                    } else {
                        "Chapter ${session.chapterNumber}: ${session.chapterTitle}"
                    }
                    val timeText = if (session.mode == TestMode.FULL_MOCK) {
                        "${AnalyticsEngine.formatClock(session.remainingSeconds)} remaining"
                    } else {
                        "${AnalyticsEngine.formatClock(session.questionRemainingSeconds)} left on current Q"
                    }
                    Text(
                        text = "$modeText · Q${session.currentIndex + 1}/${session.questionIds.size} · ${session.answers.size} answered · $timeText",
                        style = MaterialTheme.typography.bodySmall.merge(TabularMonoStyle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDiscard,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("resume_banner_discard_button")
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discard")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NismColors.Amber),
                    modifier = Modifier.testTag("resume_banner_resume_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resume")
                }
            }
        }
    }
}

@Composable
private fun RecentAttemptRowCard(
    attempt: TestAttempt,
    isDarkMode: Boolean,
    onReview: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
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
                    Text(
                        text = if (attempt.mode == TestMode.FULL_MOCK) {
                            "Full Mock Test"
                        } else {
                            "Chapter ${attempt.chapterNumber}: ${attempt.chapterTitle}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${attempt.totalQuestions} Qs · ${AnalyticsEngine.formatDateTime(attempt.completedAt)}",
                        style = MaterialTheme.typography.bodySmall.merge(TabularMonoStyle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                PassFailChip(passed = attempt.passed, isDarkMode = isDarkMode)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Score: ${attempt.marksObtained}/${attempt.totalMarks} (${attempt.percentage}%) · Acc: ${attempt.accuracy}% · ${AnalyticsEngine.formatDuration(attempt.timeTakenSeconds)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onReview,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("recent_attempt_review_${attempt.id}")
                ) {
                    Text("Review", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
