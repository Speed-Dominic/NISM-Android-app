package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.ActiveTestSession
import com.example.domain.model.OptionKey
import com.example.domain.model.Question
import com.example.domain.model.TestMode
import com.example.domain.util.AnalyticsEngine
import com.example.ui.theme.NismColors
import com.example.ui.theme.TabularMonoStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamWorkbenchScreen(
    session: ActiveTestSession,
    questions: List<Question>,
    isDarkMode: Boolean,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onSelectAnswer: (OptionKey) -> Unit,
    onClearAnswer: () -> Unit,
    onToggleReview: () -> Unit,
    onNavigateToIndex: (Int) -> Unit,
    onSubmitTest: () -> Unit,
    onPauseAndExit: () -> Unit
) {
    DisposableEffect(Unit) {
        onStartTimer()
        onDispose {
            onStopTimer()
        }
    }

    var showSubmitDialog by remember { mutableStateOf(false) }
    var showPaletteSheet by remember { mutableStateOf(false) }
    var showPauseExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showPauseExitDialog = true
    }

    val currentQuestion = questions.getOrNull(session.currentIndex)
    val totalQuestions = questions.size
    val isFullMock = session.mode == TestMode.FULL_MOCK

    if (currentQuestion == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading question...")
        }
        return
    }

    val selectedOption = session.answers[currentQuestion.id]
    val isMarked = currentQuestion.id in session.markedForReview
    val isCurrentTimedOut = !isFullMock && (currentQuestion.id in session.timedOutQuestionIds)

    val isTimerCritical = if (isFullMock) {
        session.remainingSeconds <= 300
    } else {
        session.questionRemainingSeconds <= 10
    }

    val answeredCount = remember(session.answers, session.questionIds) {
        session.questionIds.count { session.answers.containsKey(it) }
    }
    val unansweredCount = totalQuestions - answeredCount
    val markedCount = session.markedForReview.size
    val answeredAndMarkedCount = remember(session.answers, session.markedForReview, session.questionIds) {
        session.questionIds.count { session.answers.containsKey(it) && it in session.markedForReview }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFullMock) {
                                    "${session.examName.substringBefore(":")} · Full Mock"
                                } else {
                                    "Ch. ${session.chapterNumber}: ${session.chapterTitle}"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Question ${session.currentIndex + 1} of $totalQuestions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val timerBg = if (isTimerCritical) {
                                if (isDarkMode) NismColors.RoseBgDark else NismColors.RoseBgLight
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                            val timerBorder = if (isTimerCritical) {
                                if (isDarkMode) NismColors.Rose else NismColors.Rose.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                            val timerTint = if (isTimerCritical) {
                                if (isDarkMode) NismColors.RoseLight else NismColors.Rose
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = timerBg,
                                border = BorderStroke(1.dp, timerBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Remaining Time",
                                        tint = timerTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = AnalyticsEngine.formatClock(
                                            if (isFullMock) session.remainingSeconds else session.questionRemainingSeconds
                                        ),
                                        style = MaterialTheme.typography.labelLarge.merge(TabularMonoStyle),
                                        fontWeight = FontWeight.Bold,
                                        color = timerTint,
                                        modifier = Modifier.testTag("exam_timer_text")
                                    )
                                }
                            }

                            Button(
                                onClick = { showSubmitDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("exam_submit_top_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Submit", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    if (!isFullMock) {
                        LinearProgressIndicator(
                            progress = { (session.questionRemainingSeconds / 60f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = if (session.questionRemainingSeconds <= 10) NismColors.Rose else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    } else {
                        val mockProgress = if (totalQuestions > 0) {
                            (answeredCount.toFloat() / totalQuestions.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        LinearProgressIndicator(
                            progress = { mockProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isFullMock) {
                            OutlinedButton(
                                onClick = onToggleReview,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isMarked) NismColors.Amber else MaterialTheme.colorScheme.outline
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("exam_mark_review_button")
                            ) {
                                Icon(
                                    imageVector = if (isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (isMarked) "Marked for review" else "Mark for review",
                                    tint = if (isMarked) NismColors.Amber else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onClearAnswer,
                            enabled = selectedOption != null && !isCurrentTimedOut,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("exam_clear_button")
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear answer", modifier = Modifier.size(18.dp))
                        }

                        OutlinedButton(
                            onClick = { showPaletteSheet = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("exam_palette_button")
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = "Question Palette", modifier = Modifier.size(18.dp))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onNavigateToIndex(session.currentIndex - 1) },
                            enabled = session.currentIndex > 0,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("exam_prev_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Prev")
                        }

                        if (session.currentIndex < totalQuestions - 1) {
                            Button(
                                onClick = { onNavigateToIndex(session.currentIndex + 1) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("exam_next_button")
                            ) {
                                Text("Next")
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Button(
                                onClick = { showSubmitDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NismColors.Emerald),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("exam_finish_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Finish")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 840.dp

            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
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
                                Text(
                                    text = "Q${session.currentIndex + 1} · Ch. ${currentQuestion.chapterNumber}: ${currentQuestion.chapterTitle}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (isFullMock) "1 Mark · No Neg." else "60s Limit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentQuestion.topic,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (isCurrentTimedOut) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDarkMode) NismColors.RoseBgDark else NismColors.RoseBgLight,
                                    border = BorderStroke(1.dp, NismColors.Rose.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = if (isDarkMode) NismColors.RoseLight else NismColors.Rose,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Time expired for this question. It has been marked unanswered automatically.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDarkMode) NismColors.RoseLight else NismColors.Rose
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = currentQuestion.questionText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("exam_question_text")
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OptionKey.entries.forEach { optKey ->
                                    val isSelected = selectedOption == optKey
                                    val optionText = currentQuestion.options[optKey].orEmpty()

                                    Surface(
                                        onClick = { onSelectAnswer(optKey) },
                                        enabled = !isCurrentTimedOut,
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("exam_option_${optKey.name}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                ),
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = optKey.name,
                                                        style = MaterialTheme.typography.labelLarge.merge(TabularMonoStyle),
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = optionText,
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isWideScreen) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Question Palette ($answeredCount/$totalQuestions)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            QuestionPaletteContent(
                                session = session,
                                answeredCount = answeredCount,
                                unansweredCount = unansweredCount,
                                markedCount = markedCount,
                                answeredAndMarkedCount = answeredAndMarkedCount,
                                isFullMock = isFullMock,
                                isDarkMode = isDarkMode,
                                onSelectIndex = onNavigateToIndex
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPaletteSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPaletteSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Question Palette ($answeredCount/$totalQuestions answered)",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.height(340.dp)) {
                    QuestionPaletteContent(
                        session = session,
                        answeredCount = answeredCount,
                        unansweredCount = unansweredCount,
                        markedCount = markedCount,
                        answeredAndMarkedCount = answeredAndMarkedCount,
                        isFullMock = isFullMock,
                        isDarkMode = isDarkMode,
                        onSelectIndex = { idx ->
                            showPaletteSheet = false
                            onNavigateToIndex(idx)
                        }
                    )
                }
            }
        }
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Confirm Test Submission", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Review your attempt summary before submitting. Once submitted, answers cannot be modified.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Total Qs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$totalQuestions", style = MaterialTheme.typography.titleMedium.merge(TabularMonoStyle))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Answered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$answeredCount", style = MaterialTheme.typography.titleMedium.merge(TabularMonoStyle), color = NismColors.Emerald)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Unanswered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$unansweredCount", style = MaterialTheme.typography.titleMedium.merge(TabularMonoStyle))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Marked", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$markedCount", style = MaterialTheme.typography.titleMedium.merge(TabularMonoStyle), color = NismColors.Amber)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        onSubmitTest()
                    },
                    modifier = Modifier.testTag("exam_confirm_submit_button")
                ) {
                    Text("Submit Final Answers")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("Return to Test")
                }
            }
        )
    }

    if (showPauseExitDialog) {
        AlertDialog(
            onDismissRequest = { showPauseExitDialog = false },
            title = { Text("Pause Examination?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "Your current progress and remaining timer are saved locally. You can resume this test from the dashboard at any time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPauseExitDialog = false
                        onPauseAndExit()
                    },
                    modifier = Modifier.testTag("exam_pause_exit_confirm")
                ) {
                    Text("Save & Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPauseExitDialog = false }) {
                    Text("Stay in Test")
                }
            }
        )
    }
}

@Composable
private fun QuestionPaletteContent(
    session: ActiveTestSession,
    answeredCount: Int,
    unansweredCount: Int,
    markedCount: Int,
    answeredAndMarkedCount: Int,
    isFullMock: Boolean,
    isDarkMode: Boolean,
    onSelectIndex: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaletteLegendChip(
                color = NismColors.Emerald,
                label = "Answered (${answeredCount - answeredAndMarkedCount})",
                modifier = Modifier.weight(1f)
            )
            PaletteLegendChip(
                color = MaterialTheme.colorScheme.outline,
                label = "Unanswered ($unansweredCount)",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaletteLegendChip(
                color = NismColors.Amber,
                label = "Marked (${markedCount - answeredAndMarkedCount})",
                modifier = Modifier.weight(1f)
            )
            PaletteLegendChip(
                color = NismColors.Indigo,
                label = "Ans & Marked ($answeredAndMarkedCount)",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(session.questionIds) { idx, qId ->
                val isCurrent = idx == session.currentIndex
                val hasAns = session.answers.containsKey(qId)
                val hasMark = qId in session.markedForReview
                val timedOut = !isFullMock && (qId in session.timedOutQuestionIds)

                val bgColor: Color
                val textColor: Color
                val borderColor: Color

                when {
                    hasAns && hasMark -> {
                        bgColor = NismColors.Indigo
                        textColor = Color.White
                        borderColor = NismColors.Indigo
                    }
                    hasAns -> {
                        bgColor = NismColors.Emerald
                        textColor = Color.White
                        borderColor = NismColors.Emerald
                    }
                    hasMark -> {
                        bgColor = NismColors.Amber
                        textColor = Color.White
                        borderColor = NismColors.Amber
                    }
                    timedOut -> {
                        bgColor = if (isDarkMode) NismColors.RoseBgDark else NismColors.RoseBgLight
                        textColor = if (isDarkMode) NismColors.RoseLight else NismColors.Rose
                        borderColor = NismColors.Rose.copy(alpha = 0.5f)
                    }
                    else -> {
                        bgColor = MaterialTheme.colorScheme.surface
                        textColor = MaterialTheme.colorScheme.onSurface
                        borderColor = MaterialTheme.colorScheme.outline
                    }
                }

                Surface(
                    onClick = { onSelectIndex(idx) },
                    shape = RoundedCornerShape(8.dp),
                    color = bgColor,
                    border = BorderStroke(
                        width = if (isCurrent) 2.5.dp else 1.dp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else borderColor
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("palette_q_${idx + 1}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${idx + 1}",
                            style = MaterialTheme.typography.labelLarge.merge(TabularMonoStyle),
                            color = textColor
                        )
                        if (hasAns && hasMark) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NismColors.EmeraldLight)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteLegendChip(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
