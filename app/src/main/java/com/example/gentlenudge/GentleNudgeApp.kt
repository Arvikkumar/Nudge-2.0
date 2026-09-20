package com.example.gentlenudge

import android.app.Application
import com.example.gentlenudge.backup.NudgeBackupScheduler
import com.example.gentlenudge.data.db.AppDatabase
import com.example.gentlenudge.data.repository.NudgeRepository
import com.example.gentlenudge.notification.NudgeEventNotificationScheduler
import com.example.gentlenudge.notification.NudgeNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class GentleNudgeApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { NudgeRepository(database.nudgeTaskDao(), database.timeGoalDao()) }

    override fun onCreate() {
        super.onCreate()
        try {
            NudgeNotificationHelper.createNotificationChannels(this)
            NudgeBackupScheduler.rescheduleAll(this)
            NudgeEventNotificationScheduler.rescheduleIfEnabled(this)
        } catch (e: Exception) {
            android.util.Log.e("GentleNudgeApp", "Error during app init: ${e.message}", e)
        }
    }
}
