package com.calcnotes.app.data

import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    fun exportNotesToJson(notes: List<NoteEntity>): String {
        val root = JSONObject()
        root.put("app", "CalcNotes")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val array = JSONArray()
        for (note in notes) {
            val obj = JSONObject().apply {
                put("title", note.title)
                put("content", note.content)
                put("folder", note.folder)
                put("tagsCsv", note.tagsCsv)
                put("colorTag", note.colorTag)
                put("isPinned", note.isPinned)
                put("isArchived", note.isArchived)
                put("isTrashed", note.isTrashed)
                if (note.trashedAt != null) put("trashedAt", note.trashedAt)
                put("isLocked", note.isLocked)
                put("manualSortOrder", note.manualSortOrder)
                put("cachedTotal", note.cachedTotal)
                put("createdAt", note.createdAt)
                put("updatedAt", note.updatedAt)
            }
            array.put(obj)
        }
        root.put("notes", array)
        return root.toString(2)
    }

    fun importNotesFromJson(jsonString: String): List<NoteEntity> {
        val root = JSONObject(jsonString)
        val array = root.optJSONArray("notes") ?: return emptyList()
        val list = mutableListOf<NoteEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                NoteEntity(
                    title = obj.optString("title", "Imported Note"),
                    content = obj.optString("content", ""),
                    folder = obj.optString("folder", "All"),
                    tagsCsv = obj.optString("tagsCsv", ""),
                    colorTag = obj.optInt("colorTag", 0),
                    isPinned = obj.optBoolean("isPinned", false),
                    isArchived = obj.optBoolean("isArchived", false),
                    isTrashed = obj.optBoolean("isTrashed", false),
                    trashedAt = if (obj.has("trashedAt")) obj.optLong("trashedAt") else null,
                    isLocked = obj.optBoolean("isLocked", false),
                    manualSortOrder = obj.optInt("manualSortOrder", i),
                    cachedTotal = obj.optString("cachedTotal", "0"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }
        return list
    }
}
