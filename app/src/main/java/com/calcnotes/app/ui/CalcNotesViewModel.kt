package com.calcnotes.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calcnotes.app.calc.CalcEvaluator
import com.calcnotes.app.calc.CalcFormatConfig
import com.calcnotes.app.calc.NoteEvaluationSummary
import com.calcnotes.app.data.BackupManager
import com.calcnotes.app.data.CalcNotesDatabase
import com.calcnotes.app.data.NoteEntity
import com.calcnotes.app.data.NoteRepository
import com.calcnotes.app.data.NoteSortOrder
import com.calcnotes.app.data.SettingsPreferences
import com.calcnotes.app.data.UserSettings
import com.calcnotes.app.widget.CalcNotesWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NavFilter {
    ALL,
    ARCHIVED,
    TRASH
}

class CalcNotesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CalcNotesDatabase.getInstance(application)
    private val repository = NoteRepository(database.noteDao())
    val settingsPrefs = SettingsPreferences(application)
    private val evaluator = CalcEvaluator()

    val settings: StateFlow<UserSettings> = settingsPrefs.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolder = MutableStateFlow("All")
    val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

    private val _navFilter = MutableStateFlow(NavFilter.ALL)
    val navFilter: StateFlow<NavFilter> = _navFilter.asStateFlow()

    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedNoteIds: StateFlow<Set<Long>> = _selectedNoteIds.asStateFlow()

    private val _activeNote = MutableStateFlow<NoteEntity?>(null)
    val activeNote: StateFlow<NoteEntity?> = _activeNote.asStateFlow()

    private val _editorTitle = MutableStateFlow("")
    val editorTitle: StateFlow<String> = _editorTitle.asStateFlow()

    private val _editorContent = MutableStateFlow("")
    val editorContent: StateFlow<String> = _editorContent.asStateFlow()

    private val _evaluation = MutableStateFlow<NoteEvaluationSummary?>(null)
    val evaluation: StateFlow<NoteEvaluationSummary?> = _evaluation.asStateFlow()

    private val _recentResults = MutableStateFlow<List<String>>(emptyList())
    val recentResults: StateFlow<List<String>> = _recentResults.asStateFlow()

    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()
    private var evalJob: Job? = null

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val availableFolders: StateFlow<List<String>> = allNotes.combine(settings) { notes, _ ->
        val custom = notes.filter { !it.isTrashed }.map { it.folder.ifBlank { "All" } }.distinct()
        (listOf("All", "Personal", "Finance", "Work", "Shopping") + custom).distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All", "Personal", "Finance", "Work", "Shopping"))

    val filteredNotes: StateFlow<List<NoteEntity>> = combine(
        allNotes,
        _searchQuery,
        _selectedFolder,
        _navFilter,
        settings
    ) { notes, query, folder, nav, cfg ->
        val base = notes.filter { note ->
            when (nav) {
                NavFilter.ALL -> !note.isTrashed && !note.isArchived
                NavFilter.ARCHIVED -> !note.isTrashed && note.isArchived
                NavFilter.TRASH -> note.isTrashed
            }
        }.filter { note ->
            if (nav != NavFilter.ALL || folder == "All") true
            else note.folder.equals(folder, ignoreCase = true) ||
                note.tagsCsv.split(',').any { it.trim().equals(folder, ignoreCase = true) }
        }.filter { note ->
            if (query.isBlank()) true
            else note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true) ||
                note.tagsCsv.contains(query, ignoreCase = true)
        }

        val sorted = when (cfg.sortOrder) {
            NoteSortOrder.DATE_EDITED -> base.sortedByDescending { it.updatedAt }
            NoteSortOrder.DATE_CREATED -> base.sortedByDescending { it.createdAt }
            NoteSortOrder.TITLE -> base.sortedBy { it.title.lowercase() }
            NoteSortOrder.MANUAL -> base.sortedBy { it.manualSortOrder }
        }
        sorted.sortedByDescending { it.isPinned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.purgeOldTrash(30)
            val currentSettings = settingsPrefs.settingsFlow.first()
            if (!currentSettings.hasSeededSampleNote && repository.getAllNotesOnce().isEmpty()) {
                seedWelcomeNote(currentSettings)
                settingsPrefs.markSampleSeeded()
            }
        }
        viewModelScope.launch {
            settings.collect { cfg ->
                if (_activeNote.value != null) {
                    recalculateImmediate(_editorContent.value, cfg)
                }
            }
        }
    }

    private suspend fun seedWelcomeNote(cfg: UserSettings) {
        val sampleContent = """
            # Monthly Shared Expenses
            56 deepak
            91 Rajan
            rent 12000 and food 4500
            paid 250 to Sam
            sum

            # Quick Shopping & Math
            price = 250
            price * 3
            12 x 3 apples
            20% of 500
            500 + 10%
            - [ ] 5 km in miles
            - [x] 2h 30m + 45m
        """.trimIndent()

        val summary = evaluator.evaluateNote(sampleContent, cfg.toFormatConfig())
        repository.saveNote(
            NoteEntity(
                title = "Welcome to CalcNotes",
                content = sampleContent,
                folder = "Finance",
                tagsCsv = "budget, demo",
                colorTag = 0,
                isPinned = true,
                cachedTotal = summary.formattedGrandTotal
            )
        )
        CalcNotesWidgetUpdater.updateWidget(getApplication())
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun setSelectedFolder(folder: String) {
        _selectedFolder.value = folder
    }

    fun setNavFilter(filter: NavFilter) {
        _navFilter.value = filter
        _selectedNoteIds.value = emptySet()
    }

    fun toggleMultiSelect(noteId: Long) {
        val current = _selectedNoteIds.value.toMutableSet()
        if (!current.add(noteId)) current.remove(noteId)
        _selectedNoteIds.value = current
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun bulkPinSelected() {
        viewModelScope.launch {
            val ids = _selectedNoteIds.value
            val notes = allNotes.value.filter { it.id in ids }
            for (n in notes) {
                repository.saveNote(n.copy(isPinned = !n.isPinned))
            }
            _selectedNoteIds.value = emptySet()
            CalcNotesWidgetUpdater.updateWidget(getApplication())
        }
    }

    fun bulkDeleteSelected() {
        viewModelScope.launch {
            val ids = _selectedNoteIds.value
            val notes = allNotes.value.filter { it.id in ids }
            for (n in notes) {
                repository.moveToTrash(n)
            }
            _selectedNoteIds.value = emptySet()
            CalcNotesWidgetUpdater.updateWidget(getApplication())
        }
    }

    fun createNewNote(initialFolder: String = "All") {
        viewModelScope.launch {
            val folder = if (initialFolder == "All") "Personal" else initialFolder
            val newNote = NoteEntity(
                title = "",
                content = "",
                folder = folder,
                cachedTotal = "0"
            )
            val id = repository.saveNote(newNote)
            val saved = repository.getNoteById(id) ?: newNote.copy(id = id)
            openNote(saved)
        }
    }

    fun openNote(note: NoteEntity) {
        undoStack.clear()
        redoStack.clear()
        _activeNote.value = note
        _editorTitle.value = note.title
        _editorContent.value = note.content
        recalculateImmediate(note.content, settings.value)
    }

    fun closeEditor() {
        _activeNote.value = null
    }

    fun onTitleChanged(newTitle: String) {
        _editorTitle.value = newTitle
        val current = _activeNote.value ?: return
        val updated = current.copy(title = newTitle)
        _activeNote.value = updated
        viewModelScope.launch {
            repository.saveNote(updated)
            CalcNotesWidgetUpdater.updateWidget(getApplication())
        }
    }

    fun onContentChanged(newContent: String, recordUndo: Boolean = true) {
        val previous = _editorContent.value
        if (recordUndo && newContent != previous) {
            if (undoStack.size >= 60) undoStack.removeFirst()
            undoStack.addLast(previous)
            redoStack.clear()
        }
        _editorContent.value = newContent

        evalJob?.cancel()
        evalJob = viewModelScope.launch {
            if (newContent.length > 600) delay(16)
            val cfg = settings.value
            val summary = withContext(Dispatchers.Default) {
                evaluator.evaluateNote(newContent, cfg.toFormatConfig())
            }
            _evaluation.value = summary
            recordRecentResult(summary.formattedGrandTotal)

            val current = _activeNote.value
            if (current != null) {
                val updated = current.copy(
                    title = _editorTitle.value,
                    content = newContent,
                    cachedTotal = summary.formattedGrandTotal
                )
                _activeNote.value = updated
                repository.saveNote(updated)
                CalcNotesWidgetUpdater.updateWidget(getApplication())
            }
        }
    }

    private fun recalculateImmediate(content: String, cfg: UserSettings) {
        viewModelScope.launch {
            val summary = withContext(Dispatchers.Default) {
                evaluator.evaluateNote(content, cfg.toFormatConfig())
            }
            _evaluation.value = summary
        }
    }

    private fun recordRecentResult(formatted: String) {
        if (formatted.isBlank() || formatted == "0") return
        val current = _recentResults.value.toMutableList()
        if (current.firstOrNull() != formatted) {
            current.remove(formatted)
            current.add(0, formatted)
            _recentResults.value = current.take(10)
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _editorContent.value
        redoStack.addLast(current)
        val prev = undoStack.removeLast()
        onContentChanged(prev, recordUndo = false)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _editorContent.value
        undoStack.addLast(current)
        val next = redoStack.removeLast()
        onContentChanged(next, recordUndo = false)
    }

    fun toggleChecklistAtLine(lineIndex: Int) {
        val lines = _editorContent.value.split('\n').toMutableList()
        if (lineIndex !in lines.indices) return
        val line = lines[lineIndex]
        lines[lineIndex] = when {
            line.contains("- [ ]") -> line.replaceFirst("- [ ]", "- [x]")
            line.contains("- [x]", ignoreCase = true) -> line.replaceFirst(Regex("- \\[[xX]\\]"), "- [ ]")
            else -> "- [ ] $line"
        }
        onContentChanged(lines.joinToString("\n"))
    }

    fun updateActiveNoteMeta(
        folder: String? = null,
        tagsCsv: String? = null,
        colorTag: Int? = null,
        isPinned: Boolean? = null,
        isArchived: Boolean? = null,
        isLocked: Boolean? = null
    ) {
        val current = _activeNote.value ?: return
        val updated = current.copy(
            folder = folder ?: current.folder,
            tagsCsv = tagsCsv ?: current.tagsCsv,
            colorTag = colorTag ?: current.colorTag,
            isPinned = isPinned ?: current.isPinned,
            isArchived = isArchived ?: current.isArchived,
            isLocked = isLocked ?: current.isLocked
        )
        _activeNote.value = updated
        viewModelScope.launch {
            repository.saveNote(updated)
            CalcNotesWidgetUpdater.updateWidget(getApplication())
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.saveNote(note.copy(isPinned = !note.isPinned))
            CalcNotesWidgetUpdater.updateWidget(getApplication())
        }
    }

    fun toggleArchive(note: NoteEntity) {
        viewModelScope.launch {
            repository.saveNote(note.copy(isArchived = !note.isArchived, isPinned = false))
            CalcNotesWidgetUpdater.updateWidget(getApplication())
        }
    }

    fun duplicateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.duplicateNote(note)
        }
    }

    fun moveToTrash(note: NoteEntity) {
        viewModelScope.launch {
            repository.moveToTrash(note)
            if (_activeNote.value?.id == note.id) {
                _activeNote.value = null
            }
            CalcNotesWidgetUpdater.updateWidget(getApplication())
        }
    }

    fun restoreFromTrash(note: NoteEntity) {
        viewModelScope.launch {
            repository.restoreFromTrash(note)
            CalcNotesWidgetUpdater.updateWidget(getApplication())
        }
    }

    fun deletePermanently(note: NoteEntity) {
        viewModelScope.launch {
            repository.deletePermanently(note.id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    fun importTxtNote(title: String, content: String) {
        viewModelScope.launch {
            val summary = evaluator.evaluateNote(content, settings.value.toFormatConfig())
            repository.saveNote(
                NoteEntity(
                    title = title.removeSuffix(".txt").removeSuffix(".md"),
                    content = content,
                    folder = "Personal",
                    cachedTotal = summary.formattedGrandTotal
                )
            )
            CalcNotesWidgetUpdater.updateWidget(getApplication())
        }
    }

    suspend fun exportBackupJson(): String {
        val all = repository.getAllNotesOnce()
        return BackupManager.exportNotesToJson(all)
    }

    fun restoreBackupJson(json: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val restored = runCatching { BackupManager.importNotesFromJson(json) }.getOrDefault(emptyList())
            if (restored.isNotEmpty()) {
                repository.importNotes(restored)
                CalcNotesWidgetUpdater.updateWidget(getApplication())
            }
            onComplete(restored.size)
        }
    }

    private fun UserSettings.toFormatConfig(): CalcFormatConfig {
        return CalcFormatConfig(
            decimalPlaces = decimalPlaces,
            separatorStyle = separatorStyle,
            currencySymbol = currencySymbol,
            multiNumberMode = multiNumberMode
        )
    }
}
