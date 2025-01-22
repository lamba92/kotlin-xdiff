package com.github.lamba92.xdiff

import kotlinx.serialization.Serializable

/**
 * Configuration settings for computing and emitting text differences.
 *
 * This class provides a comprehensive configuration for diff computation and emission,
 * separating concerns into two key areas:
 * - [computation]: Defines how the differences are identified between two text inputs.
 * - [emission]: Defines how the identified differences are formatted and presented.
 *
 * ### Diffing System Overview
 * The diffing system compares two text inputs and identifies changes such as additions,
 * deletions, and unchanged lines. It produces a structured output, typically grouped into
 * hunks, with optional context lines around the changes for clarity.
 *
 * - **Computation**: Handles how differences are determined (e.g., ignoring whitespace,
 *   aligning anchors, or applying regex filters to exclude lines).
 * - **Emission**: Determines how the differences are presented (e.g., number of context lines
 *   or suppression of unchanged lines).
 *
 * ### Example Inputs
 * **Input A:**
 * ```
 * Line 1
 * Line 2
 * Line 3
 * ```
 *
 * **Input B:**
 * ```
 * Line 1
 * Line 2 modified
 * Line 3
 * ```
 *
 * ### Example Output (Unified Diff Format)
 * ```
 * @@ -1,3 +1,3 @@
 * Line 1
 * -Line 2
 * +Line 2 modified
 * Line 3
 * ```
 *
 * @property computation Settings for computing the diff (e.g., ignoring whitespace or applying regex filters).
 * Defaults to [DiffComputationSettings.DEFAULT].
 *
 * @property emission Settings for formatting and presenting the diff output (e.g., context lines or minimal output).
 * Defaults to [DiffEmissionSettings.DEFAULT].
 */
@Serializable
public data class TextDiffSettings(
    val computation: DiffComputationSettings = DiffComputationSettings.DEFAULT,
    val emission: DiffEmissionSettings = DiffEmissionSettings.DEFAULT,
) {
    public companion object {
        /**
         * Default settings for text diffing, with no special computation or emission behaviors.
         */
        public val DEFAULT: TextDiffSettings = TextDiffSettings()
    }
}

/**
 * Settings for computing text differences.
 *
 * @property useMinimal Whether to use minimal diff computation.
 * @property ignoreAllWhitespace Ignore all whitespace changes.
 * @property ignoreWhitespaceChange Ignore changes in the amount of whitespace.
 * @property ignoreWhitespaceAtEndOfLine Ignore trailing whitespace changes.
 * @property ignoreCarriageReturnAtEndOfLine Ignore carriage return at the end of lines.
 * @property ignoreBlankLines Ignore blank lines in diff computation.
 * @property algorithm [DiffAlgorithm] to use for computing the diff.
 * @property useIndentationHeuristic Use indentation heuristics to improve diff quality.
 * @property ignoreRegex List of regex patterns for lines to ignore during diff computation.
 * @property anchors List of anchor strings to guide the alignment during diff computation.
 */
@Serializable
public data class DiffComputationSettings(
    val useMinimal: Boolean = false,
    val ignoreAllWhitespace: Boolean = false,
    val ignoreWhitespaceChange: Boolean = false,
    val ignoreWhitespaceAtEndOfLine: Boolean = false,
    val ignoreCarriageReturnAtEndOfLine: Boolean = false,
    val ignoreBlankLines: Boolean = false,
    val algorithm: DiffAlgorithm = DiffAlgorithm.MYERS,
    val useIndentationHeuristic: Boolean = false,
    val ignoreRegex: List<String> = emptyList(),
    val anchors: List<String> = emptyList(),
) {
    public companion object {
        public val DEFAULT: DiffComputationSettings = DiffComputationSettings()
    }
}

/**
 * Represents the algorithm to use for computing the diff.
 *
 * Each algorithm provides specific optimizations or behaviors and is mutually exclusive.
 */
@Serializable
public enum class DiffAlgorithm {
    /**
     * Uses the default diff algorithm for computing differences.
     * This is the default algorithm.
     */
    MYERS,

    /**
     * Uses a patience-based diff algorithm, optimized for clarity and minimal noise
     * when inputs are highly disordered.
     */
    PATIENCE,

    /**
     * Uses a histogram-based diff algorithm, designed for efficiently handling large inputs.
     */
    HISTOGRAM,
}

/**
 * Settings for emitting differences between text inputs.
 *
 * These settings control how differences are formatted and presented in the output.
 *
 * @property contextLinesCount Number of lines of context to include around each hunk of changes.
 * @property interHunkContextLinesCount Number of lines of context to include between hunks.
 * @property emitFunctionNames Include function names in the diff output.
 * @property suppressHunkHeaders Suppress headers for each hunk in the diff output.
 * @property emitFunctionContext Emit additional context around function names.
 */
@Serializable
public data class DiffEmissionSettings(
    val contextLinesCount: Int = 3,
    val interHunkContextLinesCount: Int = 0,
    val emitFunctionNames: Boolean = true,
    val suppressHunkHeaders: Boolean = false,
    val emitFunctionContext: Boolean = true,
) {
    public companion object {
        public val DEFAULT: DiffEmissionSettings = DiffEmissionSettings()
    }
}

/**
 * Parameters for merging text inputs.
 *
 * These parameters define how three-way merge operations are performed.
 *
 * @property computation Computation settings for the merge operation.
 * @property markerSize Size of the conflict marker used in the merge output.
 * @property level Simplification level for the merge (e.g., minimal, eager, zealous).
 * @property favor Conflict resolution strategy (e.g., favor ours, theirs, or union).
 * @property style Output style for the merge (e.g., diff3 or zealous diff3).
 * @property ancestor Label for the ancestor file.
 * @property file1Label Label for the first file.
 * @property file2Label Label for the second file.
 */
@Serializable
public data class MergeSettings(
    val computation: DiffComputationSettings = DiffComputationSettings(),
    val markerSize: Int = 7,
    val level: MergeSimplificationLevel = MergeSimplificationLevel.MINIMAL,
    val favor: MergeFavorMode = MergeFavorMode.OURS,
    val style: MergeOutputStyle = MergeOutputStyle.DIFF3,
    val ancestor: String? = null,
    val file1Label: String? = null,
    val file2Label: String? = null,
) {
    public companion object {
        public val DEFAULT: MergeSettings = MergeSettings()
    }
}

/**
 * Merge simplification levels.
 */
@Serializable
public enum class MergeSimplificationLevel {
    MINIMAL,
    EAGER,
    ZEALOUS,
    ZEALOUS_ALNUM,
}

/**
 * Merge favor modes.
 */
@Serializable
public enum class MergeFavorMode {
    OURS,
    THEIRS,
    UNION,
}

/**
 * Merge output styles.
 */
@Serializable
public enum class MergeOutputStyle {
    DIFF3,
    ZEALOUS_DIFF3,
}
