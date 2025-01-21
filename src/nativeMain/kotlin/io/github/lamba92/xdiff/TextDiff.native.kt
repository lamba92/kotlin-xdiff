package io.github.lamba92.xdiff

public actual fun computeTextDiff(
    source: String,
    target: String,
    settings: TextDiffSettings
): TextDiff {
    val hunks = buildList {
        rawTextDiff(source, target, settings) { add(it) }
    }
    return TextDiff(hunks)
}
