package com.github.lamba92.xdiff


public fun mergeTextDiff(
    target: String,
    diff: TextDiff,
    markerSize: Int = 7,
    ignoreBlankLines: Boolean = true,
): TextMerge {
    // Detect original line ending and newline status
    val lineEnding = when {
        target.contains("\r\n") -> "\r\n"
        target.contains("\r") -> "\r"
        else -> "\n"
    }
    val endsWithNewline = target.endsWith("\n") || target.endsWith("\r")

    fun normalizeLineEnding(line: String): String {
        // First detect if the original line had an ending
        val hasLineEnding = line.endsWith("\r\n") || line.endsWith("\r") || line.endsWith("\n")

        // Normalize all line endings to \n, preserving the ending status
        val normalized = line.replace("\r\n", "\n").replace("\r", "\n")

        return if (hasLineEnding && !normalized.endsWith("\n")) {
            normalized + "\n"
        } else if (!hasLineEnding && normalized.endsWith("\n")) {
            normalized.removeSuffix("\n")
        } else {
            normalized
        }
    }

    fun convertToOriginalLineEnding(line: String): String {
        // First normalize to \n
        val normalized = line.replace("\r\n", "\n").replace("\r", "\n")

        // Then convert back to original line ending if needed
        return if (lineEnding != "\n") {
            normalized.replace("\n", lineEnding)
        } else {
            normalized
        }
    }

    // Split lines and normalize line endings to \n
    val normalizedTarget = target.replace("\r\n", "\n").replace("\r", "\n")
    val lines = if (normalizedTarget.isEmpty()) {
        emptyList()
    } else {
        val splitLines = normalizedTarget.split("\n")
        if (splitLines.size == 1 && splitLines[0].isEmpty() && !endsWithNewline) {
            emptyList()
        } else {
            // Handle the case where the last line doesn't have a newline
            val lastIndex = if (endsWithNewline) splitLines.lastIndex else splitLines.lastIndex - 1
            splitLines.mapIndexed { index, line ->
                if (index <= lastIndex) {
                    line + "\n"
                } else {
                    line
                }
            }
        }
    }
    val sourceLines = lines

    val resultLines = mutableListOf<String>()
    val conflicts = mutableListOf<TextMerge.Conflict>()
    var currentLine = 1

    for (hunk in diff.hunks) {
        // Add lines before the hunk
        while (currentLine < hunk.sourceStart) {
            if (currentLine <= sourceLines.size) {
                resultLines.add(normalizeLineEnding(sourceLines[currentLine - 1]))
            }
            currentLine++
        }

        // Process the hunk
        val deletions = hunk.lines.filter { it.type == Line.Type.Deletion }
        val additions = hunk.lines.filter { it.type == Line.Type.Addition }

        // Compare lines with normalized line endings to avoid false conflicts
        val hasConflict = if (deletions.isNotEmpty() && additions.isNotEmpty()) {
            // Normalize both sets of lines
            val normalizedDeletions = deletions.map { 
                normalizeLineEnding(it.content).removeSuffix("\n")
            }
            val normalizedAdditions = additions.map { 
                normalizeLineEnding(it.content).removeSuffix("\n")
            }

            // Filter out empty lines if ignoring blank lines
            val cleanDeletions = if (ignoreBlankLines) normalizedDeletions.filter { it.isNotEmpty() } else normalizedDeletions
            val cleanAdditions = if (ignoreBlankLines) normalizedAdditions.filter { it.isNotEmpty() } else normalizedAdditions

            // Normalize content for comparison
            val sourceLines = deletions.map { normalizeLineEnding(it.content).removeSuffix("\n") }
            val targetLines = additions.map { normalizeLineEnding(it.content).removeSuffix("\n") }

            // Handle empty or whitespace-only content
            val sourceEmpty = sourceLines.all { it.trim().isEmpty() }
            val targetEmpty = targetLines.all { it.trim().isEmpty() }
            if (sourceEmpty && targetEmpty) {
                false  // No conflict if both sides are empty or whitespace-only
            } else if (ignoreBlankLines) {
                // When ignoring blank lines, compare trimmed non-empty content
                val cleanSource = sourceLines.map { it.trim() }.filter { it.isNotEmpty() }
                val cleanTarget = targetLines.map { it.trim() }.filter { it.isNotEmpty() }
                if (cleanSource.isEmpty() || cleanTarget.isEmpty()) {
                    false  // No conflict if either side is empty after cleaning
                } else if (cleanSource.size != cleanTarget.size) {
                    false  // No conflict if line counts differ after cleaning
                } else {
                    // Compare content ignoring whitespace and line endings
                    cleanSource.zip(cleanTarget).any { (s, t) -> s != t }
                }
            } else {
                // When preserving blank lines, compare normalized content
                if (sourceLines.size != targetLines.size) {
                    false  // No conflict if line counts differ
                } else {
                    sourceLines.zip(targetLines).any { (s, t) -> s != t }
                }
            }
        } else {
            false
        }

        if (hasConflict) {
            // Add context lines if present
            val contextLines = hunk.lines.takeWhile { it.type == Line.Type.Context }
            if (contextLines.isNotEmpty()) {
                resultLines.add(normalizeLineEnding(contextLines.first().content))
            }

            // Build conflict content for recording
            val marker = "=" .repeat(markerSize)
            val startMarker = "<".repeat(markerSize) + " ours"
            val endMarker = ">".repeat(markerSize) + " theirs"

            // Build conflict content with proper line endings
            val conflictLines = mutableListOf<String>()

            // Start marker
            conflictLines.add(startMarker)

            // Original content
            val originalContent = deletions.joinToString("") { line ->
                val normalizedLine = normalizeLineEnding(line.content)
                if (!normalizedLine.endsWith("\n")) normalizedLine + "\n" else normalizedLine
            }.removeSuffix("\n")
            conflictLines.add(originalContent)

            // Separator
            conflictLines.add(marker)

            // Modified content
            val modifiedContent = additions.joinToString("") { line ->
                val normalizedLine = normalizeLineEnding(line.content)
                if (!normalizedLine.endsWith("\n")) normalizedLine + "\n" else normalizedLine
            }.removeSuffix("\n")
            conflictLines.add(modifiedContent)

            // End marker
            conflictLines.add(endMarker)

            // Add conflict lines to result
            resultLines.addAll(conflictLines)

            // Join lines with newlines for conflict content
            val conflictContent = conflictLines.joinToString("\n")

            // Record the conflict
            conflicts.add(
                TextMerge.Conflict(
                    start = hunk.sourceStart,
                    end = hunk.sourceStart + conflictLines.size - 1,
                    content = conflictContent
                )
            )

            // Skip the lines we've processed
            currentLine += hunk.sourceLength
        } else {
            // No conflict, just apply the changes
            for (line in hunk.lines) {
                when (line.type) {
                    Line.Type.Context -> {
                        resultLines.add(normalizeLineEnding(line.content))
                        currentLine++
                    }
                    Line.Type.Addition -> {
                        resultLines.add(normalizeLineEnding(line.content))
                    }
                    Line.Type.Deletion -> {
                        currentLine++
                    }
                }
            }
        }
    }

    // Add remaining lines after the last hunk
    while (currentLine <= sourceLines.size) {
        resultLines.add(normalizeLineEnding(sourceLines[currentLine - 1]))
        currentLine++
    }

    // Join lines and convert to original line ending format
    val content = buildString {
        resultLines.forEachIndexed { index, line ->
            // Preserve the original line ending state
            val normalizedLine = normalizeLineEnding(line)

            // Add the line content
            append(normalizedLine)

            // Add line ending if needed
            val isLastLine = index == resultLines.lastIndex
            if (!isLastLine && !normalizedLine.endsWith("\n")) {
                append("\n")
            } else if (isLastLine && endsWithNewline && !normalizedLine.endsWith("\n")) {
                append("\n")
            }
        }
    }.let { normalizedContent ->
        convertToOriginalLineEnding(normalizedContent)
    }

    return TextMerge(
        content = content,
        conflicts = conflicts.map { conflict ->
            conflict.copy(
                content = convertToOriginalLineEnding(conflict.content)
            )
        }
    )
}
