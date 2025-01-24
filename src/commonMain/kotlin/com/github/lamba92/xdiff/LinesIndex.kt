package com.github.lamba92.xdiff

/**
 * A utility class that provides line-based indexing for a given string.
 *
 * This class allows efficient access to specific lines of text within a source string
 * without the need to repeatedly traverse the string. The underlying implementation keeps
 * track of the starting positions of lines and retrieves lines on demand.
 *
 * @property source The input string to be indexed.
 *
 * @constructor Initializes the `LinesIndex` with the provided source string.
 *              Precomputes line start indices as needed during line retrieval.
 */
internal class LinesIndex(val source: String) {
    /**
     * Tracks the current index position during the scanning process of the source text.
     *
     * This variable is used internally to keep a reference to the position in the source
     * string while iterating or processing its lines. It ensures efficient retrieval
     * and manipulation of line data based on the current state.
     */
    private var scannerIndex = 0

    /**
     * A list used to store the starting indices of lines in the source string.
     *
     * This list serves as a precomputed index to optimize line retrieval operations.
     * Each element in the list corresponds to the starting character position of a
     * respective line in the source text. The indices are computed incrementally as
     * lines are accessed, enabling efficient navigation between lines without needing
     * to repeatedly traverse the source string.
     */
    private val index = mutableListOf<Int>()

    /**
     * Retrieves the line at the specified index from the source string.
     *
     * This method provides efficient access to a specific line in the source text by
     * leveraging a precomputed index of line starting positions. If the line index is
     * out of bounds or the source string is empty, `null` is returned.
     *
     * @param lineIndex The zero-based index of the line to retrieve.
     * @return The content of the specified line as a string, or `null` if the index
     *         is out of range or the source string is empty.
     */
    fun getLineAt(lineIndex: Int): String? {
        // If there's nothing to read, bail out early
        if (source.isEmpty()) return null

        // Try to expand lines until we can satisfy `lineIndex` or run out
        while (lineIndex + 1 >= index.size) {
            val nextLine = source.substringUntilLineBreak(scannerIndex) ?: break
            index.add(scannerIndex)
            // Advance the scanner index past the line plus one line break character
            scannerIndex += nextLine.length + 1
        }

        // If after trying to expand we still don't have enough lines, return null
        if (lineIndex >= index.size) return null

        // Otherwise, we can safely retrieve the line
        val start = index[lineIndex]
        val end = index.getOrNull(lineIndex + 1) ?: source.length
        return source.substring(start, end)
            .removeSuffix("\r")
            .removeSuffix("\n")
    }
}

/**
 * Retrieves the line at the specified index from the `LinesIndex`.
 *
 * This operator function provides convenient access to a line from the indexed source
 * string by leveraging the precomputed line start positions. It delegates the retrieval
 * operation to the `getLineAt` method.
 *
 * @param lineIndex The zero-based index of the line to retrieve.
 * @return The content of the specified line as a string, or `null` if the index
 *         is out of range or the source string is empty.
 */
internal operator fun LinesIndex.get(lineIndex: Int) = getLineAt(lineIndex)

/**
 * Returns the substring from [from] until the next newline or carriage return,
 * or until the end of the string if no further line break is found.
 *
 * If [from] >= length, returns null.
 */
internal fun String.substringUntilLineBreak(from: Int): String? {
    if (from >= length) return null

    var endIndex = from
    while (endIndex < length && this[endIndex] != '\n' && this[endIndex] != '\r') {
        endIndex++
    }

    // Return the chunk [from, endIndex) (excluding the line break itself).
    return substring(from, endIndex)
}
