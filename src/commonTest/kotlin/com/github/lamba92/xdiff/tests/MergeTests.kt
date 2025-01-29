package com.github.lamba92.xdiff.tests

import com.github.lamba92.xdiff.Hunk
import com.github.lamba92.xdiff.Line
import com.github.lamba92.xdiff.TextDiff
import com.github.lamba92.xdiff.TextMerge
import com.github.lamba92.xdiff.mergeTextDiff
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MergeTests {
    val prettyJson
        get() = Json { 
            prettyPrint = true
            serializersModule = kotlinx.serialization.modules.SerializersModule {
                contextual(TextMerge::class, TextMerge.serializer())
            }
        }

    @Test
    fun testMergeTextDiffClean() {
        val source =
            """
            Hello, world!
            this is a test
            of the diff algorithm
            another line
            """.trimIndent()

        val cleanDiff =
            TextDiff(
                listOf(
                    Hunk(
                        sourceStart = 3,
                        sourceLength = 2,
                        targetStart = 3,
                        targetLength = 3,
                        lines =
                            listOf(
                                Line("of the diff algorithm\n", Line.Type.Context),
                                Line("new line\n", Line.Type.Addition),
                                Line("another line", Line.Type.Context),
                            ),
                    ),
                ),
            )

        val cleanResult = mergeTextDiff(source, cleanDiff)
        assertTrue(cleanResult.conflicts.isEmpty())
        assertEquals(
            """
            Hello, world!
            this is a test
            of the diff algorithm
            new line
            another line
            """.trimIndent(),
            cleanResult.content,
        )

        println(prettyJson.encodeToString(TextMerge.serializer(), cleanResult))
    }

    @Test
    fun testMergeTextDiffWithConflicts() {
        val source =
            """
            Hello, world!
            this is a test
            of the diff algorithm
            another line
            """.trimIndent()

        val conflictDiff =
            TextDiff(
                listOf(
                    Hunk(
                        sourceStart = 3,
                        sourceLength = 2,
                        targetStart = 3,
                        targetLength = 2,
                        lines =
                            listOf(
                                Line("of the diff algorithm\n", Line.Type.Context),
                                Line("another line", Line.Type.Deletion),
                                Line("modified line", Line.Type.Addition),
                            ),
                    ),
                ),
            )

        val conflictResult = mergeTextDiff(source, conflictDiff)
        assertEquals(1, conflictResult.conflicts.size)

        val conflict = conflictResult.conflicts.first()
        assertEquals(3, conflict.start) // Conflict starts at line 3
        assertTrue(conflict.content.contains("<<<<<<< ours"))
        assertTrue(conflict.content.contains("======="))
        assertTrue(conflict.content.contains(">>>>>>> theirs"))
        assertTrue(conflict.content.contains("another line"))
        assertTrue(conflict.content.contains("modified line"))

        println(prettyJson.encodeToString(TextMerge.serializer(), conflictResult))
    }
}
