package com.example.gentlenudge.attachment

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.gentlenudge.data.model.TaskAttachment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AudioMemoManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTime: Long = 0L

    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    var isRecording: Boolean = false
        private set

    var isPlaying: Boolean = false
        private set

    var playingAttachmentId: String? = null
        private set

    fun startRecording(): Boolean {
        if (isRecording) {
            cancelRecording()
        }
        return try {
            stopPlayback()
            val attachmentsDir = AttachmentManager.getAttachmentsDir(context)
            if (!attachmentsDir.exists()) {
                attachmentsDir.mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(attachmentsDir, "voice_${timeStamp}.m4a")
            currentRecordingFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()
            isRecording = true
            true
        } catch (e: Exception) {
            try {
                mediaRecorder?.reset()
            } catch (_: Exception) {}
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {}
            mediaRecorder = null
            isRecording = false
            currentRecordingFile?.delete()
            currentRecordingFile = null
            false
        }
    }

    fun stopRecording(): TaskAttachment? {
        if (!isRecording && mediaRecorder == null) return null
        val recorder = mediaRecorder
        mediaRecorder = null
        isRecording = false

        return try {
            val durationMs = System.currentTimeMillis() - recordingStartTime
            try {
                recorder?.stop()
            } catch (e: Exception) {
                // MediaRecorder.stop can throw IllegalStateException if called too quickly
            }
            try {
                recorder?.release()
            } catch (_: Exception) {}

            val file = currentRecordingFile
            if (file != null && file.exists() && file.length() > 0 && durationMs > 500) {
                val timeLabel = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date())
                TaskAttachment(
                    id = UUID.randomUUID().toString(),
                    type = TaskAttachment.TYPE_AUDIO,
                    filePath = file.absolutePath,
                    fileName = "Voice memo ($timeLabel)",
                    fileSize = file.length(),
                    durationMs = durationMs,
                    mimeType = "audio/mp4"
                )
            } else {
                file?.delete()
                null
            }
        } catch (e: Exception) {
            try {
                recorder?.release()
            } catch (_: Exception) {}
            null
        } finally {
            currentRecordingFile = null
        }
    }

    fun cancelRecording() {
        val recorder = mediaRecorder
        mediaRecorder = null
        isRecording = false
        try {
            recorder?.stop()
        } catch (_: Exception) {}
        try {
            recorder?.release()
        } catch (_: Exception) {}
        try {
            currentRecordingFile?.delete()
        } catch (_: Exception) {}
        currentRecordingFile = null
    }

    fun startPlayback(
        audioFile: File,
        onProgress: (currentMs: Int, totalMs: Int) -> Unit = { _, _ -> },
        onCompletion: () -> Unit = {}
    ) {
        stopPlayback()
        if (!audioFile.exists()) return

        try {
            val player = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    this@AudioMemoManager.isPlaying = false
                    this@AudioMemoManager.playingAttachmentId = null
                    stopProgressUpdates()
                    onCompletion()
                }
                start()
            }
            mediaPlayer = player
            isPlaying = true

            val totalDuration = player.duration
            progressRunnable = object : Runnable {
                override fun run() {
                    val p = mediaPlayer
                    if (p != null && isPlaying) {
                        val current = p.currentPosition
                        onProgress(current, totalDuration)
                        handler.postDelayed(this, 100)
                    }
                }
            }
            progressRunnable?.let { handler.post(it) }
        } catch (e: Exception) {
            stopPlayback()
        }
    }

    fun stopPlayback() {
        stopProgressUpdates()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        isPlaying = false
        playingAttachmentId = null
    }

    private fun stopProgressUpdates() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
    }

    fun cleanup() {
        if (isRecording) {
            cancelRecording()
        }
        stopPlayback()
    }
}
