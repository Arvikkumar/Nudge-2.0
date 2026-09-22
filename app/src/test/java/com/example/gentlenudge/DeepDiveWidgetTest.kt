package com.example.gentlenudge

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.gentlenudge.deepdive.DeepDiveManager
import com.example.gentlenudge.widget.DeepDiveWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = GentleNudgeApp::class)
class DeepDiveWidgetTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        DeepDiveManager.init(context)
        DeepDiveManager.endSession(context)
    }

    @Test
    fun testLayoutInflation() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.widget_deep_dive, null)
        assertNotNull("widget_deep_dive layout must inflate", view)
        assertNotNull("Idle reminders text view must exist", view.findViewById(R.id.widget_idle_reminders_text))
        assertNotNull("Idle sound text view must exist", view.findViewById(R.id.widget_idle_sound_text))
        assertNotNull("Idle config button must exist", view.findViewById(R.id.widget_idle_btn_config))
        assertNotNull("Active reminders text view must exist", view.findViewById(R.id.widget_active_reminders_text))
        assertNotNull("Active sound text view must exist", view.findViewById(R.id.widget_active_sound_text))
        assertNotNull("Active config button must exist", view.findViewById(R.id.widget_active_btn_config))

        val shortView = inflater.inflate(R.layout.widget_deep_dive_short, null)
        assertNotNull("widget_deep_dive_short layout must inflate", shortView)
        assertNotNull("Short idle reminders text view must exist", shortView.findViewById(R.id.widget_idle_reminders_text))
        assertNotNull("Short idle sound text view must exist", shortView.findViewById(R.id.widget_idle_sound_text))
        assertNotNull("Short active reminders text view must exist", shortView.findViewById(R.id.widget_active_reminders_text))
    }

    @Test
    fun testRemoteViewsApply() {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_deep_dive)
        val appliedView = remoteViews.apply(context, FrameLayout(context))
        assertNotNull("RemoteViews must apply without exception", appliedView)

        val shortRemoteViews = RemoteViews(context.packageName, R.layout.widget_deep_dive_short)
        val appliedShortView = shortRemoteViews.apply(context, FrameLayout(context))
        assertNotNull("Short RemoteViews must apply without exception", appliedShortView)
    }

    @Test
    fun testFormatRemindersCountLabel() {
        assertEquals("🔔 No reminders", DeepDiveManager.formatRemindersCountLabel(0))
        assertEquals("🔔 1 reminder", DeepDiveManager.formatRemindersCountLabel(1))
        assertEquals("🔔 2 reminders", DeepDiveManager.formatRemindersCountLabel(2))
        assertEquals("🔔 3 reminders", DeepDiveManager.formatRemindersCountLabel(3))
        assertEquals("🔔 4 reminders", DeepDiveManager.formatRemindersCountLabel(4))
    }

    @Test
    fun testFormatNotificationStyleLabel() {
        assertEquals("♪ One Shot", DeepDiveManager.formatNotificationStyleLabel("One Shot"))
        assertEquals("♪ Full Ringtone", DeepDiveManager.formatNotificationStyleLabel("Full Ringtone"))
    }

    @Test
    fun testUpdateWidgetIdle_ReflectsConfiguredSettings() {
        // Save 2 configured reminders and Full Ringtone
        DeepDiveManager.saveConfiguredReminders(
            context,
            listOf(
                DeepDiveManager.ConfiguredReminderItem(id = "r1", label = "Halfway", offsetMinutes = 15),
                DeepDiveManager.ConfiguredReminderItem(id = "r2", label = "5 min left", offsetMinutes = 5)
            )
        )
        DeepDiveManager.saveNotificationStyle(context, "Full Ringtone")

        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val provider = DeepDiveWidgetProvider()
        provider.onUpdate(context, appWidgetManager, intArrayOf(1))

        assertEquals(2, DeepDiveManager.getConfiguredRemindersCount(context))
        assertEquals("Full Ringtone", DeepDiveManager.getSavedNotificationStyle(context))
    }

    @Test
    fun testUpdateWidgetActive() {
        // Start a real session
        val now = System.currentTimeMillis()
        DeepDiveManager.startSession(
            context = context,
            targetEndTimeMillis = now + 1800000L,
            notificationStyle = "One Shot",
            reminders = listOf(
                DeepDiveManager.ReminderPoint(triggerTimeMillis = now + 900000L, label = "Halfway")
            )
        )

        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val provider = DeepDiveWidgetProvider()
        provider.onUpdate(context, appWidgetManager, intArrayOf(1))

        val state = DeepDiveManager.state.value
        assertTrue("Session must be active", state.isActive)
        assertEquals(1, state.reminderPoints.size)

        // Clean up
        DeepDiveManager.endSession(context)
    }

    @Test
    fun testWidgetProvider_HandlesBroadcastsWithoutCrashing() {
        val provider = DeepDiveWidgetProvider()
        provider.onReceive(context, Intent(DeepDiveWidgetProvider.ACTION_UPDATE_DEEP_DIVE_WIDGET))
        provider.onReceive(context, Intent(Intent.ACTION_TIMEZONE_CHANGED))
        provider.onReceive(context, Intent(Intent.ACTION_DATE_CHANGED))
    }

    @Test
    fun testWidgetSizes_SimulationAndInflation() {
        val inflater = LayoutInflater.from(context)

        // 1. Current Large size (e.g. 260dp x 140dp)
        val largeContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(700, 380)
        }
        val normalView = inflater.inflate(R.layout.widget_deep_dive, largeContainer, true)
        assertNotNull(normalView.findViewById(R.id.widget_deep_dive_title))
        assertNotNull(normalView.findViewById(R.id.widget_deep_dive_subtitle))
        assertNotNull(normalView.findViewById(R.id.widget_btn_30min))
        assertNotNull(normalView.findViewById(R.id.widget_btn_1hour))
        assertNotNull(normalView.findViewById(R.id.widget_btn_quick_play))

        // 2. Current Small / Compact size (e.g. 220dp x 90dp)
        val smallContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(550, 240)
        }
        val shortView = inflater.inflate(R.layout.widget_deep_dive_short, smallContainer, true)
        assertNotNull(shortView.findViewById(R.id.widget_deep_dive_title))
        assertNotNull(shortView.findViewById(R.id.widget_btn_30min))
        assertNotNull(shortView.findViewById(R.id.widget_btn_1hour))
        assertNotNull(shortView.findViewById(R.id.widget_btn_quick_play))
        assertNotNull(shortView.findViewById(R.id.widget_idle_reminders_text))
        assertNotNull(shortView.findViewById(R.id.widget_idle_sound_text))

        // 3. Short and Wide size (e.g. 360dp x 80dp)
        val shortWideContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(900, 210)
        }
        val shortWideView = inflater.inflate(R.layout.widget_deep_dive_short, shortWideContainer, true)
        assertNotNull(shortWideView)

        // 4. Narrow and Tall / Intermediate size (e.g. 180dp x 120dp)
        val narrowContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(450, 320)
        }
        val narrowView = inflater.inflate(R.layout.widget_deep_dive_short, narrowContainer, true)
        assertNotNull(narrowView)

        // 5. Active state in compact size
        val activeShortContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(550, 240)
        }
        val activeShortView = inflater.inflate(R.layout.widget_deep_dive_short, activeShortContainer, true)
        assertNotNull(activeShortView.findViewById(R.id.widget_active_chronometer))
        assertNotNull(activeShortView.findViewById(R.id.widget_active_end_time))
        assertNotNull(activeShortView.findViewById(R.id.widget_btn_stop))
        assertNotNull(activeShortView.findViewById(R.id.widget_active_badge))
    }
}
