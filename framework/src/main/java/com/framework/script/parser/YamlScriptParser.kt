package com.framework.script.parser

import com.framework.script.models.*
import org.yaml.snakeyaml.Yaml
import java.io.InputStream

/**
 * Parses YAML test scripts into [TestSuite] objects.
 *
 * Expected YAML schema:
 *
 * ```yaml
 * name: "Login Flow Suite"
 * description: "Tests covering the login screen"
 * scenarios:
 *   - id: "login_success"
 *     name: "Successful login with valid credentials"
 *     steps:
 *       - action: TYPE_TEXT
 *         target: "et_username"
 *         value: "john@example.com"
 *       - action: TYPE_TEXT
 *         target: "et_password"
 *         value: "secret123"
 *       - action: CLICK
 *         target: "btn_login"
 *       - action: ASSERT_VISIBLE
 *         target: "tv_welcome"
 *       - action: ASSERT_TEXT
 *         target: "tv_welcome"
 *         value: "Welcome, John!"
 * ```
 */
class YamlScriptParser {

    private val yaml = Yaml()

    /**
     * Parse a YAML [InputStream] into a [TestSuite].
     * Throws [ScriptParseException] on malformed input.
     */
    @Suppress("UNCHECKED_CAST")
    fun parse(stream: InputStream): TestSuite {
        try {
            val root = yaml.load<Map<String, Any>>(stream)
                ?: throw ScriptParseException("Empty YAML file")

            val suiteName = root.requireString("name")
            val suiteDesc = root.optString("description")
            val rawScenarios = root["scenarios"] as? List<Map<String, Any>>
                ?: throw ScriptParseException("'scenarios' key is missing or not a list")

            val scenarios = rawScenarios.mapIndexed { index, raw ->
                parseScenario(raw, index)
            }

            return TestSuite(
                name = suiteName,
                description = suiteDesc,
                scenarios = scenarios
            )
        } catch (e: ScriptParseException) {
            throw e
        } catch (e: Exception) {
            throw ScriptParseException("Failed to parse YAML: ${e.message}", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseScenario(raw: Map<String, Any>, index: Int): TestScenario {
        val id = raw.optString("id") ?: "scenario_$index"
        val name = raw.requireString("name")
        val desc = raw.optString("description")
        val rawSteps = raw["steps"] as? List<Map<String, Any>>
            ?: throw ScriptParseException("Scenario '$name' has no steps or steps is not a list")

        val steps = rawSteps.mapIndexed { stepIndex, rawStep ->
            parseStep(rawStep, stepIndex, name)
        }

        return TestScenario(id = id, name = name, description = desc, steps = steps)
    }

    private fun parseStep(raw: Map<String, Any>, index: Int, scenarioName: String): TestStep {
        val actionRaw = raw.requireString("action")
        val action = runCatching { StepAction.valueOf(actionRaw.uppercase()) }.getOrElse {
            throw ScriptParseException(
                "Unknown action '$actionRaw' at step $index in scenario '$scenarioName'. " +
                "Valid actions: ${StepAction.values().joinToString()}"
            )
        }

        val targetTypeRaw = raw.optString("targetType") ?: "ID"
        val targetType = runCatching { TargetType.valueOf(targetTypeRaw.uppercase()) }.getOrElse {
            throw ScriptParseException(
                "Unknown targetType '$targetTypeRaw' at step $index. " +
                "Valid types: ${TargetType.values().joinToString()}"
            )
        }

        val timeout = when (val t = raw["timeout"]) {
            is Int -> t.toLong()
            is Long -> t
            is String -> t.toLong()
            else -> 5_000L
        }

        return TestStep(
            action = action,
            target = raw.optString("target"),
            targetType = targetType,
            value = raw.optString("value"),
            timeout = timeout,
            optional = raw["optional"] as? Boolean ?: false
        )
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun Map<String, Any>.requireString(key: String): String =
        (this[key] as? String)?.takeIf { it.isNotBlank() }
            ?: throw ScriptParseException("Required field '$key' is missing or blank")

    private fun Map<String, Any>.optString(key: String): String? =
        this[key] as? String
}

class ScriptParseException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
