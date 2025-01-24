@file:Suppress("FunctionName", "SpellCheckingInspection", "ClassName", "PropertyName", "LocalVariableName")

package com.github.lamba92.xdiff.jvm

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.PointerType
import com.sun.jna.ptr.PointerByReference

/**
 * The `LibXDiff` object provides a set of native bindings for xdiff library functions, enabling
 * operations like creating and destroying various opaque pointer types used internally, performing
 * diffs, and configuring diff and merge operations with customizable parameters and callbacks.
 *
 * This includes functions for working with file and buffer structures, regex handling, and diff
 * results, as well as utilities for configuring and managing callbacks and parameters used for
 * fine-grained control over diffing and merging behavior. Additionally, various constants related
 * to diff algorithms, whitespace treatment, and merge configurations are provided.
 *
 * Initialization of the `LibXDiff` object registers the native library `xdiff`, making its
 * functionalities available for use. The methods are primarily designed to interact with the xdiff
 * API and use opaque pointer types to represent internal structures.
 */
public object LibXDiff {
    init {
        Native.register(LibXDiff::class.java, "xdiff")
    }

    // Opaque pointers for structs
    public class mmfile_t : PointerType()

    public class mmbuffer_t : PointerType()

    public class xpparam_t : PointerType()

    public class xdemitcb_t : PointerType()

    public class xdemitconf_t : PointerType()

    public class xmparam_t : PointerType()

    public class xdl_regex_t : PointerType()

    public class xdiff_hunk_t : PointerType()

    public class xdiff_result_t : PointerType()

    // mmfile_t Functions
    @JvmStatic
    public external fun create_mmfile(
        data: String?,
        size: Long,
    ): mmfile_t

    @JvmStatic
    public external fun destroy_mmfile(mmfile: mmfile_t)

    // mmbuffer_t Functions
    @JvmStatic
    public external fun create_mmbuffer(
        data: String?,
        size: Long,
    ): mmbuffer_t

    @JvmStatic
    public external fun destroy_mmbuffer(mmbuffer: mmbuffer_t)

    @JvmStatic
    public external fun get_mmbuffer_string(mmbuffer: mmbuffer_t): String?

    // xpparam_t Functions
    @JvmStatic
    public external fun create_xpparam(
        flags: Long,
        ignoreRegex: PointerByReference?,
        ignoreRegexCount: Long,
        anchors: PointerByReference?,
        anchorsCount: Long,
    ): xpparam_t

    @JvmStatic
    public external fun destroy_xpparam(xpparam: xpparam_t)

    // xdemitcb_t Functions
    @JvmStatic
    public external fun create_xdemitcb(
        privateData: Pointer?,
        outputHunkCallback: Pointer?,
        outputLineCallback: Pointer?,
    ): xdemitcb_t

    @JvmStatic
    public external fun destroy_xdemitcb(xdemitcb: xdemitcb_t)

    // xdemitconf_t Functions
    @JvmStatic
    public external fun create_xdemitconf(
        contextLength: Long,
        interhunkContextLength: Long,
        flags: Long,
        findFunction: Pointer?,
        findFunctionPrivate: Pointer?,
        hunkFunction: Pointer?,
    ): xdemitconf_t

    @JvmStatic
    public external fun destroy_xdemitconf(xdemitconf: xdemitconf_t)

    // xmparam_t Functions
    @JvmStatic
    public external fun create_xmparam(
        markerSize: Int,
        mergeLevel: Int,
        favorMode: Int,
        style: Int,
        ancestorLabel: String?,
        file1Label: String?,
        file2Label: String?,
    ): xmparam_t

    @JvmStatic
    public external fun destroy_xmparam(xmparam: xmparam_t)

    // Regex Helper Functions
    @JvmStatic
    public external fun create_regex(
        pattern: String?,
        flags: Int,
    ): xdl_regex_t

    @JvmStatic
    public external fun destroy_regex(regex: xdl_regex_t)

    // xdiff_hunk_t Functions
    @JvmStatic
    public external fun xdiff_hunk_get_line_count(hunk: xdiff_hunk_t): Long

    @JvmStatic
    public external fun xdiff_hunk_get_line_at(
        hunk: xdiff_hunk_t,
        index: Long,
    ): String?

    @JvmStatic
    public external fun xdiff_hunk_get_old_begin(hunk: xdiff_hunk_t): Long

    @JvmStatic
    public external fun xdiff_hunk_get_old_count(hunk: xdiff_hunk_t): Long

    @JvmStatic
    public external fun xdiff_hunk_get_new_begin(hunk: xdiff_hunk_t): Long

    @JvmStatic
    public external fun xdiff_hunk_get_new_count(hunk: xdiff_hunk_t): Long

    // xdiff_result_t Functions
    @JvmStatic
    public external fun xdiff_result_get_hunk_count(result: xdiff_result_t): Long

    @JvmStatic
    public external fun xdiff_result_get_hunk_at(
        result: xdiff_result_t,
        index: Long,
    ): xdiff_hunk_t

    @JvmStatic
    public external fun xdiff_result_destroy(result: xdiff_result_t)

    // Diff Wrapper
    @JvmStatic
    public external fun xdl_xdiff_simple(
        mf1: mmfile_t,
        mf2: mmfile_t,
        xpp: xpparam_t?,
        xemitconf: xdemitconf_t?,
    ): xdiff_result_t?

    // Miscellaneous Flags and Constants
    public const val XDF_NEED_MINIMAL: Int = 1 shl 0
    public const val XDF_IGNORE_WHITESPACE: Int = 1 shl 1
    public const val XDF_IGNORE_WHITESPACE_CHANGE: Int = 1 shl 2
    public const val XDF_IGNORE_WHITESPACE_AT_EOL: Int = 1 shl 3
    public const val XDF_IGNORE_CR_AT_EOL: Int = 1 shl 4
    public const val XDF_WHITESPACE_FLAGS: Int =
        XDF_IGNORE_WHITESPACE or
            XDF_IGNORE_WHITESPACE_CHANGE or
            XDF_IGNORE_WHITESPACE_AT_EOL or
            XDF_IGNORE_CR_AT_EOL
    public const val XDF_IGNORE_BLANK_LINES: Int = 1 shl 7
    public const val XDF_PATIENCE_DIFF: Int = 1 shl 14
    public const val XDF_HISTOGRAM_DIFF: Int = 1 shl 15
    public const val XDF_DIFF_ALGORITHM_MASK: Int = XDF_PATIENCE_DIFF or XDF_HISTOGRAM_DIFF
    public const val XDF_INDENT_HEURISTIC: Int = 1 shl 23
    public const val XDL_EMIT_FUNCNAMES: Int = 1 shl 0
    public const val XDL_EMIT_NO_HUNK_HDR: Int = 1 shl 1
    public const val XDL_EMIT_FUNCCONTEXT: Int = 1 shl 2
    public const val XDL_MERGE_MINIMAL: Int = 0
    public const val XDL_MERGE_EAGER: Int = 1
    public const val XDL_MERGE_ZEALOUS: Int = 2
    public const val XDL_MERGE_ZEALOUS_ALNUM: Int = 3
    public const val XDL_MERGE_FAVOR_OURS: Int = 1
    public const val XDL_MERGE_FAVOR_THEIRS: Int = 2
    public const val XDL_MERGE_FAVOR_UNION: Int = 3
    public const val XDL_MERGE_DIFF3: Int = 1
    public const val XDL_MERGE_ZEALOUS_DIFF3: Int = 2
    public const val DEFAULT_CONFLICT_MARKER_SIZE: Int = 7
}
