package com.example.data.repository

import android.content.Context
import com.example.data.NismExamCatalog
import com.example.data.local.ActiveSessionEntity
import com.example.data.local.ExamDao
import com.example.data.local.PreferencesManager
import com.example.data.local.QuestionEntity
import com.example.data.local.TestAttemptEntity
import com.example.domain.model.ActiveTestSession
import com.example.domain.model.OptionKey
import com.example.domain.model.Question
import com.example.domain.model.TestAttempt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class RawBuiltinQuestion(
    val id: String,
    val chapterNumber: Int,
    val topic: String = "General Concept",
    val questionText: String,
    val options: Map<String, String>,
    val correctAnswer: String,
    val explanation: String,
    val difficulty: String = "Moderate",
    val questionType: String = "Conceptual"
)

class ExamRepository(
    private val context: Context,
    private val dao: ExamDao,
    private val prefs: PreferencesManager,
    private val json: Json
) {
    private val exam = NismExamCatalog.NISM_VA_EXAM

    val questionsFlow: Flow<List<Question>> = dao.observeQuestionsByExam(exam.id)
        .map { list -> list.map { it.toDomain() } }

    val attemptsFlow: Flow<List<TestAttempt>> = dao.observeAttemptsByExam(exam.id)
        .map { list -> list.map { it.toDomain(json) } }

    val activeSessionFlow: Flow<ActiveTestSession?> = dao.observeActiveSession()
        .map { entity -> entity?.toDomain(json) }

    val isDarkModeFlow: Flow<Boolean> = prefs.isDarkModeFlow

    suspend fun initializeAndSeedIfNeeded() = withContext(Dispatchers.IO) {
        val currentVersion = prefs.getBankVersion()
        val existingCount = dao.countQuestionsByExam(exam.id)
        if (currentVersion < NismExamCatalog.BUILTIN_BANK_VERSION || (currentVersion == 0 && existingCount == 0)) {
            val builtin = loadBuiltinQuestionsFromAssets()
            dao.deleteAttemptsNotInExam(exam.id)
            dao.clearActiveSession()
            dao.replaceExamQuestions(exam.id, builtin.map { QuestionEntity.fromDomain(it) })
            prefs.setBankVersion(NismExamCatalog.BUILTIN_BANK_VERSION)
        }
    }

    private fun loadBuiltinQuestionsFromAssets(): List<Question> {
        val rawJson = context.assets.open("nism_va_builtin_questions.json")
            .bufferedReader()
            .use { it.readText() }
        val rawItems = json.decodeFromString<List<RawBuiltinQuestion>>(rawJson)
        val chapterMap = exam.chapters.associateBy { it.chapterNumber }

        return rawItems.mapNotNull { item ->
            val chapter = chapterMap[item.chapterNumber] ?: return@mapNotNull null
            val optA = item.options["A"] ?: return@mapNotNull null
            val optB = item.options["B"] ?: return@mapNotNull null
            val optC = item.options["C"] ?: return@mapNotNull null
            val optD = item.options["D"] ?: return@mapNotNull null
            val ans = runCatching { OptionKey.valueOf(item.correctAnswer.uppercase()) }.getOrNull()
                ?: return@mapNotNull null

            Question(
                id = item.id,
                examId = exam.id,
                examName = exam.name,
                chapterNumber = item.chapterNumber,
                chapterTitle = chapter.title,
                topic = item.topic,
                questionText = item.questionText,
                options = mapOf(
                    OptionKey.A to optA,
                    OptionKey.B to optB,
                    OptionKey.C to optC,
                    OptionKey.D to optD
                ),
                correctAnswer = ans,
                explanation = item.explanation,
                difficulty = item.difficulty,
                questionType = item.questionType
            )
        }
    }

    suspend fun importQuestions(questions: List<Question>) = withContext(Dispatchers.IO) {
        if (questions.isEmpty()) return@withContext
        questions.chunked(200).forEach { chunk ->
            dao.upsertQuestions(chunk.map { QuestionEntity.fromDomain(it) })
        }
    }

    suspend fun deleteQuestions(questionIds: List<String>) = withContext(Dispatchers.IO) {
        if (questionIds.isEmpty()) return@withContext
        questionIds.chunked(200).forEach { chunk ->
            dao.deleteQuestionsByIds(chunk)
        }
    }

    suspend fun replaceExamQuestions(questions: List<Question>) = withContext(Dispatchers.IO) {
        dao.replaceExamQuestions(exam.id, questions.map { QuestionEntity.fromDomain(it) })
    }

    suspend fun clearAllQuestions() = withContext(Dispatchers.IO) {
        dao.clearAllQuestions()
    }

    suspend fun saveAttempt(attempt: TestAttempt) = withContext(Dispatchers.IO) {
        dao.insertAttempt(TestAttemptEntity.fromDomain(attempt, json))
    }

    suspend fun deleteAttempt(attemptId: String) = withContext(Dispatchers.IO) {
        dao.deleteAttemptById(attemptId)
    }

    suspend fun saveActiveSession(session: ActiveTestSession) = withContext(Dispatchers.IO) {
        dao.upsertActiveSession(ActiveSessionEntity.fromDomain(session, json))
    }

    suspend fun clearActiveSession() = withContext(Dispatchers.IO) {
        dao.clearActiveSession()
    }

    suspend fun setDarkMode(darkMode: Boolean) {
        prefs.setDarkMode(darkMode)
    }
}
