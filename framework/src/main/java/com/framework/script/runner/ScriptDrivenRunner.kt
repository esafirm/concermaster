package com.framework.script.runner

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.framework.script.executor.StepExecutor
import com.framework.script.loader.ScriptLoader
import com.framework.script.models.TestScenario
import com.framework.script.models.TestSuite
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import org.junit.runners.ParentRunner
import org.junit.runners.model.InitializationError

/**
 * A JUnit4 [Runner] that dynamically generates one test case per [TestScenario]
 * loaded from YAML scripts on external storage.
 *
 * ## Usage
 *
 * ```kotlin
 * @RunWith(ScriptDrivenRunner::class)
 * class MyScriptTest
 * ```
 *
 * ## Instrumentation arguments
 *
 * Pass via `adb shell am instrument -e key value`:
 *
 * | Argument      | Default               | Description                              |
 * |---------------|-----------------------|------------------------------------------|
 * | scriptDir     | /sdcard/test-scripts  | Root directory for .yaml files           |
 * | scriptFile    | (none)                | Run a single file instead of the whole dir |
 * | scenarioId    | (none)                | Run only the scenario with this id       |
 *
 * Example:
 * ```
 * adb shell am instrument \
 *   -w \
 *   -e scriptDir /sdcard/automation \
 *   -e scriptFile login.yaml \
 *   -e scenarioId login_success \
 *   com.example.app.test/androidx.test.runner.AndroidJUnitRunner
 * ```
 */
class ScriptDrivenRunner @Throws(InitializationError::class) constructor(
    private val testClass: Class<*>
) : ParentRunner<ScenarioTestCase>(testClass) {

    private val executor = StepExecutor()
    private val scenarios: List<ScenarioTestCase> by lazy { loadScenarios() }

    // ── ParentRunner contract ─────────────────────────────────────────────────

    override fun getChildren(): List<ScenarioTestCase> = scenarios

    override fun describeChild(child: ScenarioTestCase): Description =
        Description.createTestDescription(
            child.suiteName,           // class name column in results
            child.scenario.name,       // method name column in results
            child.scenario.id          // unique annotation key
        )

    override fun runChild(child: ScenarioTestCase, notifier: RunNotifier) {
        val description = describeChild(child)
        notifier.fireTestStarted(description)
        try {
            child.scenario.steps.forEachIndexed { index, step ->
                try {
                    executor.execute(step)
                } catch (e: Throwable) {
                    // Annotate failure with step context for easier debugging
                    val stepInfo = "Step ${index + 1}/${child.scenario.steps.size} " +
                        "[${step.action}] target='${step.target}' value='${step.value}'"
                    throw AssertionError("$stepInfo\n↳ ${e.message}", e)
                }
            }
            notifier.fireTestFinished(description)
        } catch (e: Throwable) {
            notifier.fireTestFailure(Failure(description, e))
            notifier.fireTestFinished(description)
        }
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    private fun loadScenarios(): List<ScenarioTestCase> {
        val args = instrumentationArgs()
        val loader = buildLoader(args)
        val suites = loadSuites(loader, args)
        val scenarioIdFilter = args.getString("scenarioId")

        return suites.flatMap { suite ->
            suite.scenarios
                .filter { s -> scenarioIdFilter == null || s.id == scenarioIdFilter }
                .map { scenario -> ScenarioTestCase(suite.name, scenario) }
        }.also { list ->
            if (list.isEmpty()) {
                throw InitializationError(
                    "No test scenarios were loaded. " +
                    "Check your scriptDir / scriptFile / scenarioId arguments."
                )
            }
        }
    }

    private fun buildLoader(args: Bundle): ScriptLoader {
        val scriptDir = args.getString("scriptDir")
        return if (scriptDir != null) ScriptLoader.fromPath(scriptDir) else ScriptLoader()
    }

    private fun loadSuites(loader: ScriptLoader, args: Bundle): List<TestSuite> {
        val scriptFile = args.getString("scriptFile")
        return if (scriptFile != null) {
            listOf(loader.loadFile(scriptFile))
        } else {
            loader.loadAll()
        }
    }

    private fun instrumentationArgs(): Bundle =
        InstrumentationRegistry.getArguments()

    // ── Suppress default constructor check (no public no-arg ctor needed) ─────

    override fun collectInitializationErrors(errors: MutableList<Throwable>) {
        // Skip the default JUnit checks that require a public no-arg constructor
        // on the test class — our test class is just a marker.
    }
}

/**
 * Wraps a [TestScenario] together with its parent suite name,
 * so the runner can produce correct JUnit [Description] labels.
 */
data class ScenarioTestCase(
    val suiteName: String,
    val scenario: TestScenario
)
