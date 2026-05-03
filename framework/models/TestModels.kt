package com.framework.script.models

/**
 * Top-level test suite parsed from a YAML file.
 */
data class TestSuite(
    val name: String,
    val description: String? = null,
    val scenarios: List<TestScenario> = emptyList()
)

/**
 * A single test case / scenario, maps to one JUnit test.
 */
data class TestScenario(
    val id: String,
    val name: String,
    val description: String? = null,
    val steps: List<TestStep> = emptyList()
)

/**
 * A single executable step within a scenario.
 */
data class TestStep(
    val action: StepAction,
    val target: String? = null,       // view id, text, content-desc
    val targetType: TargetType = TargetType.ID,
    val value: String? = null,        // text to type, scroll direction, etc.
    val timeout: Long = 5_000L,       // ms to wait for view
    val optional: Boolean = false     // if true, failure won't fail the test
)

enum class StepAction {
    CLICK,
    TYPE_TEXT,
    ASSERT_VISIBLE,
    ASSERT_NOT_VISIBLE,
    ASSERT_TEXT,
    SCROLL_DOWN,
    SCROLL_UP,
    SCROLL_TO_VIEW
}

enum class TargetType {
    ID,           // R.id.xxx  — just the name, no package
    TEXT,         // matches by text content
    CONTENT_DESC  // accessibility content description
}
