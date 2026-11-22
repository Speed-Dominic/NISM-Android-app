# NISM Exam Prep Studio (Android)

Native Android application built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Room**, and **DataStore Preferences** for preparing for the **NISM Series V-A: Mutual Fund Distributors Certification Examination**.

## Features

- **Full-Length Mock Tests**: 100-question weighted mock exams across all 12 official chapters with a 120-minute countdown timer, 1 mark per question, zero negative marking, and a 50% passing threshold.
- **Chapter-Wise Timed Practice**: Select any of the 12 NISM Series V-A chapters, customize question count (up to 25), and practice with a 60-second per-question timer featuring auto-advance and auto-submit.
- **Distraction-Free Exam Workbench**: Mark questions for review, clear answers, track answered/unanswered/review status in an adaptive question palette, and safely pause/resume interrupted sessions.
- **Results & Detailed Review**: Pass/fail verdict, chapter-wise accuracy breakdown, and filterable question-by-question answer explanations.
- **Performance Analytics & Test History**: KPI summary, dual-line Improvement Over Time chart, weak-chapter drill shortcuts, full 12-chapter mastery breakdown, and filterable attempt history.
- **Offline Question Bank Manager**: Bundled 300-question bank (Chapters 1–5) seeded automatically into Room, plus search/filter, manual question creation, JSON and `.doc`/`.txt` file import/delete via Storage Access Framework, and JSON export.
- **Light & Dark Theme**: Material 3 adaptive UI with Light and OLED Dark modes persisted via DataStore.

## Tech Stack

- **Language**: Kotlin 2.2
- **UI**: Jetpack Compose + Material Design 3 + Navigation Compose (type-safe `@Serializable` routes)
- **Persistence**: Room (`2.7.0`) with KSP (`2.3.5`) + Jetpack DataStore Preferences
- **Build**: Android Gradle Plugin `9.1.1`, Gradle `9.3.1`, Compile/Target SDK `36`, Min SDK `24`
