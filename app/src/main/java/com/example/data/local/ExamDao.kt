package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM questions WHERE examId = :examId ORDER BY chapterNumber ASC, id ASC")
    fun observeQuestionsByExam(examId: String): Flow<List<QuestionEntity>>

    @Query("SELECT COUNT(*) FROM questions WHERE examId = :examId")
    suspend fun countQuestionsByExam(examId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE id IN (:questionIds)")
    suspend fun deleteQuestionsByIds(questionIds: List<String>)

    @Query("DELETE FROM questions WHERE examId = :examId")
    suspend fun deleteQuestionsByExam(examId: String)

    @Query("DELETE FROM questions")
    suspend fun clearAllQuestions()

    @Transaction
    suspend fun replaceExamQuestions(examId: String, questions: List<QuestionEntity>) {
        deleteQuestionsByExam(examId)
        upsertQuestions(questions)
    }

    @Query("SELECT * FROM attempts WHERE examId = :examId ORDER BY completedAt DESC")
    fun observeAttemptsByExam(examId: String): Flow<List<TestAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: TestAttemptEntity)

    @Query("DELETE FROM attempts WHERE id = :attemptId")
    suspend fun deleteAttemptById(attemptId: String)

    @Query("DELETE FROM attempts WHERE examId != :examId")
    suspend fun deleteAttemptsNotInExam(examId: String)

    @Query("SELECT * FROM active_session WHERE id = 'current_session' LIMIT 1")
    fun observeActiveSession(): Flow<ActiveSessionEntity?>

    @Query("SELECT * FROM active_session WHERE id = 'current_session' LIMIT 1")
    suspend fun getActiveSession(): ActiveSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActiveSession(session: ActiveSessionEntity)

    @Query("DELETE FROM active_session WHERE id = 'current_session'")
    suspend fun clearActiveSession()
}
