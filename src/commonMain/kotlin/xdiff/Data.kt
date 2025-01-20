package xdiff


/**
 * Represents a single change in a diff, encapsulating the content of the change and its type.
 *
 * A diff typically consists of several changes, each representing an addition, deletion, or a line of unchanged content.
 * This class models such an individual change, detailing the associated content and its type.
 *
 * @property content The textual content of the change.
 * @property type The type of the change, specifying whether it is an addition, deletion, or context.
 */
public data class Change(
    val content: String,
    val type: Type
) {
    /**
     * The type of change.
     */
    public enum class Type {
        /**
         * Represents an addition of content.
         */
        Addition,

        /**
         * Represents a deletion of content.
         */
        Deletion,

        /**
         * Represents an unchanged context line.
         */
        Context
    }
}

/**
 * Represents a contiguous block of changes in a diff, known as a "hunk."
 *
 * In diff terminology, a **hunk** is a group of sequential changes surrounded by
 * lines of unchanged content (context). Hunks are used to segment large diffs into
 * more manageable parts. Each hunk includes:
 *
 * - The starting line in the source ([sourceStart]) and its length ([sourceLength]).
 * - The starting line in the target ([targetStart]) and its length ([targetLength]).
 * - A list of [changes] within the hunk.
 *
 * For example, a unified diff hunk may look like this:
 * ```
 * @@ -1,3 +1,3 @@
 * Line 1
 * -Line 2
 * +Line 2 modified
 * Line 3
 * ```
 *
 * @property sourceStart The starting line number in the source file for this hunk.
 * @property sourceLength The number of lines in the source file affected by this hunk.
 * @property targetStart The starting line number in the target file for this hunk.
 * @property targetLength The number of lines in the target file affected by this hunk.
 * @property changes The list of changes (additions, deletions, modifications) in this hunk.
 */
public data class Hunk(
    val sourceStart: Int,
    val sourceLength: Int,
    val targetStart: Int,
    val targetLength: Int,
    val changes: List<Change>
)

/**
 * Represents the complete set of differences (diff) between two text files or strings.
 *
 * A **diff** is a comparison between a "source" (original) and "target" (modified)
 * input that highlights the differences. It is used extensively in version control
 * systems like Git to show what has been added, removed, or modified between commits.
 *
 * A diff consists of multiple [hunks], each of which represents a specific part of the
 * input where changes occurred. By organizing changes into hunks, diffs provide a
 * structured way to review and apply modifications.
 *
 * Example in a unified diff format:
 * ```
 * @@ -1,3 +1,3 @@
 * Line 1
 * -Line 2
 * +Line 2 modified
 * Line 3
 * ```
 *
 * @property hunks The list of hunks that collectively represent the diff.
 */
public data class Diff(
    val hunks: List<Hunk>
)

public class HunkBuilder(
    public var sourceStart: Int? = null,
    public var sourceLength: Int? = null,
    public var targetStart: Int? = null,
    public var targetLength: Int? = null,
) {
    private var isValid = false
    private val changes: MutableList<Change> = mutableListOf<Change>()

    public fun addChange(change: Change) {
        changes.add(change)
        if (change.type == Change.Type.Addition || change.type == Change.Type.Deletion) {
            isValid = true
        }
    }

    public fun addAddition(content: String) {
        addChange(Change(content, Change.Type.Addition))
    }

    public fun addDeletion(content: String) {
        addChange(Change(content, Change.Type.Deletion))
    }

    public fun addContext(content: String) {
        addChange(Change(content, Change.Type.Context))
    }

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
            changes = changes.toList()
        )
    }
}