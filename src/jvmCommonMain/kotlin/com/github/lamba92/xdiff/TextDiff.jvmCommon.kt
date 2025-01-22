package com.github.lamba92.xdiff

import com.github.lamba92.xdiff.jvm.rawTextDiff

public actual fun computeTextDiff(
    source: String,
    target: String,
    settings: TextDiffSettings,
): TextDiff {
    val hunks =
        buildList {
            rawTextDiff(source, target, settings) { add(it) }
        }
    return TextDiff(hunks)
}
