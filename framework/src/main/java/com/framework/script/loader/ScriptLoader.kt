package com.framework.script.loader

import android.os.Environment
import com.framework.script.models.TestSuite
import com.framework.script.parser.YamlScriptParser
import java.io.File
import java.io.FileNotFoundException

/**
 * Loads [TestSuite] objects from YAML files on external storage.
 *
 * Default root directory: /sdcard/test-scripts/
 * Override via instrumentation argument: -e scriptDir /sdcard/my-scripts
 *
 * Usage:
 *   val loader = ScriptLoader()
 *   val suites = loader.loadAll()              // all .yaml/.yml in root
 *   val suite  = loader.loadFile("login.yaml") // specific file
 */
class ScriptLoader(
    private val rootDir: File = defaultScriptDir()
) {

    private val parser = YamlScriptParser()

    /**
     * Load every .yaml / .yml file found directly inside [rootDir].
     */
    fun loadAll(): List<TestSuite> {
        ensureDirExists()
        val files = rootDir.listFiles { f ->
            f.isFile && (f.extension.equals("yaml", true) || f.extension.equals("yml", true))
        } ?: return emptyList()

        return files.sortedBy { it.name }.map { file ->
            loadFile(file)
        }
    }

    /**
     * Load a single file by name relative to [rootDir].
     */
    fun loadFile(fileName: String): TestSuite =
        loadFile(File(rootDir, fileName))

    /**
     * Load a single [File] directly.
     */
    fun loadFile(file: File): TestSuite {
        if (!file.exists()) throw FileNotFoundException("Script not found: ${file.absolutePath}")
        return file.inputStream().use { parser.parse(it) }
    }

    /**
     * Verify the script directory exists and is readable.
     * Creates it if missing (so the tester gets a clear path to drop files into).
     */
    fun ensureDirExists() {
        if (!rootDir.exists()) {
            rootDir.mkdirs()
            throw ScriptDirNotFoundException(
                "Script directory did not exist — created it at: ${rootDir.absolutePath}\n" +
                "Push your .yaml files there and re-run the test.\n" +
                "  adb push login.yaml ${rootDir.absolutePath}/"
            )
        }
        if (!rootDir.isDirectory) {
            throw IllegalStateException("${rootDir.absolutePath} is a file, expected a directory")
        }
        if (!rootDir.canRead()) {
            throw IllegalStateException(
                "Cannot read ${rootDir.absolutePath} — " +
                "grant READ_EXTERNAL_STORAGE permission to the test APK"
            )
        }
    }

    companion object {
        private const val DEFAULT_SUBDIR = "test-scripts"

        fun defaultScriptDir(): File =
            File(Environment.getExternalStorageDirectory(), DEFAULT_SUBDIR)

        /**
         * Create a loader pointing at a custom path, e.g. from an
         * instrumentation argument:
         *   -e scriptDir /sdcard/automation/suite-a
         */
        fun fromPath(path: String): ScriptLoader =
            ScriptLoader(rootDir = File(path))
    }
}

class ScriptDirNotFoundException(message: String) : RuntimeException(message)
