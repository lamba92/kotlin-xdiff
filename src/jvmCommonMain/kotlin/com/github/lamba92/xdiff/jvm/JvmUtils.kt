package com.github.lamba92.xdiff.jvm

import com.github.lamba92.xdiff.DiffAlgorithm
import com.github.lamba92.xdiff.DiffComputationSettings
import com.github.lamba92.xdiff.DiffEmissionSettings
import com.github.lamba92.xdiff.Hunk
import com.github.lamba92.xdiff.HunkBuilder
import com.github.lamba92.xdiff.TextDiffSettings
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import java.nio.charset.Charset

internal fun rawTextDiff(
    text1: String,
    text2: String,
    settings: TextDiffSettings = TextDiffSettings(),
    outputs: (Hunk) -> Unit,
) {
    val mmfile1 = LibXDiff.create_mmfile(text1, text1.length.toLong())
    val mmfile2 = LibXDiff.create_mmfile(text2, text2.length.toLong())

    val computationSettings = settings.computation.toNative()
    val emissionSettings = settings.emission.toNative()

    val text1Lines = text1.lines()
    val text2Lines = text2.lines()
    var hunkBuilder: HunkBuilder? = null
    var targetIndex = 0
    var sourceIndex = 0

    val callbacks =
        LibXDiff.create_xdemitcb(
            privateData = null,
            outputHunkCallback = {
                    privateData: Pointer?,
                    oldBegin: Long,
                    oldCount: Long,
                    newBegin: Long,
                    newCount: Long,
                    functionName: String?,
                    functionNameLength: Long,
                ->
                hunkBuilder?.build()?.let { outputs(it) }
                hunkBuilder =
                    HunkBuilder(
                        sourceStart = oldBegin.toInt(),
                        sourceLength = oldCount.toInt(),
                        targetStart = newBegin.toInt(),
                        targetLength = newCount.toInt(),
                    )
                sourceIndex = oldBegin.toInt() - 1
                targetIndex = newBegin.toInt() - 1
                0
            },
            outputLineCallback = outputLineCallback@{ privateData: Pointer?, buffer: mmbuffer_t?, lineType: Int ->
                val content =
                    buffer
                        ?.let { LibXDiff.get_mmbuffer_string(it) }
                        ?: return@outputLineCallback 0
                when (content) {
                    " " -> {
                        hunkBuilder?.addContext(text1Lines[sourceIndex])
                        sourceIndex++
                        targetIndex++
                    }

                    "-" -> {
                        hunkBuilder?.addDeletion(text1Lines[sourceIndex])
                        sourceIndex++
                    }

                    "+" -> {
                        hunkBuilder?.addAddition(text2Lines[targetIndex])
                        targetIndex++
                    }

                    else -> error("Invalid line type: $content")
                }
                0
            },
        )

    val result =
        LibXDiff.xdl_diff(
            mmfile1 = mmfile1,
            mmfile2 = mmfile2,
            xpparam = computationSettings,
            xemitconf = emissionSettings,
            xdemitcb = callbacks,
        )

    if (result == 0) {
        hunkBuilder?.build()?.let { outputs(it) }
    } else {
        error("Error while diffing with code $result")
    }

    LibXDiff.destroy_mmfile(mmfile1)
    LibXDiff.destroy_mmfile(mmfile2)
    LibXDiff.destroy_xpparam(computationSettings)
    LibXDiff.destroy_xdemitconf(emissionSettings)
    LibXDiff.destroy_xdemitcb(callbacks)
}

internal fun DiffEmissionSettings.toNative(): xdemitconf_t {
    var flags = 0
    if (emitFunctionNames) flags = flags or XDL_EMIT_FUNCNAMES
    if (suppressHunkHeaders) flags = flags or XDL_EMIT_NO_HUNK_HDR
    if (emitFunctionContext) flags = flags or XDL_EMIT_FUNCCONTEXT

    return LibXDiff.create_xdemitconf(
        contextLength = contextLinesCount.toLong(),
        interhunkContextLength = interHunkContextLinesCount.toLong(),
        flags = flags.toLong(),
        findFunction = null,
        findFunctionPrivate = null,
        hunkFunction = null,
    )
}

internal fun DiffComputationSettings.toNative(): xpparam_t {
    var flags = 0

    if (useMinimal) flags = flags or XDF_NEED_MINIMAL
    if (ignoreAllWhitespace) flags = flags or XDF_IGNORE_WHITESPACE
    if (ignoreWhitespaceChange) flags = flags or XDF_IGNORE_WHITESPACE_CHANGE
    if (ignoreWhitespaceAtEndOfLine) flags = flags or XDF_IGNORE_WHITESPACE_AT_EOL
    if (ignoreCarriageReturnAtEndOfLine) flags = flags or XDF_IGNORE_CR_AT_EOL
    if (ignoreBlankLines) flags = flags or XDF_IGNORE_BLANK_LINES
    if (algorithm == DiffAlgorithm.PATIENCE) flags or XDF_PATIENCE_DIFF
    if (algorithm == DiffAlgorithm.HISTOGRAM) flags or XDF_HISTOGRAM_DIFF
    if (useIndentationHeuristic) flags = flags or XDF_INDENT_HEURISTIC

    val params =
        LibXDiff.create_xpparam(
            flags = flags.toLong(),
            ignoreRegex =
                ignoreRegex
                    .map { LibXDiff.create_regex(it, 0) }
                    .map { it.pointer }
                    .toPointerByReference(),
            ignoreRegexCount = ignoreRegex.size.toNativeLong(),
            anchors =
                anchors
                    .map { it.toPointer() }
                    .toPointerByReference(),
            anchorsCount = anchors.size.toNativeLong(),
        )
    return params
}

internal fun Number.toNativeLong() = NativeLong(toLong())

internal fun String.toPointer(charset: Charset = Charsets.UTF_8): Memory {
    val data = toByteArray(charset) + byteArrayOf(0) // Add null terminator
    val mem = Memory(data.size.toLong())
    mem.write(0, data, 0, data.size)
    return mem
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
