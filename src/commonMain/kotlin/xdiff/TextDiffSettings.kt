package xdiff

/**
 * Configuration settings for computing and emitting text differences.
 *
 * This class separates settings into two categories:
 * - [computation] controls how the diff algorithm identifies differences.
 * - [emission] controls how the differences are formatted and presented.
 *
 * @property computation Settings for computing the diff.
 * @property emission Settings for emitting the diff.
 */
public data class TextDiffSettings(
    val computation: DiffComputationSettings = DiffComputationSettings(),
    val emission: DiffEmissionSettings = DiffEmissionSettings()
)

/**
 * Settings for computing the diff.
 *
 * These settings define how the algorithm identifies and organizes differences
 * between two inputs, including which lines to ignore and algorithm-specific flags.
 *
 * @property flags A combination of flags that control the diff algorithm's behavior.
 * Use [DiffComputationFlag] to specify behaviors like ignoring whitespace or using
 * a specific algorithm. Defaults to an empty set (no special behavior).
 *
 * @property anchors A list of "anchor" strings. Anchors are lines that must be aligned
 * between the two files, ensuring they appear in the same relative position in the diff output.
 * Defaults to an empty list (no anchors).
 *
 * @property ignoreRegex A list of regular expressions for lines to ignore during the diff computation.
 * Lines matching any of the provided regex patterns are excluded from the diff.
 * Defaults to an empty list (no lines ignored).
 */
public data class DiffComputationSettings(
    val flags: Set<DiffComputationFlag> = emptySet(),
    val anchors: List<String> = emptyList(),
    val ignoreRegex: List<String> = emptyList()
)

/**
 * Settings for emitting the diff.
 *
 * These settings define how the computed differences are grouped, formatted, and presented.
 *
 * @property contextLines Number of context lines to include around each hunk.
 * This provides additional surrounding lines for better readability. Defaults to 3.
 *
 * @property interHunkContextLines Number of context lines to include between consecutive hunks.
 * If two hunks are close enough that their surrounding context overlaps or almost overlaps,
 * they will be merged into a single hunk. Defaults to 0 (no merging).
 *
 * @property flags A combination of flags that control how the diff is formatted.
 * Use [DiffEmissionFlag] to specify behaviors like suppressing unchanged lines or enabling minimal output.
 * Defaults to an empty set (no special behavior).
 */
public data class DiffEmissionSettings(
    val contextLines: Int = 3,
    val interHunkContextLines: Int = 0,
    val flags: Set<DiffEmissionFlag> = emptySet()
)

/**
 * Flags for customizing the behavior of the diff computation phase.
 *
 * These flags influence how the algorithm identifies differences between the two inputs.
 * Pass them as a set to the [DiffComputationSettings.flags] field.
 *
 * @property value The integer bitmask representing the flag for native interop.
 */
public enum class DiffComputationFlag(public val value: UInt) {
    /** Ignores changes in whitespace, treating lines with different amounts of whitespace as identical. */
    IGNORE_WHITESPACE(0x1u),

    /** Uses a patience-based diff algorithm, which produces better results for highly disordered input. */
    PATIENCE_DIFF(0x2u),

    /** Uses a histogram-based diff algorithm for handling large inputs efficiently. */
    HISTOGRAM_DIFF(0x4u)
}

/**
 * Flags for customizing the behavior of the diff emission phase.
 *
 * These flags influence how the computed differences are presented in the output.
 * Pass them as a set to the [DiffEmissionSettings.flags] field.
 *
 * @property value The integer bitmask representing the flag for native interop.
 */
public enum class DiffEmissionFlag(public val value: UInt) {
    /** Suppresses unchanged lines in the diff output. */
    SUPPRESS_UNCHANGED_LINES(0x1u),

    /** Produces minimal output by merging hunks where possible. */
    MINIMAL_OUTPUT(0x2u)
}
