@file:OptIn(UnsafeNumber::class)

package io.github.lamba92.xdiff

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.staticCFunction
import platform.posix.REG_EXTENDED
import platform.posix.regex_t
import xdiff.DiffComputationSettings
import xdiff.DiffEmissionSettings
import xdiff.Hunk
import xdiff.HunkBuilder
import xdiff.TextDiffSettings
import xdiff.mmfile_t
import xdiff.xdemitcb_t
import xdiff.xdemitconf_t
import xdiff.xpparam_t

public object DiffUtils {

    public fun textDiff(
        text1: String,
        text2: String,
        settings: TextDiffSettings = TextDiffSettings()
    ): List<Hunk> = buildList {
        rawTextDiff(text1, text2, settings) { add(it) }
    }

}


private fun rawTextDiff(
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

    val xpparam = settings.computation.toNative(this)
    val xemitconf = settings.emission.toNative(this)

    var hunkBuilder: HunkBuilder? = null

    val xemitcb = alloc<xdemitcb_t>()
    xemitcb.out_hunk =
        staticCFunction { _, oldBegin, oldNr, newBegin,
                          newNr, func, funcLen ->
            hunkBuilder?.build()?.let { outputs(it) }
            hunkBuilder = HunkBuilder(
                sourceStart = oldBegin,
                sourceLength = oldNr,
                targetStart = newBegin,
                targetLength = newNr
            )
            0
        }
    xemitcb.out_line = staticCFunction { _, line, len ->
        if (line == null) return@staticCFunction 0
        val content = line.pointed.ptr ?: return@staticCFunction 0
        val line = ByteArray(len) { content[it] }.decodeToString()
        hunkBuilder?.addChangeFromString(line)
        0
    }
    // Unified diff context

    val result = xdiff.xdl_diff(
        mf1 = interpretCPointer(mmFile1.rawPtr),
        mf2 = interpretCPointer(mmFile2.rawPtr),
        xpp = xpparam.ptr,
        xecfg = xemitconf.ptr,
        ecb = xemitcb.ptr
    )

    if (result != 0) {
        hunkBuilder?.build()?.let { outputs(it) }
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

    val combinedFlags = flags.fold(0u) { acc, flag -> acc or flag.value }

    return scope.memScope.alloc<xpparam_t>().apply {
        flags = combinedFlags.toUInt()
        anchors = anchorArray
        anchors_nr = this@toNative.anchors.size.convert()
        ignore_regex = regexArray
        ignore_regex_nr = this@toNative.ignoreRegex.size.convert()
    }
}


private fun DiffEmissionSettings.toNative(memScope: MemScope): xdemitconf_t {
    return memScope.alloc<xdemitconf_t>().apply {
        ctxlen = contextLines.convert()
        flags = this@toNative.flags.fold(0u) { acc, flag -> acc or flag.value }
        interhunkctxlen = interHunkContextLines.convert()
    }
}
