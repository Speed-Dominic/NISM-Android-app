package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.ExamDefinition
import com.example.domain.model.OptionKey
import com.example.domain.model.Question
import com.example.domain.util.QuestionImportParser
import com.example.ui.theme.NismColors
import com.example.ui.theme.TabularMonoStyle
import com.example.ui.viewmodel.StatusBanner
import kotlinx.coroutines.launch

enum class ManagerTab {
    JSON_ADD, JSON_DELETE, TEXT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuestionDialog(
    exam: ExamDefinition,
    initialChapter: Int,
    onDismiss: () -> Unit,
    onSave: suspend (Question) -> Unit
) {
    val scope = rememberCoroutineScope()

    var chapterNumber by remember { mutableIntStateOf(initialChapter.coerceIn(1, exam.chapters.size)) }
    var chapterMenuExpanded by remember { mutableStateOf(false) }

    var topic by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Moderate") }
    var diffMenuExpanded by remember { mutableStateOf(false) }

    var questionType by remember { mutableStateOf("Conceptual") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    var questionText by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correctAnswer by remember { mutableStateOf(OptionKey.A) }
    var explanation by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val selectedChapter = exam.chapters.find { it.chapterNumber == chapterNumber } ?: exam.chapters.first()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Add Question to ${exam.code}", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Create a new MCQ in local storage for Full Mock Tests and Chapter Practice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = chapterMenuExpanded,
                    onExpandedChange = { chapterMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "Ch. ${selectedChapter.chapterNumber}: ${selectedChapter.title}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Chapter") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = chapterMenuExpanded,
                        onDismissRequest = { chapterMenuExpanded = false }
                    ) {
                        exam.chapters.forEach { ch ->
                            DropdownMenuItem(
                                text = { Text("Ch. ${ch.chapterNumber}: ${ch.title}") },
                                onClick = {
                                    chapterNumber = ch.chapterNumber
                                    chapterMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic / Subtopic") },
                    placeholder = { Text("e.g., Expense Ratio, KYC Norms") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = diffMenuExpanded,
                        onExpandedChange = { diffMenuExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = difficulty,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Difficulty") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = diffMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = diffMenuExpanded,
                            onDismissRequest = { diffMenuExpanded = false }
                        ) {
                            QuestionImportParser.VALID_DIFFICULTIES.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d) },
                                    onClick = {
                                        difficulty = d
                                        diffMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = typeMenuExpanded,
                        onExpandedChange = { typeMenuExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = questionType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false }
                        ) {
                            QuestionImportParser.VALID_TYPES.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t) },
                                    onClick = {
                                        questionType = t
                                        typeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("Question Text") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_question_text_input")
                )

                OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth().testTag("add_option_a_input"))
                OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth().testTag("add_option_b_input"))
                OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option C") }, modifier = Modifier.fillMaxWidth().testTag("add_option_c_input"))
                OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option D") }, modifier = Modifier.fillMaxWidth().testTag("add_option_d_input"))

                Text("Correct Answer", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OptionKey.entries.forEach { key ->
                        val active = correctAnswer == key
                        Surface(
                            onClick = { correctAnswer = key },
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = key.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("Explanation") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_explanation_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = NismColors.Rose
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = !isSaving,
                        onClick = {
                            if (questionText.isBlank() || optA.isBlank() || optB.isBlank() ||
                                optC.isBlank() || optD.isBlank() || explanation.isBlank()
                            ) {
                                errorMessage = "Please fill in the question text, all four options (A–D), and the explanation."
                                return@Button
                            }
                            val chPad = selectedChapter.chapterNumber.toString().padStart(2, '0')
                            val newQ = Question(
                                id = "${exam.id.uppercase()}-C$chPad-USER-${System.currentTimeMillis()}",
                                examId = exam.id,
                                examName = exam.name,
                                chapterNumber = selectedChapter.chapterNumber,
                                chapterTitle = selectedChapter.title,
                                topic = topic.trim().ifEmpty { "General Concept" },
                                questionText = questionText.trim(),
                                options = mapOf(
                                    OptionKey.A to optA.trim(),
                                    OptionKey.B to optB.trim(),
                                    OptionKey.C to optC.trim(),
                                    OptionKey.D to optD.trim()
                                ),
                                correctAnswer = correctAnswer,
                                explanation = explanation.trim(),
                                difficulty = difficulty,
                                questionType = questionType
                            )
                            isSaving = true
                            scope.launch {
                                try {
                                    onSave(newQ)
                                    onDismiss()
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Failed to save question."
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        modifier = Modifier.testTag("save_single_question_button")
                    ) {
                        Text(if (isSaving) "Saving..." else "Save Question")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportQuestionsDialog(
    exam: ExamDefinition,
    initialTab: ManagerTab,
    initialText: String,
    existingQuestionIds: Set<String>,
    onDismiss: () -> Unit,
    onImportMerge: suspend (List<Question>) -> Unit,
    onReplaceBank: suspend (List<Question>) -> Unit,
    onDeleteIds: suspend (List<String>) -> Unit,
    onStatusBanner: (StatusBanner) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var managerTab by remember { mutableStateOf(initialTab) }
    var jsonWriteMode by remember { mutableStateOf("merge") }
    var defaultChapter by remember { mutableIntStateOf(1) }
    var chapterDropdownExpanded by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf(initialText) }
    var feedback by remember { mutableStateOf<StatusBanner?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        feedback = null
        try {
            var fileName = "uploaded_file"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(idx) ?: fileName
                }
            }
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Could not read selected file.")

            if (fileName.lowercase().endsWith(".json")) {
                if (managerTab == ManagerTab.TEXT) {
                    managerTab = ManagerTab.JSON_ADD
                }
                importText = String(bytes, Charsets.UTF_8)
            } else {
                managerTab = ManagerTab.TEXT
                importText = QuestionImportParser.extractTextFromBytes(fileName, bytes)
            }
        } catch (e: Exception) {
            feedback = StatusBanner(false, e.message ?: "Failed to read file.")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Manage Questions via .json & Text", style = MaterialTheme.typography.titleLarge)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            ManagerTab.JSON_ADD to "Add / Sync",
                            ManagerTab.JSON_DELETE to "Delete .json",
                            ManagerTab.TEXT to ".doc / Text"
                        ).forEach { (tab, label) ->
                            val active = managerTab == tab
                            Surface(
                                onClick = {
                                    feedback = null
                                    managerTab = tab
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        color = if (active) {
                                            if (tab == ManagerTab.JSON_DELETE) NismColors.Rose else MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = when (managerTab) {
                        ManagerTab.JSON_ADD -> "Upload a .json file or paste JSON questions to add/merge or replace the entire bank."
                        ManagerTab.JSON_DELETE -> "Upload or paste JSON IDs ([\"ID-1\"], {\"deleteIds\": [...]}, or [{\"id\": \"...\"}]) to delete."
                        ManagerTab.TEXT -> "Upload a .doc or .txt file, or paste chapter-wise MCQs from Word."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (managerTab == ManagerTab.JSON_ADD) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(selected = jsonWriteMode == "merge", onClick = { jsonWriteMode = "merge" })
                                Text("Add / Update existing bank", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(selected = jsonWriteMode == "replace", onClick = { jsonWriteMode = "replace" })
                                Text("Replace entire bank (Sync Add & Delete)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val mimeTypes = if (managerTab == ManagerTab.TEXT) {
                                arrayOf("application/json", "text/plain", "application/msword", "*/*")
                            } else {
                                arrayOf("application/json", "text/plain", "*/*")
                            }
                            filePickerLauncher.launch(mimeTypes)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (managerTab == ManagerTab.TEXT) "Load File" else "Upload .json")
                    }

                    TextButton(
                        onClick = {
                            importText = when (managerTab) {
                                ManagerTab.JSON_ADD -> QuestionImportParser.SAMPLE_ADD_JSON
                                ManagerTab.JSON_DELETE -> QuestionImportParser.SAMPLE_DELETE_JSON
                                ManagerTab.TEXT -> QuestionImportParser.SAMPLE_TEXT_TEMPLATE
                            }
                        }
                    ) {
                        Text("Insert Sample")
                    }
                }

                if (managerTab == ManagerTab.TEXT) {
                    ExposedDropdownMenuBox(
                        expanded = chapterDropdownExpanded,
                        onExpandedChange = { chapterDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "Default: Ch. $defaultChapter",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fallback Chapter") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = chapterDropdownExpanded,
                            onDismissRequest = { chapterDropdownExpanded = false }
                        ) {
                            exam.chapters.forEach { ch ->
                                DropdownMenuItem(
                                    text = { Text("Ch. ${ch.chapterNumber}: ${ch.title}") },
                                    onClick = {
                                        defaultChapter = ch.chapterNumber
                                        chapterDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    minLines = 6,
                    maxLines = 9,
                    textStyle = MaterialTheme.typography.bodySmall.merge(TabularMonoStyle),
                    placeholder = {
                        Text(
                            when (managerTab) {
                                ManagerTab.JSON_ADD -> "Paste JSON array of questions..."
                                ManagerTab.JSON_DELETE -> "Paste JSON IDs or { \"deleteIds\": [...] }..."
                                ManagerTab.TEXT -> "Paste chapter-wise MCQs..."
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_modal_textarea")
                )

                if (feedback != null) {
                    val f = feedback!!
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (f.isSuccess) NismColors.EmeraldBgLight else NismColors.RoseBgLight,
                        border = BorderStroke(1.dp, if (f.isSuccess) NismColors.Emerald else NismColors.Rose)
                    ) {
                        Text(
                            text = f.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (f.isSuccess) NismColors.Emerald else NismColors.Rose,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = importText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (managerTab == ManagerTab.JSON_DELETE) NismColors.Rose else MaterialTheme.colorScheme.primary
                        ),
                        onClick = {
                            feedback = null
                            scope.launch {
                                try {
                                    when (managerTab) {
                                        ManagerTab.TEXT -> {
                                            val result = QuestionImportParser.parseChapterWiseMcqText(
                                                rawText = importText,
                                                exam = exam,
                                                defaultChapterNumber = defaultChapter
                                            )
                                            if (result.questions.isEmpty()) {
                                                feedback = StatusBanner(
                                                    false,
                                                    "No complete MCQs detected. Ensure numbered questions, options A–D, and Answer line."
                                                )
                                                return@launch
                                            }
                                            onImportMerge(result.questions)
                                            val warnSuffix = if (result.warnings.isNotEmpty()) {
                                                " (${result.warnings.size} incomplete skipped)"
                                            } else ""
                                            val msg = "Added ${result.questions.size} MCQ(s) for ${exam.code}.$warnSuffix"
                                            feedback = StatusBanner(true, msg)
                                            onStatusBanner(StatusBanner(true, msg))
                                            importText = ""
                                        }
                                        ManagerTab.JSON_ADD -> {
                                            val validated = QuestionImportParser.validateJsonQuestions(importText, exam)
                                            if (jsonWriteMode == "replace") {
                                                onReplaceBank(validated)
                                                val msg = "Replaced ${exam.code} bank with ${validated.size} question(s)."
                                                feedback = StatusBanner(true, msg)
                                                onStatusBanner(StatusBanner(true, msg))
                                            } else {
                                                onImportMerge(validated)
                                                val msg = "Added/updated ${validated.size} question(s) in the bank."
                                                feedback = StatusBanner(true, msg)
                                                onStatusBanner(StatusBanner(true, msg))
                                            }
                                            importText = ""
                                        }
                                        ManagerTab.JSON_DELETE -> {
                                            val requestedIds = QuestionImportParser.extractDeleteIdsFromJson(importText)
                                            val matching = requestedIds.filter { it in existingQuestionIds }
                                            if (matching.isEmpty()) {
                                                feedback = StatusBanner(
                                                    false,
                                                    "None of the ${requestedIds.size} ID(s) matched questions in the bank."
                                                )
                                                return@launch
                                            }
                                            onDeleteIds(matching)
                                            val skipped = requestedIds.size - matching.size
                                            val msg = "Deleted ${matching.size} question(s)." +
                                                if (skipped > 0) " ($skipped ID(s) not found)" else ""
                                            feedback = StatusBanner(true, msg)
                                            onStatusBanner(StatusBanner(true, msg))
                                            importText = ""
                                        }
                                    }
                                } catch (e: Exception) {
                                    feedback = StatusBanner(false, e.message ?: "Invalid input format.")
                                }
                            }
                        },
                        modifier = Modifier.testTag("import_modal_submit_button")
                    ) {
                        Text(
                            when {
                                managerTab == ManagerTab.JSON_DELETE -> "Delete Matching"
                                managerTab == ManagerTab.JSON_ADD && jsonWriteMode == "replace" -> "Replace Bank"
                                else -> "Save to Bank"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = NismColors.Rose)
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
