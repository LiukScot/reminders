package com.liukscot.reminders.data

import org.json.JSONArray
import org.json.JSONObject

// A whole-database snapshot: every list, tag and task, with each task's tags kept as ids so the
// cross-reference table is rebuilt on restore rather than exported as its own opaque array.
data class RemindersBackup(
    val lists: List<TaskList>,
    val tags: List<Tag>,
    val tasks: List<Task>,
    val tagIdsByTaskId: Map<Long, List<Long>>,
)

// Bumped only when a change makes an older file unreadable; restore refuses what it can't read
// rather than importing a half-understood file over the user's data.
const val BACKUP_FORMAT_VERSION = 1

fun encodeBackup(backup: RemindersBackup, exportedAt: Long): String {
    val root = JSONObject()
    root.put("version", BACKUP_FORMAT_VERSION)
    root.put("exportedAt", exportedAt)
    root.put("lists", backup.lists.jsonArray { list ->
        JSONObject()
            .put("id", list.id)
            .put("name", list.name)
            .put("icon", list.icon)
    })
    root.put("tags", backup.tags.jsonArray { tag ->
        JSONObject()
            .put("id", tag.id)
            .put("name", tag.name)
    })
    root.put("tasks", backup.tasks.jsonArray { task ->
        JSONObject()
            .put("id", task.id)
            .put("listId", task.listId)
            .put("parentId", task.parentId)
            .put("title", task.title)
            .put("notes", task.notes)
            .put("dueAt", task.dueAt)
            .put("hasDueTime", task.hasDueTime)
            .put("flagged", task.flagged)
            .put("priority", task.priority)
            .put("completed", task.completed)
            .put("createdAt", task.createdAt)
            .put("recurrenceFreq", task.recurrenceFreq)
            .put("recurrenceInterval", task.recurrenceInterval)
            .put("recurrenceByDay", task.recurrenceByDay)
            .put("recurrenceAnchor", task.recurrenceAnchor)
            .put("tagIds", JSONArray(backup.tagIdsByTaskId[task.id].orEmpty()))
    })
    return root.toString(2)
}

// Throws JSONException on a malformed file and IllegalArgumentException on an unsupported version.
// Both are caught at the restore boundary and surfaced to the user — a backup file is untrusted
// input, it can be any file the picker returned.
fun decodeBackup(text: String): RemindersBackup {
    val root = JSONObject(text)
    val version = root.getInt("version")
    require(version <= BACKUP_FORMAT_VERSION) {
        "backup format v$version is newer than this app supports (v$BACKUP_FORMAT_VERSION)"
    }

    val lists = root.getJSONArray("lists").map { obj ->
        TaskList(
            id = obj.getLong("id"),
            name = obj.getString("name"),
            icon = obj.getString("icon"),
        )
    }
    val tags = root.getJSONArray("tags").map { obj ->
        Tag(id = obj.getLong("id"), name = obj.getString("name"))
    }

    val taskObjects = root.getJSONArray("tasks").map { it }
    val tasks = taskObjects.map { obj ->
        Task(
            id = obj.getLong("id"),
            listId = obj.getLong("listId"),
            parentId = obj.optLongOrNull("parentId"),
            title = obj.getString("title"),
            notes = obj.optStringOrNull("notes"),
            dueAt = obj.optLongOrNull("dueAt"),
            hasDueTime = obj.getBoolean("hasDueTime"),
            flagged = obj.getBoolean("flagged"),
            priority = obj.getInt("priority"),
            completed = obj.getBoolean("completed"),
            createdAt = obj.getLong("createdAt"),
            recurrenceFreq = obj.optStringOrNull("recurrenceFreq"),
            recurrenceInterval = obj.getInt("recurrenceInterval"),
            recurrenceByDay = obj.optStringOrNull("recurrenceByDay"),
            recurrenceAnchor = obj.optLongOrNull("recurrenceAnchor"),
        )
    }
    val tagIdsByTaskId = taskObjects.associate { obj ->
        val ids = obj.optJSONArray("tagIds") ?: JSONArray()
        obj.getLong("id") to List(ids.length()) { ids.getLong(it) }
    }

    return RemindersBackup(lists, tags, tasks, tagIdsByTaskId)
}

private fun <T> List<T>.jsonArray(toJson: (T) -> JSONObject): JSONArray =
    JSONArray().also { array -> forEach { array.put(toJson(it)) } }

private fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
    List(length()) { transform(getJSONObject(it)) }

// JSONObject.put(null) stores JSONObject.NULL, and getLong/getString on it throws rather than
// returning null — so nullable columns need an explicit isNull check on the way back in.
private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)

private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
