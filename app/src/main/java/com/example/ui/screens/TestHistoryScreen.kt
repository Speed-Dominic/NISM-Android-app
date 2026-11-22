package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ExamDefinition
import com.example.domain.model.TestAttempt
import com.example.domain.model.TestMode
import com.example.domain.util.AnalyticsEngine
import com.example.ui.components.PassFailChip
import com.example.ui.theme.NismColors
import com.example.ui.theme.TabularMonoStyle

private enum class HistoryFilter {
    ALL, FULL_MOCK, CHAPTER_PRACTICE
}

@Composable
fun TestHistoryScreen(
    exam: ExamDefinition,
    attempts: List<TestAttempt>,
    isDarkMode: Boolean,
    onReviewAttempt: (TestAttempt) -> Unit,
    onDeleteAttempt: (String) -> Unit,
    onStartFullMock: () -> Unit,
    onGoToChapters: () -> Unit
) {
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }

    val fullMockCount = remember(attempts) { attempts.count { it.mode == TestMode.FULL_MOCK } }
    val chapterCount = remember(attempts) { attempts.count { it.mode == TestMode.CHAPTER_PRACTICE } }

    val filteredAttempts = remember(attempts, filter) {
        when (filter) {
            HistoryFilter.ALL -> attempts
            HistoryFilter.FULL_MOCK -> attempts.filter { it.mode == TestMode.FULL_MOCK }
            HistoryFilter.CHAPTER_PRACTICE -> attempts.filter { it.mode == TestMode.CHAPTER_PRACTICE }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "history_header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${exam.code} · Saved Records",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Test History",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Open any past mock examination or chapter practice attempt to inspect your responses and detailed explanations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Triple(HistoryFilter.ALL, "All (${attempts.size})", "history_filter_all"),
                                Triple(HistoryFilter.FULL_MOCK, "Full Mock ($fullMockCount)", "history_filter_mock"),
                                Triple(HistoryFilter.CHAPTER_PRACTICE, "Chapter ($chapterCount)", "history_filter_chapter")
                            ).forEach { (tab, label, tag) ->
                                val active = filter == tab
                                Surface(
                                    onClick = { filter = tab },
                                    shape = RoundedCornerShape(7.dp),
                                    color = if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(tag)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
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
            }

            if (filteredAttempts.isEmpty()) {
                item(key = "history_empty") {
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
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No test attempts found", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "All completed full mock exams and chapter-wise drills are saved locally on your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onStartFullMock,
                                    modifier = Modifier.testTag("history_empty_start_mock")
                                ) {
                                    Text("Start Full Mock")
                                }
                                OutlinedButton(
                                    onClick = onGoToChapters,
                                    modifier = Modifier.testTag("history_empty_chapters")
                                ) {
                                    Text("Chapter Practice")
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredAttempts, key = { it.id }) { attempt ->
                    val isConfirming = confirmDeleteId == attempt.id
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
                                    Text(
                                        text = if (attempt.mode == TestMode.FULL_MOCK) "Full Mock Test" else "Chapter Practice",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = if (attempt.mode == TestMode.CHAPTER_PRACTICE) {
                                            "Ch. ${attempt.chapterNumber}: ${attempt.chapterTitle}"
                                        } else {
                                            "All Syllabus Chapters"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                PassFailChip(passed = attempt.passed, isDarkMode = isDarkMode)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = AnalyticsEngine.formatDateTime(attempt.completedAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Score", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${attempt.marksObtained}/${attempt.totalMarks} (${attempt.percentage}%)",
                                            style = MaterialTheme.typography.labelLarge.merge(TabularMonoStyle),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Accuracy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${attempt.accuracy}%",
                                            style = MaterialTheme.typography.labelLarge.merge(TabularMonoStyle)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = AnalyticsEngine.formatDuration(attempt.timeTakenSeconds),
                                            style = MaterialTheme.typography.labelLarge.merge(TabularMonoStyle)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isConfirming) {
                                    Button(
                                        onClick = {
                                            onDeleteAttempt(attempt.id)
                                            confirmDeleteId = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NismColors.Rose),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("history_confirm_delete_${attempt.id}")
                                    ) {
                                        Text("Confirm Delete", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    TextButton(onClick = { confirmDeleteId = null }) {
                                        Text("Cancel")
                                    }
                                } else {
                                    IconButton(
                                        onClick = { confirmDeleteId = attempt.id },
                                        modifier = Modifier.testTag("history_delete_${attempt.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete attempt",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Button(
                                        onClick = { onReviewAttempt(attempt) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("history_review_${attempt.id}")
                                    ) {
                                        Text("Review Answers", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
