package com.github.lamba92.xdiff.tests

import com.github.lamba92.xdiff.TextDiff
import kotlinx.serialization.json.Json
import kotlin.test.Test

class DiffTests {
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
        val json = Json { prettyPrint = true }
        println(json.encodeToString(TextDiff.compute(a, b)))
    }
}
