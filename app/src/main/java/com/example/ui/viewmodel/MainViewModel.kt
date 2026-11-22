package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.NismExamCatalog
import com.example.data.repository.ExamRepository
import com.example.domain.model.ActiveTestSession
import com.example.domain.model.ExamAnalyticsSummary
import com.example.domain.model.ExamDefinition
import com.example.domain.model.OptionKey
import com.example.domain.model.Question
import com.example.domain.model.TestAttempt
import com.example.domain.model.TestMode
import com.example.domain.util.AnalyticsEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface PendingStartRequest {
    data object FullMock : PendingStartRequest
    data class ChapterPractice(val chapterNumber: Int, val requestedCount: Int) : PendingStartRequest
}

data class StatusBanner(
    val isSuccess: Boolean,
    val message: String
)

data class MainUiState(
    val isLoading: Boolean = true,
    val errorNotice: String? = null,
    val exam: ExamDefinition = NismExamCatalog.NISM_VA_EXAM,
    val questions: List<Question> = emptyList(),
    val questionMap: Map<String, Question> = emptyMap(),
    val attempts: List<TestAttempt> = emptyList(),
    val activeSession: ActiveTestSession? = null,
    val analytics: ExamAnalyticsSummary = ExamAnalyticsSummary(),
    val isDarkMode: Boolean = false
)

sealed interface NavigationEvent {
    data object OpenWorkbench : NavigationEvent
    data class OpenResults(val attemptId: String) : NavigationEvent
    data object OpenQuestionBank : NavigationEvent
}

class MainViewModel(
    private val repository: ExamRepository
) : ViewModel() {

    private val exam = NismExamCatalog.NISM_VA_EXAM

    private val _isLoading = MutableStateFlow(true)
    private val _errorNotice = MutableStateFlow<String?>(null)

    private val _pendingStartRequest = MutableStateFlow<PendingStartRequest?>(null)
    val pendingStartRequest: StateFlow<PendingStartRequest?> = _pendingStartRequest.asStateFlow()

    private val _questionBankBanner = MutableStateFlow<StatusBanner?>(null)
    val questionBankBanner: StateFlow<StatusBanner?> = _questionBankBanner.asStateFlow()

    private val _navEvents = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 4)
    val navEvents: SharedFlow<NavigationEvent> = _navEvents.asSharedFlow()

    private var timerJob: Job? = null
    private var isSubmitting = false

    val uiState: StateFlow<MainUiState> = combine(
        _isLoading,
        _errorNotice,
        repository.questionsFlow,
        repository.attemptsFlow,
        repository.activeSessionFlow,
        repository.isDarkModeFlow
    ) { values ->
        val loading = values[0] as Boolean
        val error = values[1] as String?
        @Suppress("UNCHECKED_CAST")
        val questions = values[2] as List<Question>
        @Suppress("UNCHECKED_CAST")
        val attempts = values[3] as List<TestAttempt>
        val activeSession = values[4] as ActiveTestSession?
        val isDarkMode = values[5] as Boolean

        val questionMap = questions.associateBy { it.id }
        val analytics = AnalyticsEngine.computeExamAnalytics(exam, attempts, questions)

        MainUiState(
            isLoading = loading,
            errorNotice = error,
            exam = exam,
            questions = questions,
            questionMap = questionMap,
            attempts = attempts,
            activeSession = activeSession,
            analytics = analytics,
            isDarkMode = isDarkMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    init {
        viewModelScope.launch {
            try {
                repository.initializeAndSeedIfNeeded()
            } catch (e: Exception) {
                _errorNotice.value = e.message ?: "Local database initialization notice."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleDarkMode() {
        val next = !uiState.value.isDarkMode
        viewModelScope.launch {
            repository.setDarkMode(next)
        }
    }

    fun requestStartFullMock() {
        val state = uiState.value
        if (state.questions.isEmpty()) {
            _navEvents.tryEmit(NavigationEvent.OpenQuestionBank)
            return
        }
        if (state.activeSession != null) {
            _pendingStartRequest.value = PendingStartRequest.FullMock
        } else {
            launchFullMockInternal()
        }
    }

    fun requestStartChapterPractice(chapterNumber: Int, requestedCount: Int) {
        val state = uiState.value
        if (state.activeSession != null) {
            _pendingStartRequest.value = PendingStartRequest.ChapterPractice(chapterNumber, requestedCount)
        } else {
            launchChapterPracticeInternal(chapterNumber, requestedCount)
        }
    }

    fun dismissPendingStartRequest() {
        _pendingStartRequest.value = null
    }

    fun resumeFromPendingDialog() {
        _pendingStartRequest.value = null
        resumeSavedSession()
    }

    fun confirmDiscardAndStartPending() {
        val req = _pendingStartRequest.value ?: return
        _pendingStartRequest.value = null
        when (req) {
            PendingStartRequest.FullMock -> launchFullMockInternal()
            is PendingStartRequest.ChapterPractice ->
                launchChapterPracticeInternal(req.chapterNumber, req.requestedCount)
        }
    }

    private fun launchFullMockInternal() {
        val selectedQs = NismExamCatalog.selectWeightedMockQuestions(
            exam = exam,
            allExamQuestions = uiState.value.questions,
            targetCount = exam.totalQuestionsFullMock
        )
        if (selectedQs.isEmpty()) return

        val now = AnalyticsEngine.nowIsoString()
        val session = ActiveTestSession(
            id = "current_session",
            examId = exam.id,
            examName = exam.name,
            mode = TestMode.FULL_MOCK,
            questionIds = selectedQs.map { it.id },
            currentIndex = 0,
            answers = emptyMap(),
            markedForReview = emptyList(),
            remainingSeconds = exam.durationMinutes * 60,
            questionRemainingSeconds = 60,
            perQuestionRemainingSeconds = emptyMap(),
            timedOutQuestionIds = emptyList(),
            timeSpentPerQuestion = emptyMap(),
            totalElapsedSeconds = 0,
            startedAt = now,
            lastUpdatedAt = now
        )
        isSubmitting = false
        viewModelScope.launch {
            repository.saveActiveSession(session)
            _navEvents.tryEmit(NavigationEvent.OpenWorkbench)
        }
    }

    private fun launchChapterPracticeInternal(chapterNumber: Int, requestedCount: Int) {
        val chapter = exam.chapters.find { it.chapterNumber == chapterNumber } ?: return
        val selectedQs = NismExamCatalog.selectChapterPracticeQuestions(
            examQuestions = uiState.value.questions,
            chapterNumber = chapterNumber,
            requestedCount = requestedCount
        )
        if (selectedQs.isEmpty()) return

        val firstQId = selectedQs.first().id
        val now = AnalyticsEngine.nowIsoString()
        val session = ActiveTestSession(
            id = "current_session",
            examId = exam.id,
            examName = exam.name,
            mode = TestMode.CHAPTER_PRACTICE,
            chapterNumber = chapter.chapterNumber,
            chapterTitle = chapter.title,
            questionIds = selectedQs.map { it.id },
            currentIndex = 0,
            answers = emptyMap(),
            markedForReview = emptyList(),
            remainingSeconds = 0,
            questionRemainingSeconds = 60,
            perQuestionRemainingSeconds = mapOf(firstQId to 60),
            timedOutQuestionIds = emptyList(),
            timeSpentPerQuestion = emptyMap(),
            totalElapsedSeconds = 0,
            startedAt = now,
            lastUpdatedAt = now
        )
        isSubmitting = false
        viewModelScope.launch {
            repository.saveActiveSession(session)
            _navEvents.tryEmit(NavigationEvent.OpenWorkbench)
        }
    }

    fun resumeSavedSession() {
        if (uiState.value.activeSession == null) return
        isSubmitting = false
        _navEvents.tryEmit(NavigationEvent.OpenWorkbench)
    }

    fun discardSavedSession() {
        stopExamTimer()
        viewModelScope.launch {
            repository.clearActiveSession()
        }
    }

    fun startExamTimer() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                tickOneSecond()
            }
        }
    }

    fun stopExamTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private suspend fun tickOneSecond() {
        if (isSubmitting) return
        val s = uiState.value.activeSession ?: return
        val curQId = s.questionIds.getOrNull(s.currentIndex) ?: return

        val updatedTimeSpent = s.timeSpentPerQuestion.toMutableMap().apply {
            this[curQId] = (this[curQId] ?: 0) + 1
        }

        if (s.mode == TestMode.FULL_MOCK) {
            val nextRemaining = maxOf(0, s.remainingSeconds - 1)
            val nextSession = s.copy(
                remainingSeconds = nextRemaining,
                totalElapsedSeconds = s.totalElapsedSeconds + 1,
                timeSpentPerQuestion = updatedTimeSpent,
                lastUpdatedAt = AnalyticsEngine.nowIsoString()
            )
            if (nextRemaining <= 0) {
                submitSessionInternal(nextSession, autoSubmitted = true)
            } else {
                repository.saveActiveSession(nextSession)
            }
        } else {
            val isAlreadyTimedOut = curQId in s.timedOutQuestionIds
            if (isAlreadyTimedOut) {
                repository.saveActiveSession(
                    s.copy(
                        totalElapsedSeconds = s.totalElapsedSeconds + 1,
                        timeSpentPerQuestion = updatedTimeSpent,
                        lastUpdatedAt = AnalyticsEngine.nowIsoString()
                    )
                )
                return
            }

            val nextQRem = maxOf(0, s.questionRemainingSeconds - 1)
            val nextPerQ = s.perQuestionRemainingSeconds.toMutableMap().apply {
                this[curQId] = nextQRem
            }

            if (nextQRem <= 0) {
                val cleanedAnswers = s.answers.toMutableMap().apply { remove(curQId) }
                val nextTimedOut = if (curQId in s.timedOutQuestionIds) {
                    s.timedOutQuestionIds
                } else {
                    s.timedOutQuestionIds + curQId
                }

                if (s.currentIndex >= s.questionIds.size - 1) {
                    val finalSess = s.copy(
                        answers = cleanedAnswers,
                        timedOutQuestionIds = nextTimedOut,
                        questionRemainingSeconds = 0,
                        perQuestionRemainingSeconds = nextPerQ,
                        totalElapsedSeconds = s.totalElapsedSeconds + 1,
                        timeSpentPerQuestion = updatedTimeSpent,
                        lastUpdatedAt = AnalyticsEngine.nowIsoString()
                    )
                    submitSessionInternal(finalSess, autoSubmitted = true)
                } else {
                    val nextIndex = s.currentIndex + 1
                    val nextQId = s.questionIds[nextIndex]
                    val nextQTimer = nextPerQ[nextQId] ?: 60
                    nextPerQ[nextQId] = nextQTimer
                    repository.saveActiveSession(
                        s.copy(
                            currentIndex = nextIndex,
                            answers = cleanedAnswers,
                            timedOutQuestionIds = nextTimedOut,
                            questionRemainingSeconds = nextQTimer,
                            perQuestionRemainingSeconds = nextPerQ,
                            totalElapsedSeconds = s.totalElapsedSeconds + 1,
                            timeSpentPerQuestion = updatedTimeSpent,
                            lastUpdatedAt = AnalyticsEngine.nowIsoString()
                        )
                    )
                }
            } else {
                repository.saveActiveSession(
                    s.copy(
                        questionRemainingSeconds = nextQRem,
                        perQuestionRemainingSeconds = nextPerQ,
                        totalElapsedSeconds = s.totalElapsedSeconds + 1,
                        timeSpentPerQuestion = updatedTimeSpent,
                        lastUpdatedAt = AnalyticsEngine.nowIsoString()
                    )
                )
            }
        }
    }

    fun navigateToQuestionIndex(targetIndex: Int) {
        val s = uiState.value.activeSession ?: return
        if (targetIndex < 0 || targetIndex >= s.questionIds.size) return
        val curQId = s.questionIds[s.currentIndex]
        val targetQId = s.questionIds[targetIndex]

        viewModelScope.launch {
            if (s.mode == TestMode.CHAPTER_PRACTICE) {
                val updatedPerQ = s.perQuestionRemainingSeconds.toMutableMap().apply {
                    this[curQId] = s.questionRemainingSeconds
                }
                val nextTimer = updatedPerQ[targetQId] ?: 60
                updatedPerQ[targetQId] = nextTimer
                repository.saveActiveSession(
                    s.copy(
                        currentIndex = targetIndex,
                        questionRemainingSeconds = nextTimer,
                        perQuestionRemainingSeconds = updatedPerQ,
                        lastUpdatedAt = AnalyticsEngine.nowIsoString()
                    )
                )
            } else {
                repository.saveActiveSession(
                    s.copy(
                        currentIndex = targetIndex,
                        lastUpdatedAt = AnalyticsEngine.nowIsoString()
                    )
                )
            }
        }
    }

    fun selectAnswer(option: OptionKey) {
        val s = uiState.value.activeSession ?: return
        val curQId = s.questionIds.getOrNull(s.currentIndex) ?: return
        if (s.mode == TestMode.CHAPTER_PRACTICE && curQId in s.timedOutQuestionIds) return

        val nextAnswers = s.answers.toMutableMap().apply { this[curQId] = option }
        viewModelScope.launch {
            repository.saveActiveSession(
                s.copy(
                    answers = nextAnswers,
                    lastUpdatedAt = AnalyticsEngine.nowIsoString()
                )
            )
        }
    }

    fun clearCurrentAnswer() {
        val s = uiState.value.activeSession ?: return
        val curQId = s.questionIds.getOrNull(s.currentIndex) ?: return
        if (s.mode == TestMode.CHAPTER_PRACTICE && curQId in s.timedOutQuestionIds) return

        val nextAnswers = s.answers.toMutableMap().apply { remove(curQId) }
        viewModelScope.launch {
            repository.saveActiveSession(
                s.copy(
                    answers = nextAnswers,
                    lastUpdatedAt = AnalyticsEngine.nowIsoString()
                )
            )
        }
    }

    fun toggleMarkForReview() {
        val s = uiState.value.activeSession ?: return
        val curQId = s.questionIds.getOrNull(s.currentIndex) ?: return
        val nextMarked = if (curQId in s.markedForReview) {
            s.markedForReview - curQId
        } else {
            s.markedForReview + curQId
        }
        viewModelScope.launch {
            repository.saveActiveSession(
                s.copy(
                    markedForReview = nextMarked,
                    lastUpdatedAt = AnalyticsEngine.nowIsoString()
                )
            )
        }
    }

    fun submitActiveTest(autoSubmitted: Boolean = false) {
        val s = uiState.value.activeSession ?: return
        viewModelScope.launch {
            submitSessionInternal(s, autoSubmitted)
        }
    }

    private suspend fun submitSessionInternal(session: ActiveTestSession, autoSubmitted: Boolean) {
        if (isSubmitting) return
        isSubmitting = true
        stopExamTimer()
        val attempt = AnalyticsEngine.evaluateTestAttempt(
            session = session,
            exam = exam,
            questionMap = uiState.value.questionMap,
            autoSubmitted = autoSubmitted
        )
        repository.saveAttempt(attempt)
        repository.clearActiveSession()
        _navEvents.tryEmit(NavigationEvent.OpenResults(attempt.id))
    }

    fun deleteAttempt(attemptId: String) {
        viewModelScope.launch {
            repository.deleteAttempt(attemptId)
        }
    }

    fun setQuestionBankBanner(banner: StatusBanner?) {
        _questionBankBanner.value = banner
    }

    suspend fun importQuestions(newQuestions: List<Question>) {
        repository.importQuestions(newQuestions)
    }

    suspend fun replaceAllExamQuestions(nextQuestions: List<Question>) {
        repository.replaceExamQuestions(nextQuestions)
        clearSessionIfQuestionsImpacted(null)
    }

    suspend fun deleteQuestions(ids: List<String>) {
        repository.deleteQuestions(ids)
        clearSessionIfQuestionsImpacted(ids.toSet())
    }

    suspend fun deleteAllQuestions() {
        repository.clearAllQuestions()
        clearSessionIfQuestionsImpacted(null)
    }

    private suspend fun clearSessionIfQuestionsImpacted(deletedIds: Set<String>?) {
        val sess = uiState.value.activeSession ?: return
        if (deletedIds == null || sess.questionIds.any { it in deletedIds }) {
            stopExamTimer()
            repository.clearActiveSession()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopExamTimer()
    }

    class Factory(private val repository: ExamRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
