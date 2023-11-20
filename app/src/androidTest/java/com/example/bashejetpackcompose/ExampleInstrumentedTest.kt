package com.example.bashejetpackcompose

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @JvmField
    @Rule
    val activity = createAndroidComposeRule<MainActivity>()

    @Test
    fun performMove() {
        var count = 8

        while (count > 0) {
            activity.onNodeWithTag("NUMBER").performTextInput("1")
            activity.onNodeWithTag("BUTTON").performClick()
            activity.onNodeWithTag("NUMBER").performTextClearance()
            count -= 1
        }

        activity.onNodeWithTag("RESULT").assertTextEquals(R.string.player_win.toString())
    }
}