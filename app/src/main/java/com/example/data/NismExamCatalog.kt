package com.example.data

import com.example.domain.model.ChapterDefinition
import com.example.domain.model.ExamDefinition
import com.example.domain.model.Question
import kotlin.math.floor

object NismExamCatalog {
    const val BUILTIN_BANK_VERSION = 5

    val NISM_VA_EXAM = ExamDefinition(
        id = "nism-va",
        code = "NISM-Series-V-A",
        name = "NISM-Series-V-A: Mutual Fund Distributors Certification Examination",
        shortName = "Mutual Fund Distributors (V-A)",
        description = "Covers the fundamentals of mutual funds, legal and regulatory framework, scheme disclosures, distribution practices, NAV & TER computation, taxation, investor services, and risk-return evaluation.",
        totalQuestionsFullMock = 100,
        totalMarksFullMock = 100,
        marksPerQuestion = 1,
        negativeMarking = 0,
        durationMinutes = 120,
        passingPercentage = 50,
        chapters = listOf(
            ChapterDefinition(
                chapterNumber = 1,
                title = "Investment Landscape",
                weightage = 6,
                summary = "Financial goals, asset classes, inflation impact, risk-return trade-off, and asset allocation.",
                keyTopics = listOf("Financial vs Physical Assets", "Inflation & Real Return", "Asset Allocation", "Investment Risks")
            ),
            ChapterDefinition(
                chapterNumber = 2,
                title = "Concept and Role of a Mutual Fund",
                weightage = 8,
                summary = "Pooling mechanism, mutual fund classification, open-ended vs close-ended, SEBI scheme categorization.",
                keyTopics = listOf("Advantages of Mutual Funds", "Open vs Close-ended", "SEBI Categorization", "IDCW vs Growth")
            ),
            ChapterDefinition(
                chapterNumber = 3,
                title = "Legal Structure of Mutual Funds in India",
                weightage = 6,
                summary = "Three-tier structure: Sponsor, Trust/Trustee, Asset Management Company, and key constituents like Custodian and RTA.",
                keyTopics = listOf("Sponsor Eligibility", "Board of Trustees", "AMC Responsibilities", "Custodian & RTA")
            ),
            ChapterDefinition(
                chapterNumber = 4,
                title = "Legal and Regulatory Framework",
                weightage = 10,
                summary = "SEBI (Mutual Funds) Regulations, AMFI Code of Ethics, investor rights, and advertisement guidelines.",
                keyTopics = listOf("SEBI Regulations", "AMFI Code of Conduct", "Due Diligence", "Unclaimed Dividend & Redemption")
            ),
            ChapterDefinition(
                chapterNumber = 5,
                title = "Scheme Related Information",
                weightage = 8,
                summary = "SID, SAI, KIM, addendum, risk-o-meter, and mandatory statutory disclosures.",
                keyTopics = listOf("SID & SAI", "Key Information Memorandum", "Risk-o-meter", "Scheme Disclosures")
            ),
            ChapterDefinition(
                chapterNumber = 6,
                title = "Fund Distribution and Channel Management Practices",
                weightage = 10,
                summary = "ARN registration, EUIN, commission structures, B30 incentives, and digital distribution channels.",
                keyTopics = listOf("ARN & KYD", "EUIN Norms", "Trail Commission", "Direct vs Regular Plans")
            ),
            ChapterDefinition(
                chapterNumber = 7,
                title = "Net Asset Value, Total Expense Ratio and Pricing of Units",
                weightage = 10,
                summary = "NAV calculation, mark-to-market valuation, TER slabs, cut-off timings, and stamp duty.",
                keyTopics = listOf("NAV Formula", "TER Limits", "Cut-off Timings", "Exit Load & Stamp Duty")
            ),
            ChapterDefinition(
                chapterNumber = 8,
                title = "Taxation",
                weightage = 8,
                summary = "Capital gains taxation, equity vs debt definitions, indexation rules, STT, TDS, and dividend taxation.",
                keyTopics = listOf("STCG & LTCG", "Specified Mutual Funds", "IDCW Taxation", "STT & TDS")
            ),
            ChapterDefinition(
                chapterNumber = 9,
                title = "Investor Services",
                weightage = 12,
                summary = "KYC norms, PAN exemptions, SIP/STP/SWP, nomination, transmission, and demat vs SoA.",
                keyTopics = listOf("KYC & Aadhaar", "SIP, STP & SWP", "Nomination & Transmission", "Pledge & Lien")
            ),
            ChapterDefinition(
                chapterNumber = 10,
                title = "Risk, Return and Performance of Funds",
                weightage = 8,
                summary = "CAGR, XIRR, standard deviation, beta, Sharpe ratio, Treynor ratio, and Macaulay duration.",
                keyTopics = listOf("CAGR & XIRR", "Standard Deviation & Beta", "Sharpe & Treynor", "Modified Duration")
            ),
            ChapterDefinition(
                chapterNumber = 11,
                title = "Mutual Fund Scheme Performance",
                weightage = 7,
                summary = "TRI benchmarks, Alpha, Tracking Error, Information Ratio, and peer comparison.",
                keyTopics = listOf("Total Return Index (TRI)", "Jensen Alpha", "Tracking Error", "Credit Quality & Yield")
            ),
            ChapterDefinition(
                chapterNumber = 12,
                title = "Mutual Fund Scheme Selection",
                weightage = 7,
                summary = "Matching schemes to investor profile, goal horizons, portfolio rebalancing, and model portfolios.",
                keyTopics = listOf("Risk Profiling", "Time Horizon Mapping", "Core & Satellite Portfolio", "Rebalancing")
            )
        )
    )

    private data class ChapterQuota(
        val chapterNumber: Int,
        val remainder: Double,
        var taken: Int
    )

    fun selectWeightedMockQuestions(
        exam: ExamDefinition,
        allExamQuestions: List<Question>,
        targetCount: Int = exam.totalQuestionsFullMock
    ): List<Question> {
        if (allExamQuestions.isEmpty()) return emptyList()

        val byChapter = mutableMapOf<Int, MutableList<Question>>()
        for (ch in exam.chapters) {
            byChapter[ch.chapterNumber] = mutableListOf()
        }
        for (q in allExamQuestions) {
            byChapter.getOrPut(q.chapterNumber) { mutableListOf() }.add(q)
        }
        byChapter.keys.forEach { chNum ->
            byChapter[chNum] = byChapter[chNum].orEmpty().shuffled().toMutableList()
        }

        val count = minOf(targetCount, allExamQuestions.size)
        val totalWeightage = exam.chapters.sumOf { it.weightage }.takeIf { it > 0 } ?: 100

        val selectedIds = mutableSetOf<String>()
        val selected = mutableListOf<Question>()

        val allocations = exam.chapters.map { ch ->
            val exact = (count.toDouble() * ch.weightage) / totalWeightage.toDouble()
            val ideal = floor(exact).toInt()
            val pool = byChapter[ch.chapterNumber].orEmpty()
            val take = minOf(ideal, pool.size)
            ChapterQuota(
                chapterNumber = ch.chapterNumber,
                remainder = exact - ideal,
                taken = take
            )
        }

        var allocated = allocations.sumOf { it.taken }
        val sortedForRemainder = allocations.sortedByDescending { it.remainder }
        for (item in sortedForRemainder) {
            if (allocated >= count) break
            val pool = byChapter[item.chapterNumber].orEmpty()
            if (item.taken < pool.size) {
                item.taken += 1
                allocated += 1
            }
        }

        for (alloc in allocations) {
            val pool = byChapter[alloc.chapterNumber].orEmpty()
            for (i in 0 until alloc.taken) {
                val q = pool.getOrNull(i)
                if (q != null && selectedIds.add(q.id)) {
                    selected.add(q)
                }
            }
        }

        if (selected.size < count) {
            val leftovers = allExamQuestions.filter { it.id !in selectedIds }.shuffled()
            for (q in leftovers) {
                if (selected.size >= count) break
                if (selectedIds.add(q.id)) {
                    selected.add(q)
                }
            }
        }

        return selected.shuffled()
    }

    fun selectChapterPracticeQuestions(
        examQuestions: List<Question>,
        chapterNumber: Int,
        requestedCount: Int
    ): List<Question> {
        val capped = requestedCount.coerceIn(1, 25)
        val chapterOnly = examQuestions.filter { it.chapterNumber == chapterNumber }.shuffled()
        return chapterOnly.take(minOf(capped, chapterOnly.size))
    }
}
