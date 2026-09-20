package com.example.gentlenudge.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

data class TaskAttachment(
    val id: String = UUID.randomUUID().toString(),
    val type: String = TYPE_FILE,
    val filePath: String = "",
    val fileName: String = "Attachment",
    val fileSize: Long = 0L,
    val durationMs: Long? = null,
    val mimeType: String? = null
) {
    fun formattedSize(): String {
        if (fileSize <= 0) return ""
        val kb = fileSize / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$fileSize B"
        }
    }

    fun formattedDuration(): String {
        if (durationMs == null || durationMs <= 0) return "0:00"
        val totalSec = durationMs / 1000
        val mins = totalSec / 60
        val secs = totalSec % 60
        return String.format(Locale.US, "%d:%02d", mins, secs)
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("type", type)
            put("filePath", filePath)
            put("fileName", fileName)
            put("fileSize", fileSize)
            if (durationMs != null) put("durationMs", durationMs)
            if (mimeType != null) put("mimeType", mimeType)
        }
    }

    companion object {
        const val TYPE_IMAGE = "image"
        const val TYPE_FILE = "file"
        const val TYPE_AUDIO = "audio"

        fun fromJson(json: JSONObject): TaskAttachment {
            return TaskAttachment(
                id = json.optString("id", UUID.randomUUID().toString()),
                type = json.optString("type", TYPE_FILE),
                filePath = json.optString("filePath", json.optString("uri", "")),
                fileName = json.optString("fileName", json.optString("displayName", "Attachment")),
                fileSize = json.optLong("fileSize", json.optLong("sizeBytes", 0L)),
                durationMs = if (json.has("durationMs")) json.getLong("durationMs") else null,
                mimeType = if (json.has("mimeType")) json.getString("mimeType") else null
            )
        }

        fun listToJson(attachments: List<TaskAttachment>): String {
            val array = JSONArray()
            attachments.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(jsonStr: String?): List<TaskAttachment> {
            if (jsonStr.isNullOrBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<TaskAttachment>()
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
