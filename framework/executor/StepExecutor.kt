package com.framework.script.executor

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.framework.script.models.StepAction
import com.framework.script.models.TargetType
import com.framework.script.models.TestStep
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*

/**
 * Translates a [TestStep] into concrete Espresso interactions.
 *
 * Supports:
 *  - CLICK               → click the target view
 *  - TYPE_TEXT           → clear + type text into target view
 *  - ASSERT_VISIBLE      → check view is displayed
 *  - ASSERT_NOT_VISIBLE  → check view does not exist / is hidden
 *  - ASSERT_TEXT         → check view has the expected text
 *  - SCROLL_DOWN         → swipe down on the target view (or root if no target)
 *  - SCROLL_UP           → swipe up
 *  - SCROLL_TO_VIEW      → scroll until the target view is visible
 */
class StepExecutor {

    /**
     * Execute a single [step]. Throws [StepExecutionException] on failure.
     */
    fun execute(step: TestStep) {
        try {
            when (step.action) {
                StepAction.CLICK -> performClick(step)
                StepAction.TYPE_TEXT -> performTypeText(step)
                StepAction.ASSERT_VISIBLE -> performAssertVisible(step)
                StepAction.ASSERT_NOT_VISIBLE -> performAssertNotVisible(step)
                StepAction.ASSERT_TEXT -> performAssertText(step)
                StepAction.SCROLL_DOWN -> performScroll(step, scrollDown = true)
                StepAction.SCROLL_UP -> performScroll(step, scrollDown = false)
                StepAction.SCROLL_TO_VIEW -> performScrollToView(step)
            }
        } catch (e: StepExecutionException) {
            if (!step.optional) throw e
        } catch (e: Exception) {
            if (!step.optional) {
                throw StepExecutionException(
                    "Step [${step.action}] on '${step.target}' failed: ${e.message}", e
                )
            }
        }
    }

    // ── Action implementations ────────────────────────────────────────────────

    private fun performClick(step: TestStep) {
        requireTarget(step)
        viewFor(step).perform(click())
    }

    private fun performTypeText(step: TestStep) {
        requireTarget(step)
        val text = step.value
            ?: throw StepExecutionException("TYPE_TEXT action requires a 'value' field")
        viewFor(step).perform(clearText(), typeText(text), closeSoftKeyboard())
    }

    private fun performAssertVisible(step: TestStep) {
        requireTarget(step)
        viewFor(step).check(matches(isDisplayed()))
    }

    private fun performAssertNotVisible(step: TestStep) {
        requireTarget(step)
        // try doesNotExist first; if the view exists but is hidden, check !isDisplayed
        try {
            viewFor(step).check(doesNotExist())
        } catch (e: Exception) {
            viewFor(step).check(matches(not(isDisplayed())))
        }
    }

    private fun performAssertText(step: TestStep) {
        requireTarget(step)
        val expected = step.value
            ?: throw StepExecutionException("ASSERT_TEXT action requires a 'value' field")
        viewFor(step).check(matches(withText(expected)))
    }

    private fun performScroll(step: TestStep, scrollDown: Boolean) {
        val action = if (scrollDown) swipeUp() else swipeDown()  // swipeUp = scroll content down
        if (step.target != null) {
            viewFor(step).perform(action)
        } else {
            // Scroll the root view
            onView(isRoot()).perform(action)
        }
    }

    private fun performScrollToView(step: TestStep) {
        requireTarget(step)
        viewFor(step).perform(scrollTo())
    }

    // ── View resolution ───────────────────────────────────────────────────────

    private fun viewFor(step: TestStep): ViewInteraction {
        val target = step.target!!
        val matcher: Matcher<android.view.View> = when (step.targetType) {
            TargetType.ID -> withId(resolveId(target))
            TargetType.TEXT -> withText(target)
            TargetType.CONTENT_DESC -> withContentDescription(target)
        }
        return onView(matcher)
    }

    /**
     * Resolve a string view name like "btn_login" to its integer resource ID
     * in the target app under test.
     */
    private fun resolveId(name: String): Int {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = context.resources.getIdentifier(name, "id", context.packageName)
        if (id == 0) {
            throw StepExecutionException(
                "View id '$name' not found in package '${context.packageName}'. " +
                "Check the 'target' field in your script."
            )
        }
        return id
    }

    private fun requireTarget(step: TestStep) {
        if (step.target.isNullOrBlank()) {
            throw StepExecutionException(
                "Action '${step.action}' requires a non-empty 'target' field"
            )
        }
    }
}

class StepExecutionException(message: String, cause: Throwable? = null) :
    AssertionError(message, cause)
