package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.ActiveTestSession
import com.example.domain.model.TestMode
import com.example.ui.theme.NismColors
import com.example.ui.theme.TabularMonoStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NismTopBar(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onStartFullMock: () -> Unit,
    onBrandClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            title = {
                Surface(
                    onClick = onBrandClick,
                    color = Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("top_bar_brand")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "NISM Series V-A Prep",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(7.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NISM V-A Prep",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Mutual Fund Certification",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            actions = {
                Button(
                    onClick = onStartFullMock,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .testTag("top_bar_start_mock_button"),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mock",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkMode) "Switch to light mode" else "Switch to dark mode"
                    )
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (icon != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = iconTint.copy(alpha = 0.12f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.merge(TabularMonoStyle),
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PassFailChip(passed: Boolean?, isDarkMode: Boolean) {
    when (passed) {
        true -> {
            val tint = if (isDarkMode) NismColors.EmeraldLight else NismColors.Emerald
            val bg = if (isDarkMode) NismColors.EmeraldBgDark else NismColors.EmeraldBgLight
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = bg,
                border = BorderStroke(1.dp, tint.copy(alpha = 0.35f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Pass",
                        tint = tint,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pass",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = tint
                    )
                }
            }
        }
        false -> {
            val tint = if (isDarkMode) NismColors.RoseLight else NismColors.Rose
            val bg = if (isDarkMode) NismColors.RoseBgDark else NismColors.RoseBgLight
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = bg,
                border = BorderStroke(1.dp, tint.copy(alpha = 0.35f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Fail",
                        tint = tint,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Fail",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = tint
                    )
                }
            }
        }
        null -> {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = "Drill",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ReplaceSessionDialog(
    activeSession: ActiveTestSession,
    onCancel: () -> Unit,
    onResume: () -> Unit,
    onDiscardAndStart: () -> Unit
) {
    val label = if (activeSession.mode == TestMode.FULL_MOCK) {
        "Full Mock Test"
    } else {
        "Chapter ${activeSession.chapterNumber} Practice"
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Unfinished Test in Progress",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "You have an interrupted $label saved. Starting a new test will discard that unfinished session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onResume,
                    modifier = Modifier.testTag("dialog_resume_saved_button")
                ) {
                    Text("Resume")
                }
                Button(
                    onClick = onDiscardAndStart,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("dialog_discard_and_start_button")
                ) {
                    Text("Discard & Start")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag("dialog_cancel_replace_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
