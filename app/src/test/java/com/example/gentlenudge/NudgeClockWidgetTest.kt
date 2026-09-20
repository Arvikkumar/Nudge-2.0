package com.example.gentlenudge

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.gentlenudge.widget.NudgeClockWidgetProvider
import com.example.gentlenudge.widget.NudgeClockWidgetRenderer
import com.example.gentlenudge.widget.NudgeWidgetPinningHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = GentleNudgeApp::class)
class NudgeClockWidgetTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
    }

    @Test
    fun testRenderer_GeneratesValidClockBitmap() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 10)
            set(Calendar.SECOND, 30)
            set(Calendar.MILLISECOND, 0)
        }

        val bitmap = NudgeClockWidgetRenderer.renderClockBitmap(256, 256, cal)
        assertNotNull("Bitmap must not be null", bitmap)
        assertEquals(256, bitmap.width)
        assertEquals(256, bitmap.height)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun testRenderer_HandlesExtremeDimensionsSafely() {
        // Very small dimension should be clamped safely to minimum 120
        val smallBitmap = NudgeClockWidgetRenderer.renderClockBitmap(50, 50)
        assertEquals(120, smallBitmap.width)
        assertEquals(120, smallBitmap.height)

        // Very large dimension should be clamped safely to maximum 512
        val largeBitmap = NudgeClockWidgetRenderer.renderClockBitmap(1024, 1024)
        assertEquals(512, largeBitmap.width)
        assertEquals(512, largeBitmap.height)
    }

    @Test
    fun testRenderer_DrawsAtDifferentTimesWithoutError() {
        val testTimes = listOf(
            Pair(0, 0),    // Midnight
            Pair(6, 15),   // Early morning
            Pair(12, 30),  // Noon
            Pair(18, 45),  // Evening
            Pair(23, 59)   // End of day
        )

        val cal = Calendar.getInstance()
        val canvas = Canvas()

        for ((hour, minute) in testTimes) {
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val bitmap = NudgeClockWidgetRenderer.renderClockBitmap(200, 200, cal)
            assertNotNull("Rendered bitmap must not be null for $hour:$minute", bitmap)
            assertEquals(200, bitmap.width)
            assertEquals(200, bitmap.height)

            // Direct drawing verification on canvas
            NudgeClockWidgetRenderer.drawClockOnCanvas(canvas, 200f, 200f, cal)
        }
    }

    @Test
    fun testRenderer_GeneratesValidProgressRingBitmap() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }
        val ringBitmap = NudgeClockWidgetRenderer.renderProgressRingBitmap(200, 200, cal)
        assertNotNull("Ring bitmap must not be null", ringBitmap)
        assertEquals(200, ringBitmap.width)
        assertEquals(200, ringBitmap.height)

        val defaultRingBitmap = NudgeClockWidgetRenderer.renderProgressRingBitmap(calendar = cal)
        assertNotNull("Default ring bitmap must not be null", defaultRingBitmap)
        assertEquals(512, defaultRingBitmap.width)
        assertEquals(512, defaultRingBitmap.height)
    }

    @Test
    fun testWidgetProvider_ScheduleAndCancelMinuteAlarm() {
        // Schedule alarm
        NudgeClockWidgetProvider.scheduleNextMinuteAlarm(context)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue("Next minute update alarm must be scheduled", scheduledAlarms.isNotEmpty())
        val alarm = scheduledAlarms.first()
        assertTrue("Alarm trigger time must be in future", alarm.triggerAtTime > System.currentTimeMillis())

        // Cancel alarm
        NudgeClockWidgetProvider.cancelMinuteAlarm(context)
        val remainingAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue("Scheduled alarm must be cancelled", remainingAlarms.isEmpty())
    }

    @Test
    fun testWidgetProvider_HandlesBroadcastsWithoutCrashing() {
        val provider = NudgeClockWidgetProvider()

        // 1. ACTION_UPDATE_CLOCK_WIDGET
        provider.onReceive(context, Intent(NudgeClockWidgetProvider.ACTION_UPDATE_CLOCK_WIDGET))

        // 2. TIMEZONE_CHANGED
        provider.onReceive(context, Intent(Intent.ACTION_TIMEZONE_CHANGED))

        // 3. TIME_SET
        provider.onReceive(context, Intent(Intent.ACTION_TIME_CHANGED))

        // 4. DATE_CHANGED
        provider.onReceive(context, Intent(Intent.ACTION_DATE_CHANGED))

        // 5. BOOT_COMPLETED
        provider.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
    }

    @Test
    fun testWidgetPinningHelper_HandlesSupportCheckAndFallback() {
        // Test that isPinningSupported does not throw
        val isSupported = NudgeWidgetPinningHelper.isPinningSupported(context)

        // Request pin with fallback handler
        var fallbackCalled = false
        var fallbackMessage = ""

        val requested = NudgeWidgetPinningHelper.requestPinClockWidget(context) { msg ->
            fallbackCalled = true
            fallbackMessage = msg
        }

        if (isSupported) {
            assertTrue("If supported, request should return true", requested)
        } else {
            assertTrue("If unsupported, fallback message must be triggered", fallbackCalled)
            assertTrue("Fallback message must not be empty", fallbackMessage.isNotEmpty())
        }
    }

    @Test
    fun testScreenReceiver_RegistrationAndUnregistration() {
        // Register receiver
        NudgeClockWidgetProvider.ensureScreenReceiverRegistered(context)
        assertTrue("Screen receiver must be registered", NudgeClockWidgetProvider.isScreenReceiverActive())

        // Ensure idempotency
        NudgeClockWidgetProvider.ensureScreenReceiverRegistered(context)
        assertTrue("Screen receiver must remain registered without error", NudgeClockWidgetProvider.isScreenReceiverActive())

        // Unregister receiver
        NudgeClockWidgetProvider.unregisterScreenReceiver(context)
        assertTrue("Screen receiver must be unregistered", !NudgeClockWidgetProvider.isScreenReceiverActive())
    }

    @Test
    fun testTickerLifecycle_StartAndStop() {
        // Stop ticker initially
        NudgeClockWidgetProvider.stopTicker()
        assertTrue("Ticker must be stopped initially", !NudgeClockWidgetProvider.isTickerActive())

        // Stopping an already stopped ticker must be safe
        NudgeClockWidgetProvider.stopTicker()
        assertTrue("Ticker must remain stopped", !NudgeClockWidgetProvider.isTickerActive())

        // OnDisabled must clean up ticker and receiver
        val provider = NudgeClockWidgetProvider()
        provider.onDisabled(context)
        assertTrue("Ticker must be stopped on disabled", !NudgeClockWidgetProvider.isTickerActive())
        assertTrue("Receiver must be unregistered on disabled", !NudgeClockWidgetProvider.isScreenReceiverActive())
    }
}
