package com.github.lamba92.xdiff.tests

import com.github.lamba92.xdiff.computeTextDiff
import com.github.lamba92.xdiff.mergeTextDiff
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffRoundTripTests {
    @Test
    fun testSimpleRoundTrip() {
        val source = """
            Hello, world!
            This is a test
            of the diff algorithm
            """.trimIndent()

        val target = """
            Hello, world!
            This is an updated test
            of the diff algorithm
            And a new line
            """.trimIndent()

        // Calculate diff from source to target
        val diff = computeTextDiff(source, target, logging = { println("[DEBUG_LOG] $it") })
        
        // Apply diff back to source
        val result = mergeTextDiff(source, diff)
        
        // Verify no conflicts
        assertTrue(result.conflicts.isEmpty(), "Round-trip merge should not produce conflicts")
        
        // Verify the result matches the target
        assertEquals(target, result.content, "Round-trip result should match target text")
    }

    @Test
    fun testMultipleHunksRoundTrip() {
        val source = """
            First line
            Second line
            Third line
            Fourth line
            Fifth line
            Sixth line
            Seventh line
            """.trimIndent()

        val target = """
            Modified first line
            Second line
            Modified third line
            Fourth line
            New line here
            Fifth line
            Modified sixth line
            Seventh line
            """.trimIndent()

        val diff = computeTextDiff(source, target, logging = { println("[DEBUG_LOG] $it") })
        val result = mergeTextDiff(source, diff)

        assertTrue(result.conflicts.isEmpty(), "Round-trip merge should not produce conflicts")
        assertEquals(target, result.content, "Round-trip result should match target text")
    }

    @Test
    fun testEmptyAndWhitespaceRoundTrip() {
        // Test empty to non-empty
        val emptySource = ""
        val nonEmptyTarget = "Some content\nWith multiple lines"
        
        var diff = computeTextDiff(emptySource, nonEmptyTarget)
        var result = mergeTextDiff(emptySource, diff)
        
        assertTrue(result.conflicts.isEmpty(), "Empty to non-empty round-trip should not produce conflicts")
        assertEquals(nonEmptyTarget, result.content, "Empty to non-empty round-trip should match target")

        // Test whitespace changes
        val whitespaceSource = "  Line  with  spaces  \n\tTabbed line\n"
        val whitespaceTarget = "Line with spaces\nTabbed line  \n"
        
        diff = computeTextDiff(whitespaceSource, whitespaceTarget)
        result = mergeTextDiff(whitespaceSource, diff)
        
        assertTrue(result.conflicts.isEmpty(), "Whitespace round-trip should not produce conflicts")
        assertEquals(whitespaceTarget, result.content, "Whitespace round-trip should match target")
    }

    @Test
    fun testComplexContentRoundTrip() {
        val source = """
            # Heading 1
            
            Some paragraph with *markdown* and **formatting**.
            - List item 1
            - List item 2
            
            ```kotlin
            fun example() {
                println("Hello")
            }
            ```
            """.trimIndent()

        val target = """
            # Modified Heading 1
            
            Some paragraph with *updated markdown* and **formatting**.
            - Modified list item 1
            - List item 2
            - New list item
            
            ```kotlin
            fun example() {
                println("Hello, World!")
                return Unit
            }
            ```
            
            Additional content
            """.trimIndent()

        val diff = computeTextDiff(source, target, logging = { println("[DEBUG_LOG] $it") })
        val result = mergeTextDiff(source, diff)

        assertTrue(result.conflicts.isEmpty(), "Complex content round-trip should not produce conflicts")
        assertEquals(target, result.content, "Complex content round-trip should match target")
    }
}