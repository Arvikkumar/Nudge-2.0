package com.example.gentlenudge

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gentlenudge.attachment.AttachmentManager
import com.example.gentlenudge.data.model.TaskAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AttachmentManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        try {
            val providerInfo = android.content.pm.ProviderInfo().apply {
                authority = "${context.packageName}.fileprovider"
                grantUriPermissions = true
                metaData = android.os.Bundle().apply {
                    putInt("android.support.FILE_PROVIDER_PATHS", R.xml.file_paths)
                }
            }
            org.robolectric.Robolectric.buildContentProvider(androidx.core.content.FileProvider::class.java).create(providerInfo)
        } catch (_: Exception) {}
    }

    @Test
    fun testCreateAttachmentFromLocalFile() {
        val testFile = File(context.filesDir, "test_doc.pdf").apply {
            writeBytes(ByteArray(512) { 0x20 })
        }
        val attachment = AttachmentManager.createAttachmentFromLocalFile(testFile, "application/pdf", TaskAttachment.TYPE_FILE)
        assertNotNull(attachment)
        assertEquals(TaskAttachment.TYPE_FILE, attachment.type)
        assertEquals("application/pdf", attachment.mimeType)
        assertEquals(512L, attachment.fileSize)
        assertEquals("test_doc.pdf", attachment.fileName)
    }

    @Test
    fun testDeleteAttachmentFile() {
        val testFile = File(context.filesDir, "test_delete_me.txt").apply {
            writeText("Hello Gentle Nudge")
        }
        assertTrue(testFile.exists())

        val deleted = AttachmentManager.deleteAttachmentFile(testFile.absolutePath)
        assertTrue(deleted)
        assertTrue(!testFile.exists())
    }
}
