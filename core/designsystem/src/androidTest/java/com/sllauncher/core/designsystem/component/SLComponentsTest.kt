package com.sllauncher.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sllauncher.core.designsystem.theme.SLTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on an emulator or device: `./gradlew :core:designsystem:connectedDebugAndroidTest`. */
@RunWith(AndroidJUnit4::class)
class SLComponentsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun searchField_clearButtonAppearsAndClears() {
        var query by mutableStateOf("")
        rule.setContent {
            SLTheme(reduceMotion = true) {
                SLSearchField(query = query, onQueryChange = { query = it }, placeholder = "Search apps")
            }
        }
        rule.onNodeWithText("Search apps").assertExists()
        rule.onNode(hasSetTextAction()).performTextInput("wha")
        rule.waitForIdle()
        assertEquals("wha", query)

        rule.onNodeWithContentDescription("Clear search").performClick()
        rule.waitForIdle()
        assertEquals("", query)
    }

    @Test
    fun switchRow_togglesWhenRowIsTapped() {
        rule.setContent {
            SLTheme(reduceMotion = true) {
                var checked by remember { mutableStateOf(false) }
                SLSwitchRow(title = "Show labels", checked = checked, onCheckedChange = { checked = it })
            }
        }
        rule.onNodeWithText("Show labels").assertIsOff().performClick()
        rule.onNodeWithText("Show labels").assertIsOn()
    }

    @Test
    fun chipRow_selectsTappedChip() {
        rule.setContent {
            SLTheme(reduceMotion = true) {
                var selected by remember { mutableStateOf("All") }
                SLChipRow(items = listOf("All", "Games"), selected = selected, label = { it }, onSelect = { selected = it })
            }
        }
        rule.onNodeWithText("GAMES").assertIsNotSelected().performClick()
        rule.onNodeWithText("GAMES").assertIsSelected()
        rule.onNodeWithText("ALL").assertIsNotSelected()
    }

    @Test
    fun floatingPanel_handleCloseActionDismisses() {
        var dismissed = false
        rule.setContent {
            SLTheme(reduceMotion = true) {
                SLFloatingPanel(visible = true, onDismissRequest = { dismissed = true }, title = "Apps") {}
            }
        }
        rule.onNodeWithContentDescription("Drag down to close").performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(dismissed)
    }
}
