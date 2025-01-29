package com.github.lamba92.xdiff.tests

import com.github.lamba92.xdiff.Hunk
import com.github.lamba92.xdiff.Line
import com.github.lamba92.xdiff.TextDiff
import com.github.lamba92.xdiff.mergeTextDiff
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextDiffMergeTests {
    @Test
    fun testMergeTextDiffClean() {
        val target = """
            Hello, world!
            this is a test
            of the diff algorithm
            another line
            """.trimIndent()

        val cleanDiff = TextDiff(
            listOf(
                Hunk(
                    sourceStart = 3,
                    sourceLength = 2,
                    targetStart = 3,
                    targetLength = 3,
                    lines = listOf(
                        Line("of the diff algorithm\n", Line.Type.Context),
                        Line("new line\n", Line.Type.Addition),
                        Line("another line", Line.Type.Context),
                    ),
                ),
            ),
        )

        val cleanResult = mergeTextDiff(target, cleanDiff)
        assertTrue(cleanResult.conflicts.isEmpty())

        val resultLines = cleanResult.content.lines()
        assertEquals(5, resultLines.size, "Result should have exactly 5 lines")
        assertEquals("Hello, world!", resultLines[0], "Line 1 should be unchanged")
        assertEquals("this is a test", resultLines[1], "Line 2 should be unchanged")
        assertEquals("of the diff algorithm", resultLines[2], "Line 3 should be context line")
        assertEquals("new line", resultLines[3], "Line 4 should be added line")
        assertEquals("another line", resultLines[4], "Line 5 should be context line")
    }

    @Test
    fun testMergeTextDiffWithConflicts() {
        val target = """
            Hello, world!
            this is a test
            of the diff algorithm
            another line
            """.trimIndent()

        val conflictDiff = TextDiff(
            listOf(
                Hunk(
                    sourceStart = 3,
                    sourceLength = 2,
                    targetStart = 3,
                    targetLength = 2,
                    lines = listOf(
                        Line("of the diff algorithm\n", Line.Type.Context),
                        Line("another line", Line.Type.Deletion),
                        Line("modified line", Line.Type.Addition),
                    ),
                ),
            ),
        )

        val conflictResult = mergeTextDiff(target, conflictDiff)
        assertEquals(1, conflictResult.conflicts.size)

        val conflict = conflictResult.conflicts.first()
        assertEquals(3, conflict.start)

        val conflictLines = conflict.content.lines()
        println("[DEBUG_LOG] Conflict lines (${conflictLines.size}):")
        conflictLines.forEachIndexed { index, line ->
            println("[DEBUG_LOG] $index: '$line'")
        }
        assertEquals(5, conflictLines.size, "Conflict should have 5 lines")
        assertEquals("<<<<<<< ours", conflictLines[0], "First line should be opening marker")
        assertEquals("another line", conflictLines[1], "Second line should be original content")
        assertEquals("=======", conflictLines[2], "Third line should be separator")
        assertEquals("modified line", conflictLines[3], "Fourth line should be modified content")
        assertEquals(">>>>>>> theirs", conflictLines[4], "Fifth line should be closing marker")

        val resultLines = conflictResult.content.lines()
        println("[DEBUG_LOG] Result lines (${resultLines.size}):")
        resultLines.forEachIndexed { index, line ->
            println("[DEBUG_LOG] $index: '$line'")
        }
        assertEquals(6, resultLines.size, "Result should have 6 lines")
        assertEquals("Hello, world!", resultLines[0], "Line 1 should be unchanged")
        assertEquals("this is a test", resultLines[1], "Line 2 should be unchanged")
        assertEquals("of the diff algorithm", resultLines[2], "Line 3 should be context line")
        assertEquals("<<<<<<< ours", resultLines[3], "Line 4 should be conflict start")
        assertTrue(resultLines[4] in listOf("another line", "modified line"), "Line 5 should be one of the conflicting lines")
        assertTrue(">>>>>>> theirs" in resultLines, "Result should contain conflict end marker")
    }

    @Test
    fun testMergeTextDiffWithCustomMarkerSize() {
        val target = """
            Hello, world!
            this is a test
            of the diff algorithm
            another line
            """.trimIndent()

        val conflictDiff = TextDiff(
            listOf(
                Hunk(
                    sourceStart = 3,
                    sourceLength = 2,
                    targetStart = 3,
                    targetLength = 2,
                    lines = listOf(
                        Line("of the diff algorithm\n", Line.Type.Context),
                        Line("another line", Line.Type.Deletion),
                        Line("modified line", Line.Type.Addition),
                    ),
                ),
            ),
        )

        val markerSize = 5
        val conflictResult = mergeTextDiff(target, conflictDiff, markerSize)
        val conflict = conflictResult.conflicts.first()

        assertEquals(3, conflict.start, "Conflict should start at line 3")

        val conflictLines = conflict.content.lines()
        assertEquals(5, conflictLines.size, "Conflict should have 5 lines")
        assertEquals("<".repeat(markerSize) + " ours", conflictLines[0], "First line should be opening marker with size 5")
        assertEquals("another line", conflictLines[1], "Second line should be original content")
        assertEquals("=".repeat(markerSize), conflictLines[2], "Third line should be separator with size 5")
        assertEquals("modified line", conflictLines[3], "Fourth line should be modified content")
        assertEquals(">".repeat(markerSize) + " theirs", conflictLines[4], "Fifth line should be closing marker with size 5")
    }

    @Test
    fun testMergeTextDiffWithMultipleHunks() {
        val target = """
            Hello, world!
            this is a test
            of the diff algorithm
            middle line
            another line
            final line
            """.trimIndent()

        val diff = TextDiff(
            listOf(
                Hunk(
                    sourceStart = 2,
                    sourceLength = 1,
                    targetStart = 2,
                    targetLength = 2,
                    lines = listOf(
                        Line("this is a test", Line.Type.Deletion),
                        Line("this is an updated test", Line.Type.Addition),
                    ),
                ),
                Hunk(
                    sourceStart = 5,
                    sourceLength = 2,
                    targetStart = 5,
                    targetLength = 2,
                    lines = listOf(
                        Line("another line\n", Line.Type.Context),
                        Line("final line", Line.Type.Deletion),
                        Line("modified final line", Line.Type.Addition),
                    ),
                ),
            ),
        )

        val result = mergeTextDiff(target, diff)
        assertEquals(2, result.conflicts.size, "Should have exactly 2 conflicts")

        // Check first conflict
        val firstConflict = result.conflicts[0]
        assertEquals(2, firstConflict.start, "First conflict should start at line 2")
        val firstConflictLines = firstConflict.content.lines()
        assertEquals(5, firstConflictLines.size, "First conflict should have 5 lines")
        assertEquals("<<<<<<< ours", firstConflictLines[0], "First line should be opening marker")
        assertEquals("this is a test", firstConflictLines[1], "Second line should be original content")
        assertEquals("=======", firstConflictLines[2], "Third line should be separator")
        assertEquals("this is an updated test", firstConflictLines[3], "Fourth line should be modified content")
        assertEquals(">>>>>>> theirs", firstConflictLines[4], "Fifth line should be closing marker")

        // Check second conflict
        val secondConflict = result.conflicts[1]
        assertEquals(5, secondConflict.start, "Second conflict should start at line 5")
        val secondConflictLines = secondConflict.content.lines()
        assertEquals(5, secondConflictLines.size, "Second conflict should have 5 lines")
        assertEquals("<<<<<<< ours", secondConflictLines[0], "First line should be opening marker")
        assertEquals("final line", secondConflictLines[1], "Second line should be original content")
        assertEquals("=======", secondConflictLines[2], "Third line should be separator")
        assertEquals("modified final line", secondConflictLines[3], "Fourth line should be modified content")
        assertEquals(">>>>>>> theirs", secondConflictLines[4], "Fifth line should be closing marker")

        // Check the complete result content
        val resultLines = result.content.lines()
        assertEquals("Hello, world!", resultLines[0], "Line 1 should be unchanged")
        assertTrue(resultLines.contains("of the diff algorithm"), "Result should contain unchanged line")
        assertTrue(resultLines.contains("middle line"), "Result should contain unchanged line")
        assertTrue(resultLines.contains("another line"), "Result should contain context line")
    }
}
