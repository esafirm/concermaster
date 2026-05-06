package com.framework.script.parser

import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.io.FileInputStream

class YamlScriptParserTest {

    private val parser = YamlScriptParser()

    @Test
    fun testParseLoginYaml() {
        val paths = listOf("framework/scripts/login.yaml", "scripts/login.yaml", "../scripts/login.yaml")
        val file = paths.map { File(it) }.firstOrNull { it.exists() }
            ?: throw AssertionError("Login YAML not found. Current dir: ${File(".").absolutePath}")
        
        val suite = parser.parse(FileInputStream(file))
        
        assertEquals("Login Flow Suite", suite.name)
        assertTrue(suite.scenarios.isNotEmpty())
        
        val firstScenario = suite.scenarios.first()
        assertEquals("login_success", firstScenario.id)
        assertEquals("Successful login with valid credentials", firstScenario.name)
        assertTrue(firstScenario.steps.isNotEmpty())
    }

    @Test
    fun testParseCheckoutYaml() {
        val paths = listOf("framework/scripts/checkout.yaml", "scripts/checkout.yaml", "../scripts/checkout.yaml")
        val file = paths.map { File(it) }.firstOrNull { it.exists() }
            ?: throw AssertionError("Checkout YAML not found. Current dir: ${File(".").absolutePath}")
        
        val suite = parser.parse(FileInputStream(file))
        
        assertEquals("Product Checkout Suite", suite.name)
        assertTrue(suite.scenarios.isNotEmpty())
    }
}
