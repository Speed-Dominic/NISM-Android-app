package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.ActiveTestSession
import com.example.domain.model.OptionKey
import com.example.domain.model.Question
import com.example.domain.model.TestAttempt
import com.example.domain.model.TestMode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["examId"]),
        Index(value = ["examId", "chapterNumber"])
    ]
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val examId: String,
    val examName: String,
    val chapterNumber: Int,
    val chapterTitle: String,
    val topic: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String,
    val explanation: String,
    val difficulty: String,
    val questionType: String
) {
    fun toDomain(): Question = Question(
        id = id,
        examId = examId,
        examName = examName,
        chapterNumber = chapterNumber,
        chapterTitle = chapterTitle,
        topic = topic,
        questionText = questionText,
        options = mapOf(
            OptionKey.A to optionA,
            OptionKey.B to optionB,
            OptionKey.C to optionC,
            OptionKey.D to optionD
        ),
        correctAnswer = runCatching { OptionKey.valueOf(correctAnswer) }.getOrDefault(OptionKey.A),
        explanation = explanation,
        difficulty = difficulty,
        questionType = questionType
    )

    companion object {
        fun fromDomain(q: Question): QuestionEntity = QuestionEntity(
            id = q.id,
            examId = q.examId,
            examName = q.examName,
            chapterNumber = q.chapterNumber,
            chapterTitle = q.chapterTitle,
            topic = q.topic,
            questionText = q.questionText,
            optionA = q.options[OptionKey.A].orEmpty(),
            optionB = q.options[OptionKey.B].orEmpty(),
            optionC = q.options[OptionKey.C].orEmpty(),
            optionD = q.options[OptionKey.D].orEmpty(),
            correctAnswer = q.correctAnswer.name,
            explanation = q.explanation,
            difficulty = q.difficulty,
            questionType = q.questionType
        )
    }
}

@Entity(
    tableName = "attempts",
    indices = [
        Index(value = ["examId"]),
        Index(value = ["completedAt"])
    ]
)
data class TestAttemptEntity(
    @PrimaryKey val id: String,
    val examId: String,
    val examName: String,
    val mode: String,
    val chapterNumber: Int?,
    val chapterTitle: String?,
    val questionIdsJson: String,
    val answersJson: String,
    val markedForReviewJson: String,
    val timedOutQuestionIdsJson: String,
    val timeSpentPerQuestionJson: String,
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
) {
    fun toDomain(json: Json): TestAttempt = TestAttempt(
        id = id,
        examId = examId,
        examName = examName,
        mode = runCatching { TestMode.valueOf(mode) }.getOrDefault(TestMode.FULL_MOCK),
        chapterNumber = chapterNumber,
        chapterTitle = chapterTitle,
        questionIds = json.decodeFromString(questionIdsJson),
        answers = json.decodeFromString(answersJson),
        markedForReview = json.decodeFromString(markedForReviewJson),
        timedOutQuestionIds = json.decodeFromString(timedOutQuestionIdsJson),
        timeSpentPerQuestion = json.decodeFromString(timeSpentPerQuestionJson),
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
        timeTakenSeconds = timeTakenSeconds,
        completedAt = completedAt,
        autoSubmitted = autoSubmitted
    )

    companion object {
        fun fromDomain(attempt: TestAttempt, json: Json): TestAttemptEntity = TestAttemptEntity(
            id = attempt.id,
            examId = attempt.examId,
            examName = attempt.examName,
            mode = attempt.mode.name,
            chapterNumber = attempt.chapterNumber,
            chapterTitle = attempt.chapterTitle,
            questionIdsJson = json.encodeToString(attempt.questionIds),
            answersJson = json.encodeToString(attempt.answers),
            markedForReviewJson = json.encodeToString(attempt.markedForReview),
            timedOutQuestionIdsJson = json.encodeToString(attempt.timedOutQuestionIds),
            timeSpentPerQuestionJson = json.encodeToString(attempt.timeSpentPerQuestion),
            totalQuestions = attempt.totalQuestions,
            totalMarks = attempt.totalMarks,
            marksObtained = attempt.marksObtained,
            percentage = attempt.percentage,
            accuracy = attempt.accuracy,
            correctCount = attempt.correctCount,
            incorrectCount = attempt.incorrectCount,
            unansweredCount = attempt.unansweredCount,
            passed = attempt.passed,
            passingPercentage = attempt.passingPercentage,
            timeTakenSeconds = attempt.timeTakenSeconds,
            completedAt = attempt.completedAt,
            autoSubmitted = attempt.autoSubmitted
        )
    }
}

@Entity(tableName = "active_session")
data class ActiveSessionEntity(
    @PrimaryKey val id: String = "current_session",
    val payloadJson: String,
    val lastUpdatedAt: String
) {
    fun toDomain(json: Json): ActiveTestSession? =
        runCatching { json.decodeFromString<ActiveTestSession>(payloadJson) }.getOrNull()

    companion object {
        fun fromDomain(session: ActiveTestSession, json: Json): ActiveSessionEntity =
            ActiveSessionEntity(
                id = "current_session",
                payloadJson = json.encodeToString(session),
                lastUpdatedAt = session.lastUpdatedAt
            )
    }
}
