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
    private var isValid = false
    private val changes: MutableList<Change> = mutableListOf<Change>()

    /**
     * Adds a change to the list of changes and updates the validity status of the hunk.
     *
     * @param change The change to be added. It represents a modification, addition, or deletion in the diff.
     */
    public fun addChange(change: Change) {
        changes.add(change)
        if (change.type == Change.Type.Addition || change.type == Change.Type.Deletion) {
            isValid = true
        }
    }

    /**
     * Adds an addition change to the hunk with the given content.
     *
     * @param content The content to be added as an addition.
     */
    public fun addAddition(content: String) {
        addChange(Change(content, Change.Type.Addition))
    }

    /**
     * Adds a deletion change to the hunk with the specified content.
     *
     * A deletion represents the removal of content from the target compared to the source.
     *
     * @param content The content to be marked as deleted.
     */
    public fun addDeletion(content: String) {
        addChange(Change(content, Change.Type.Deletion))
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
        addChange(Change(content, Change.Type.Context))
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
            changes = changes.toList(),
        )
    }
}

/**
 * Builds a new instance of [Hunk] using the provided configuration block.
 *
 * This method utilizes the [HunkBuilder] to incrementally construct a hunk based on
 * the changes and metadata applied in the given block. The resulting [Hunk] object
 * represents a contiguous block of changes in a diff, including additions, deletions,
 * and context lines.
 */
public fun buildHunk(block: HunkBuilder.() -> Unit): Hunk = HunkBuilder().apply(block).build()
