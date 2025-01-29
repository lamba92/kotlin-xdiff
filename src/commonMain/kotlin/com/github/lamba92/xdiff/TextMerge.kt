package com.github.lamba92.xdiff

import kotlinx.serialization.Serializable

/**
 * Represents the result of a three-way merge operation.
 *
 * This class contains the merged content and information about any conflicts that occurred
 * during the merge process. The merge result can be either clean (no conflicts) or contain
 * conflicts that need manual resolution.
 *
 * @property content The merged content as a string, including conflict markers if conflicts exist.
 * @property conflicts A list of conflicts that occurred during the merge process.
 */
@Serializable
public data class TextMerge(
    val content: String,
    val conflicts: List<Conflict> = emptyList(),
) {
    /**
     * Represents a conflict in the merge result.
     *
     * @property start The start line number of the conflict in the merged content
     * @property end The end line number of the conflict in the merged content
     * @property content The conflicting content including conflict markers
     */
    @Serializable
    public data class Conflict(
        val start: Int,
        val end: Int,
        val content: String,
    )
}
