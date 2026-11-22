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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ExamDefinition
import com.example.domain.model.OptionKey
import com.example.domain.model.Question
import com.example.domain.model.TestAttempt
import com.example.domain.model.TestMode
import com.example.domain.util.AnalyticsEngine
import com.example.ui.theme.NismColors
import com.example.ui.theme.TabularMonoStyle

private enum class ReviewFilter {
    ALL, CORRECT, INCORRECT, UNANSWERED, MARKED
}

private enum class ReviewStatus {
    CORRECT, INCORRECT, UNANSWERED
}

private data class ReviewItem(
    val index: Int,
    val question: Question,
    val selected: OptionKey?,
    val status: ReviewStatus,
    val isMarked: Boolean,
    val timedOut: Boolean,
    val timeSpent: Int
)

private data class ChapterScoreRow(
    val chapterNumber: Int,
    val title: String,
    val total: Int,
    val correct: Int,
    val incorrect: Int,
    val unanswered: Int
)

@Composable
fun ResultsScreen(
    attempt: TestAttempt?,
    exam: ExamDefinition,
    questionMap: Map<String, Question>,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onRetakeTest: (TestAttempt) -> Unit
) {
    BackHandler(onBack = onBack)

    if (attempt == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Attempt not found", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onBack) { Text("Back to Dashboard") }
            }
        }
        return
    }

    var filter by remember { mutableStateOf(ReviewFilter.ALL) }

    val reviewItems = remember(attempt, questionMap) {
        attempt.questionIds.mapIndexedNotNull { idx, qId ->
            val question = questionMap[qId] ?: return@mapIndexedNotNull null
            val selected = attempt.answers[qId]
            val status = when {
                selected == null -> ReviewStatus.UNANSWERED
                selected == question.correctAnswer -> ReviewStatus.CORRECT
                else -> ReviewStatus.INCORRECT
            }
            ReviewItem(
                index = idx + 1,
                question = question,
                selected = selected,
                status = status,
                isMarked = qId in attempt.markedForReview,
                timedOut = qId in attempt.timedOutQuestionIds,
                timeSpent = attempt.timeSpentPerQuestion[qId] ?: 0
            )
        }
    }

    val filteredItems = remember(reviewItems, filter) {
        when (filter) {
            ReviewFilter.ALL -> reviewItems
            ReviewFilter.CORRECT -> reviewItems.filter { it.status == ReviewStatus.CORRECT }
            ReviewFilter.INCORRECT -> reviewItems.filter { it.status == ReviewStatus.INCORRECT }
            ReviewFilter.UNANSWERED -> reviewItems.filter { it.status == ReviewStatus.UNANSWERED }
            ReviewFilter.MARKED -> reviewItems.filter { it.isMarked }
        }
    }

    val chapterBreakdown = remember(attempt.mode, reviewItems) {
        if (attempt.mode != TestMode.FULL_MOCK) emptyList()
        else {
            val map = linkedMapOf<Int, ChapterScoreRow>()
            for (item in reviewItems) {
                val chNum = item.question.chapterNumber
                val prev = map[chNum] ?: ChapterScoreRow(
                    chapterNumber = chNum,
                    title = item.question.chapterTitle,
                    total = 0,
                    correct = 0,
                    incorrect = 0,
                    unanswered = 0
                )
                map[chNum] = prev.copy(
                    total = prev.total + 1,
                    correct = prev.correct + if (item.status == ReviewStatus.CORRECT) 1 else 0,
                    incorrect = prev.incorrect + if (item.status == ReviewStatus.INCORRECT) 1 else 0,
                    unanswered = prev.unanswered + if (item.status == ReviewStatus.UNANSWERED) 1 else 0
                )
            }
            map.values.sortedBy { it.chapterNumber }
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
            item(key = "top_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("results_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back to Dashboard")
                    }

                    Button(
                        onClick = { onRetakeTest(attempt) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("results_retake_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (attempt.mode == TestMode.FULL_MOCK) "New Mock" else "Retake Drill")
                    }
                }
            }

            item(key = "hero_summary") {
                ResultsHeroCard(attempt = attempt, exam = exam, isDarkMode = isDarkMode)
            }

            if (chapterBreakdown.isNotEmpty()) {
                item(key = "chapter_breakdown") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Chapter-wise Score Breakdown", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(10.dp))
                            chapterBreakdown.forEachIndexed { i, ch ->
                                val pct = if (ch.total > 0) ((ch.correct.toDouble() / ch.total) * 100).toInt() else 0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Ch. ${ch.chapterNumber}: ${ch.title}", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${ch.total} Qs · ${ch.correct} Correct · ${ch.incorrect} Incorrect · ${ch.unanswered} Unans.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.titleSmall.merge(TabularMonoStyle),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (i < chapterBreakdown.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }

            item(key = "review_header_and_tabs") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Detailed Answer Review (${filteredItems.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val tabs = buildList {
                            add(ReviewFilter.ALL to "All (${reviewItems.size})")
                            add(ReviewFilter.CORRECT to "Correct (${attempt.correctCount})")
                            add(ReviewFilter.INCORRECT to "Incorrect (${attempt.incorrectCount})")
                            add(ReviewFilter.UNANSWERED to "Unanswered (${attempt.unansweredCount})")
                            if (attempt.markedForReview.isNotEmpty()) {
                                add(ReviewFilter.MARKED to "Marked (${attempt.markedForReview.size})")
                            }
                        }
                        items(tabs) { (tab, label) ->
                            val active = filter == tab
                            Surface(
                                onClick = { filter = tab },
                                shape = RoundedCornerShape(8.dp),
                                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.testTag("results_filter_${tab.name.lowercase()}")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            items(filteredItems, key = { "${it.index}_${it.question.id}" }) { item ->
                ReviewQuestionCard(item = item, isDarkMode = isDarkMode)
            }
        }
    }
}

@Composable
private fun ResultsHeroCard(
    attempt: TestAttempt,
    exam: ExamDefinition,
    isDarkMode: Boolean
) {
    val emeraldTint = if (isDarkMode) NismColors.EmeraldLight else NismColors.Emerald
    val roseTint = if (isDarkMode) NismColors.RoseLight else NismColors.Rose

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            val modeTitle = if (attempt.mode == TestMode.FULL_MOCK) {
                "Full Mock Test"
            } else {
                "Chapter ${attempt.chapterNumber}: ${attempt.chapterTitle}"
            }
            Text(
                text = "${exam.code} · $modeTitle · ${AnalyticsEngine.formatDateTime(attempt.completedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (attempt.autoSubmitted) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Auto-submitted on timer expiry",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDarkMode) NismColors.AmberLight else NismColors.Amber
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Examination Result & Answer Review",
                style = MaterialTheme.typography.headlineMedium
            )

            if (attempt.passed != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val passBg = if (attempt.passed) {
                    if (isDarkMode) NismColors.EmeraldBgDark else NismColors.EmeraldBgLight
                } else {
                    if (isDarkMode) NismColors.RoseBgDark else NismColors.RoseBgLight
                }
                val passTint = if (attempt.passed) emeraldTint else roseTint
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = passBg,
                    border = BorderStroke(1.dp, passTint.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (attempt.passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = passTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (attempt.passed) {
                                "PASSED (Pass Mark ${attempt.passingPercentage}%)"
                            } else {
                                "DID NOT PASS (Pass Mark ${attempt.passingPercentage}%)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = passTint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallStatTile("Marks", "${attempt.marksObtained}/${attempt.totalMarks}", Modifier.weight(1f))
                    SmallStatTile("Percentage", "${attempt.percentage}%", Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                    SmallStatTile("Accuracy", "${attempt.accuracy}%", Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallStatTile("Correct", "${attempt.correctCount}", Modifier.weight(1f), emeraldTint)
                    SmallStatTile("Incorrect", "${attempt.incorrectCount}", Modifier.weight(1f), roseTint)
                    SmallStatTile("Unanswered", "${attempt.unansweredCount}", Modifier.weight(1f))
                }
                SmallStatTile("Time Taken", AnalyticsEngine.formatDuration(attempt.timeTakenSeconds), Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(14.dp))
            val total = attempt.totalQuestions.coerceAtLeast(1).toFloat()
            val correctWeight = (attempt.correctCount / total).coerceIn(0f, 1f)
            val incorrectWeight = (attempt.incorrectCount / total).coerceIn(0f, 1f)
            val unansweredWeight = (1f - correctWeight - incorrectWeight).coerceAtLeast(0f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (correctWeight > 0f) {
                    Box(modifier = Modifier.weight(correctWeight).height(10.dp).background(NismColors.Emerald))
                }
                if (incorrectWeight > 0f) {
                    Box(modifier = Modifier.weight(incorrectWeight).height(10.dp).background(NismColors.Rose))
                }
                if (unansweredWeight > 0f) {
                    Box(modifier = Modifier.weight(unansweredWeight).height(10.dp).background(Color.Transparent))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Correct ${attempt.correctCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = emeraldTint
                )
                Text(
                    text = "Incorrect ${attempt.incorrectCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = roseTint
                )
                Text(
                    text = "Unanswered ${attempt.unansweredCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SmallStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.merge(TabularMonoStyle), color = valueColor)
        }
    }
}

@Composable
private fun ReviewQuestionCard(
    item: ReviewItem,
    isDarkMode: Boolean
) {
    val q = item.question
    val emeraldTint = if (isDarkMode) NismColors.EmeraldLight else NismColors.Emerald
    val roseTint = if (isDarkMode) NismColors.RoseLight else NismColors.Rose

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
                Text(
                    text = "Q${item.index} · Ch. ${q.chapterNumber}: ${q.chapterTitle}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                when (item.status) {
                    ReviewStatus.CORRECT -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = emeraldTint, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Correct", style = MaterialTheme.typography.labelMedium, color = emeraldTint)
                    }
                    ReviewStatus.INCORRECT -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = roseTint, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Incorrect", style = MaterialTheme.typography.labelMedium, color = roseTint)
                    }
                    ReviewStatus.UNANSWERED -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.timedOut) "Timed Out" else "Unanswered",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "${q.topic} · ${q.difficulty}" + if (item.timeSpent > 0) " · ${item.timeSpent}s spent" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = q.questionText, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionKey.entries.forEach { optKey ->
                    val isCorrectOpt = q.correctAnswer == optKey
                    val isStudentOpt = item.selected == optKey

                    val bgColor = when {
                        isCorrectOpt -> if (isDarkMode) NismColors.EmeraldBgDark else NismColors.EmeraldBgLight
                        isStudentOpt -> if (isDarkMode) NismColors.RoseBgDark else NismColors.RoseBgLight
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    }
                    val borderColor = when {
                        isCorrectOpt -> NismColors.Emerald
                        isStudentOpt -> NismColors.Rose
                        else -> MaterialTheme.colorScheme.outline
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = bgColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    isCorrectOpt -> NismColors.Emerald
                                    isStudentOpt -> NismColors.Rose
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = optKey.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCorrectOpt || isStudentOpt) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = q.options[optKey].orEmpty(), style = MaterialTheme.typography.bodyMedium)
                                if (isCorrectOpt || isStudentOpt) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (isCorrectOpt) {
                                            Text("Correct Answer", style = MaterialTheme.typography.labelSmall, color = emeraldTint)
                                        }
                                        if (isStudentOpt) {
                                            Text(
                                                text = "Your Selection",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isCorrectOpt) emeraldTint else roseTint
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Your Answer: ${item.selected?.let { "Option ${it.name}" } ?: "Not Answered"} · Correct: Option ${q.correctAnswer.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Explanation: ${q.explanation}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
