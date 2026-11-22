package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class OptionKey {
    A, B, C, D
}

@Serializable
enum class TestMode {
    FULL_MOCK,
    CHAPTER_PRACTICE
}

@Serializable
data class Question(
    val id: String,
    val examId: String,
    val examName: String,
    val chapterNumber: Int,
    val chapterTitle: String,
    val topic: String,
    val questionText: String,
    val options: Map<OptionKey, String>,
    val correctAnswer: OptionKey,
    val explanation: String,
    val difficulty: String = "Moderate",
    val questionType: String = "Conceptual"
)

data class ChapterDefinition(
    val chapterNumber: Int,
    val title: String,
    val weightage: Int,
    val summary: String,
    val keyTopics: List<String>
)

data class ExamDefinition(
    val id: String,
    val code: String,
    val name: String,
    val shortName: String,
    val description: String,
    val totalQuestionsFullMock: Int,
    val totalMarksFullMock: Int,
    val marksPerQuestion: Int,
    val negativeMarking: Int,
    val durationMinutes: Int,
    val passingPercentage: Int?,
    val chapters: List<ChapterDefinition>
)

@Serializable
data class ActiveTestSession(
    val id: String = "current_session",
    val examId: String,
    val examName: String,
    val mode: TestMode,
    val chapterNumber: Int? = null,
    val chapterTitle: String? = null,
    val questionIds: List<String>,
    val currentIndex: Int = 0,
    val answers: Map<String, OptionKey> = emptyMap(),
    val markedForReview: List<String> = emptyList(),
    val remainingSeconds: Int = 7200,
    val questionRemainingSeconds: Int = 60,
    val perQuestionRemainingSeconds: Map<String, Int> = emptyMap(),
    val timedOutQuestionIds: List<String> = emptyList(),
    val timeSpentPerQuestion: Map<String, Int> = emptyMap(),
    val totalElapsedSeconds: Int = 0,
    val startedAt: String,
    val lastUpdatedAt: String
)

@Serializable
data class TestAttempt(
    val id: String,
    val examId: String,
    val examName: String,
    val mode: TestMode,
    val chapterNumber: Int? = null,
    val chapterTitle: String? = null,
    val questionIds: List<String>,
    val answers: Map<String, OptionKey>,
    val markedForReview: List<String>,
    val timedOutQuestionIds: List<String>,
    val timeSpentPerQuestion: Map<String, Int>,
    val totalQuestions: Int,
    val totalMarks: Int,
    val marksObtained: Int,
    val percentage: Double,
    val accuracy: Double,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val passed: Boolean?,
    val passingPercentage: Int?,
    val timeTakenSeconds: Int,
    val completedAt: String,
    val autoSubmitted: Boolean
)

data class ChapterPerformanceStat(
    val chapterNumber: Int,
    val chapterTitle: String,
    val weightage: Int,
    val availableQuestions: Int,
    val attemptsCount: Int,
    val chapterPracticeAttempts: Int,
    val questionsSeen: Int,
    val questionsAttempted: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val accuracy: Double?,
    val scorePercent: Double?,
    val avgTimePerQuestion: Double?,
    val lastAttemptedAt: String?
)

data class TrendPoint(
    val id: String,
    val label: String,
    val dateShort: String,
    val scorePercent: Double,
    val accuracy: Double,
    val modeLabel: String
)

data class ExamAnalyticsSummary(
    val totalTestsAttempted: Int = 0,
    val fullMockCount: Int = 0,
    val chapterPracticeCount: Int = 0,
    val highestScorePercent: Double? = null,
    val highestMarksLabel: String? = null,
    val averageScorePercent: Double? = null,
    val averageMarks: Double? = null,
    val overallAccuracy: Double? = null,
    val chaptersPractisedCount: Int = 0,
    val totalChaptersCount: Int = 12,
    val avgTimePerQuestionSeconds: Double? = null,
    val chapterStats: List<ChapterPerformanceStat> = emptyList(),
    val strongestChapters: List<ChapterPerformanceStat> = emptyList(),
    val weakestChapters: List<ChapterPerformanceStat> = emptyList(),
    val chronologicalTrend: List<TrendPoint> = emptyList()
)
