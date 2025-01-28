package com.github.lamba92.xdiff

import com.github.lamba92.xdiff.jvm.LibXDiff
import com.github.lamba92.xdiff.jvm.LibXDiff.xdemitconf_t
import com.github.lamba92.xdiff.jvm.LibXDiff.xpparam_t
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference

public actual fun computeTextDiff(
    source: String,
    target: String,
    settings: TextDiffSettings,
): TextDiff = TextDiff(imperativeTextDiff(source, target, settings))

/**
 * Computes the textual differences (diff) between a source string and a target string
 * and returns the result as a list of hunks, which encapsulate the changes.
 *
 * The method leverages the `LibXDiff` library to efficiently identify differences
 * using specified computation and emission settings. The result is grouped into
 * discrete hunks for easier interpretation of changes.
 *
 * @param source The original text to compare.
 * @param target The modified text to compare against the source.
 * @param settings Configuration settings that determine how the differences are computed
 * and formatted, such as ignoring whitespace or specifying the number of context lines.
 * @return A list of hunks representing the differences between `source` and `target`.
 * Each hunk contains details about changed lines and their respective positions.
 */
public fun imperativeTextDiff(
    source: String,
    target: String,
    settings: TextDiffSettings,
): List<Hunk> {
    val mmFile1 = LibXDiff.create_mmfile(source, source.length.toLong())
    val mmFile2 = LibXDiff.create_mmfile(target, target.length.toLong())

    val computationSettings = settings.computation.toNative()
    val emissionSettings = settings.emission.toNative()

    val result =
        LibXDiff.xdl_xdiff_simple(
            mf1 = mmFile1,
            mf2 = mmFile2,
            xpp = computationSettings,
            xemitconf = emissionSettings,
        ) ?: error("Error while computing diff")

    val hunks =
        buildList {
            val hunksCount = LibXDiff.xdiff_result_get_hunk_count(result)
            for (i in 0 until hunksCount) {
                val hunkPointer = LibXDiff.xdiff_result_get_hunk_at(result, i.toLong())

                val sourceStart = LibXDiff.xdiff_hunk_get_old_begin(hunkPointer).toInt()
                val sourceLength = LibXDiff.xdiff_hunk_get_old_count(hunkPointer).toInt()
                val targetStart = LibXDiff.xdiff_hunk_get_new_begin(hunkPointer).toInt()
                val targetLength = LibXDiff.xdiff_hunk_get_new_count(hunkPointer).toInt()

                val lineCount = LibXDiff.xdiff_hunk_get_line_count(hunkPointer)

                val hunk =
                    buildHunk {
                        this.sourceStart = sourceStart
                        this.sourceLength = sourceLength
                        this.targetStart = targetStart
                        this.targetLength = targetLength

                        for (j in 0 until lineCount) {
                            val line =
                                LibXDiff.xdiff_hunk_get_line_at(hunkPointer, j.toLong())
                                    ?: error("Invalid line index $j for hunk $i")
                            println(line)
                            addChangeFromString(line)
                        }
                    }

                add(hunk)
            }
        }

    // Cleanup
    LibXDiff.xdiff_result_destroy(result)
    LibXDiff.destroy_mmfile(mmFile1)
    LibXDiff.destroy_mmfile(mmFile2)
    LibXDiff.destroy_xpparam(computationSettings)
    LibXDiff.destroy_xdemitconf(emissionSettings)

    return hunks
}

private fun DiffComputationSettings.toNative(): xpparam_t {
    var flags =
        when (algorithm) {
            DiffAlgorithm.MYERS -> 0L
            DiffAlgorithm.PATIENCE -> LibXDiff.XDF_PATIENCE_DIFF.toLong()
            DiffAlgorithm.HISTOGRAM -> LibXDiff.XDF_HISTOGRAM_DIFF.toLong()
        }

    if (useMinimal) flags = flags or LibXDiff.XDF_NEED_MINIMAL.toLong()
    if (ignoreAllWhitespace) flags = flags or LibXDiff.XDF_IGNORE_WHITESPACE.toLong()
    if (ignoreWhitespaceChange) flags = flags or LibXDiff.XDF_IGNORE_WHITESPACE_CHANGE.toLong()
    if (ignoreWhitespaceAtEndOfLine) {
        flags =
            flags or LibXDiff.XDF_IGNORE_WHITESPACE_AT_EOL.toLong()
    }
    if (ignoreCarriageReturnAtEndOfLine) {
        flags =
            flags or LibXDiff.XDF_IGNORE_CR_AT_EOL.toLong()
    }
    if (ignoreBlankLines) flags = flags or LibXDiff.XDF_IGNORE_BLANK_LINES.toLong()

    val ignoreRegexPointers =
        ignoreRegex
            .map { LibXDiff.create_regex(it, 0).pointer }
            .takeIf { it.isNotEmpty() }
            ?.toPointerByReference()

    val anchorsPointers =
        anchors
            .map { LibXDiff.create_regex(it, 0).pointer }
            .takeIf { it.isNotEmpty() }
            ?.toPointerByReference()

    return LibXDiff.create_xpparam(
        flags = flags,
        ignoreRegex = ignoreRegexPointers,
        ignoreRegexCount = ignoreRegex.size.toLong(),
        anchors = anchorsPointers,
        anchorsCount = anchors.size.toLong(),
    )
}

private fun DiffEmissionSettings.toNative(): xdemitconf_t {
    var flags = 0L
    if (emitFunctionNames) flags = flags or LibXDiff.XDL_EMIT_FUNCNAMES.toLong()
    if (suppressHunkHeaders) flags = flags or LibXDiff.XDL_EMIT_NO_HUNK_HDR.toLong()
    if (emitFunctionContext) flags = flags or LibXDiff.XDL_EMIT_FUNCCONTEXT.toLong()

    return LibXDiff.create_xdemitconf(
        contextLength = contextLinesCount.toLong(),
        interhunkContextLength = interHunkContextLinesCount.toLong(),
        flags = flags,
        findFunction = null,
        findFunctionPrivate = null,
        hunkFunction = null,
    )
}

internal fun List<Pointer>.toPointerByReference(): PointerByReference? {
    if (isEmpty()) return null

    // Allocate memory for the pointer array (char**)
    val pointerArray = Memory(Native.POINTER_SIZE.toLong() * size)

    // Write each string pointer into the pointer array
    forEachIndexed { index, pointer ->
        pointerArray.setPointer(index.toLong() * Native.POINTER_SIZE.toLong(), pointer)
    }

    // Create a PointerByReference pointing to the pointer array
    return PointerByReference(pointerArray)
}
