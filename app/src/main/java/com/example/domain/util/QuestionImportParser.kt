package com.example.domain.util

import com.example.domain.model.ExamDefinition
import com.example.domain.model.OptionKey
import com.example.domain.model.Question
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ExportQuestionDto(
    val id: String,
    val examId: String,
    val examName: String,
    val chapterNumber: Int,
    val chapterTitle: String,
    val topic: String,
    val questionText: String,
    val options: Map<String, String>,
    val correctAnswer: String,
    val explanation: String,
    val difficulty: String,
    val questionType: String
)

object QuestionImportParser {

    private val prettyJson = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    val VALID_DIFFICULTIES = listOf("Easy", "Moderate", "Difficult")
    val VALID_TYPES = listOf(
        "Conceptual",
        "Definition",
        "Practical Application",
        "Scenario-based",
        "Numerical",
        "Case Study",
        "Regulatory & Compliance",
        "Exceptions & Conditions"
    )

    val SAMPLE_ADD_JSON: String = """
[
  {
    "id": "VA-CUSTOM-001",
    "chapterNumber": 1,
    "topic": "Asset Allocation",
    "questionText": "Which of the following best describes real rate of return?",
    "options": {
      "A": "Nominal return plus inflation rate",
      "B": "Nominal return adjusted for inflation",
      "C": "Pre-tax return before compounding",
      "D": "Dividend yield divided by face value"
    },
    "correctAnswer": "B",
    "explanation": "Real rate of return reflects purchasing power after adjusting nominal return for inflation.",
    "difficulty": "Moderate",
    "questionType": "Conceptual"
  }
]
    """.trimIndent()

    val SAMPLE_DELETE_JSON: String = """
{
  "deleteIds": [
    "VA-CUSTOM-001",
    "VA-C01-Q001"
  ]
}
    """.trimIndent()

    val SAMPLE_TEXT_TEMPLATE: String = """
Chapter 1: Investment Landscape
1. Which of the following best describes real rate of return?
A. Nominal return plus inflation
B. Nominal return adjusted for inflation
C. Pre-tax return before compounding
D. Dividend yield divided by NAV
Answer: B
Explanation: Real rate of return adjusts nominal returns for purchasing power erosion caused by inflation.
    """.trimIndent()

    fun exportQuestionsToJson(questions: List<Question>): String {
        val dtos = questions.map { q ->
            ExportQuestionDto(
                id = q.id,
                examId = q.examId,
                examName = q.examName,
                chapterNumber = q.chapterNumber,
                chapterTitle = q.chapterTitle,
                topic = q.topic,
                questionText = q.questionText,
                options = linkedMapOf(
                    "A" to q.options[OptionKey.A].orEmpty(),
                    "B" to q.options[OptionKey.B].orEmpty(),
                    "C" to q.options[OptionKey.C].orEmpty(),
                    "D" to q.options[OptionKey.D].orEmpty()
                ),
                correctAnswer = q.correctAnswer.name,
                explanation = q.explanation,
                difficulty = q.difficulty,
                questionType = q.questionType
            )
        }
        return prettyJson.encodeToString(dtos)
    }

    fun validateJsonQuestions(rawText: String, exam: ExamDefinition): List<Question> {
        val root: JsonElement = try {
            prettyJson.parseToJsonElement(rawText)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON syntax: ${e.message ?: "Parse failed"}")
        }

        val rawItems: List<JsonElement> = when (root) {
            is JsonArray -> root
            is JsonObject -> {
                val questionsProp = root["questions"]
                when {
                    questionsProp is JsonArray -> questionsProp
                    else -> listOf(root)
                }
            }
            else -> throw IllegalArgumentException(
                "JSON must be an array of question objects or { \"questions\": [...] }."
            )
        }

        if (rawItems.isEmpty()) {
            throw IllegalArgumentException("The JSON file contains 0 questions.")
        }

        val validated = mutableListOf<Question>()
        val seenIds = mutableSetOf<String>()
        val baseTs = System.currentTimeMillis()
        val chapterMap = exam.chapters.associateBy { it.chapterNumber }

        rawItems.forEachIndexed { index, element ->
            val obj = (element as? JsonObject)
                ?: throw IllegalArgumentException("Item ${index + 1}: Must be a JSON object.")

            val chNum = (obj["chapterNumber"] as? JsonPrimitive)?.intOrNull
                ?: (obj["chapterNumber"] as? JsonPrimitive)?.content?.toIntOrNull()
                ?: throw IllegalArgumentException(
                    "Item ${index + 1}: chapterNumber must be between 1 and ${exam.chapters.size}."
                )

            val chapterObj = chapterMap[chNum]
                ?: throw IllegalArgumentException(
                    "Item ${index + 1}: chapterNumber \"$chNum\" must be between 1 and ${exam.chapters.size}."
                )

            val qText = (obj["questionText"] as? JsonPrimitive)?.content?.trim().orEmpty()
            val optsObj = (obj["options"] as? JsonObject)
            val optA = (optsObj?.get("A") as? JsonPrimitive)?.content?.trim().orEmpty()
            val optB = (optsObj?.get("B") as? JsonPrimitive)?.content?.trim().orEmpty()
            val optC = (optsObj?.get("C") as? JsonPrimitive)?.content?.trim().orEmpty()
            val optD = (optsObj?.get("D") as? JsonPrimitive)?.content?.trim().orEmpty()
            val correctRaw = (obj["correctAnswer"] as? JsonPrimitive)?.content?.trim()?.uppercase().orEmpty()
            val correctKey = runCatching { OptionKey.valueOf(correctRaw) }.getOrNull()
            val explanation = (obj["explanation"] as? JsonPrimitive)?.content?.trim().orEmpty()

            if (qText.isEmpty() || optA.isEmpty() || optB.isEmpty() || optC.isEmpty() || optD.isEmpty() ||
                correctKey == null || explanation.isEmpty()
            ) {
                throw IllegalArgumentException(
                    "Item ${index + 1}: Missing required fields (questionText, options.A–D, correctAnswer A/B/C/D, explanation)."
                )
            }

            val rawId = (obj["id"] as? JsonPrimitive)?.content?.trim().orEmpty()
            var qId = rawId.ifEmpty { "${exam.id.uppercase()}-JSON-$baseTs-${index + 1}" }
            if (!seenIds.add(qId)) {
                qId = "$qId-${index + 1}"
                seenIds.add(qId)
            }

            val topic = (obj["topic"] as? JsonPrimitive)?.content?.trim().orEmpty()
                .ifEmpty { "General Concept" }
            val diffRaw = (obj["difficulty"] as? JsonPrimitive)?.content?.trim().orEmpty()
            val difficulty = if (diffRaw in VALID_DIFFICULTIES) diffRaw else "Moderate"
            val qType = (obj["questionType"] as? JsonPrimitive)?.content?.trim().orEmpty()
                .ifEmpty { "Conceptual" }

            validated.add(
                Question(
                    id = qId,
                    examId = exam.id,
                    examName = exam.name,
                    chapterNumber = chNum,
                    chapterTitle = chapterObj.title,
                    topic = topic,
                    questionText = qText,
                    options = mapOf(
                        OptionKey.A to optA,
                        OptionKey.B to optB,
                        OptionKey.C to optC,
                        OptionKey.D to optD
                    ),
                    correctAnswer = correctKey,
                    explanation = explanation,
                    difficulty = difficulty,
                    questionType = qType
                )
            )
        }

        return validated
    }

    fun extractDeleteIdsFromJson(rawText: String): List<String> {
        val root: JsonElement = try {
            prettyJson.parseToJsonElement(rawText)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON syntax: ${e.message ?: "Parse failed"}")
        }

        val rawList: List<JsonElement>? = when (root) {
            is JsonArray -> root
            is JsonObject -> {
                val deleteIds = root["deleteIds"] as? JsonArray
                val ids = root["ids"] as? JsonArray
                val questions = root["questions"] as? JsonArray
                val singleId = root["id"] as? JsonPrimitive
                when {
                    deleteIds != null -> deleteIds
                    ids != null -> ids
                    questions != null -> questions
                    singleId != null && singleId.isString -> listOf(singleId)
                    else -> null
                }
            }
            else -> null
        }

        if (rawList.isNullOrEmpty()) {
            throw IllegalArgumentException(
                "Provide a JSON array of question IDs (e.g. [\"VA-C01-Q001\"]), { \"deleteIds\": [...] }, or question objects containing \"id\"."
            )
        }

        val extracted = linkedSetOf<String>()
        rawList.forEachIndexed { idx, entry ->
            when {
                entry is JsonPrimitive && entry.isString && entry.content.trim().isNotEmpty() -> {
                    extracted.add(entry.content.trim())
                }
                entry is JsonObject -> {
                    val idVal = (entry["id"] as? JsonPrimitive)?.content?.trim().orEmpty()
                    if (idVal.isEmpty()) {
                        throw IllegalArgumentException("Entry ${idx + 1}: Object is missing a non-empty \"id\" field.")
                    }
                    extracted.add(idVal)
                }
                else -> {
                    throw IllegalArgumentException(
                        "Entry ${idx + 1}: Expected a question ID string or object with an \"id\" field."
                    )
                }
            }
        }
        return extracted.toList()
    }

    fun extractTextFromBytes(fileName: String, bytes: ByteArray): String {
        val lower = fileName.lowercase()
        val previewLen = minOf(512, bytes.size)
        val utf8Preview = String(bytes, 0, previewLen, Charsets.UTF_8)

        if (lower.endsWith(".txt") ||
            utf8Preview.contains("<html", ignoreCase = true) ||
            utf8Preview.contains("<?xml", ignoreCase = true) ||
            utf8Preview.contains("<w:wordDocument", ignoreCase = true)
        ) {
            val raw = String(bytes, Charsets.UTF_8)
            if (raw.contains("<")) {
                return raw
                    .replace(Regex("</(p|div|li|tr|h[1-6]|w:p)>", RegexOption.IGNORE_CASE), "\n")
                    .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                    .replace(Regex("<[^>]+>"), " ")
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
            }
            return raw
        }

        // Legacy OLE2 binary .doc (D0 CF 11 E0)
        if (bytes.size >= 4 &&
            bytes[0] == 0xD0.toByte() &&
            bytes[1] == 0xCF.toByte() &&
            bytes[2] == 0x11.toByte() &&
            bytes[3] == 0xE0.toByte()
        ) {
            val latin = String(bytes, Charsets.ISO_8859_1)
            return latin
                .replace("\r", "\n")
                .replace(Regex("[^\\x20-\\x7E\\n]"), " ")
                .replace(Regex("[ \\t]{2,}"), " ")
                .split("\n")
                .map { it.trim() }
                .filter { line -> line.length >= 2 && line.any { it.isLetterOrDigit() } }
                .joinToString("\n")
        }

        return String(bytes, Charsets.UTF_8)
    }

    data class TextParseResult(
        val questions: List<Question>,
        val warnings: List<String>
    )

    fun parseChapterWiseMcqText(
        rawText: String,
        exam: ExamDefinition,
        defaultChapterNumber: Int = 1
    ): TextParseResult {
        val lines = rawText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .split("\n")
            .map { it.trim() }

        var currentChapter = defaultChapterNumber
        val questions = mutableListOf<Question>()
        val warnings = mutableListOf<String>()

        var qTextParts = mutableListOf<String>()
        var options = mutableMapOf<OptionKey, String>()
        var currentOptKey: OptionKey? = null
        var correctAnswer: OptionKey? = null
        var explanationParts = mutableListOf<String>()
        var inExplanation = false

        val chapterMap = exam.chapters.associateBy { it.chapterNumber }

        fun flushCurrent() {
            val qText = qTextParts.joinToString(" ").trim()
            val optA = options[OptionKey.A]
            val optB = options[OptionKey.B]
            val optC = options[OptionKey.C]
            val optD = options[OptionKey.D]
            val ans = correctAnswer

            if (qText.isNotEmpty() && optA != null && optB != null && optC != null && optD != null && ans != null) {
                val chObj = chapterMap[currentChapter] ?: exam.chapters.first()
                val idx = questions.size + 1
                val chPad = chObj.chapterNumber.toString().padStart(2, '0')
                val ts = System.currentTimeMillis().toString(36)
                val fallbackExp = "Correct option is ${ans.name}: ${options[ans]?.trim().orEmpty()}."
                questions.add(
                    Question(
                        id = "${exam.id.uppercase()}-DOC-C$chPad-$ts-$idx",
                        examId = exam.id,
                        examName = exam.name,
                        chapterNumber = chObj.chapterNumber,
                        chapterTitle = chObj.title,
                        topic = chObj.keyTopics.firstOrNull() ?: chObj.title,
                        questionText = qText,
                        options = mapOf(
                            OptionKey.A to optA.trim(),
                            OptionKey.B to optB.trim(),
                            OptionKey.C to optC.trim(),
                            OptionKey.D to optD.trim()
                        ),
                        correctAnswer = ans,
                        explanation = explanationParts.joinToString(" ").trim().ifEmpty { fallbackExp },
                        difficulty = "Moderate",
                        questionType = "Conceptual"
                    )
                )
            } else if (qText.isNotEmpty() && (optA != null || optB != null)) {
                warnings.add("Skipped incomplete question: \"${qText.take(60)}...\"")
            }

            qTextParts = mutableListOf()
            options = mutableMapOf()
            currentOptKey = null
            correctAnswer = null
            explanationParts = mutableListOf()
            inExplanation = false
        }

        val chapterRegex = Regex("^(?:chapter|ch\\.?|unit|module)\\s*[-:]?\\s*(\\d{1,2})\\b", RegexOption.IGNORE_CASE)
        val questionStartRegex = Regex("^(?:q(?:uestion)?\\s*\\d+\\s*[.):-]|\\d{1,3}\\s*[.)])\\s*(.+)$", RegexOption.IGNORE_CASE)
        val optionRegex = Regex("^\\(?([A-Da-d])\\)?[.):\\-]\\s*(.+)$")
        val answerRegex = Regex("^(?:ans(?:wer)?|correct\\s*(?:ans(?:wer)?|option)?)\\s*[:=-]?\\s*\\(?([A-Da-d])\\)?\\b", RegexOption.IGNORE_CASE)
        val explanationRegex = Regex("^(?:exp(?:lanation)?|rationale|solution|reason)\\s*[:=-]\\s*(.*)$", RegexOption.IGNORE_CASE)

        for (line in lines) {
            if (line.isEmpty()) continue

            val chMatch = chapterRegex.find(line)
            if (chMatch != null) {
                val num = chMatch.groupValues[1].toIntOrNull()
                if (num != null && chapterMap.containsKey(num)) {
                    flushCurrent()
                    currentChapter = num
                    continue
                }
            }

            val ansMatch = answerRegex.find(line)
            if (ansMatch != null) {
                correctAnswer = runCatching {
                    OptionKey.valueOf(ansMatch.groupValues[1].uppercase())
                }.getOrNull()
                currentOptKey = null
                inExplanation = false
                continue
            }

            val expMatch = explanationRegex.find(line)
            if (expMatch != null) {
                inExplanation = true
                currentOptKey = null
                val rest = expMatch.groupValues[1].trim()
                if (rest.isNotEmpty()) explanationParts.add(rest)
                continue
            }

            val optMatch = optionRegex.find(line)
            if (optMatch != null && (qTextParts.isNotEmpty() || options.isNotEmpty())) {
                val key = runCatching { OptionKey.valueOf(optMatch.groupValues[1].uppercase()) }.getOrNull()
                if (key != null) {
                    currentOptKey = key
                    inExplanation = false
                    options[key] = optMatch.groupValues[2]
                    continue
                }
            }

            val qMatch = questionStartRegex.find(line)
            if (qMatch != null && (options.size >= 2 || correctAnswer != null || qTextParts.isEmpty())) {
                if (qTextParts.isNotEmpty() || options.isNotEmpty()) {
                    flushCurrent()
                }
                qTextParts.add(qMatch.groupValues[1])
                continue
            }

            when {
                inExplanation -> explanationParts.add(line)
                currentOptKey != null && options.containsKey(currentOptKey) -> {
                    val key = currentOptKey!!
                    options[key] = "${options[key]} $line"
                }
                qTextParts.isNotEmpty() -> qTextParts.add(line)
            }
        }

        flushCurrent()
        return TextParseResult(questions = questions, warnings = warnings)
    }
}
