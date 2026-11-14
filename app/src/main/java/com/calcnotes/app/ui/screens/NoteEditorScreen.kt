package com.calcnotes.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calcnotes.app.calc.LineKind
import com.calcnotes.app.calc.LineResult
import com.calcnotes.app.calc.NoteEvaluationSummary
import com.calcnotes.app.data.NoteEntity
import com.calcnotes.app.data.UserSettings
import com.calcnotes.app.ui.theme.GlassCard
import com.calcnotes.app.ui.theme.GlassChip
import com.calcnotes.app.ui.theme.LocalGlassColors
import com.calcnotes.app.ui.theme.NoteTagColors

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun NoteEditorScreen(
    note: NoteEntity,
    title: String,
    content: String,
    summary: NoteEvaluationSummary?,
    recentResults: List<String>,
    settings: UserSettings,
    canUndo: Boolean,
    canRedo: Boolean,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onToggleChecklistLine: (Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onUpdateMeta: (folder: String?, tagsCsv: String?, colorTag: Int?, isPinned: Boolean?, isArchived: Boolean?, isLocked: Boolean?) -> Unit,
    onMoveToTrash: () -> Unit,
    onShareText: (String) -> Unit,
    onExportFile: (extension: String) -> Unit,
    onBack: () -> Unit
) {
    val glass = LocalGlassColors.current
    val context = LocalContext.current
    val view = LocalView.current

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = content, selection = TextRange(content.length)))
    }
    if (textFieldValue.text != content) {
        textFieldValue = textFieldValue.copy(
            text = content,
            selection = TextRange(textFieldValue.selection.start.coerceAtMost(content.length))
        )
    }

    var showChartSheet by remember { mutableStateOf(false) }
    var showSplitSheet by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showRecentTray by remember { mutableStateOf(false) }

    LaunchedEffect(summary?.formattedGrandTotal) {
        if (settings.hapticsEnabled && summary != null && summary.activeLineCount > 0) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    val syntaxVisualTransformation = remember(glass.accent, glass.accentSecondary, glass.textDimmed, summary?.variables) {
        SmartCalcSyntaxTransformation(
            numberColor = glass.accent,
            variableColor = glass.accentSecondary,
            headingColor = Color(0xFFFBBF24),
            dimmedWordColor = glass.textDimmed,
            knownVariables = summary?.variables?.keys ?: emptySet()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = glass.textPrimary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onUndo, enabled = canUndo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) glass.textPrimary else glass.textDimmed.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = onRedo, enabled = canRedo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) glass.textPrimary else glass.textDimmed.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = { onUpdateMeta(null, null, null, !note.isPinned, null, null) }) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (note.isPinned) glass.accent else glass.textDimmed
                        )
                    }
                    IconButton(onClick = { onUpdateMeta(null, null, null, null, null, !note.isLocked) }) {
                        Icon(
                            if (note.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Toggle Biometric Lock",
                            tint = if (note.isLocked) glass.accent else glass.textDimmed
                        )
                    }
                    IconButton(onClick = { showSplitSheet = true }) {
                        Icon(Icons.Default.CallSplit, contentDescription = "Split Bill Mode", tint = glass.accent)
                    }
                    Box {
                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Share or Export", tint = glass.textPrimary)
                        }
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share Note with Results") },
                                onClick = {
                                    showShareMenu = false
                                    val formattedShare = buildShareText(title, summary)
                                    onShareText(formattedShare)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as .txt") },
                                onClick = { showShareMenu = false; onExportFile("txt") }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as .md (Markdown)") },
                                onClick = { showShareMenu = false; onExportFile("md") }
                            )
                            DropdownMenuItem(
                                text = { Text("Archive Note") },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                onClick = {
                                    showShareMenu = false
                                    onUpdateMeta(null, null, null, null, !note.isArchived, null)
                                    onBack()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Trash") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    showShareMenu = false
                                    onMoveToTrash()
                                }
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoteTagColors.forEachIndexed { idx, color ->
                Box(
                    modifier = Modifier
                        .size(if (note.colorTag == idx) 22.dp else 16.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (note.colorTag == idx) 2.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .clickable { onUpdateMeta(null, null, idx, null, null, null) }
                )
            }

            listOf("Personal", "Finance", "Work", "Shopping").forEach { folderOption ->
                GlassChip(
                    selected = note.folder.equals(folderOption, ignoreCase = true),
                    onClick = { onUpdateMeta(folderOption, null, null, null, null, null) }
                ) {
                    Text(folderOption, fontSize = 12.sp, color = glass.textPrimary)
                }
            }

            summary?.variables?.forEach { (varName, varVal) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(glass.accentSecondary.copy(alpha = 0.22f))
                        .border(1.dp, glass.accentSecondary, RoundedCornerShape(50))
                        .clickable {
                            insertAtCursor(textFieldValue, varName) { updated ->
                                textFieldValue = updated
                                onContentChange(updated.text)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$varName = $varVal",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = glass.accentSecondary
                    )
                }
            }
        }

        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            textStyle = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = glass.textPrimary
            ),
            cursorBrush = SolidColor(glass.accent),
            decorationBox = { inner ->
                Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                    if (title.isEmpty()) {
                        Text("Note Title...", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = glass.textDimmed)
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        val scrollState = rememberScrollState()
        val lineResults = summary?.lineResults ?: emptyList()
        val lineHeightSp = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                if (settings.showLineNumbers) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        val count = maxOf(lineResults.size, 1)
                        for (i in 0 until count) {
                            val raw = lineResults.getOrNull(i)?.rawLine ?: ""
                            val isChecklist = raw.trimStart().startsWith("- [ ]") ||
                                raw.trimStart().startsWith("- [x]", ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .height((settings.fontSizeSp * settings.lineSpacingMultiplier + 4f).dp)
                                    .clickable(enabled = isChecklist) { onToggleChecklistLine(i) },
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = if (isChecklist) "☑" else "${i + 1}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (settings.fontSizeSp - 3f).sp,
                                    color = if (isChecklist) glass.accent else glass.textDimmed.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(0.64f)) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newVal ->
                            textFieldValue = newVal
                            onContentChange(newVal.text)
                        },
                        visualTransformation = syntaxVisualTransformation,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = settings.fontSizeSp.sp,
                            lineHeight = lineHeightSp,
                            color = glass.textPrimary
                        ),
                        cursorBrush = SolidColor(glass.accent),
                        decorationBox = { inner ->
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Type notes & math naturally...\ne.g.\n56 deepak\n91 Rajan\nrent 12000 and food 4500",
                                    fontSize = settings.fontSizeSp.sp,
                                    lineHeight = lineHeightSp,
                                    color = glass.textDimmed.copy(alpha = 0.65f)
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height((maxOf(lineResults.size, 12) * (settings.fontSizeSp * settings.lineSpacingMultiplier + 4f)).dp)
                        .background(glass.borderLight)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(0.36f),
                    horizontalAlignment = Alignment.End
                ) {
                    lineResults.forEach { res ->
                        ResultRowCell(
                            result = res,
                            fontSizeSp = settings.fontSizeSp,
                            lineSpacingMultiplier = settings.lineSpacingMultiplier,
                            onTapCopy = { textToCopy ->
                                copyToClipboard(context, textToCopy)
                                if (settings.hapticsEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                }
                                Toast.makeText(context, "Copied $textToCopy", Toast.LENGTH_SHORT).show()
                            },
                            onLongPressInsert = { textToInsert ->
                                insertAtCursor(textFieldValue, textToInsert) { updated ->
                                    textFieldValue = up
