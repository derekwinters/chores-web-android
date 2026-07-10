package com.derekwinters.chores.ui.theme

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Iteration 3 of the design-token rollout (derekwinters/chores-web-docs#11, this repo's #23):
 * a lightweight lint-style guard that keeps swept design literals from creeping back into the
 * UI layer. It walks the `ui/` source tree on disk (JVM test, no instrumentation) and fails on:
 *
 *  - hardcoded color literals (`Color(0x...)`) outside the theme-layer allowlist — colors must
 *    come from `MaterialTheme.colorScheme`, `LocalThemeOption`, or the DesignTokens artifact;
 *  - raw `tween(300)` motion durations — use `DesignTokens.Motion.DURATION_LG`.
 *
 * A full detekt/konsist rule set (including `.dp`/`.sp` literal bans with allowlists) is out of
 * scope for this iteration and tracked by the guardrail item of #23 / Iteration 4's #24.
 */
class SweepGuardTest {

    /** Theme-layer files allowed to construct colors directly (token plumbing, hex parsing). */
    private val colorLiteralAllowlist = setOf("Tokens.kt", "ChoresTheme.kt", "ColorHex.kt")

    private val colorLiteralPattern = Regex("""Color\(\s*0x""")
    private val rawTweenPattern = Regex("""tween\(\s*300\s*\)""")

    @Test
    fun `no hardcoded Color literals in ui outside the theme allowlist`() {
        val offenders = uiSourceFiles()
            .filter { it.name !in colorLiteralAllowlist }
            .flatMap { file -> matchingLines(file, colorLiteralPattern) }
        if (offenders.isNotEmpty()) {
            fail(
                "Hardcoded Color(0x...) literals found — use MaterialTheme.colorScheme / " +
                    "DesignTokens instead (see issue #23):\n" + offenders.joinToString("\n")
            )
        }
    }

    @Test
    fun `no raw tween(300) durations in ui`() {
        val offenders = uiSourceFiles().flatMap { file -> matchingLines(file, rawTweenPattern) }
        if (offenders.isNotEmpty()) {
            fail(
                "Raw tween(300) found — use tween(DesignTokens.Motion.DURATION_LG) instead " +
                    "(see issue #23):\n" + offenders.joinToString("\n")
            )
        }
    }

    private fun uiSourceFiles(): List<File> {
        val files = uiSourceRoot().walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("Expected to find UI sources under ${uiSourceRoot()}", files.isNotEmpty())
        return files
    }

    /**
     * Gradle runs JVM unit tests with the module directory (`app/`) as the working directory,
     * but tolerate a repo-root working directory too so the test isn't runner-sensitive.
     */
    private fun uiSourceRoot(): File {
        val relative = "src/main/java/com/derekwinters/chores/ui"
        return listOf(File(relative), File("app/$relative"))
            .firstOrNull(File::isDirectory)
            ?: error("Could not locate the ui/ source tree from ${File(".").absolutePath}")
    }

    private fun matchingLines(file: File, pattern: Regex): List<String> =
        file.readLines().mapIndexedNotNull { index, line ->
            if (pattern.containsMatchIn(line)) "${file.path}:${index + 1}: ${line.trim()}" else null
        }
}
