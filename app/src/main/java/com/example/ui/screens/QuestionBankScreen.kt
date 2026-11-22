package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ExamDefinition
import com.example.domain.model.OptionKey
import com.example.domain.model.Question
import com.example.domain.util.QuestionImportParser
import com.example.ui.theme.NismColors
import com.example.ui.theme.TabularMonoStyle
import com.example.ui.viewmodel.StatusBanner
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionBankScreen(
    exam: ExamDefinition,
    questions: List<Question>,
    statusBanner: StatusBanner?,
    isDarkMode: Boolean,
    onDismissBanner: () -> Unit,
    onSetBanner: (StatusBanner) -> Unit,
    onImportQuestions: suspend (List<Question>) -> Unit,
    onDeleteQuestions: suspend (List<String>) -> Unit,
    onReplaceQuestions: suspend (List<Question>) -> Unit,
    onDeleteAllQuestions: suspend () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var chapterFilter by remember { mutableStateOf("all") }
    var chapterMenuOpen by remember { mutableStateOf(false) }

    var difficultyFilter by remember { mutableStateOf("all") }
    var diffMenuOpen by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var expandedQuestionIds by remember { mutableStateOf(setOf<String>()) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    var showFormatGuide by remember { mutableStateOf(false) }
    var formatGuideTab by remember { mutableStateOf("add") }
    var copiedFormat by remember { mutableStateOf(false) }

    var showAddModal by remember { mutableStateOf(false) }
    var showImportModal by remember { mutableStateOf(false) }
    var importInitialTab by remember { mutableStateOf(ManagerTab.JSON_ADD) }
    var importInitialText by remember { mutableStateOf("") }

    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val jsonPayload = pendingExportJson
        pendingExportJson = null
        if (uri != null && jsonPayload != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonPayload.toByteArray(Charsets.UTF_8))
                }
                onSetBanner(StatusBanner(true, "Exported .json file successfully."))
            } catch (e: Exception) {
                onSetBanner(StatusBanner(false, e.message ?: "Failed to export .json file."))
            }
        }
    }

    val filteredQuestions = remember(questions, chapterFilter, difficultyFilter, searchQuery) {
        val qLower = searchQuery.trim().lowercase()
        questions.filter { q ->
            if (chapterFilter != "all" && q.chapterNumber != chapterFilter.toIntOrNull()) return@filter false
            if (difficultyFilter != "all" && q.difficulty != difficultyFilter) return@filter false
            if (qLower.isNotEmpty() &&
                !q.questionText.lowercase().contains(qLower) &&
                !q.topic.lowercase().contains(qLower) &&
                !q.id.lowercase().contains(qLower)
            ) {
                return@filter false
            }
            true
        }
    }

    val totalPages = maxOf(1, (filteredQuestions.size + PAGE_SIZE - 1) / PAGE_SIZE)
    val safePage = page.coerceIn(1, totalPages)
    val paginatedQuestions = remember(filteredQuestions, safePage) {
        val start = (safePage - 1) * PAGE_SIZE
        val end = minOf(start + PAGE_SIZE, filteredQuestions.size)
        if (start < end) filteredQuestions.subList(start, end) else emptyList()
    }

    val questionIdSet = remember(questions) { questions.map { it.id }.toSet() }
    val allPageSelected = paginatedQuestions.isNotEmpty() && paginatedQuestions.all { it.id in selectedIds }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "qb_header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${exam.code} · Structured Repository",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Question Bank (${questions.size} MCQs)",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add or delete questions, import bulk updates via .json or .doc/.txt files, or export the bank.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddModal = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("qb_add_question_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Question", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                importInitialTab = ManagerTab.JSON_ADD
                                importInitialText = ""
                                showImportModal = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("qb_import_json_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import / Doc", maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val hasFilter = chapterFilter != "all" || difficultyFilter != "all" || searchQuery.isNotBlank()
                                val rows = if (hasFilter) filteredQuestions else questions
                                val suffix = if (hasFilter) "filtered" else "full"
                                pendingExportJson = QuestionImportParser.exportQuestionsToJson(rows)
                                exportLauncher.launch("${exam.id}-question-bank-$suffix.json")
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("qb_export_json_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export .json", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { showFormatGuide = !showFormatGuide },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("qb_toggle_format_guide_button")
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showFormatGuide) "Hide Guide" else ".json Format", maxLines = 1)
                        }
                    }
                }
            }

            if (statusBanner != null) {
                item(key = "qb_status_banner") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (statusBanner.isSuccess) {
                            if (isDarkMode) NismColors.EmeraldBgDark else NismColors.EmeraldBgLight
                        } else {
                            if (isDarkMode) NismColors.RoseBgDark else NismColors.RoseBgLight
                        },
                        border = BorderStroke(
                            1.dp,
                            if (statusBanner.isSuccess) NismColors.Emerald else NismColors.Rose
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusBanner.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (statusBanner.isSuccess) {
                                    if (isDarkMode) NismColors.EmeraldLight else NismColors.Emerald
                                } else {
                                    if (isDarkMode) NismColors.RoseLight else NismColors.Rose
                                },
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissBanner) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            if (showFormatGuide) {
                item(key = "qb_format_guide") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Supported .json File Format", style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { formatGuideTab = "add"; copiedFormat = false }) {
                                        Text(
                                            "Add",
                                            fontWeight = if (formatGuideTab == "add") FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    TextButton(onClick = { formatGuideTab = "delete"; copiedFormat = false }) {
                                        Text(
                                            "Delete",
                                            fontWeight = if (formatGuideTab == "delete") FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            val sampleText = if (formatGuideTab == "add") {
                                QuestionImportParser.SAMPLE_ADD_JSON
                            } else {
                                QuestionImportParser.SAMPLE_DELETE_JSON
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = sampleText,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(sampleText))
                                        copiedFormat = true
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (copiedFormat) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (copiedFormat) "Copied" else "Copy Format")
                                }
                                OutlinedButton(
                                    onClick = {
                                        pendingExportJson = sampleText
                                        val fname = if (formatGuideTab == "add") "${exam.id}-sample-add.json" else "${exam.id}-sample-delete.json"
                                        exportLauncher.launch(fname)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Sample")
                                }
                            }
                        }
                    }
                }
            }

            item(key = "qb_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            page = 1
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("Search by topic, text, or ID...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qb_search_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = chapterMenuOpen,
                            onExpandedChange = { chapterMenuOpen = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            val chLabel = if (chapterFilter == "all") {
                                "All Chapters"
                            } else {
                                "Ch. $chapterFilter"
                            }
                            OutlinedTextField(
                                value = chLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Chapter") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterMenuOpen) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = chapterMenuOpen,
                                onDismissRequest = { chapterMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Chapters (${exam.chapters.size})") },
                                    onClick = {
                                        chapterFilter = "all"
                                        page = 1
                                        chapterMenuOpen = false
                                    }
                                )
                                exam.chapters.forEach { ch ->
                                    DropdownMenuItem(
                                        text = { Text("Ch. ${ch.chapterNumber}: ${ch.title}") },
                                        onClick = {
                                            chapterFilter = ch.chapterNumber.toString()
                                            page = 1
                                            chapterMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = diffMenuOpen,
                            onExpandedChange = { diffMenuOpen = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = if (difficultyFilter == "all") "All Levels" else difficultyFilter,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Difficulty") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = diffMenuOpen) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = diffMenuOpen,
                                onDismissRequest = { diffMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Difficulties") },
                                    onClick = {
                                        difficultyFilter = "all"
                                        page = 1
                                        diffMenuOpen = false
                                    }
                                )
                                QuestionImportParser.VALID_DIFFICULTIES.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d) },
                                        onClick = {
                                            difficultyFilter = d
                                            page = 1
                                            diffMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "qb_bulk_bar") {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = allPageSelected,
                                    enabled = paginatedQuestions.isNotEmpty(),
                                    onCheckedChange = {
                                        val pageIds = paginatedQuestions.map { it.id }.toSet()
                                        selectedIds = if (allPageSelected) {
                                            selectedIds - pageIds
                                        } else {
                                            selectedIds + pageIds
                                        }
                                    }
                                )
                                Text(
                                    text = "Page (${paginatedQuestions.size})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            if (selectedIds.isNotEmpty()) {
                                TextButton(onClick = { selectedIds = emptySet() }) {
                                    Text("Clear (${selectedIds.size})")
                                }
                            } else {
                                val startNum = if (filteredQuestions.isEmpty()) 0 else (safePage - 1) * PAGE_SIZE + 1
                                val endNum = minOf(safePage * PAGE_SIZE, filteredQuestions.size)
                                Text(
                                    text = "$startNum–$endNum of ${filteredQuestions.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (selectedIds.isNotEmpty() || questions.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (selectedIds.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = {
                                            val sel = questions.filter { it.id in selectedIds }
                                            pendingExportJson = QuestionImportParser.exportQuestionsToJson(sel)
                                            exportLauncher.launch("${exam.id}-selected-questions.json")
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Export (${selectedIds.size})", style = MaterialTheme.typography.labelMedium)
                                    }

                                    Button(
                                        onClick = { showBulkDeleteConfirm = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = NismColors.Rose),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Delete (${selectedIds.size})", style = MaterialTheme.typography.labelMedium)
                                    }
                                } else if (questions.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { showDeleteAllConfirm = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Delete All", style = MaterialTheme.typography.labelMedium, color = NismColors.Rose)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filteredQuestions.isEmpty()) {
                item(key = "qb_empty") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (questions.isEmpty()) "Question bank is empty (0 MCQs)" else "No questions match current filters",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (questions.isEmpty()) {
                                    "Add questions individually or import from a .json or .doc/.txt file."
                                } else {
                                    "Try adjusting your search query, chapter filter, or difficulty filter."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            if (questions.isEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { showAddModal = true }) { Text("Add Question") }
                                    OutlinedButton(onClick = { showImportModal = true }) { Text("Upload .json") }
                                }
                            } else {
                                OutlinedButton(onClick = {
                                    searchQuery = ""
                                    chapterFilter = "all"
                                    difficultyFilter = "all"
                                    page = 1
                                }) {
                                    Text("Clear Filters")
                                }
                            }
                        }
                    }
                }
            } else {
                items(paginatedQuestions, key = { it.id }) { q ->
                    val isSelected = q.id in selectedIds
                    val isExpanded = q.id in expandedQuestionIds
                    val isConfirming = confirmDeleteId == q.id

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        selectedIds = if (isSelected) selectedIds - q.id else selectedIds + q.id
                                    }
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(top = 4.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "Ch. ${q.chapterNumber}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        val diffColor = when (q.difficulty.lowercase()) {
                                            "easy" -> if (isDarkMode) NismColors.EmeraldLight else NismColors.Emerald
                                            "hard" -> if (isDarkMode) NismColors.RoseLight else NismColors.Rose
                                            else -> if (isDarkMode) NismColors.AmberLight else NismColors.Amber
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = diffColor.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = q.difficulty,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = diffColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = q.id,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = q.questionText,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${q.topic} · ${q.questionType}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        expandedQuestionIds = if (isExpanded) {
                                            expandedQuestionIds - q.id
                                        } else {
                                            expandedQuestionIds + q.id
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse details" else "Expand details"
                                    )
                                }
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OptionKey.entries.forEach { k ->
                                        val isCorrect = q.correctAnswer == k
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isCorrect) {
                                                if (isDarkMode) NismColors.EmeraldBgDark else NismColors.EmeraldBgLight
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                if (isCorrect) NismColors.Emerald else MaterialTheme.colorScheme.outline
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "${k.name}. ${q.options[k].orEmpty()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isCorrect) FontWeight.SemiBold else FontWeight.Normal,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Explanation: ${q.explanation}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isConfirming) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                onDeleteQuestions(listOf(q.id))
                                                selectedIds = selectedIds - q.id
                                                confirmDeleteId = null
                                                onSetBanner(StatusBanner(true, "Deleted question ${q.id}."))
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NismColors.Rose),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Confirm", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    TextButton(onClick = { confirmDeleteId = null }) { Text("Cancel") }
                                } else {
                                    TextButton(onClick = { confirmDeleteId = q.id }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (totalPages > 1) {
                item(key = "qb_pagination") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            enabled = safePage > 1,
                            onClick = { page = maxOf(1, safePage - 1) }
                        ) { Text("Previous") }
                        Text(
                            text = "Page $safePage / $totalPages",
                            style = MaterialTheme.typography.labelMedium.merge(TabularMonoStyle)
                        )
                        OutlinedButton(
                            enabled = safePage < totalPages,
                            onClick = { page = minOf(totalPages, safePage + 1) }
                        ) { Text("Next") }
                    }
                }
            }
        }
    }

    if (showAddModal) {
        AddQuestionDialog(
            exam = exam,
            initialChapter = chapterFilter.toIntOrNull() ?: 1,
            onDismiss = { showAddModal = false },
            onSave = { newQ ->
                onImportQuestions(listOf(newQ))
                onSetBanner(StatusBanner(true, "Added question (${newQ.id}) to Chapter ${newQ.chapterNumber}."))
            }
        )
    }

    if (showImportModal) {
        ImportExportQuestionsDialog(
            exam = exam,
            initialTab = importInitialTab,
            initialText = importInitialText,
            existingQuestionIds = questionIdSet,
            onDismiss = { showImportModal = false },
            onImportMerge = onImportQuestions,
            onReplaceBank = { next ->
                onReplaceQuestions(next)
                selectedIds = emptySet()
            },
            onDeleteIds = { ids ->
                onDeleteQuestions(ids)
                selectedIds = selectedIds - ids.toSet()
            },
            onStatusBanner = onSetBanner
        )
    }

    if (showBulkDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Delete ${selectedIds.size} Selected Question(s)?",
            body = "This removes the selected questions from local storage.",
            confirmLabel = "Delete Selected",
            onDismiss = { showBulkDeleteConfirm = false },
            onConfirm = {
                val ids = selectedIds.toList()
                showBulkDeleteConfirm = false
                scope.launch {
                    onDeleteQuestions(ids)
                    selectedIds = emptySet()
                    onSetBanner(StatusBanner(true, "Deleted ${ids.size} selected question(s)."))
                }
            }
        )
    }

    if (showDeleteAllConfirm) {
        ConfirmDeleteDialog(
            title = "Delete All Questions?",
            body = "This removes all ${questions.size} question(s) from the bank. You can re-import anytime via .json.",
            confirmLabel = "Delete All",
            onDismiss = { showDeleteAllConfirm = false },
            onConfirm = {
                showDeleteAllConfirm = false
                scope.launch {
                    onDeleteAllQuestions()
                    selectedIds = emptySet()
                    onSetBanner(StatusBanner(true, "Deleted all questions from the bank."))
                }
            }
        )
    }
}
