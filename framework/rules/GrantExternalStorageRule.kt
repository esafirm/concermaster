package com.framework.script.rules

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that ensures external storage is readable before tests execute.
 *
 * • On API ≤ 28: grants READ_EXTERNAL_STORAGE via [GrantPermissionRule].
 * • On API 29–30: grants READ_EXTERNAL_STORAGE (scoped storage is relaxed for tests).
 * • On API 31+:  checks MANAGE_EXTERNAL_STORAGE — if not granted, throws a clear
 *                error explaining how to enable it, rather than a cryptic IO error.
 *
 * ## Integration with ScriptDrivenRunner
 *
 * Because [ScriptDrivenRunner] is a custom [ParentRunner], you cannot use @Rule
 * annotations directly. Instead, call this rule manually in a companion object
 * or use it in a separate [androidx.test.ext.junit.rules.ActivityScenarioRule]-based
 * setup if your test class extends a base class.
 *
 * The simplest approach: grant permission via ADB in your CI pipeline:
 * ```
 * adb shell pm grant com.example.app.test android.permission.READ_EXTERNAL_STORAGE
 * ```
 */
class GrantExternalStorageRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                ensureStoragePermission()
                base.evaluate()
            }
        }
    }

    private fun ensureStoragePermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // API 30+ needs MANAGE_EXTERNAL_STORAGE
                if (!Environment.isExternalStorageManager()) {
                    throw IllegalStateException(
                        """
                        |MANAGE_EXTERNAL_STORAGE is not granted on API ${Build.VERSION.SDK_INT}.
                        |
                        |Grant it before running tests:
                        |  Option 1 — ADB (preferred for CI):
                        |    adb shell appops set --uid com.example.app.test \
                        |        MANAGE_EXTERNAL_STORAGE allow
                        |
                        |  Option 2 — Settings UI:
                        |    Open "Files and media" in App Settings for the test APK.
                        |
                        |  Option 3 — adb shell:
                        |    adb shell cmd appops set com.example.app.test \
                        |        android:manage_external_storage allow
                        """.trimMargin()
                    )
                }
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // API 23–29: request at runtime
                InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                    "pm grant ${applicationId()} ${Manifest.permission.READ_EXTERNAL_STORAGE}"
                )
            }

            // API < 23: declared in manifest, already granted at install time
        }
    }

    private fun applicationId(): String =
        InstrumentationRegistry.getInstrumentation().context.packageName
}
