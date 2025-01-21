@file:OptIn(UnsafeNumber::class)

package io.github.lamba92.xdiff

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import platform.posix.REG_EXTENDED
import platform.posix.regex_t
import xdiff.mmfile_t
import xdiff.xdemitcb_t
import xdiff.xdemitconf_t
import xdiff.xpparam_t

private class CallbackPrivateData(
    val outputs: (Hunk) -> Unit,
    val text1Lines: List<String>,
    val text2Lines: List<String>,
    var hunkBuilder: HunkBuilder? = null,
    var targetIndex: Int = 0,
    var sourceIndex: Int = 0
)

internal fun rawTextDiff(
    text1: String,
    text2: String,
    settings: TextDiffSettings = TextDiffSettings(),
    outputs: (Hunk) -> Unit
): Unit = memScoped {

    val mmFile1 = alloc<mmfile_t>()
    val mmFile2 = alloc<mmfile_t>()
    mmFile1.ptr = text1.cstr.getPointer(this)
    mmFile1.size = text1.length.convert()
    mmFile2.ptr = text2.cstr.getPointer(this)
    mmFile2.size = text2.length.convert()

    val computationSettings = settings.computation.toNative(this)
    val emissionSettings = settings.emission.toNative(this)

    // this makes no sense, but the xdiff library requires
    // a "private" object to be passed to the callbacks;
    // otherwise "staticCFunction" will fail because
    // it will capture variables from the outer scope
    val private = CallbackPrivateData(
        outputs = outputs,
        text1Lines = text1.lines(),
        text2Lines = text2.lines()
    )

    val callbacks = alloc<xdemitcb_t>()
    callbacks.priv = StableRef.create(private).asCPointer()
    callbacks.out_hunk =
        staticCFunction { private, oldBegin, oldNr, newBegin,
                          newNr, func, funcLen ->
            val private = private
                ?.asStableRef<CallbackPrivateData>()
                ?.get()
                ?: return@staticCFunction 0
            private.hunkBuilder?.build()?.let { private.outputs(it) }
            private.hunkBuilder = HunkBuilder(
                sourceStart = oldBegin.convert(),
                sourceLength = oldNr.convert(),
                targetStart = newBegin.convert(),
                targetLength = newNr.convert()
            )
            private.sourceIndex = oldBegin.toInt() - 1
            private.targetIndex = newBegin.toInt() - 1
            0
        }

    callbacks.out_line = staticCFunction { private, line, len ->
        if (line == null) return@staticCFunction 0
        val content = line.pointed.ptr?.toKString() ?: return@staticCFunction 0
        val private = private
            ?.asStableRef<CallbackPrivateData>()
            ?.get()
            ?: return@staticCFunction 0
        when (content) {
            " " -> {
                private.hunkBuilder?.addContext(private.text1Lines[private.sourceIndex])
                private.sourceIndex++
                private.targetIndex++
            }
            "-" -> {
                private.hunkBuilder?.addDeletion(private.text1Lines[private.sourceIndex])
                private.sourceIndex++
            }
            "+" -> {
                private.hunkBuilder?.addAddition(private.text2Lines[private.targetIndex])
                private.targetIndex++
            }
            else -> error("Invalid line type: $content")
        }
        0
    }

    val result = xdiff.xdl_diff(
        mf1 = interpretCPointer(mmFile1.rawPtr),
        mf2 = interpretCPointer(mmFile2.rawPtr),
        xpp = computationSettings.ptr,
        xecfg = emissionSettings.ptr,
        ecb = callbacks.ptr
    )

    if (result == 0) {
        private.hunkBuilder?.build()?.let { outputs(it) }
    } else {
        error("Error while diffing with code $result")
    }
}

private fun DiffComputationSettings.toNative(scope: MemScope): xpparam_t {
    val anchorArray = if (anchors.isNotEmpty()) {
        val cAnchors = scope.allocArray<CPointerVar<ByteVar>>(anchors.size + 1)
        anchors.forEachIndexed { index, anchor ->
            cAnchors[index] = anchor.cstr.getPointer(scope)
        }
        cAnchors[anchors.size] = null
        cAnchors
    } else {
        null
    }

    val regexArray = if (ignoreRegex.isNotEmpty()) {
        val cRegex = scope.allocArray<CPointerVar<regex_t>>(ignoreRegex.size + 1)
        ignoreRegex.forEachIndexed { index, pattern ->
            val regex = scope.alloc<regex_t>()
            if (platform.posix.regcomp(regex.ptr, pattern, REG_EXTENDED) != 0) {
                throw RuntimeException("Failed to compile regex: $pattern")
            }
            cRegex[index] = regex.ptr
        }
        cRegex[ignoreRegex.size] = null // Null-terminate the array
        cRegex
    } else {
        null
    }

    var flags = when (algorithm) {
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

    val params = scope.memScope.alloc<xpparam_t>()
    params.flags = flags.convert()
    params.anchors = anchorArray
    params.anchors_nr = this@toNative.anchors.size.convert()
    params.ignore_regex = regexArray
    params.ignore_regex_nr = this@toNative.ignoreRegex.size.convert()
    return params
}

private fun DiffEmissionSettings.toNative(memScope: MemScope): xdemitconf_t {
    val conf = memScope.alloc<xdemitconf_t>()

    var flags = 0u
    if (emitFunctionNames) flags = flags or xdiff.XDL_EMIT_FUNCNAMES.convert()
    if (suppressHunkHeaders) flags = flags or xdiff.XDL_EMIT_NO_HUNK_HDR.convert()
    if (emitFunctionContext) flags = flags or xdiff.XDL_EMIT_FUNCCONTEXT.convert()

    conf.ctxlen = contextLines.convert()
    conf.flags = flags.convert()
    conf.interhunkctxlen = interHunkContextLines.convert()
    return conf
}
