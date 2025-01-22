@file:OptIn(UnsafeNumber::class)

package com.github.lamba92.xdiff

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.toKString
import xdiff.xdemitconf_t
import xdiff.xpparam_t

public actual fun computeTextDiff(
    source: String,
    target: String,
    settings: TextDiffSettings,
): TextDiff {
    val hunks =
        buildList {
            rawTextDiff(source, target, settings) { add(it) }
        }
    return TextDiff(hunks)
}

private class CallbackPrivateData(
    val outputs: (Hunk) -> Unit,
    val text1Lines: List<String>,
    val text2Lines: List<String>,
    var hunkBuilder: HunkBuilder? = null,
    var targetIndex: Int = 0,
    var sourceIndex: Int = 0,
)

private fun rawTextDiff(
    text1: String,
    text2: String,
    settings: TextDiffSettings = TextDiffSettings(),
    outputs: (Hunk) -> Unit,
) = memScoped {
    val mmFile1 = xdiff.create_mmfile(text1, text1.length.convert())
    val mmFile2 = xdiff.create_mmfile(text2, text2.length.convert())

    val computationSettings = settings.computation.toNative(memScope)
    val emissionSettings = settings.emission.toNative()

    // this makes no sense, but the xdiff library requires
    // a "private" object to be passed to the callbacks;
    // otherwise "staticCFunction" will fail because
    // it will capture variables from the outer scope
    val privateData =
        CallbackPrivateData(
            outputs = outputs,
            text1Lines = text1.lines(),
            text2Lines = text2.lines(),
        )

    val callbacks =
        xdiff.create_xdemitcb(
            private_data = StableRef.create(privateData).asCPointer(),
            output_hunk_callback =
                staticCFunction {
                        private,
                        oldBegin,
                        oldNr,
                        newBegin,
                        newNr,
                        func,
                        funcLen,
                    ->
                    val actualPrivate =
                        private
                            ?.asStableRef<CallbackPrivateData>()
                            ?.get()
                            ?: return@staticCFunction 0
                    actualPrivate.hunkBuilder?.build()?.let { actualPrivate.outputs(it) }
                    actualPrivate.hunkBuilder =
                        HunkBuilder(
                            sourceStart = oldBegin.convert(),
                            sourceLength = oldNr.convert(),
                            targetStart = newBegin.convert(),
                            targetLength = newNr.convert(),
                        )

                    // Keep the cast because oldBegin and newBegin change
                    // type between Long and Int based on the platform
                    @Suppress("RemoveRedundantCallsOfConversionMethods")
                    actualPrivate.sourceIndex = oldBegin.toInt() - 1
                    @Suppress("RemoveRedundantCallsOfConversionMethods")
                    actualPrivate.targetIndex = newBegin.toInt() - 1
                    0
                },
            output_line_callback =
                staticCFunction { private, line, len ->
                    if (line == null) return@staticCFunction 0
                    val content = xdiff.get_mmbuffer_string(line)?.toKString() ?: return@staticCFunction 0
                    val actualPrivate =
                        private
                            ?.asStableRef<CallbackPrivateData>()
                            ?.get()
                            ?: return@staticCFunction 0
                    when (content) {
                        " " -> {
                            actualPrivate.hunkBuilder?.addContext(actualPrivate.text1Lines[actualPrivate.sourceIndex])
                            actualPrivate.sourceIndex++
                            actualPrivate.targetIndex++
                        }

                        "-" -> {
                            actualPrivate.hunkBuilder?.addDeletion(actualPrivate.text1Lines[actualPrivate.sourceIndex])
                            actualPrivate.sourceIndex++
                        }

                        "+" -> {
                            actualPrivate.hunkBuilder?.addAddition(actualPrivate.text2Lines[actualPrivate.targetIndex])
                            actualPrivate.targetIndex++
                        }

                        else -> error("Invalid line type: $content")
                    }
                    0
                },
        )

    val result =
        xdiff.xdl_diff(
            mf1 = mmFile1,
            mf2 = mmFile2,
            xpp = computationSettings,
            xecfg = emissionSettings,
            ecb = callbacks,
        )

    if (result == 0) {
        privateData.hunkBuilder?.build()?.let { outputs(it) }
    } else {
        error("Error while diffing with code $result")
    }

    xdiff.destroy_mmfile(mmFile1)
    xdiff.destroy_mmfile(mmFile2)
    xdiff.destroy_xpparam(computationSettings)
    xdiff.destroy_xdemitconf(emissionSettings)
    xdiff.destroy_xdemitcb(callbacks)
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
        xdiff.create_xpparam(
            flags = flags.convert(),
            ignore_regex =
                ignoreRegex
                    .map { xdiff.create_regex(it, 0) }
                    .toCValues(),
            ignore_regex_count = ignoreRegex.size.convert(),
            anchors = anchors.map { it.cstr.getPointer(memScope) }.toCValues(),
            anchors_count = anchors.size.convert(),
        )

    if (params == null) {
        error("Error while creating xpparam_t")
    }

    return params
}

private fun DiffEmissionSettings.toNative(): CPointer<xdemitconf_t> {
    var flags = 0u
    if (emitFunctionNames) flags = flags or xdiff.XDL_EMIT_FUNCNAMES.convert()
    if (suppressHunkHeaders) flags = flags or xdiff.XDL_EMIT_NO_HUNK_HDR.convert()
    if (emitFunctionContext) flags = flags or xdiff.XDL_EMIT_FUNCCONTEXT.convert()

    val conf =
        xdiff.create_xdemitconf(
            flags = flags.convert(),
            context_length = contextLinesCount.convert(),
            interhunk_context_length = interHunkContextLinesCount.convert(),
            find_function = null,
            hunk_function = null,
            find_function_private = null,
        )

    if (conf == null) {
        error("Error while creating xdemitconf_t")
    }

    return conf
}
