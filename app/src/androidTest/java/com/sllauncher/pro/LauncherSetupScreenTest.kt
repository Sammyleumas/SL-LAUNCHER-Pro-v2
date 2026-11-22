package com.sllauncher.pro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sllauncher.pro.setup.LauncherSetupScreen
import com.sllauncher.pro.setup.LauncherSetupUiState
import com.sllauncher.core.designsystem.theme.SLTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on an emulator or device: `./gradlew connectedDebugAndroidTest`. */
@RunWith(AndroidJUnit4::class)
class LauncherSetupScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun notDefault_requestButtonTriggersCallback() {
        var requested = false
        composeRule.setContent {
            SLTheme {
                LauncherSetupScreen(
                    state = LauncherSetupUiState(isDefaultHome = false, showSettingsFallback = false),
                    settingsUnavailable = false,
                    onRequestDefault = { requested = true },
                    onOpenSettings = {},
                    onDone = {},
                )
            }
        }

        composeRule.onNodeWithText("SET AS DEFAULT HOME APP").assertIsDisplayed().performClick()
        assertTrue(requested)
    }

    @Test
    fun fallbackShowsSettingsButton() {
        composeRule.setContent {
            SLTheme {
                LauncherSetupScreen(
                    state = LauncherSetupUiState(isDefaultHome = false, showSettingsFallback = true),
                    settingsUnavailable = false,
                    onRequestDefault = {},
                    onOpenSettings = {},
                    onDone = {},
                )
            }
        }

        composeRule.onNodeWithText("OPEN ANDROID HOME SETTINGS").assertIsDisplayed()
    }

    @Test
    fun isDefault_showsDoneButton() {
        composeRule.setContent {
            SLTheme {
                LauncherSetupScreen(
                    state = LauncherSetupUiState(isDefaultHome = true, showSettingsFallback = false),
                    settingsUnavailable = false,
                    onRequestDefault = {},
                    onOpenSettings = {},
                    onDone = {},
                )
            }
        }

        composeRule.onNodeWithText("BACK TO HOME").assertIsDisplayed()
    }
}
