package com.calcnotes.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.calcnotes.app.ui.CalcNotesViewModel
import com.calcnotes.app.ui.screens.NoteEditorScreen
import com.calcnotes.app.ui.screens.NotesListScreen
import com.calcnotes.app.ui.screens.SettingsScreen
import com.calcnotes.app.ui.theme.AnimatedMeshBackground
import com.calcnotes.app.ui.theme.CalcNotesTheme
import com.calcnotes.app.ui.theme.LocalGlassColors
import com.calcnotes.app.util.BiometricHelper
import kotlinx.coroutines.launch
import java.io.File

private enum class AppDestination {
    LIST,
    EDITOR,
    SETTINGS
}

class MainActivity : AppCompatActivity() {

    private val viewModel: CalcNotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val notes by viewModel.filteredNotes.collectAsState()
            val folders by viewModel.availableFolders.collectAsState()
            val selectedFolder by viewModel.selectedFolder.collectAsState()
            val navFilter by viewModel.navFilter.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()
            val activeNote by viewModel.activeNote.collectAsState()
            val editorTitle by viewModel.editorTitle.collectAsState()
            val editorContent by viewModel.editorContent.collectAsState()
            val evaluation by viewModel.evaluation.collectAsState()
            val recentResults by viewModel.recentResults.collectAsState()
            val scope = rememberCoroutineScope()

            var destination by rememberSaveable { mutableStateOf(AppDestination.LIST) }
            var isAppUnlocked by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(settings.appLockEnabled) {
                if (settings.appLockEnabled && !isAppUnlocked) {
                    BiometricHelper.authenticate(
                        activity = this@MainActivity,
                        title = "Unlock CalcNotes",
                        onSuccess = { isAppUnlocked = true },
                        onError = { msg ->
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            val importTxtLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    val fileName = queryFileName(uri) ?: "Imported Note"
                    val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    viewModel.importTxtNote(fileName, content)
                    Toast.makeText(this, "Imported $fileName", Toast.LENGTH_SHORT).show()
                }
            }

            val exportJsonLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri: Uri? ->
                if (uri != null) {
                    scope.launch {
                        val json = viewModel.exportBackupJson()
                        contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                        Toast.makeText(this@MainActivity, "Backup saved successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            val restoreJsonLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    viewModel.restoreBackupJson(json) { count ->
                        Toast.makeText(this, "Restored $count notes", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            CalcNotesTheme(
                themeMode = settings.themeMode,
                accentIndex = settings.accentColorIndex
            ) {
                val glass = LocalGlassColors.current
                AnimatedMeshBackground(
                    blurRadius = settings.blurIntensityDp.dp,
                    animateEnabled = settings.animatedBackground
                ) {
                    if (settings.appLockEnabled && !isAppUnlocked) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CalcNotes is Locked",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = glass.textPrimary
                            )
                            Button(
                                onClick = {
                                    BiometricHelper.authenticate(
                                        activity = this@MainActivity,
                                        onSuccess = { isAppUnlocked = true },
                                        onError = {}
                                    )
                                },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Unlock with Biometrics / PIN")
                            }
                        }
                    } else {
                        BackHandler(enabled = destination != AppDestination.LIST) {
                            viewModel.closeEditor()
                            destination = AppDestination.LIST
                        }

                        AnimatedContent(
                            targetState = destination,
                            transitionSpec = {
                                (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                                    (slideOutHorizontally { -it / 4 } + fadeOut())
                            },
                            label = "ScreenTransition"
                        ) { screen ->
                            when (screen) {
                                AppDestination.LIST -> {
                                    NotesListScreen(
                                        notes = notes,
                                        folders = folders,
                                        selectedFolder = selectedFolder,
                                        navFilter = navFilter,
                                        searchQuery = searchQuery,
                                        selectedNoteIds = selectedNoteIds,
                                        settings = settings,
                                        onSearchChange = viewModel::setSearchQuery,
                                        onSelectFolder = viewModel::setSelectedFolder,
                                        onSelectNavFilter = viewModel::setNavFilter,
                                        onToggleGrid = {
                                            scope.launch {
                                                viewModel.settingsPrefs.updateGridView(!settings.isGridView)
                                            }
                                        },
                                        onChangeSort = { order ->
                                            scope.launch { viewModel.settingsPrefs.updateSortOrder(order) }
                                        },
                                        onOpenNote = { note ->
                                            if (note.isLocked) {
                                                BiometricHelper.authenticate(
                                                    activity = this@MainActivity,
                                                    title = "Unlock Note",
                                                    onSuccess = {
                                                        viewModel.openNote(note)
                                                        destination = AppDestination.EDITOR
                                                    },
                                                    onError = { msg ->
                                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            } else {
                                                viewModel.openNote(note)
                                                destination = AppDestination.EDITOR
                                            }
                                        },
                                        onCreateNote = {
                                            viewModel.createNewNote(selectedFolder)
                                            destination = AppDestination.EDITOR
                                        },
                                        onTogglePin = viewModel::togglePin,
                                        onDuplicateNote = viewModel::duplicateNote,
                                        onMoveToTrash = viewModel::moveToTrash,
                                        onRestoreNote = viewModel::restoreFromTrash,
                                        onDeletePermanently = viewModel::deletePermanently,
                                        onEmptyTrash = viewModel::emptyTrash,
                                        onToggleMultiSelect = viewModel::toggleMultiSelect,
                                        onClearSelection = viewModel::clearSelection,
                                        onBulkPin = viewModel::bulkPinSelected,
                                        onBulkDelete = viewModel::bulkDeleteSelected,
                                        onImportTxtClick = { importTxtLauncher.launch(arrayOf("text/*")) },
                                        onOpenSettings = { destination = AppDestination.SETTINGS }
                                    )
                                }

                                AppDestination.EDITOR -> {
                                    val currentNote = activeNote
                                    if (currentNote != null) {
                                        NoteEditorScreen(
                                            note = currentNote,
                                            title = editorTitle,
                                            content = editorContent,
                                            summary = evaluation,
                                            recentResults = recentResults,
                                            settings = settings,
                                            canUndo = viewModel.canUndo(),
                                            canRedo = viewModel.canRedo(),
                                            onTitleChange = viewModel::onTitleChanged,
                                            onContentChange = { viewModel.onContentChanged(it) },
                                            onToggleChecklistLine = viewModel::toggleChecklistAtLine,
                                            onUndo = viewModel::undo,
                                            onRedo = viewModel::redo,
                                            onUpdateMeta = viewModel::updateActiveNoteMeta,
                                            onMoveToTrash = {
                                                viewModel.moveToTrash(currentNote)
                                                destination = AppDestination.LIST
                                            },
                                            onShareText = { text -> sharePlainText(text) },
                                            onExportFile = { ext ->
                                                exportNoteAsFile(
                                                    title = editorTitle.ifBlank { "calc_note" },
                                                    content = editorContent,
                                                    extension = ext
                                                )
                                            },
                                            onBack = {
                                                viewModel.closeEditor()
                                                destination = AppDestination.LIST
                                            }
                                        )
                                    }
                                }

                                AppDestination.SETTINGS -> {
                                    SettingsScreen(
                                        settings = settings,
                                        prefs = viewModel.settingsPrefs,
                                        scope = scope,
                                        onExportBackupJson = {
                                            exportJsonLauncher.launch("calcnotes_backup.json")
                                        },
                                        onImportBackupJson = {
                                            restoreJsonLauncher.launch(arrayOf("application/json", "text/*"))
                                        },
                                        onEmptyTrash = {
                                            viewModel.emptyTrash()
                                            Toast.makeText(this@MainActivity, "Trash emptied", Toast.LENGTH_SHORT).show()
                                        },
                                        onBack = { destination = AppDestination.LIST }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sharePlainText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share CalcNote"))
    }

    private fun exportNoteAsFile(title: String, content: String, extension: String) {
        runCatching {
            val exportDir = File(cacheDir, "exports").apply { mkdirs() }
            val safeName = title.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_").ifBlank { "note" }
            val file = File(exportDir, "$safeName.$extension")
            file.writeText(content)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (extension == "md") "text/markdown" else "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(sendIntent, "Export .$extension file"))
        }
    }

    private fun queryFileName(uri: Uri): String? {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }
}
