package com.example.app.test

import com.framework.script.runner.ScriptDrivenRunner
import org.junit.runner.RunWith

/**
 * Entry point for script-driven instrumentation tests.
 *
 * This class itself contains NO test logic — it's simply the marker
 * that tells the Android test runner to use [ScriptDrivenRunner].
 * All test definitions live in YAML files on the device.
 *
 * ## Running all scripts in the default directory (/sdcard/test-scripts/)
 *
 * ```bash
 * # 1. Push your YAML scripts
 * adb push login.yaml    /sdcard/test-scripts/
 * adb push checkout.yaml /sdcard/test-scripts/
 *
 * # 2. Run
 * adb shell am instrument \
 *   -w \
 *   com.example.app.test/androidx.test.runner.AndroidJUnitRunner
 * ```
 *
 * ## Running a single YAML file
 *
 * ```bash
 * adb shell am instrument \
 *   -w \
 *   -e scriptFile login.yaml \
 *   com.example.app.test/androidx.test.runner.AndroidJUnitRunner
 * ```
 *
 * ## Running one specific scenario by id
 *
 * ```bash
 * adb shell am instrument \
 *   -w \
 *   -e scriptFile login.yaml \
 *   -e scenarioId login_with_invalid_password \
 *   com.example.app.test/androidx.test.runner.AndroidJUnitRunner
 * ```
 *
 * ## Using a custom script directory
 *
 * ```bash
 * adb shell am instrument \
 *   -w \
 *   -e scriptDir /sdcard/automation/suite-regression \
 *   com.example.app.test/androidx.test.runner.AndroidJUnitRunner
 * ```
 */
@RunWith(ScriptDrivenRunner::class)
class ScriptDrivenTest
