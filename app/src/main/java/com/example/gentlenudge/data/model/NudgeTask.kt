package com.example.gentlenudge.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "nudge_tasks")
data class NudgeTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timeLabel: String = "Any time",
    val dateLabel: String = "Today",
    val category: String = "",
    val priority: String = "Normal", // "Normal" or "Important"
    val repeat: String = "Does not repeat",
    val soundType: String = "Small nudge", // "Small nudge" or "Full ringtone"
    val ringtoneUri: String? = null,
    val ringtoneTitle: String? = null,
    val attachmentsJson: String? = null,
    val isDone: Boolean = false,
    val section: String = "today", // "today" or "later"
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
) {
    @delegate:Ignore
    val attachments: List<TaskAttachment> by lazy {
        TaskAttachment.listFromJson(attachmentsJson)
    }
}

