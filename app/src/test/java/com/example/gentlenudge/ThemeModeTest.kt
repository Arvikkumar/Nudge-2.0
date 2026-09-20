package com.example.gentlenudge

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gentlenudge.ui.theme.ThemeMode
import com.example.gentlenudge.ui.viewmodel.NudgeViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = GentleNudgeApp::class)
class ThemeModeTest {

    private lateinit var app: GentleNudgeApp

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        val prefs = app.getSharedPreferences("gentle_nudge_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun testDefaultThemeModeIsSystem() {
        val vm = NudgeViewModel(app, app.repository)
        assertEquals(ThemeMode.SYSTEM, vm.themeMode.value)
    }

    @Test
    fun testSetThemeModeLightPersists() {
        val vm = NudgeViewModel(app, app.repository)
        vm.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, vm.themeMode.value)

        val prefs = app.getSharedPreferences("gentle_nudge_prefs", Context.MODE_PRIVATE)
        assertEquals("LIGHT", prefs.getString("app_theme_mode", null))

        // Create new VM to simulate app restart
        val newVm = NudgeViewModel(app, app.repository)
        assertEquals(ThemeMode.LIGHT, newVm.themeMode.value)
    }

    @Test
    fun testSetThemeModeDarkPersists() {
        val vm = NudgeViewModel(app, app.repository)
        vm.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, vm.themeMode.value)

        val prefs = app.getSharedPreferences("gentle_nudge_prefs", Context.MODE_PRIVATE)
        assertEquals("DARK", prefs.getString("app_theme_mode", null))

        // Create new VM to simulate app restart
        val newVm = NudgeViewModel(app, app.repository)
        assertEquals(ThemeMode.DARK, newVm.themeMode.value)
    }

    @Test
    fun testInvalidStoredThemeFallsBackToSystem() {
        val prefs = app.getSharedPreferences("gentle_nudge_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("app_theme_mode", "INVALID_MODE").commit()

        val vm = NudgeViewModel(app, app.repository)
        assertEquals(ThemeMode.SYSTEM, vm.themeMode.value)
    }

    @Test
    fun testHomeToggleFromLightSwitchesToDark() {
        val vm = NudgeViewModel(app, app.repository)
        vm.setThemeMode(ThemeMode.LIGHT)

        val isResolvedDark = when (vm.themeMode.value) {
            ThemeMode.SYSTEM -> false
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        val nextMode = if (isResolvedDark) ThemeMode.LIGHT else ThemeMode.DARK
        vm.setThemeMode(nextMode)

        assertEquals(ThemeMode.DARK, vm.themeMode.value)
    }

    @Test
    fun testHomeToggleFromDarkSwitchesToLight() {
        val vm = NudgeViewModel(app, app.repository)
        vm.setThemeMode(ThemeMode.DARK)

        val isResolvedDark = when (vm.themeMode.value) {
            ThemeMode.SYSTEM -> false
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        val nextMode = if (isResolvedDark) ThemeMode.LIGHT else ThemeMode.DARK
        vm.setThemeMode(nextMode)

        assertEquals(ThemeMode.LIGHT, vm.themeMode.value)
    }

    @Test
    fun testHomeToggleFromSystemLightSwitchesToDark() {
        val vm = NudgeViewModel(app, app.repository)
        assertEquals(ThemeMode.SYSTEM, vm.themeMode.value)

        val isSystemDark = false
        val isResolvedDark = when (vm.themeMode.value) {
            ThemeMode.SYSTEM -> isSystemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        val nextMode = if (isResolvedDark) ThemeMode.LIGHT else ThemeMode.DARK
        vm.setThemeMode(nextMode)

        // Must explicitly convert from SYSTEM to DARK
        assertEquals(ThemeMode.DARK, vm.themeMode.value)
    }

    @Test
    fun testHomeToggleFromSystemDarkSwitchesToLight() {
        val vm = NudgeViewModel(app, app.repository)
        assertEquals(ThemeMode.SYSTEM, vm.themeMode.value)

        val isSystemDark = true
        val isResolvedDark = when (vm.themeMode.value) {
            ThemeMode.SYSTEM -> isSystemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        val nextMode = if (isResolvedDark) ThemeMode.LIGHT else ThemeMode.DARK
        vm.setThemeMode(nextMode)

        // Must explicitly convert from SYSTEM to LIGHT
        assertEquals(ThemeMode.LIGHT, vm.themeMode.value)
    }
}
