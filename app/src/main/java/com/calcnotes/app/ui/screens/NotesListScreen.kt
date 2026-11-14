package com.calcnotes.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calcnotes.app.data.NoteEntity
import com.calcnotes.app.data.NoteSortOrder
import com.calcnotes.app.data.UserSettings
import com.calcnotes.app.ui.NavFilter
import com.calcnotes.app.ui.theme.GlassCard
import com.calcnotes.app.ui.theme.GlassChip
import com.calcnotes.app.ui.theme.LocalGlassColors
import com.calcnotes.app.ui.theme.NoteTagColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    notes: List<NoteEntity>,
    folders: List<String>,
    selectedFolder: String,
    navFilter: NavFilter,
    searchQuery: String,
    selectedNoteIds: Set<Long>,
    settings: UserSettings,
    onSearchChange: (String) -> Unit,
    onSelectFolder: (String) -> Unit,
    onSelectNavFilter: (NavFilter) -> Unit,
    onToggleGrid: () -> Unit,
    onChangeSort: (NoteSortOrder) -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onCreateNote: () -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onDuplicateNote: (NoteEntity) -> Unit,
    onMoveToTrash: (NoteEntity) -> Unit,
    onRestoreNote: (NoteEntity) -> Unit,
    onDeletePermanently: (NoteEntity) -> Unit,
    onEmptyTrash: () -> Unit,
    onToggleMultiSelect: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onBulkPin: () -> Unit,
    onBulkDelete: () -> Unit,
    onImportTxtClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val glass = LocalGlassColors.current
    var showSortMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            if (selectedNoteIds.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onClearSelection) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear selection", tint = glass.textPrimary)
                            }
                            Text(
                                text = "${selectedNoteIds.size} selected",
                                fontWeight = FontWeight.Bold,
                                color = glass.textPrimary
                            )
                        }
                        Row {
                            IconButton(onClick = onBulkPin) {
                                Icon(Icons.Default.PushPin, contentDescription = "Pin selected", tint = glass.accent)
                            }
                            IconButton(onClick = onBulkDelete) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = Color(0xFFFB7185))
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CalcNotes",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = glass.textPrimary
                        )
                        Text(
                            text = "Live Calculator & Smart Notes",
                            fontSize = 13.sp,
                            color = glass.textDimmed
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onImportTxtClick) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Import .txt file", tint = glass.textPrimary)
                        }
                        IconButton(onClick = onToggleGrid) {
                            Icon(
                                imageVector = if (settings.isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                contentDescription = "Toggle Grid/List View",
                                tint = glass.textPrimary
                            )
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort Notes", tint = glass.textPrimary)
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sort by Date Edited") },
                                    onClick = { onChangeSort(NoteSortOrder.DATE_EDITED); showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Date Created") },
                                    onClick = { onChangeSort(NoteSortOrder.DATE_CREATED); showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Title (A–Z)") },
                                    onClick = { onChangeSort(NoteSortOrder.TITLE); showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Manual Order") },
                                    onClick = { onChangeSort(NoteSortOrder.MANUAL); showSortMenu = false }
                                )
                            }
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = glass.textPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = 4.dp
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search notes, numbers, or #tags...", color = glass.textDimmed) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = glass.accent) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = glass.textDimmed)
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = glass.textPrimary,
                        unfocusedTextColor = glass.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassChip(
                    selected = navFilter == NavFilter.ALL && selectedFolder == "All",
                    onClick = {
                        onSelectNavFilter(NavFilter.ALL)
                        onSelectFolder("All")
                    }
                ) {
                    Text("All Notes", color = glass.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                folders.filter { it != "All" }.forEach { folderName ->
                    GlassChip(
                        selected = navFilter == NavFilter.ALL && selectedFolder.equals(folderName, ignoreCase = true),
                        onClick = {
                            onSelectNavFilter(NavFilter.ALL)
                            onSelectFolder(folderName)
                        }
                    ) {
                        Text(folderName, color = glass.textPrimary, fontSize = 13.sp)
                    }
                }

                GlassChip(
                    selected = navFilter == NavFilter.ARCHIVED,
                    onClick = { onSelectNavFilter(NavFilter.ARCHIVED) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp), tint = glass.textPrimary)
                        Text("Archived", color = glass.textPrimary, fontSize = 13.sp)
                    }
                }

                GlassChip(
                    selected = navFilter == NavFilter.TRASH,
                    onClick = { onSelectNavFilter(NavFilter.TRASH) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = glass.textPrimary)
                        Text("Trash (30d)", color = glass.textPrimary, fontSize = 13.sp)
                    }
                }
            }

            if (navFilter == NavFilter.TRASH && notes.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notes in Trash auto-delete after 30 days", fontSize = 12.sp, color = glass.textDimmed)
                    GlassChip(onClick = onEmptyTrash) {
                        Text("Empty Trash", fontSize = 12.sp, color = Color(0xFFFB7185), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (navFilter) {
                            NavFilter.ALL -> "No notes yet. Tap + to create your first live calculation note!"
                            NavFilter.ARCHIVED -> "No archived notes."
                            NavFilter.TRASH -> "Trash is empty."
                        },
                        color = glass.textDimmed
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(if (settings.isGridView) 2 else 1),
                    verticalItemSpacing = 12.dp,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(notes, key = { it.id }) { note ->
                        val isSelected = note.id in selectedNoteIds
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        onTogglePin(note)
                                        false
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        if (note.isTrashed) onDeletePermanently(note) else onMoveToTrash(note)
                                        true
                                    }
                                    SwipeToDismissBoxValue.Settled -> false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {},
                            modifier = Modifier.animateItemPlacement()
                        ) {
                            val tagColor = NoteTagColors[note.colorTag % NoteTagColors.size]
                            GlassCard(
                                tintColor = if (isSelected) glass.accent else tagColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (selectedNoteIds.isNotEmpty()) {
                                                onToggleMultiSelect(note.id)
                                            } else {
                                                onOpenNote(note)
                                            }
                                        },
                                        onLongClick = { onToggleMultiSelect(note.id) }
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(tagColor)
                                            )
                                            Text(
                                                text = highlightSearch(
                                                   
