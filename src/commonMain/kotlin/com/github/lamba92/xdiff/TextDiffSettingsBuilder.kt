package com.github.lamba92.xdiff

/**
 * A builder class for constructing instances of [TextDiffSettings].
 *
 * This class allows configuration of both computation and emission settings for generating text diffs.
 * It provides a fluent API to define settings for how differences between texts are computed
 * and how those differences are emitted (formatted).
 *
 * The builder maintains an internal [DiffComputationSettingsBuilder] for configuring computation-specific options
 * and an [DiffEmissionSettingsBuilder] for emission-specific options.
 *
 * - [DiffComputationSettingsBuilder] provides methods to set parameters like ignoring whitespace, defining diff anchors,
 * or specifying patterns of text to ignore during diff generation.
 * - [DiffEmissionSettingsBuilder] provides methods to control formatting parameters such as the number of context lines
 * included around changes and flags that influence output content.
 *
 * The [build] function is used to generate a fully configured [TextDiffSettings] object containing
 * the defined computation and emission settings.
 */
public class TextDiffSettingsBuilder {
    private val computation = DiffComputationSettingsBuilder()
    private val emission = DiffEmissionSettingsBuilder()

    /**
     * Configures the computation-specific settings for generating text diffs.
     */
    public fun computation(block: DiffComputationSettingsBuilder.() -> Unit) {
        computation.apply(block)
    }

    /**
     * Configures the emission-specific settings for generating text diffs.
     */
    public fun emission(block: DiffEmissionSettingsBuilder.() -> Unit) {
        emission.apply(block)
    }

    public fun build(): TextDiffSettings =
        TextDiffSettings(
            computation = computation.build(),
            emission = emission.build(),
        )
}

/**
 * Builder class for constructing instances of [DiffComputationSettings].
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
public class DiffComputationSettingsBuilder {
    public var useMinimal: Boolean =
        DiffComputationSettings.DEFAULT.useMinimal

    public var ignoreAllWhitespace: Boolean =
        DiffComputationSettings.DEFAULT.ignoreAllWhitespace

    public var ignoreWhitespaceChange: Boolean =
        DiffComputationSettings.DEFAULT.ignoreWhitespaceChange

    public var ignoreWhitespaceAtEndOfLine: Boolean =
        DiffComputationSettings.DEFAULT.ignoreWhitespaceAtEndOfLine

    public var ignoreCarriageReturnAtEndOfLine: Boolean =
        DiffComputationSettings.DEFAULT.ignoreCarriageReturnAtEndOfLine

    public var ignoreBlankLines: Boolean =
        DiffComputationSettings.DEFAULT.ignoreBlankLines

    public var algorithm: DiffAlgorithm =
        DiffComputationSettings.DEFAULT.algorithm

    public var useIndentationHeuristic: Boolean =
        DiffComputationSettings.DEFAULT.useIndentationHeuristic

    public var ignoreRegex: List<String> =
        DiffComputationSettings.DEFAULT.ignoreRegex

    public var anchors: List<String> =
        DiffComputationSettings.DEFAULT.anchors

    public fun build(): DiffComputationSettings =
        DiffComputationSettings(
            useMinimal = useMinimal,
            ignoreAllWhitespace = ignoreAllWhitespace,
            ignoreWhitespaceChange = ignoreWhitespaceChange,
            ignoreWhitespaceAtEndOfLine = ignoreWhitespaceAtEndOfLine,
            ignoreCarriageReturnAtEndOfLine = ignoreCarriageReturnAtEndOfLine,
            ignoreBlankLines = ignoreBlankLines,
            algorithm = algorithm,
            useIndentationHeuristic = useIndentationHeuristic,
            ignoreRegex = ignoreRegex,
            anchors = anchors,
        )
}

/**
 * Builder class for constructing instances of [DiffEmissionSettings].
 *
 * @property contextLines Number of lines of context to include around each hunk of changes.
 * @property interHunkContextLines Number of lines of context to include between hunks.
 * @property emitFunctionNames Include function names in the diff output.
 * @property suppressHunkHeaders Suppress headers for each hunk in the diff output.
 * @property emitFunctionContext Emit additional context around function names.
 */
public class DiffEmissionSettingsBuilder {
    public var contextLines: Int =
        DiffEmissionSettings.DEFAULT.contextLinesCount

    public var interHunkContextLines: Int =
        DiffEmissionSettings.DEFAULT.interHunkContextLinesCount

    public var emitFunctionNames: Boolean =
        DiffEmissionSettings.DEFAULT.emitFunctionNames

    public var suppressHunkHeaders: Boolean =
        DiffEmissionSettings.DEFAULT.suppressHunkHeaders

    public var emitFunctionContext: Boolean =
        DiffEmissionSettings.DEFAULT.emitFunctionContext

    public fun build(): DiffEmissionSettings =
        DiffEmissionSettings(
            contextLinesCount = contextLines,
            interHunkContextLinesCount = interHunkContextLines,
            emitFunctionNames = emitFunctionNames,
            suppressHunkHeaders = suppressHunkHeaders,
            emitFunctionContext = emitFunctionContext,
        )
}
