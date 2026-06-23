package org.nahoft.nahoft

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.nahoft.nahoft.activities.LogInActivity

//Ensure emulator is open to the Enter Passcode Activity Screen before pressing play for EnterPasscodeActivityTest(s)
@RunWith(AndroidJUnit4::class)
class LogInActivityTest{

    @Test
    fun test_isActivityInView() {

        onView(withId(R.id.passcode_digit_1)).check(matches(isDisplayed()))
        onView(withId(R.id.passcode_digit_2)).check(matches(isDisplayed()))
        onView(withId(R.id.passcode_digit_3)).check(matches(isDisplayed()))
        onView(withId(R.id.passcode_digit_4)).check(matches(isDisplayed()))
        onView(withId(R.id.passcode_digit_5)).check(matches(isDisplayed()))
        onView(withId(R.id.passcode_digit_6)).check(matches(isDisplayed()))
    }

    @Test
    fun test_EnterPasscodeButtonIsVisible() {

        onView(withId(R.id.login_button)).check(matches(isDisplayed()))
    }

    @Test
    fun testIsTitleTextOfLoginButtonDisplayed() {

        onView(withId(R.id.login_button))
            .check(matches(withText(R.string.Login)))
    }

    private lateinit var stringToBeTyped: String
    private lateinit var firstStringToBeTyped: String
    private lateinit var secondStringToBeTyped: String

    @get:Rule
    var activityRule: ActivityScenarioRule<LogInActivity>
            = ActivityScenarioRule(LogInActivity::class.java)

    @Before
    fun initValidString() {
        // Specify a valid string.
        firstStringToBeTyped = "2"
        secondStringToBeTyped = "1"
        stringToBeTyped = "212121"
    }

    @Test
    fun performLogin() {
        //Tests if Login Works
        onView(withId(R.id.passcode_digit_1)).perform(typeText(firstStringToBeTyped))

        // Check that the text was changed.
        onView(withId(R.id.passcode_digit_1)).check(matches(withText(firstStringToBeTyped)))

        onView(withId(R.id.passcode_digit_2)).perform(typeText(secondStringToBeTyped))

        onView(withId(R.id.passcode_digit_2)).check(matches(withText(secondStringToBeTyped)))

        onView(withId(R.id.passcode_digit_3)).perform(typeText(firstStringToBeTyped))

        onView(withId(R.id.passcode_digit_3)).check(matches(withText(firstStringToBeTyped)))

        onView(withId(R.id.passcode_digit_4)).perform(typeText(secondStringToBeTyped))

        onView(withId(R.id.passcode_digit_4)).check(matches(withText(secondStringToBeTyped)))

        onView(withId(R.id.passcode_digit_5)).perform(typeText(firstStringToBeTyped))

        onView(withId(R.id.passcode_digit_5)).check(matches(withText(firstStringToBeTyped)))

        onView(withId(R.id.passcode_digit_6)).perform(typeText(secondStringToBeTyped))

        onView(withId(R.id.passcode_digit_6)).check(matches(withText(secondStringToBeTyped)))
    }
 }

