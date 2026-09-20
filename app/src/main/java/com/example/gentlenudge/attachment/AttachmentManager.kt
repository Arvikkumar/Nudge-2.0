package com.example.gentlenudge.attachment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.gentlenudge.data.model.TaskAttachment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object AttachmentManager {

    fun getAttachmentsDir(context: Context): File {
        val dir = File(context.filesDir, "attachments")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getFileProviderUri(context: Context, file: File): Uri {
        val authorities = listOf(
            "${context.packageName}.fileprovider",
            "com.aistudio.gentlenudge.zqmpwa.fileprovider",
            "com.example.gentlenudge.fileprovider",
            "${context.applicationContext.packageName}.fileprovider"
        ).distinct()

        for (auth in authorities) {
            try {
                return FileProvider.getUriForFile(context, auth, file)
            } catch (_: Exception) {
                // Try next authority
            }
        }
        // Graceful fallback for test runners or environments without initialized provider
        return Uri.fromFile(file)
    }

    fun createAttachmentFromLocalFile(file: File, mimeType: String, type: String): TaskAttachment {
        return TaskAttachment(
            id = UUID.randomUUID().toString(),
            type = type,
            filePath = file.absolutePath,
            fileName = file.name,
            fileSize = file.length(),
            mimeType = mimeType
        )
    }

    fun saveAttachmentFromUri(context: Context, sourceUri: Uri, type: String): TaskAttachment? {
        return try {
            val contentResolver = context.contentResolver
            var displayName = "Attachment"
            var sizeBytes = 0L
            val mimeType = contentResolver.getType(sourceUri)

            contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: "Attachment"
                    if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                }
            }

            val attachmentsDir = getAttachmentsDir(context)
            val extension = when (type) {
                TaskAttachment.TYPE_IMAGE -> ".jpg"
                TaskAttachment.TYPE_AUDIO -> ".m4a"
                else -> {
                    val ext = displayName.substringAfterLast('.', "")
                    if (ext.isNotEmpty()) ".$ext" else ""
                }
            }

            val uniqueFileName = "att_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}$extension"
            val destFile = File(attachmentsDir, uniqueFileName)

            contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (sizeBytes == 0L && destFile.exists()) {
                sizeBytes = destFile.length()
            }

            TaskAttachment(
                id = UUID.randomUUID().toString(),
                type = type,
                filePath = destFile.absolutePath,
                fileName = displayName,
                fileSize = sizeBytes,
                mimeType = mimeType
            )
        } catch (_: Exception) {
            null
        }
    }

    fun deleteAttachmentFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) file.delete() else false
        } catch (_: Exception) {
            false
        }
    }

    fun openAttachment(context: Context, attachment: TaskAttachment) {
        try {
            val file = File(attachment.filePath)
            if (!file.exists()) return

            val uriToShare: Uri = getFileProviderUri(context, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    uriToShare,
                    attachment.mimeType ?: when (attachment.type) {
                        TaskAttachment.TYPE_IMAGE -> "image/*"
                        TaskAttachment.TYPE_AUDIO -> "audio/*"
                        else -> "*/*"
                    }
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Intent view fallback
        }
    }
}
