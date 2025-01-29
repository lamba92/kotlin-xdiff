package com.github.lamba92.xdiff.tests

import com.github.lamba92.xdiff.Line
import com.github.lamba92.xdiff.TextDiff
import com.github.lamba92.xdiff.WhitespaceMode
import com.github.lamba92.xdiff.computeTextDiff
import com.github.lamba92.xdiff.toGitDiffString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffTests {
    companion object {
        val prettyJson
            get() = Json { prettyPrint = true }
    }

    @Test
    fun simpleText() {
        val a =
            """
            Hello, world!
            this is a test
            of the diff algorithm


            another line
            one more line
            3rd line
            4th line
            """.trimIndent()

        val b =
            """
            Hello, world!
            this is a test
            of the diff algorithm
            and it works!


            another line
            one more line
            A change!
            4th line
            """.trimIndent()

        val diff = computeTextDiff(a, b, logging = ::println)
        assertTrue(diff.hunks.isNotEmpty(), "Should detect changes")
        // Changes are grouped into a single hunk since they're close enough
        assertEquals(1, diff.hunks.size, "Changes should be grouped into a single hunk")
        // Verify that both changes are present in the hunk
        val changes = diff.hunks[0].lines
        assertTrue(changes.any { it.content.contains("and it works") }, "Should contain first change")
        assertTrue(changes.any { it.content.contains("A change") }, "Should contain second change")

        println(diff.hunks.joinToString("\n") { it.toGitDiffString() })
    }

    @Test
    fun emptyFiles() {
        val diff1 = computeTextDiff("", "")
        assertTrue(diff1.hunks.isEmpty())

        val diff2 = computeTextDiff("", "some content")
        assertEquals(1, diff2.hunks.size)
        assertEquals(1, diff2.hunks[0].lines.size)

        val diff3 = computeTextDiff("some content", "")
        assertEquals(1, diff3.hunks.size)
        assertEquals(1, diff3.hunks[0].lines.size)
    }

    @Test
    fun whitespaceHandling() {
        val a = "  line with spaces  "
        val b = "line with spaces"

        // Default behavior should detect whitespace changes
        val diffDefault = computeTextDiff(a, b)
        assertTrue(diffDefault.hunks.isNotEmpty())
        // With IGNORE_CHANGE, should not detect changes
        val diffIgnoreWs =
            computeTextDiff(a, b) {
                whitespaceMode = WhitespaceMode.IGNORE_CHANGE
            }
        assertTrue(diffIgnoreWs.hunks.isEmpty())

        // With IGNORE_AT_EOL, should detect leading but not trailing spaces
        val diffIgnoreEnd =
            computeTextDiff(a, b) {
                whitespaceMode = WhitespaceMode.IGNORE_AT_EOL
            }
        assertTrue(diffIgnoreEnd.hunks.isNotEmpty())

        // With IGNORE_ALL, should not detect any whitespace changes
        val diffIgnoreAll =
            computeTextDiff("a  b\tc", "abc") {
                whitespaceMode = WhitespaceMode.IGNORE_ALL
            }
        assertTrue(diffIgnoreAll.hunks.isEmpty())
    }

    @Test
    fun carriageReturnHandling() {
        val a = "line1\r\nline2\r\n"
        val b = "line1\nline2\n"

        println("[DEBUG_LOG] Source text bytes: ${a.encodeToByteArray().contentToString()}")
        println("[DEBUG_LOG] Target text bytes: ${b.encodeToByteArray().contentToString()}")

        // Default behavior should detect CR differences
        val diffDefault = TextDiff.compute(a, b)
        println("[DEBUG_LOG] Default diff hunks: ${diffDefault.hunks}")
        assertTrue(diffDefault.hunks.isNotEmpty(), "Default behavior should detect CR differences")

        // With ignoreCarriageReturnAtEndOfLine, should not detect CR differences
        val diffIgnoreCR =
            computeTextDiff(a, b) {
                ignoreCarriageReturnAtEndOfLine = true
            }
        println("[DEBUG_LOG] Ignore CR diff hunks: ${diffIgnoreCR.hunks}")
        assertTrue(
            diffIgnoreCR.hunks.isEmpty(),
            "With ignoreCarriageReturnAtEndOfLine, should not detect CR differences",
        )
    }

    @Test
    fun multipleChanges() {
        val a =
            """
            1
            2
            3
            4
            5
            6
            7
            8
            9
            10
            """.trimIndent()

        val b =
            """
            1
            2 modified
            3
            new line
            4 changed
            5
            6
            modified 7
            8
            9 changed
            10
            """.trimIndent()

        val diff = computeTextDiff(a, b)
        assertTrue(diff.hunks.isNotEmpty())
        // Should group nearby changes into single hunks
        assertTrue(diff.hunks.size < 5)
    }

    @Test
    fun testLargeFileWithManyChanges() {
        // Generate a large file with 1000 lines
        val baseLines = (1..1000).map { "Line $it" }
        val a = baseLines.joinToString("\n")

        // Create modified version with various changes throughout
        val modifiedLines = baseLines.toMutableList()
        // Add changes at start
        modifiedLines[0] = "Modified first line"
        modifiedLines[1] = "Added after first line"
        // Middle changes
        modifiedLines[500] = "Modified middle line"
        modifiedLines.add(501, "New line in middle")
        // End changes
        modifiedLines[999] = "Modified last line"
        modifiedLines.add("Added at end")

        val b = modifiedLines.joinToString("\n")

        val diff = computeTextDiff(a, b)
        assertTrue(diff.hunks.isNotEmpty())
        println(prettyJson.encodeToString(diff))
        // Should have multiple hunks due to changes being far apart
        assertTrue(diff.hunks.size >= 3)
    }

    @Test
    fun testComplexTextStructures() {
        val a =
            """
            ```kotlin
            fun example() {
                println("Hello")
                // Comment
            }
            ```

            # Heading 1
            ## Heading 2

            * List item 1
            * List item 2

            > Blockquote
            """.trimIndent()

        val b =
            """
            ```kotlin
            fun example() {
                println("Hello, World!")
                // Modified comment
                return Unit
            }
            ```

            # Modified Heading 1
            ## Heading 2

            * List item 1
            * Modified item 2
            * New item 3

            > Modified blockquote
            > Second line
            """.trimIndent()

        val diff = computeTextDiff(a, b)
        assertTrue(diff.hunks.isNotEmpty())
        // Verify changes in different structures
        assertTrue(
            diff.hunks.any { hunk ->
                hunk.lines.any { it.content.contains("Hello, World!") }
            },
        )
        assertTrue(
            diff.hunks.any { hunk ->
                hunk.lines.any { it.content.contains("Modified Heading 1") }
            },
        )
    }

    @Test
    fun testSpecialCharactersAndUnicode() {
        val a =
            """
            Special chars: !@#$%^&*()
            Unicode: 你好，世界
            Emojis: 😀 🌍 ⭐️
            Mixed: Hello 世界 🌍
            Tab\tand\tspaces    here
            """.trimIndent()

        val b =
            """
            Special chars: !@#$%^&*()[]{} 
            Unicode: 你好，世界！
            Emojis: 😀 🌍 ⭐️ 🎉
            Mixed: Hello World 世界 🌍
            Tab\tand\tmore\tspaces     here
            """.trimIndent()

        val diff = computeTextDiff(a, b)
        assertTrue(diff.hunks.isNotEmpty())
        // Verify proper handling of special characters
        assertTrue(
            diff.hunks.any { hunk ->
                hunk.lines.any { it.content.contains("世界！") }
            },
        )
    }

    @Test
    fun testContextLinesAndHunkSeparation() {
        val a =
            """
            1
            2
            3
            4
            5
            6
            7
            8
            9
            10
            11
            12
            13
            14
            15
            """.trimIndent()

        val b =
            """
            1
            2
            3 changed
            4
            5
            6
            7
            8
            9
            10 changed
            11
            12
            13
            14
            15
            """.trimIndent()

        // Test with default context (3 lines)
        val diffDefault = computeTextDiff(a, b, logging = { println("[DEBUG_LOG] $it") })
        println("[DEBUG_LOG] Default context hunks: ${diffDefault.hunks.joinToString("\n")}")
        // With 6 lines between changes and 3 lines context, hunks should be separate
        assertEquals(2, diffDefault.hunks.size, "Should create separate hunks with default context")
        diffDefault.hunks.forEach { hunk ->
            val contextLines = hunk.lines.count { it.type == Line.Type.Context }
            assertTrue(contextLines <= 6, "Each hunk should have at most 6 context lines (3 before + 3 after)")
        }

        // Test with 1 line context
        val diffSmallContext =
            computeTextDiff(a, b, logging = { println("[DEBUG_LOG] $it") }) {
                contextLines = 1
            }
        assertEquals(2, diffSmallContext.hunks.size, "Should create separate hunks with small context")
        diffSmallContext.hunks.forEach { hunk ->
            val contextLines = hunk.lines.count { it.type == Line.Type.Context }
            assertTrue(contextLines <= 2, "Each hunk should have at most 2 context lines (1 before + 1 after)")
        }

        // Test with large context that should merge hunks
        val diffLargeContext =
            computeTextDiff(a, b, logging = { println("[DEBUG_LOG] $it") }) {
                contextLines = 4
            }
        // With 6 lines between changes and 4 lines context on each side, hunks should merge
        assertEquals(1, diffLargeContext.hunks.size, "Should merge hunks with large context")
    }

    @Test
    fun testHunkMergingWithExactContextSize() {
        val a =
            """
            1
            2
            3
            4
            5
            6
            7
            8
            """.trimIndent()

        val b =
            """
            1
            2 changed
            3
            4
            5 changed
            6
            7 changed
            8
            """.trimIndent()

        // Test with context size that matches the gap between changes
        val diffExactContext =
            computeTextDiff(a, b, logging = { println("[DEBUG_LOG] $it") }) {
                contextLines = 2
            }
        assertEquals(1, diffExactContext.hunks.size, "Should create separate hunks for each change when context is small")
    }

    @Test
    fun testHunkSeparationWithBlankLines() {
        val aWithBlanks =
            """
            1
            2
            3

            4
            5
            6

            7
            8
            """.trimIndent()

        val bWithBlanks =
            """
            1
            2 changed
            3

            4
            5 changed
            6

            7 changed
            8
            """.trimIndent()

        val diffWithBlanks =
            computeTextDiff(aWithBlanks, bWithBlanks) {
                ignoreBlankLines = true
                contextLines = 1
            }
        assertTrue(diffWithBlanks.hunks.size == 1, "Should merge hunks with blank lines and small enough context")
    }
}
