package com.github.lamba92.xdiff

public fun computeTextDiff(
    source: String,
    target: String,
    logging: ((String) -> Unit)? = null,
    settings: TextDiffSettingsBuilder.() -> Unit,
): TextDiff =
    computeTextDiff(
        source = source,
        target = target,
        logging = logging,
        settings = textDiffSettings(settings),
    )

public fun computeTextDiff(
    source: String,
    target: String,
    logging: ((String) -> Unit)? = null,
    settings: TextDiffSettings = TextDiffSettings.DEFAULT,
): TextDiff {
    // Normalize input strings first
    fun normalizeString(input: String): String {
        logging?.invoke("[DEBUG_LOG] Normalizing string: ${input.encodeToByteArray().contentToString()}")

        // Detect the original line ending style
        val originalEnding =
            when {
                input.contains("\r\n") -> "\r\n"
                input.contains("\r") -> "\r"
                else -> "\n"
            }
        logging?.invoke("[DEBUG_LOG] Original line ending: ${originalEnding.encodeToByteArray().contentToString()}")

        // Split into lines while preserving line endings
        val lines = mutableListOf<String>()
        var pos = 0
        while (pos < input.length) {
            val lineStart = pos
            while (pos < input.length && input[pos] != '\r' && input[pos] != '\n') {
                pos++
            }
            val lineContent = input.substring(lineStart, pos)

            // Handle line endings
            if (pos < input.length) {
                if (input[pos] == '\r' && pos + 1 < input.length && input[pos + 1] == '\n') {
                    lines.add(lineContent + "\r\n")
                    pos += 2
                } else {
                    lines.add(lineContent + input[pos])
                    pos++
                }
            } else if (lineContent.isNotEmpty()) {
                lines.add(lineContent)
            }
        }

        logging?.invoke("[DEBUG_LOG] Split into lines: ${lines.map { it.trimEnd() }}")

        // Process each line
        val processedLines = lines.map { line ->
            var processed = line
            val lineEnding = when {
                line.endsWith("\r\n") -> "\r\n"
                line.endsWith("\r") || line.endsWith("\n") -> line.last().toString()
                else -> ""
            }

            // Remove line ending for processing
            processed = processed.removeSuffix("\r\n").removeSuffix("\r").removeSuffix("\n")

            // Handle whitespace based on mode
            processed = when (settings.whitespaceMode) {
                WhitespaceMode.IGNORE_ALL -> processed.replace(Regex("\\s+"), "")
                WhitespaceMode.IGNORE_CHANGE -> processed.replace(Regex("\\s+"), " ").trim()
                WhitespaceMode.IGNORE_AT_EOL -> processed.trimEnd()
                WhitespaceMode.ALL -> processed
            }

            // Add back the original line ending unless it's the last line without an ending
            if (lineEnding.isNotEmpty() || lines.last() != line) {
                processed += if (settings.ignoreCarriageReturnAtEndOfLine) "\n" else lineEnding
            }
            processed
        }

        val result = processedLines.joinToString("")
        logging?.invoke("[DEBUG_LOG] Final result: ${result.encodeToByteArray().contentToString()}")
        return result
    }

    val normalizedSource = normalizeString(source)
    val normalizedTarget = normalizeString(target)

    // Handle empty file cases after normalization
    if (normalizedSource.isEmpty() && normalizedTarget.isEmpty()) {
        return TextDiff(emptyList())
    }

    // Split into lines, normalize each line, and filter out empty trailing lines
    fun splitLines(text: String): List<String> {
        // First, find all line ending positions
        val lineEndingPositions = mutableListOf<Pair<Int, String>>()
        var pos = 0
        while (pos < text.length) {
            when {
                pos < text.length - 1 && text[pos] == '\r' && text[pos + 1] == '\n' -> {
                    lineEndingPositions.add(pos to "\r\n")
                    pos += 2
                }

                text[pos] == '\r' || text[pos] == '\n' -> {
                    lineEndingPositions.add(pos to text[pos].toString())
                    pos += 1
                }

                else -> pos += 1
            }
        }

        // Split into lines preserving the original line endings
        val lines = mutableListOf<String>()
        var start = 0
        for ((endPos, ending) in lineEndingPositions) {
            lines.add(text.substring(start, endPos) + ending)
            start = endPos + ending.length
        }
        if (start < text.length) {
            lines.add(text.substring(start))
        }

        logging?.invoke(
            "[DEBUG_LOG] Initial split lines with endings: ${
                lines.map {
                    it.encodeToByteArray().contentToString()
                }
            }",
        )

        // Remove trailing empty lines but keep empty lines in the middle
        var lastNonEmpty = lines.size - 1
        while (lastNonEmpty >= 0 && lines[lastNonEmpty].trim('\r', '\n').isEmpty()) {
            lastNonEmpty--
        }
        val result = lines.take(lastNonEmpty + 1)
        logging?.invoke(
            "[DEBUG_LOG] Final lines after trimming: ${
                result.map {
                    it.encodeToByteArray().contentToString()
                }
            }",
        )
        return result
    }

    val sourceLines = splitLines(normalizedSource)
    val targetLines = splitLines(normalizedTarget)

    logging?.invoke("[DEBUG_LOG] Source lines: $sourceLines")
    logging?.invoke("[DEBUG_LOG] Target lines: $targetLines")
    logging?.invoke("[DEBUG_LOG] Lines equal? ${sourceLines == targetLines}")
    if (sourceLines != targetLines) {
        logging?.invoke("[DEBUG_LOG] Line-by-line comparison:")
        sourceLines.zip(targetLines).forEachIndexed { index, (s, t) ->
            if (s != t) {
                logging?.invoke("[DEBUG_LOG]   Line $index differs:")
                logging?.invoke("[DEBUG_LOG]   Source: ${s.encodeToByteArray().contentToString()}")
                logging?.invoke("[DEBUG_LOG]   Target: ${t.encodeToByteArray().contentToString()}")
            }
        }
    }

    // If lines are equal after normalization, return empty diff
    if (sourceLines == targetLines) {
        return TextDiff(emptyList())
    }

    if (source.isEmpty()) {
        val builder =
            HunkBuilder(
                sourceStart = 1,
                targetStart = 1,
                sourceLength = 0,
                targetLength = targetLines.size,
            )
        targetLines.forEach { builder.addAddition(it) }
        return TextDiff(listOf(builder.build()))
    }

    if (target.isEmpty()) {
        val builder =
            HunkBuilder(
                sourceStart = 1,
                targetStart = 1,
                sourceLength = sourceLines.size,
                targetLength = 0,
            )
        sourceLines.forEach { builder.addDeletion(it) }
        return TextDiff(listOf(builder.build()))
    }

    // Helper functions for line comparison
    fun normalizeForComparison(line: String): String {
        val content = line.removeSuffix("\r\n").removeSuffix("\r").removeSuffix("\n")
        return if (settings.ignoreCarriageReturnAtEndOfLine) {
            content + "\n"
        } else {
            content + (when {
                line.endsWith("\r\n") -> "\r\n"
                line.endsWith("\r") || line.endsWith("\n") -> line.last().toString()
                else -> ""
            })
        }
    }

    fun linesEqual(line1: String, line2: String): Boolean {
        val normalized1 = normalizeForComparison(line1)
        val normalized2 = normalizeForComparison(line2)
        return normalized1 == normalized2
    }

    // Initialize variables for Myers algorithm
    val n = sourceLines.size
    val m = targetLines.size

    val max = n + m
    val size = 2 * max + 1
    val forward = IntArray(size) { 0 }

    // Store endpoints for path reconstruction
    val snakes = mutableListOf<Triple<Int, Int, Int>>()

    // Main loop of Myers algorithm
    var d = 0
    mainLoop@ while (d <= max) {
        // Forward pass
        for (k in -d..d step 2) {
            val kIndex = k + max
            var x =
                if (k == -d || (k != d && forward[kIndex - 1] < forward[kIndex + 1])) {
                    forward[kIndex + 1]
                } else {
                    forward[kIndex - 1] + 1
                }

            if (x < 0) x = 0
            var y = x - k
            if (y < 0) y = 0

            // Follow diagonal
            var startX = x
            var startY = y
            while (x < n && y < m && linesEqual(sourceLines[x], targetLines[y])) {
                x++
                y++
            }

            // Record snake if it contains significant content
            val snakeLength = x - startX
            if (snakeLength > 0) {
                // Check if this snake contains non-empty lines or is long enough
                val hasSignificantContent = (startX until x).any { sourceLines[it].isNotBlank() }
                if (hasSignificantContent || snakeLength >= 3) {
                    snakes.add(Triple(startX, startY, snakeLength))
                }
            }

            forward[kIndex] = x

            if (x >= n && y >= m) {
                break@mainLoop
            }
        }

        d++
    }

    // Build hunks from the recorded snakes
    val hunks = mutableListOf<Hunk>()
    var currentHunk: HunkBuilder? = null
    var sourcePos = 0
    var targetPos = 0
    var currentHunkSize = 0
    var currentContextLines = 0

    fun finishHunk(
        hunk: HunkBuilder,
        sourceEnd: Int,
        targetEnd: Int,
    ) {
        // Only finish the hunk if it has actual changes
        if (!hunk.isValid) {
            return
        }
        val sourceStart = hunk.sourceStart ?: 1
        val targetStart = hunk.targetStart ?: 1
        hunk.sourceStart = sourceStart
        hunk.targetStart = targetStart
        hunk.sourceLength = sourceEnd - sourceStart + 1
        hunk.targetLength = targetEnd - targetStart + 1
        hunks.add(hunk.build())
    }

    for (i in snakes.indices) {
        val (startX, startY, length) = snakes[i]

        // If there are changes before this snake
        if (sourcePos < startX || targetPos < startY) {
            if (currentHunk == null) {
                currentHunk =
                    HunkBuilder(
                        sourceStart = sourcePos + 1,
                        targetStart = targetPos + 1,
                        sourceLength = startX - sourcePos,
                        targetLength = startY - targetPos,
                    )
                currentHunkSize = 0
            }

            // Add deletions
            while (sourcePos < startX) {
                currentHunk.addDeletion(sourceLines[sourcePos])
                sourcePos++
                currentHunkSize++
            }

            // Add additions
            while (targetPos < startY) {
                currentHunk.addAddition(targetLines[targetPos])
                targetPos++
                currentHunkSize++
            }
        }

        // Add context lines from the snake
        if (length > 0) {
            if (currentHunk == null) {
                currentHunk =
                    HunkBuilder(
                        sourceStart = sourcePos + 1,
                        targetStart = targetPos + 1,
                        sourceLength = length,
                        targetLength = length,
                    )
                currentHunkSize = 0
            }

            // Look ahead to see if there are more changes coming soon
            val nextChangeDistance =
                if (i + 1 < snakes.size) {
                    val (nextX, nextY, _) = snakes[i + 1]
                    if (settings.ignoreBlankLines) {
                        // Count only non-blank lines when calculating distance
                        val nonBlankSourceLines = (sourcePos until nextX).count { sourceLines[it].isNotBlank() }
                        val nonBlankTargetLines = (targetPos until nextY).count { targetLines[it].isNotBlank() }
                        minOf(nonBlankSourceLines, nonBlankTargetLines)
                    } else {
                        minOf(nextX - sourcePos, nextY - targetPos)
                    }
                } else {
                    Int.MAX_VALUE
                }
            logging?.invoke("[DEBUG_LOG] Next change distance: $nextChangeDistance")

            // Determine if we should start a new hunk
            val minDistanceForSplit =
                if (settings.ignoreBlankLines) {
                    // When ignoring blank lines, be more lenient about merging hunks
                    settings.contextLinesCount * 2 + 1
                } else if (settings.contextLinesCount <= 2) {
                    // For small context, split more aggressively
                    settings.contextLinesCount + 1
                } else {
                    // For larger context, ensure enough space for context on both sides
                    settings.contextLinesCount * 2
                }

            val shouldStartNewHunk = nextChangeDistance > minDistanceForSplit && currentHunk.isValid

            logging?.invoke(
                buildString {
                    append("[DEBUG_LOG] Should start new hunk: ")
                    append(shouldStartNewHunk)
                    append(" (distance: ")
                    append(nextChangeDistance)
                    append(", min required: ")
                    append(minDistanceForSplit)
                    append(", context: ")
                    append(settings.contextLinesCount)
                    append(")")
                },
            )

            // If we need to start a new hunk, finish the current one first
            if (shouldStartNewHunk && currentHunk.isValid == true) {
                finishHunk(currentHunk, sourcePos - 1, targetPos - 1)
                currentHunk = null
                currentContextLines = 0
                currentHunkSize = 0
            }

            // Add context lines, but limit them based on distance to next change
            val maxContextToAdd =
                if (nextChangeDistance == Int.MAX_VALUE) {
                    settings.contextLinesCount
                } else if (currentHunk?.isValid == true) {
                    // If we're in a valid hunk, we need to leave room for context on both sides
                    minOf(settings.contextLinesCount - currentContextLines, (nextChangeDistance - 1) / 2)
                } else {
                    // For a new hunk, limit context to ensure proper separation
                    minOf(settings.contextLinesCount, (nextChangeDistance - 1) / 2)
                }
            logging?.invoke(
                buildString {
                    append("[DEBUG_LOG] Max context to add: ")
                    append(maxContextToAdd)
                    append(" (requested: ")
                    append(settings.contextLinesCount)
                    append(", distance: ")
                    append(nextChangeDistance)
                    append(")")
                },
            )

            val contextToAdd = minOf(length, maxContextToAdd)
            if (contextToAdd > 0) {
                // Only create a new hunk if we have changes to add
                if (currentHunk == null && length > contextToAdd) {
                    currentHunk =
                        HunkBuilder(
                            sourceStart = sourcePos + 1,
                            targetStart = targetPos + 1,
                            sourceLength = 0,
                            targetLength = 0,
                        )
                    currentContextLines = 0
                    currentHunkSize = 0
                }

                // Add context lines if we have a hunk
                if (currentHunk != null) {
                    repeat(contextToAdd) {
                        currentHunk.addContext(sourceLines[sourcePos])
                        sourcePos++
                        targetPos++
                        currentHunkSize++
                        currentContextLines++
                    }
                } else {
                    // Skip context lines if we don't have a hunk yet
                    sourcePos += contextToAdd
                    targetPos += contextToAdd
                }
            }

            // Skip remaining lines if any
            if (contextToAdd < length) {
                val linesToSkip = length - contextToAdd
                sourcePos += linesToSkip
                targetPos += linesToSkip
                logging?.invoke("[DEBUG_LOG] Skipping $linesToSkip lines")
            }

            // Check if we should finish the hunk due to empty lines
            val hasEmptyLines =
                !settings.ignoreBlankLines && (sourcePos - contextToAdd until sourcePos).any { sourceLines[it].isEmpty() }
            if (hasEmptyLines && currentHunk?.isValid == true) {
                logging?.invoke("[DEBUG_LOG] Finishing hunk due to empty lines")
                finishHunk(currentHunk, sourcePos - 1, targetPos - 1)
                currentHunk = null
                currentHunkSize = 0
                currentContextLines = 0
            }
        }
    }

    // Handle remaining lines
    if (sourcePos < sourceLines.size || targetPos < targetLines.size) {
        if (currentHunk == null) {
            currentHunk =
                HunkBuilder(
                    sourceStart = sourcePos + 1,
                    targetStart = targetPos + 1,
                    sourceLength = sourceLines.size - sourcePos,
                    targetLength = targetLines.size - targetPos,
                )
            currentHunkSize = 0
        }

        while (sourcePos < sourceLines.size) {
            currentHunk.addDeletion(sourceLines[sourcePos])
            sourcePos++
            currentHunkSize++
        }

        while (targetPos < targetLines.size) {
            currentHunk.addAddition(targetLines[targetPos])
            targetPos++
            currentHunkSize++
        }
    }

    // Add the last hunk if there is one and it has changes
    currentHunk?.let {
        if (it.isValid) {
            // Adjust the lengths based on actual content
            it.sourceLength = sourcePos - (it.sourceStart ?: 1) + 1
            it.targetLength = targetPos - (it.targetStart ?: 1) + 1
            hunks.add(it.build())
        }
    }

    return TextDiff(hunks)
}
