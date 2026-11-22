package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.domain.model.TestMode
import com.example.ui.components.NismTopBar
import com.example.ui.components.ReplaceSessionDialog
import com.example.ui.navigation.AppRoute
import com.example.ui.screens.ChapterPracticeScreen
import com.example.ui.screens.ExamWorkbenchScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.PerformanceScreen
import com.example.ui.screens.QuestionBankScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.TestHistoryScreen
import com.example.ui.theme.NismExamPrepTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.NavigationEvent

private data class TopLevelNavItem(
    val route: AppRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val TOP_LEVEL_ITEMS = listOf(
    TopLevelNavItem(
        route = AppRoute.Dashboard,
        label = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        tag = "nav_dashboard"
    ),
    TopLevelNavItem(
        route = AppRoute.Chapters,
        label = "Chapters",
        selectedIcon = Icons.Filled.Bookmarks,
        unselectedIcon = Icons.Outlined.Bookmarks,
        tag = "nav_chapters"
    ),
    TopLevelNavItem(
        route = AppRoute.Analytics,
        label = "Analytics",
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics,
        tag = "nav_analytics"
    ),
    TopLevelNavItem(
        route = AppRoute.History,
        label = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        tag = "nav_history"
    ),
    TopLevelNavItem(
        route = AppRoute.QuestionBank,
        label = "Bank",
        selectedIcon = Icons.Filled.FolderCopy,
        unselectedIcon = Icons.Outlined.FolderCopy,
        tag = "nav_question_bank"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NismPrepApplication
        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(app.repository)
            )
            NismPrepApp(viewModel = viewModel)
        }
    }
}

@Composable
private fun NismPrepApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingStart by viewModel.pendingStartRequest.collectAsStateWithLifecycle()
    val bankBanner by viewModel.questionBankBanner.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isInActiveExam = currentDestination?.hasRoute<AppRoute.ExamWorkbench>() == true

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                NavigationEvent.OpenWorkbench -> {
                    navController.navigate(AppRoute.ExamWorkbench) {
                        launchSingleTop = true
                    }
                }
                is NavigationEvent.OpenResults -> {
                    navController.navigate(AppRoute.Results(event.attemptId)) {
                        popUpTo(AppRoute.ExamWorkbench) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                NavigationEvent.OpenQuestionBank -> {
                    navController.navigate(AppRoute.QuestionBank) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }

    fun navigateTopLevel(route: AppRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NismExamPrepTheme(darkTheme = uiState.isDarkMode) {
        if (uiState.isLoading) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Loading NISM Series V-A Question Bank...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            return@NismExamPrepTheme
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 720.dp && !isInActiveExam

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    if (!isInActiveExam) {
                        NismTopBar(
                            isDarkMode = uiState.isDarkMode,
                            onToggleTheme = { viewModel.toggleDarkMode() },
                            onStartFullMock = { viewModel.requestStartFullMock() },
                            onBrandClick = { navigateTopLevel(AppRoute.Dashboard) }
                        )
                    }
                },
                bottomBar = {
                    if (!isInActiveExam && !useNavRail) {
                        Column {
                            androidx.compose.material3.HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline
                            )
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                TOP_LEVEL_ITEMS.forEach { item ->
                                    val selected = currentDestination?.hasRoute(item.route::class) == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopLevel(item.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        modifier = Modifier.testTag(item.tag)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TOP_LEVEL_ITEMS.forEach { item ->
                                val selected = currentDestination?.hasRoute(item.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag(item.tag)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (uiState.errorNotice != null && !isInActiveExam) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Storage Notice: ${uiState.errorNotice}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = AppRoute.Dashboard,
                            modifier = Modifier.weight(1f)
                        ) {
                            composable<AppRoute.Dashboard> {
                                HomeDashboardScreen(
                                    exam = uiState.exam,
                                    questionCount = uiState.questions.size,
                                    analytics = uiState.analytics,
                                    recentAttempts = uiState.attempts,
                                    activeSession = uiState.activeSession,
                                    isDarkMode = uiState.isDarkMode,
                                    onStartFullMock = { viewModel.requestStartFullMock() },
                                    onNavigateChapters = { navigateTopLevel(AppRoute.Chapters) },
                                    onNavigateAnalytics = { navigateTopLevel(AppRoute.Analytics) },
                                    onNavigateHistory = { navigateTopLevel(AppRoute.History) },
                                    onNavigateQuestionBank = { navigateTopLevel(AppRoute.QuestionBank) },
                                    onReviewAttempt = { attempt ->
                                        navController.navigate(AppRoute.Results(attempt.id))
                                    },
                                    onResumeSession = { viewModel.resumeSavedSession() },
                                    onDiscardSession = { viewModel.discardSavedSession() }
                                )
                            }

                            composable<AppRoute.Chapters> {
                                ChapterPracticeScreen(
                                    exam = uiState.exam,
                                    chapterStats = uiState.analytics.chapterStats,
                                    isDarkMode = uiState.isDarkMode,
                                    onStartChapterPractice = { chNum, count ->
                                        viewModel.requestStartChapterPractice(chNum, count)
                                    },
                                    onNavigateQuestionBank = { navigateTopLevel(AppRoute.QuestionBank) }
                                )
                            }

                            composable<AppRoute.Analytics> {
                                PerformanceScreen(
                                    exam = uiState.exam,
                                    analytics = uiState.analytics,
                                    totalAttempts = uiState.attempts.size,
                                    isDarkMode = uiState.isDarkMode,
                                    onStartChapterPractice = { chNum, count ->
                                        viewModel.requestStartChapterPractice(chNum, count)
                                    },
                                    onViewHistory = { navigateTopLevel(AppRoute.History) }
                                )
                            }

                            composable<AppRoute.History> {
                                TestHistoryScreen(
                                    exam = uiState.exam,
                                    attempts = uiState.attempts,
                                    isDarkMode = uiState.isDarkMode,
                                    onReviewAttempt = { attempt ->
                                        navController.navigate(AppRoute.Results(attempt.id))
                                    },
                                    onDeleteAttempt = { id -> viewModel.deleteAttempt(id) },
                                    onStartFullMock = { viewModel.requestStartFullMock() },
                                    onGoToChapters = { navigateTopLevel(AppRoute.Chapters) }
                                )
                            }

                            composable<AppRoute.QuestionBank> {
                                QuestionBankScreen(
                                    exam = uiState.exam,
                                    questions = uiState.questions,
                                    statusBanner = bankBanner,
                                    isDarkMode = uiState.isDarkMode,
                                    onDismissBanner = { viewModel.setQuestionBankBanner(null) },
                                    onSetBanner = { viewModel.setQuestionBankBanner(it) },
                                    onImportQuestions = { viewModel.importQuestions(it) },
                                    onDeleteQuestions = { viewModel.deleteQuestions(it) },
                                    onReplaceQuestions = { viewModel.replaceAllExamQuestions(it) },
                                    onDeleteAllQuestions = { viewModel.deleteAllQuestions() }
                                )
                            }

                            composable<AppRoute.ExamWorkbench> {
                                val session = uiState.activeSession
                                if (session != null) {
                                    val sessionQuestions = session.questionIds.mapNotNull { uiState.questionMap[it] }
                                    ExamWorkbenchScreen(
                                        session = session,
                                        questions = sessionQuestions,
                                        isDarkMode = uiState.isDarkMode,
                                        onStartTimer = { viewModel.startExamTimer() },
                                        onStopTimer = { viewModel.stopExamTimer() },
                                        onSelectAnswer = { viewModel.selectAnswer(it) },
                                        onClearAnswer = { viewModel.clearCurrentAnswer() },
                                        onToggleReview = { viewModel.toggleMarkForReview() },
                                        onNavigateToIndex = { viewModel.navigateToQuestionIndex(it) },
                                        onSubmitTest = { viewModel.submitActiveTest(autoSubmitted = false) },
                                        onPauseAndExit = {
                                            viewModel.stopExamTimer()
                                            navController.popBackStack()
                                        }
                                    )
                                } else {
                                    LaunchedEffect(Unit) {
                                        navController.popBackStack()
                                    }
                                }
                            }

                            composable<AppRoute.Results> { backStackEntry ->
                                val route = backStackEntry.toRoute<AppRoute.Results>()
                                val attempt = uiState.attempts.find { it.id == route.attemptId }
                                ResultsScreen(
                                    attempt = attempt,
                                    exam = uiState.exam,
                                    questionMap = uiState.questionMap,
                                    isDarkMode = uiState.isDarkMode,
                                    onBack = { navController.popBackStack() },
                                    onRetakeTest = { att ->
                                        if (att.mode == TestMode.FULL_MOCK) {
                                            viewModel.requestStartFullMock()
                                        } else if (att.chapterNumber != null) {
                                            viewModel.requestStartChapterPractice(
                                                att.chapterNumber,
                                                att.totalQuestions
                                            )
                                        } else {
                                            navigateTopLevel(AppRoute.Chapters)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (pendingStart != null && uiState.activeSession != null) {
            ReplaceSessionDialog(
                activeSession = uiState.activeSession!!,
                onCancel = { viewModel.dismissPendingStartRequest() },
                onResume = { viewModel.resumeFromPendingDialog() },
                onDiscardAndStart = { viewModel.confirmDiscardAndStartPending() }
            )
        }
    }
}
