package com.framework.script.loader

import org.junit.Test
import org.junit.Assert.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

class ScriptLoaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testLoadAll() {
        val root = tempFolder.newFolder("scripts")
        
        // Create a dummy YAML file
        val yamlFile = File(root, "test.yaml")
        yamlFile.writeText("""
            name: "Test Suite"
            scenarios:
              - name: "Test Scenario"
                steps:
                  - action: CLICK
                    target: "btn"
        """.trimIndent())
        
        // Create a non-yaml file
        File(root, "readme.txt").writeText("hello")
        
        val loader = ScriptLoader(root)
        val suites = loader.loadAll()
        
        assertEquals(1, suites.size)
        assertEquals("Test Suite", suites[0].name)
    }

    @Test
    fun testLoadFile() {
        val root = tempFolder.newFolder("scripts")
        val yamlFile = File(root, "login.yaml")
        yamlFile.writeText("""
            name: "Login Suite"
            scenarios:
              - name: "Login"
                steps:
                  - action: CLICK
                    target: "login"
        """.trimIndent())
        
        val loader = ScriptLoader(root)
        val suite = loader.loadFile("login.yaml")
        
        assertEquals("Login Suite", suite.name)
    }

    @Test(expected = java.io.FileNotFoundException::class)
    fun testLoadFileNotFound() {
        val root = tempFolder.newFolder("scripts")
        val loader = ScriptLoader(root)
        loader.loadFile("missing.yaml")
    }
}
