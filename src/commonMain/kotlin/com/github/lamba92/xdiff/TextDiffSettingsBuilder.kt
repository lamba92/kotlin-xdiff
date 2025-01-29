package com.github.lamba92.xdiff

public class TextDiffSettingsBuilder {
    public var whitespaceMode: WhitespaceMode =
        TextDiffSettings.DEFAULT.whitespaceMode

    public var ignoreCarriageReturnAtEndOfLine: Boolean =
        TextDiffSettings.DEFAULT.ignoreCarriageReturnAtEndOfLine

    public var ignoreBlankLines: Boolean =
        TextDiffSettings.DEFAULT.ignoreBlankLines

    public var contextLines: Int =
        TextDiffSettings.DEFAULT.contextLinesCount

    public fun build(): TextDiffSettings =
        TextDiffSettings(
            whitespaceMode = whitespaceMode,
            ignoreCarriageReturnAtEndOfLine = ignoreCarriageReturnAtEndOfLine,
            ignoreBlankLines = ignoreBlankLines,
            contextLinesCount = contextLines,
        )
}

/**
 * Constructs a new [TextDiffSettings] instance using the provided configuration block.
 *
 * This function allows customization of text diff settings for both computation and emission
 * by providing a lambda that configures a [TextDiffSettingsBuilder]. The builder defines
 * settings related to how differences between texts are calculated and formatted.
 *
 * @param block A lambda with receiver of type [TextDiffSettingsBuilder] used to configure
 * the computation and emission settings for the text diff.
 * @return A fully configured [TextDiffSettings] instance based on the settings provided
 * in the builder.
 */
public fun textDiffSettings(block: TextDiffSettingsBuilder.() -> Unit): TextDiffSettings = TextDiffSettingsBuilder().apply(block).build()
