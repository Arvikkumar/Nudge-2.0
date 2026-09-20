package com.example.gentlenudge.backup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.gentlenudge.GentleNudgeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NudgeBackupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext as? GentleNudgeApp ?: return
        val prefs = NudgeBackupScheduler.getPrefs(context)

        when (action) {
            NudgeBackupScheduler.ACTION_TRIGGER_AUTO_BACKUP -> {
                val isEnabled = prefs.getBoolean(NudgeBackupScheduler.KEY_AUTO_BACKUP_ENABLED, false)
                if (!isEnabled) return

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d("NudgeBackupReceiver", "Executing scheduled automatic local backup...")
                        val result = NudgeBackupManager.performAutomaticBackup(
                            context = context,
                            database = app.database,
                            prefs = prefs
                        )
                        result.onSuccess { file ->
                            Log.d("NudgeBackupReceiver", "Auto backup created successfully: ${file.name} (${file.length()} bytes)")
                            val now = System.currentTimeMillis()
                            val dayOfWeek = prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_DAY, java.util.Calendar.SUNDAY)
                            val hour = prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_HOUR, 23)
                            val minute = prefs.getInt(NudgeBackupScheduler.KEY_AUTO_BACKUP_MINUTE, 0)
                            val occurrence = NudgeBackupScheduler.calculatePreviousScheduledTime(now, dayOfWeek, hour, minute)
                            prefs.edit().putLong(NudgeBackupScheduler.KEY_LAST_PROCESSED_OCCURRENCE, occurrence).apply()
                        }.onFailure { error ->
                            Log.e("NudgeBackupReceiver", "Auto backup failed: ${error.message}", error)
                        }
                    } catch (e: Exception) {
                        Log.e("NudgeBackupReceiver", "Unexpected error in auto backup: ${e.message}", e)
                    } finally {
                        // Always schedule the next recurring automatic backup
                        NudgeBackupScheduler.scheduleAutoBackup(context)
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
