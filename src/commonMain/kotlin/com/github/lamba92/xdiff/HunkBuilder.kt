package com.github.lamba92.xdiff

/**
 * A builder class for constructing instances of [Hunk].
 *
 * A hunk represents a contiguous block of changes in a diff, and this class provides
 * methods to incrementally assemble such a block. The [HunkBuilder] includes metadata
 * about the hunk, such as source and target start positions, and allows adding various
 * types of changes (additions, deletions, and context lines).
 *
 * The builder ensures that the hunk meets specific validity criteria, requiring at
 * least one addition or deletion. The resulting [Hunk] object will encapsulate the
 * source and target ranges and the list of changes.
 *
 * @property sourceStart The starting line number in the source file for the hunk.
 * @property sourceLength The number of lines in the source file affected by the hunk.
 * @property targetStart The starting line number in the target file for the hunk.
 * @property targetLength The number of lines in the target file affected by the hunk.
 */
public class HunkBuilder(
    public var sourceStart: Int? = null,
    public var sourceLength: Int? = null,
    public var targetStart: Int? = null,
    public var targetLength: Int? = null,
) {
    public var isValid: Boolean = false
        private set
    private val lines: MutableList<Line> = mutableListOf()

    /**
     * Adds a change to the list of changes and updates the validity status of the hunk.
     *
     * @param line The change to be added. It represents a modification, addition, or deletion in the diff.
     */
    public fun addChange(line: Line) {
        lines.add(line.copy(content = line.content.removeNewLineSuffix()))
        if (line.type == Line.Type.Addition || line.type == Line.Type.Deletion) {
            isValid = true
        }
    }

    /**
     * Adds an addition change to the hunk with the given content.
     *
     * @param content The content to be added as an addition.
     */
    public fun addAddition(content: String) {
        addChange(Line(content.removeNewLineSuffix(), Line.Type.Addition))
    }

    /**
     * Adds a deletion change to the hunk with the specified content.
     *
     * A deletion represents the removal of content from the target compared to the source.
     *
     * @param content The content to be marked as deleted.
     */
    public fun addDeletion(content: String) {
        addChange(Line(content.removeNewLineSuffix(), Line.Type.Deletion))
    }

    /**
     * Adds a context change to the hunk with the specified content.
     *
     * A context change represents an unchanged line of content,
     * providing contextual information around additions or deletions
     * within a diff.
     *
     * @param content The content of the context line to be added.
     */
    public fun addContext(content: String) {
        addChange(Line(content.removeNewLineSuffix(), Line.Type.Context))
    }

    /**
     * Adds a line of change to the hunk, determining its type based on its prefix.
     *
     * The method interprets the provided string and delegates the processing to
     * appropriate methods for addition, deletion, or context changes.
     *
     * @param line The line of change to be processed. The prefix of the line determines its type:
     * - Lines starting with "+" are treated as additions.
     * - Lines starting with "-" are treated as deletions.
     * - Other lines are treated as context.
     */
    public fun addChangeFromString(line: String) {
        when {
            line.startsWith("+") -> addAddition(line.drop(1))
            line.startsWith("-") -> addDeletion(line.drop(1))
            else -> addContext(line.drop(1))
        }
    }

    public fun build(): Hunk {
        require(isValid) { "A hunk must contain at least one addition or deletion" }
        return Hunk(
            sourceStart = requireNotNull(sourceStart) { "sourceStart must be set" },
            sourceLength = requireNotNull(sourceLength) { "sourceLength must be set" },
            targetStart = requireNotNull(targetStart) { "targetStart must be set" },
            targetLength = requireNotNull(targetLength) { "targetLength must be set" },
            lines = lines.toList(),
        )
    }
}

/**
 * Removes a newline suffix from the string.
 *
 * This function checks if the string ends with a newline character (`\n`) or a carriage return
 * followed by a newline (`\r\n`) and removes that suffix, if present.
 * If the string has no newline suffix, it returns the string unchanged.
 *
 * @receiver The string from which the newline suffix should be removed.
 * @return A string with the newline suffix removed, if it was present.
 */
private fun String.removeNewLineSuffix() =
    when {
        endsWith("\n") -> dropLast(1)
        endsWith("\r\n") -> dropLast(2)
        else -> this
    }

/**
 * Builds a new instance of [Hunk] using the provided configuration block.
 *
 * This method uses the [HunkBuilder] to incrementally construct a hunk based on
 * the changes and metadata applied in the given block. The resulting [Hunk] object
 * represents a contiguous block of changes in a diff, including additions, deletions,
 * and context lines.
 */
public fun buildHunk(block: HunkBuilder.() -> Unit): Hunk = HunkBuilder().apply(block).build()
