package org.nahoft.nahoft.activities

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.nahoft.nahoft.R
import kotlin.text.matches

//Ensure emulator is open to the Home Activity Screen before pressing play for HomeActivityTest(s)
@RunWith(AndroidJUnit4ClassRunner::class)

class HomeActivityTest{

    @get: Rule
    val activityRule = ActivityScenarioRule(HomeActivity::class.java)

    @Test
    fun test_isActivityInView() {
        onView(withId(R.id.home_help_button)).check(ViewAssertions.matches(isDisplayed()))
        onView(withId(R.id.nahoft_message_bottle)).check(ViewAssertions.matches(isDisplayed()))
        onView(withId(R.id.settings_button)).check(ViewAssertions.matches(isDisplayed()))
        onView(withId(R.id.user_guide_button)).check(ViewAssertions.matches(isDisplayed()))
        onView(withId(R.id.friends_button)).check(ViewAssertions.matches(isDisplayed()))
        onView(withId(R.id.read_button)).check(ViewAssertions.matches(isDisplayed()))
    }
}