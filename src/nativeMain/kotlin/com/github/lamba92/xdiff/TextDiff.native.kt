@file:OptIn(UnsafeNumber::class)
@file:Suppress("FunctionName")

package com.github.lamba92.xdiff

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.toKString
import xdiff.mmfile_t
import xdiff.xdemitconf_t
import xdiff.xdiff_hunk_t
import xdiff.xpparam_t

public actual fun computeTextDiff(
    source: String,
    target: String,
    settings: TextDiffSettings,
): TextDiff = TextDiff(imperativeTextDiff(source, target, settings))

// the commonizer failed to commonize the following declarations
internal expect fun create_mmfile(
    text: String,
    length: Int,
): CPointer<mmfile_t>?

internal expect fun create_xpparam(
    flags: UInt,
    ignore_regex: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.CPointerVarOf<CPointer<platform.posix.regex_t>>>?,
    ignore_regex_count: UInt,
    anchors: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.CPointerVarOf<CPointer<kotlinx.cinterop.ByteVarOf<Byte>>>>?,
    anchors_count: UInt,
): CPointer<xdiff.s_xpparam>?

public expect fun create_xdemitconf(
    context_length: Int,
    interhunk_context_length: Int,
    flags: UInt,
): CPointer<xdiff.s_xdemitconf>?

internal expect fun xdiff_hunk_get_old_begin(hunk: CPointer<xdiff_hunk_t>): Int

internal expect fun xdiff_hunk_get_old_count(hunk: CPointer<xdiff_hunk_t>): Int

internal expect fun xdiff_hunk_get_new_begin(hunk: CPointer<xdiff_hunk_t>): Int

internal expect fun xdiff_hunk_get_new_count(hunk: CPointer<xdiff_hunk_t>): Int

/**
 * Computes the differences between two text inputs based on the provided settings.
 *
 * This function compares two texts line by line and identifies changes such as additions,
 * deletions, and unchanged lines. The results are grouped into "hunks," which represent
 * blocks of contiguous changes with optional lines of context for clarity. The behavior of
 * the computation and formatting is determined by the `TextDiffSettings` parameter.
 *
 * @param text1 The first input text (source) to be compared.
 * @param text2 The second input text (target) to be compared.
 * @param settings Configuration settings specifying how the differences should be computed
 * and formatted, as defined in the `TextDiffSettings` class.
 * @return A list of `Hunk` objects representing the grouped differences between the two inputs,
 * including information about changes, the starting positions, and lengths in both texts.
 */
public fun imperativeTextDiff(
    text1: String,
    text2: String,
    settings: TextDiffSettings,
): List<Hunk> =
    memScoped {
        val mmFile1 =
            create_mmfile(text1, text1.length.convert())
                ?: error("Error while creating mmfile for text1")
        val mmFile2 =
            create_mmfile(text2, text2.length.convert())
                ?: error("Error while creating mmfile for text2")

        val computationSettings = settings.computation.toNative(memScope)
        val emissionSettings = settings.emission.toNative()

        val result =
            xdiff.xdl_xdiff_simple(
                mf1 = mmFile1,
                mf2 = mmFile2,
                xpp = computationSettings,
                xecfg = emissionSettings,
            ) ?: error("Error while computing diff")

        var sourceIndex = 0
        var targetIndex = 0

        val text1Lines = LinesIndex(text1)
        val text2Lines = LinesIndex(text2)

        val results =
            buildList {
                val hunksCount = xdiff.xdiff_result_get_hunk_count(result).toLong()
                println("Hunks count: $hunksCount")
                for (i in 0 until hunksCount) {
                    val hunk =
                        buildHunk {
                            val hunk =
                                xdiff.xdiff_result_get_hunk_at(result, i.convert())
                                    ?: error("Invalid hunk index: $i")
                            sourceStart = xdiff_hunk_get_old_begin(hunk).toInt()
                            sourceLength = xdiff_hunk_get_old_count(hunk).toInt()
                            targetStart = xdiff_hunk_get_new_begin(hunk).toInt()
                            targetLength = xdiff_hunk_get_new_count(hunk).toInt()
                            val changesCount = xdiff.xdiff_hunk_get_line_count(hunk).toLong()
                            for (j in 0 until changesCount) {
                                val line =
                                    xdiff.xdiff_hunk_get_line_at(hunk, j.convert())
                                        ?.toKString()
                                        ?: error("Invalid line index $j for hunk $i")
                                when {
                                    line.startsWith("-") -> {
                                        val content =
                                            text1Lines[sourceIndex]
                                                ?: error("Invalid source index: $sourceIndex")
                                        addDeletion(content)
                                        sourceIndex++
                                    }

                                    line.startsWith("+") -> {
                                        val content =
                                            text2Lines[targetIndex]
                                                ?: error("Invalid target index: $targetIndex")
                                        addAddition(content)
                                        targetIndex++
                                    }

                                    else -> {
                                        val content =
                                            text1Lines[sourceIndex]
                                                ?: error("Invalid source index: $sourceIndex")
                                        addContext(content)
                                        sourceIndex++
                                        targetIndex++
                                    }
                                }
                            }
                        }
                    add(hunk)
                }
            }
        xdiff.xdiff_result_destroy(result)
        results
    }

private fun DiffComputationSettings.toNative(memScope: MemScope): CPointer<xpparam_t> {
    var flags =
        when (algorithm) {
            DiffAlgorithm.MYERS -> 0u
            DiffAlgorithm.PATIENCE -> xdiff.XDF_PATIENCE_DIFF.convert()
            DiffAlgorithm.HISTOGRAM -> xdiff.XDF_HISTOGRAM_DIFF.convert()
        }

    if (useMinimal) flags = flags or xdiff.XDF_NEED_MINIMAL.convert()
    if (ignoreAllWhitespace) flags = flags or xdiff.XDF_IGNORE_WHITESPACE.convert()
    if (ignoreWhitespaceChange) flags = flags or xdiff.XDF_IGNORE_WHITESPACE_CHANGE.convert()
    if (ignoreWhitespaceAtEndOfLine) flags = flags or xdiff.XDF_IGNORE_WHITESPACE_AT_EOL.convert()
    if (ignoreCarriageReturnAtEndOfLine) flags = flags or xdiff.XDF_IGNORE_CR_AT_EOL.convert()
    if (ignoreBlankLines) flags = flags or xdiff.XDF_IGNORE_BLANK_LINES.convert()

    val params =
        create_xpparam(
            flags = flags.convert(),
            ignore_regex =
                ignoreRegex
                    .takeIf { it.isNotEmpty() }
                    ?.map { xdiff.create_regex(it, 0) }
                    ?.toCValues(),
            ignore_regex_count = ignoreRegex.size.convert(),
            anchors =
                anchors
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.cstr.getPointer(memScope) }
                    ?.toCValues(),
            anchors_count = anchors.size.convert(),
        )

    if (params == null) {
        error("Error while creating xpparam_t")
    }

    return params
}

private fun DiffEmissionSettings.toNative(): CPointer<xdemitconf_t> {
    sequenceOf(1)
        .windowed(1)
    var flags = 0u
    if (emitFunctionNames) flags = flags or xdiff.XDL_EMIT_FUNCNAMES.convert()
    if (suppressHunkHeaders) flags = flags or xdiff.XDL_EMIT_NO_HUNK_HDR.convert()
    if (emitFunctionContext) flags = flags or xdiff.XDL_EMIT_FUNCCONTEXT.convert()

    val conf =
        create_xdemitconf(
            flags = flags.convert(),
            context_length = contextLinesCount.convert(),
            interhunk_context_length = interHunkContextLinesCount.convert(),
        )

    if (conf == null) {
        error("Error while creating xdemitconf_t")
    }

    return conf
}
