package org.nahoft.nahoft

import android.app.AlertDialog
import android.provider.ContactsContract
import android.provider.ContactsContract.Directory.PACKAGE_NAME
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.core.AllOf.allOf
import org.junit.After
import org.nahoft.nahoft.activities.AddFriendActivity
import org.nahoft.nahoft.activities.EnterPasscodeActivity
import org.nahoft.nahoft.activities.HomeActivity

@RunWith(AndroidJUnit4::class)
@LargeTest

class UITest {

    //Tests below are for Enter Passcode Activity

    private lateinit var stringToBeTyped: String
    private lateinit var firstStringToBeTyped: String
    private lateinit var secondStringToBeTyped: String

    @get:Rule
    var activityRule: ActivityScenarioRule<EnterPasscodeActivity>
        = ActivityScenarioRule(EnterPasscodeActivity::class.java)

    @Before
    fun initValidString() {
        // Specify a valid string.
        firstStringToBeTyped = "2"
        secondStringToBeTyped = "1"
        stringToBeTyped = "212121"
    }

    @Test
    fun breakItDown() {
        // Type text in first passcode digit edit text and see if you can get your test to run for just one edit text to start.
        onView(withId(R.id.passcode_digit_1))
            .perform(typeText(firstStringToBeTyped))

        // Check that the text was changed.
        onView(withId(R.id.passcode_digit_1))
            .check(matches(withText(firstStringToBeTyped)))

        onView(withId(R.id.passcode_digit_2))
            .perform(typeText(secondStringToBeTyped))

        onView(withId(R.id.passcode_digit_2))
            .check(matches(withText(secondStringToBeTyped)))

        onView(withId(R.id.passcode_digit_3))
            .perform(typeText(firstStringToBeTyped))

        onView(withId(R.id.passcode_digit_3))
            .check(matches(withText(firstStringToBeTyped)))

        onView(withId(R.id.passcode_digit_4))
            .perform(typeText(secondStringToBeTyped))

        onView(withId(R.id.passcode_digit_4))
            .check(matches(withText(secondStringToBeTyped)))

        onView(withId(R.id.passcode_digit_5))
            .perform(typeText(firstStringToBeTyped))

        onView(withId(R.id.passcode_digit_5))
            .check(matches(withText(firstStringToBeTyped)))

        onView(withId(R.id.passcode_digit_6))
            .perform(typeText(secondStringToBeTyped))

        onView(withId(R.id.passcode_digit_6))
            .check(matches(withText(secondStringToBeTyped)))
    }

    // Tests below are for Home Activity

    @get:Rule
    var activityRuleHome: ActivityScenarioRule<HomeActivity>
            = ActivityScenarioRule(HomeActivity::class.java)

    // Tried this from a tutorial, it didn't like it. Keeping only to see if Lita knows something I don't (both setUp and tearDown functions).
    fun setUp() {
        //Intents.init()
    }

    @After
    fun tearDown() {
        //Intents.release()
    }

    fun activityHomeRule(): Class<HomeActivity> {
        breakItDown()

        onView(withId(R.id.login_button)).perform(click())
        return HomeActivity::class.java

        // Test framework quits unexpectedly when I run this test. Not sure why.
        // Googled issue and tried quite a few answers none of which solved my problem.
    }

    fun homeHelpButtonWorks() {
        onView(withText("dialogText")).check(matches(isDisplayed()));

        onView(withId(android.R.id.button1)).perform(click())
    }

    fun homeHelpButtonWorksTwo() {
        onView(withText(R.string.dialog_button_home_help))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @get:Rule
    var activityRuleTwo: ActivityScenarioRule<AddFriendActivity>
            = ActivityScenarioRule(AddFriendActivity::class.java)

    @Before
    fun friendNameEntry() {
        stringToBeTyped = "Jane Doe"
    }

    // This test doesn't work either. I have to go back to the drawing board.
    // I think I'm improperly telling the tests to go to the wrong activities.

    @Test
    fun addAFriendTest() {
        onView(withId(R.id.nameTextField))
            .perform(typeText(stringToBeTyped))

        onView(withId(R.id.nameTextField))
            .check(matches(withText(stringToBeTyped)))
    }
}
