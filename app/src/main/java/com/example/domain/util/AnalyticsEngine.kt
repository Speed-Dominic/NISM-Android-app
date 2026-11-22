package com.example.domain.util

import com.example.domain.model.ActiveTestSession
import com.example.domain.model.ChapterPerformanceStat
import com.example.domain.model.ExamAnalyticsSummary
import com.example.domain.model.ExamDefinition
import com.example.domain.model.Question
import com.example.domain.model.TestAttempt
import com.example.domain.model.TestMode
import com.example.domain.model.TrendPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

object AnalyticsEngine {

    fun nowIsoString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    fun parseIsoMillis(iso: String): Long {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(iso)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return 0L
    }

    fun formatClock(totalSeconds: Int): String {
        val safe = maxOf(0, totalSeconds)
        val hrs = safe / 3600
        val mins = (safe % 3600) / 60
        val secs = safe % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formatDuration(totalSeconds: Int): String {
        val safe = maxOf(0, totalSeconds)
        val hrs = safe / 3600
        val mins = (safe % 3600) / 60
        val secs = safe % 60
        return when {
            hrs > 0 -> "${hrs}h ${mins}m ${secs}s"
            mins > 0 -> "${mins}m ${secs}s"
            else -> "${secs}s"
        }
    }

    fun formatDateTime(iso: String): String {
        val millis = parseIsoMillis(iso)
        if (millis <= 0L) return iso
        val sdf = SimpleDateFormat("MMM d, yyyy · hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatDateShort(iso: String): String {
        val millis = parseIsoMillis(iso)
        if (millis <= 0L) return ""
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun roundOneDecimal(value: Double): Double =
        (value * 10.0).roundToInt() / 10.0

    fun evaluateTestAttempt(
        session: ActiveTestSession,
        exam: ExamDefinition,
        questionMap: Map<String, Question>,
        autoSubmitted: Boolean
    ): TestAttempt {
        var correctCount = 0
        var incorrectCount = 0
        var unansweredCount = 0

        for (qId in session.questionIds) {
            val q = questionMap[qId]
            val chosen = session.answers[qId]
            when {
                chosen == null -> unansweredCount += 1
                q != null && chosen == q.correctAnswer -> correctCount += 1
                else -> incorrectCount += 1
            }
        }

        val totalQuestions = session.questionIds.size
        val totalMarks = totalQuestions * exam.marksPerQuestion
        val marksObtained = maxOf(
            0,
            correctCount * exam.marksPerQuestion - incorrectCount * exam.negativeMarking
        )
        val percentage = if (totalMarks > 0) {
            roundOneDecimal((marksObtained.toDouble() / totalMarks.toDouble()) * 100.0)
        } else {
            0.0
        }
        val attempted = correctCount + incorrectCount
        val accuracy = if (attempted > 0) {
            roundOneDecimal((correctCount.toDouble() / attempted.toDouble()) * 100.0)
        } else {
            0.0
        }

        val passingPercentage = if (session.mode == TestMode.FULL_MOCK) exam.passingPercentage else null
        val passed = passingPercentage?.let { percentage >= it.toDouble() }
        val suffix = java.util.UUID.randomUUID().toString().take(5)

        return TestAttempt(
            id = "ATT-${System.currentTimeMillis()}-$suffix",
            examId = exam.id,
            examName = exam.name,
            mode = session.mode,
            chapterNumber = session.chapterNumber,
            chapterTitle = session.chapterTitle,
            questionIds = session.questionIds.toList(),
            answers = session.answers.toMap(),
            markedForReview = session.markedForReview.toList(),
            timedOutQuestionIds = session.timedOutQuestionIds.toList(),
            timeSpentPerQuestion = session.timeSpentPerQuestion.toMap(),
            totalQuestions = totalQuestions,
            totalMarks = totalMarks,
            marksObtained = marksObtained,
            percentage = percentage,
            accuracy = accuracy,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            unansweredCount = unansweredCount,
            passed = passed,
            passingPercentage = passingPercentage,
            timeTakenSeconds = session.totalElapsedSeconds,
            completedAt = nowIsoString(),
            autoSubmitted = autoSubmitted
        )
    }

    private class ChapterAccumulator {
        var chapterPracticeAttempts = 0
        val appearancesInTests = mutableSetOf<String>()
        var questionsSeen = 0
        var questionsAttempted = 0
        var correctCount = 0
        var incorrectCount = 0
        var unansweredCount = 0
        var timeSum = 0
        var timeCount = 0
        var lastAttemptedAt: String? = null
    }

    fun computeExamAnalytics(
        exam: ExamDefinition,
        attempts: List<TestAttempt>,
        examQuestions: List<Question>
    ): ExamAnalyticsSummary {
        val questionMap = examQuestions.associateBy { it.id }

        val totalTestsAttempted = attempts.size
        val fullMockCount = attempts.count { it.mode == TestMode.FULL_MOCK }
        val chapterPracticeCount = attempts.count { it.mode == TestMode.CHAPTER_PRACTICE }

        var highestScorePercent: Double? = null
        var highestMarksLabel: String? = null
        var totalScorePercent = 0.0
        var totalMarksSum = 0.0
        var totalCorrect = 0
        var totalAttemptedQuestions = 0
        var totalTimeSpent = 0
        var totalQuestionsTimed = 0

        val chAcc = mutableMapOf<Int, ChapterAccumulator>()
        for (ch in exam.chapters) {
            chAcc[ch.chapterNumber] = ChapterAccumulator()
        }

        for (attempt in attempts) {
            if (highestScorePercent == null || attempt.percentage > highestScorePercent) {
                highestScorePercent = attempt.percentage
                highestMarksLabel = "${attempt.marksObtained}/${attempt.totalMarks}"
            }
            totalScorePercent += attempt.percentage
            totalMarksSum += attempt.marksObtained
            totalCorrect += attempt.correctCount
            totalAttemptedQuestions += attempt.correctCount + attempt.incorrectCount

            if (attempt.mode == TestMode.CHAPTER_PRACTICE && attempt.chapterNumber != null) {
                chAcc[attempt.chapterNumber]?.let { it.chapterPracticeAttempts += 1 }
            }

            for (qId in attempt.questionIds) {
                val q = questionMap[qId]
                val timeVal = attempt.timeSpentPerQuestion[qId] ?: 0
                if (timeVal > 0) {
                    totalTimeSpent += timeVal
                    totalQuestionsTimed += 1
                }

                if (q == null) continue
                val entry = chAcc[q.chapterNumber] ?: continue

                entry.appearancesInTests.add(attempt.id)
                entry.questionsSeen += 1
                val prevMillis = entry.lastAttemptedAt?.let { parseIsoMillis(it) } ?: 0L
                val currMillis = parseIsoMillis(attempt.completedAt)
                if (entry.lastAttemptedAt == null || currMillis > prevMillis) {
                    entry.lastAttemptedAt = attempt.completedAt
                }

                if (timeVal > 0) {
                    entry.timeSum += timeVal
                    entry.timeCount += 1
                }

                val chosen = attempt.answers[qId]
                if (chosen == null) {
                    entry.unansweredCount += 1
                } else {
                    entry.questionsAttempted += 1
                    if (chosen == q.correctAnswer) {
                        entry.correctCount += 1
                    } else {
                        entry.incorrectCount += 1
                    }
                }
            }
        }

        val availableByChapter = examQuestions.groupingBy { it.chapterNumber }.eachCount()

        val chapterStats = exam.chapters.map { ch ->
            val availableQuestions = availableByChapter[ch.chapterNumber] ?: 0
            val entry = chAcc[ch.chapterNumber] ?: ChapterAccumulator()
            val accuracy = if (entry.questionsAttempted > 0) {
                roundOneDecimal((entry.correctCount.toDouble() / entry.questionsAttempted.toDouble()) * 100.0)
            } else {
                null
            }
            val scorePercent = if (entry.questionsSeen > 0) {
                roundOneDecimal((entry.correctCount.toDouble() / entry.questionsSeen.toDouble()) * 100.0)
            } else {
                null
            }
            val avgTimePerQuestion = if (entry.timeCount > 0) {
                roundOneDecimal(entry.timeSum.toDouble() / entry.timeCount.toDouble())
            } else {
                null
            }

            ChapterPerformanceStat(
                chapterNumber = ch.chapterNumber,
                chapterTitle = ch.title,
                weightage = ch.weightage,
                availableQuestions = availableQuestions,
                attemptsCount = entry.appearancesInTests.size,
                chapterPracticeAttempts = entry.chapterPracticeAttempts,
                questionsSeen = entry.questionsSeen,
                questionsAttempted = entry.questionsAttempted,
                correctCount = entry.correctCount,
                incorrectCount = entry.incorrectCount,
                unansweredCount = entry.unansweredCount,
                accuracy = accuracy,
                scorePercent = scorePercent,
                avgTimePerQuestion = avgTimePerQuestion,
                lastAttemptedAt = entry.lastAttemptedAt
            )
        }

        val chaptersPractisedCount = chapterStats.count { it.questionsSeen > 0 }
        val practisedStats = chapterStats.filter { it.accuracy != null }

        val strongestChapters = practisedStats
            .sortedByDescending { it.accuracy ?: 0.0 }
            .take(4)

        val weakestChapters = practisedStats
            .sortedBy { it.accuracy ?: 0.0 }
            .take(4)

        val chronologicalTrend = attempts
            .asReversed()
            .takeLast(15)
            .mapIndexed { idx, a ->
                TrendPoint(
                    id = a.id,
                    label = if (a.mode == TestMode.FULL_MOCK) "Mock #${idx + 1}" else "Ch ${a.chapterNumber}",
                    dateShort = formatDateShort(a.completedAt),
                    scorePercent = a.percentage,
                    accuracy = a.accuracy,
                    modeLabel = if (a.mode == TestMode.FULL_MOCK) "Full Mock" else "Ch ${a.chapterNumber}"
                )
            }

        return ExamAnalyticsSummary(
            totalTestsAttempted = totalTestsAttempted,
            fullMockCount = fullMockCount,
            chapterPracticeCount = chapterPracticeCount,
            highestScorePercent = highestScorePercent,
            highestMarksLabel = highestMarksLabel,
            averageScorePercent = if (totalTestsAttempted > 0) {
                roundOneDecimal(totalScorePercent / totalTestsAttempted.toDouble())
            } else null,
            averageMarks = if (totalTestsAttempted > 0) {
                roundOneDecimal(totalMarksSum / totalTestsAttempted.toDouble())
            } else null,
            overallAccuracy = if (totalAttemptedQuestions > 0) {
                roundOneDecimal((totalCorrect.toDouble() / totalAttemptedQuestions.toDouble()) * 100.0)
            } else null,
            chaptersPractisedCount = chaptersPractisedCount,
            totalChaptersCount = exam.chapters.size,
            avgTimePerQuestionSeconds = if (totalQuestionsTimed > 0) {
                roundOneDecimal(totalTimeSpent.toDouble() / totalQuestionsTimed.toDouble())
            } else null,
            chapterStats = chapterStats,
            strongestChapters = strongestChapters,
            weakestChapters = weakestChapters,
            chronologicalTrend = chronologicalTrend
        )
    }
}
