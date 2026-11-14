package com.calcnotes.app.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {

    val allNotesFlow: Flow<List<NoteEntity>> = dao.observeAllNotes()

    suspend fun getAllNotesOnce(): List<NoteEntity> = dao.getAllNotesOnce()

    suspend fun getNoteById(id: Long): NoteEntity? = dao.getNoteById(id)

    suspend fun saveNote(note: NoteEntity): Long {
        return if (note.id == 0L) {
            dao.insertNote(note.copy(updatedAt = System.currentTimeMillis()))
        } else {
            dao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            note.id
        }
    }

    suspend fun duplicateNote(note: NoteEntity): Long {
        val copy = note.copy(
            id = 0L,
            title = if (note.title.isBlank()) "Untitled Copy" else "${note.title} (Copy)",
            isPinned = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return dao.insertNote(copy)
    }

    suspend fun moveToTrash(note: NoteEntity) {
        dao.updateNote(
            note.copy(
                isTrashed = true,
                isPinned = false,
                trashedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun restoreFromTrash(note: NoteEntity) {
        dao.updateNote(
            note.copy(
                isTrashed = false,
                trashedAt = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deletePermanently(id: Long) {
        dao.deletePermanently(id)
    }

    suspend fun emptyTrash() {
        dao.emptyTrash()
    }

    suspend fun purgeOldTrash(days: Int = 30) {
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24L * 60L * 60L * 1000L)
        dao.purgeExpiredTrash(cutoff)
    }

    suspend fun importNotes(notes: List<NoteEntity>) {
        dao.insertAll(notes)
    }
}
