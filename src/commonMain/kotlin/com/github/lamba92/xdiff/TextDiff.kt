package com.github.lamba92.xdiff

import kotlinx.serialization.Serializable

/**
 * Represents a single change in a diff, encapsulating the content of the change and its type.
 *
 * A diff typically consists of several changes, each representing an addition, deletion, or a line of unchanged content.
 * This class models such an individual change, detailing the associated content and its type.
 *
 * @property content The textual content of the change.
 * @property type The type of the change, specifying whether it is an addition, deletion, or context.
 */
@Serializable
public data class Line(
    val content: String,
    val type: Type,
) {
    /**
     * The type of change.
     */
    public enum class Type {
        /**
         * Represents an addition of content.
         */
        Addition,

        /**
         * Represents a deletion of content.
         */
        Deletion,

        /**
         * Represents an unchanged context line.
         */
        Context,
    }
}

/**
 * Represents a contiguous block of changes in a diff, known as a "hunk."
 *
 * In diff terminology, a **hunk** is a group of sequential changes surrounded by
 * lines of unchanged content (context). Hunks are used to segment large diffs into
 * more manageable parts. Each hunk includes:
 *
 * - The starting line in the source ([sourceStart]) and its length ([sourceLength]).
 * - The starting line in the target ([targetStart]) and its length ([targetLength]).
 * - A list of [lines] within the hunk.
 *
 * For example, a unified diff hunk may look like this:
 * ```
 * @@ -1,3 +1,3 @@
 * Line 1
 * -Line 2
 * +Line 2 modified
 * Line 3
 * ```
 *
 * @property sourceStart The starting line number in the source file for this hunk.
 * @property sourceLength The number of lines in the source file affected by this hunk.
 * @property targetStart The starting line number in the target file for this hunk.
 * @property targetLength The number of lines in the target file affected by this hunk.
 * @property lines The list of changes (additions, deletions, modifications) in this hunk.
 */
@Serializable
public data class Hunk(
    val sourceStart: Int,
    val sourceLength: Int,
    val targetStart: Int,
    val targetLength: Int,
    val lines: List<Line>,
)

/**
 * Represents the complete set of differences (diff) between two text files or strings.
 *
 * A **diff** is a comparison between a "source" (original) and "target" (modified)
 * input that highlights the differences. It is used extensively in version control
 * systems like Git to show what has been added, removed, or modified between commits.
 *
 * A diff consists of multiple [hunks], each of which represents a specific part of the
 * input where changes occurred. By organizing changes into hunks, diffs provide a
 * structured way to review and apply modifications.
 *
 * Example in a unified diff format:
 * ```
 * @@ -1,3 +1,3 @@
 * Line 1
 * -Line 2
 * +Line 2 modified
 * Line 3
 * ```
 *
 * @property hunks The list of hunks that collectively represent the diff.
 */
@Serializable
public data class TextDiff(
    val hunks: List<Hunk>,
) {
    public companion object {
        /**
         * Computes the differences (diff) between two text inputs.
         *
         * This method takes a source string and a target string, compares them, and produces a
         * structured representation of the differences in the form of a [TextDiff] object.
         *
         * @param source The original text input to compare.
         * @param target The modified text input to compare against the source.
         * @return A [TextDiff] object representing the differences between the source and
         * target strings.
         */
        public fun compute(
            source: String,
            target: String,
        ): TextDiff =
            computeTextDiff(
                source = source,
                target = target,
            )
    }
}

/**
 * Converts this hunk into a Git-style diff string representation.
 *
 * This function constructs a string that represents the hunk in the format typically
 * used by Git for unified diffs. The format includes the hunk header followed by the
 * changes, where:
 * - Lines starting with "+" indicate additions.
 * - Lines starting with "-" indicate deletions.
 * - Lines starting with a space indicate context/unchanged lines.
 *
 * Example output for a hunk could look like:
 * ```
 * @@ -1,3 +1,3 @@
 *  Line 1
 * -Removed line
 * +Added line
 *  Line 3
 * ```
 *
 * @return The Git-style diff string representation of the hunk.
 */
public fun Hunk.toGitDiffString(): String =
    buildString {
        appendLine("@@ -$sourceStart,$sourceLength +$targetStart,$targetLength @@")
        lines.forEach { change ->
            when (change.type) {
                Line.Type.Addition -> appendLine("+${change.content}")
                Line.Type.Deletion -> appendLine("-${change.content}")
                Line.Type.Context -> appendLine(" ${change.content}")
            }
        }
    }
