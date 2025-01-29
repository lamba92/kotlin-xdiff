package com.github.lamba92.xdiff

import kotlinx.serialization.Serializable

@Serializable
public data class TextDiffSettings(
    val whitespaceMode: WhitespaceMode = WhitespaceMode.ALL,
    val ignoreCarriageReturnAtEndOfLine: Boolean = false,
    val ignoreBlankLines: Boolean = false,
    val contextLinesCount: Int = 3,
) {
    public companion object {
        public val DEFAULT: TextDiffSettings = TextDiffSettings()
    }
}

/**
 * Specifies how whitespace should be handled during diff computation.
 */
@Serializable
public enum class WhitespaceMode {
    /**
     * All whitespace differences are considered significant.
     */
    ALL,

    /**
     * Ignore changes in the amount of whitespace (multiple spaces vs single space).
     */
    IGNORE_CHANGE,

    /**
     * Ignore whitespace at the end of lines.
     */
    IGNORE_AT_EOL,

    /**
     * Ignore all whitespace differences.
     */
    IGNORE_ALL,
}
