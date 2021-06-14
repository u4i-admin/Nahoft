package org.nahoft.nahoft.activities

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.nahoft.nahoft.R

//Ensure emulator is open to the Settings Activity Screen before pressing play for SettingsActivityTest
@RunWith(AndroidJUnit4ClassRunner::class)

class SettingsActivityTest{

    @get: Rule
    val activityRule = ActivityScenarioRule(SettingPasscodeActivity::class.java)

    @Test
    fun testIsSettingsActivityInView() {
        Espresso.onView(ViewMatchers.withId(R.id.settings_menu_logo)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.passcode_button)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testIsTitleTextOfPasscodeButtonDisplayed() {

        Espresso.onView(ViewMatchers.withId(R.id.passcode_button)).check(ViewAssertions.matches(ViewMatchers.withText(R.string.button_label_passcode)))
    }
}


