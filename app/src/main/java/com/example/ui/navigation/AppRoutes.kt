package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable
    data object Dashboard : AppRoute

    @Serializable
    data object Chapters : AppRoute

    @Serializable
    data object Analytics : AppRoute

    @Serializable
    data object History : AppRoute

    @Serializable
    data object QuestionBank : AppRoute

    @Serializable
    data object ExamWorkbench : AppRoute

    @Serializable
    data class Results(val attemptId: String) : AppRoute
}
