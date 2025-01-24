@file:OptIn(UnsafeNumber::class)
@file:Suppress("FunctionName")

package com.github.lamba92.xdiff

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import xdiff.mmfile_t
import xdiff.xdiff_hunk_t

internal actual fun create_mmfile(
    text: String,
    length: Int,
): CPointer<mmfile_t>? = xdiff.create_mmfile(text, length.convert())

internal actual fun create_xpparam(
    flags: UInt,
    ignore_regex: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.CPointerVarOf<CPointer<platform.posix.regex_t>>>?,
    ignore_regex_count: UInt,
    anchors: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.CPointerVarOf<CPointer<kotlinx.cinterop.ByteVarOf<Byte>>>>?,
    anchors_count: UInt,
): CPointer<xdiff.s_xpparam>? =
    xdiff.create_xpparam(
        flags = flags.convert(),
        ignore_regex = ignore_regex,
        ignore_regex_count = ignore_regex_count.convert(),
        anchors = anchors,
        anchors_count = anchors_count.convert(),
    )

public actual fun create_xdemitconf(
    context_length: Int,
    interhunk_context_length: Int,
    flags: UInt,
): CPointer<xdiff.s_xdemitconf>? =
    xdiff.create_xdemitconf(
        context_length = context_length.convert(),
        interhunk_context_length = interhunk_context_length.convert(),
        flags = flags.convert(),
        find_function = null,
        find_function_private = null,
        hunk_function = null,
    )

internal actual fun xdiff_hunk_get_old_begin(hunk: CPointer<xdiff_hunk_t>): Int = xdiff.xdiff_hunk_get_old_begin(hunk).convert()

internal actual fun xdiff_hunk_get_old_count(hunk: CPointer<xdiff_hunk_t>): Int = xdiff.xdiff_hunk_get_old_count(hunk).convert()

internal actual fun xdiff_hunk_get_new_begin(hunk: CPointer<xdiff_hunk_t>): Int = xdiff.xdiff_hunk_get_new_begin(hunk).convert()

internal actual fun xdiff_hunk_get_new_count(hunk: CPointer<xdiff_hunk_t>): Int = xdiff.xdiff_hunk_get_new_count(hunk).convert()
