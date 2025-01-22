@file:Suppress("FunctionName", "SpellCheckingInspection", "ClassName", "PropertyName", "LocalVariableName")

package com.github.lamba92.xdiff.jvm

import com.sun.jna.Callback
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.PointerType
import com.sun.jna.ptr.PointerByReference

// Struct Classes (Opaque Pointers)
public class mmfile_t : PointerType() // Opaque pointer for mmfile_t

public class mmbuffer_t : PointerType() // Opaque pointer for mmbuffer_t

public class xpparam_t : PointerType() // Opaque pointer for xpparam_t

public class xdemitcb_t : PointerType() // Opaque pointer for xdemitcb_t

public class xdemitconf_t : PointerType() // Opaque pointer for xdemitconf_t

public class xmparam_t : PointerType() // Opaque pointer for xmparam_t

public class xdl_regex_t : PointerType() // Opaque pointer for xdl_regex_t

// Callback Interfaces
public fun interface OutputHunkCallback : Callback {
    public fun invoke(
        privateData: Pointer?,
        oldBegin: Long,
        oldCount: Long,
        newBegin: Long,
        newCount: Long,
        functionName: String?,
        functionNameLength: Long,
    ): Int
}

public fun interface OutputLineCallback : Callback {
    public fun invoke(
        privateData: Pointer?,
        buffer: mmbuffer_t?,
        lineType: Int,
    ): Int
}

public interface FindFunction : Callback {
    public fun invoke(
        line: String?,
        lineLength: NativeLong,
        buffer: Pointer?,
        bufferSize: NativeLong,
        privateData: Pointer?,
    ): NativeLong
}

public interface EmitHunkConsumeFunction : Callback {
    public fun invoke(
        startA: NativeLong,
        countA: NativeLong,
        startB: NativeLong,
        countB: NativeLong,
        callbackData: Pointer?,
    ): Int
}

// Library Mapping
public object LibXDiff {
    init {
        Native.register(LibXDiff::class.java, "xdiff")
    }

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
        ignoreRegexCount: NativeLong,
        anchors: PointerByReference?,
        anchorsCount: NativeLong,
    ): xpparam_t

    @JvmStatic
    public external fun destroy_xpparam(xpparam: xpparam_t)

    // xdemitcb_t Functions
    @JvmStatic
    public external fun create_xdemitcb(
        privateData: Pointer?,
        outputHunkCallback: OutputHunkCallback?,
        outputLineCallback: OutputLineCallback?,
    ): xdemitcb_t

    @JvmStatic
    public external fun destroy_xdemitcb(xdemitcb: xdemitcb_t)

    // xdemitconf_t Functions
    @JvmStatic
    public external fun create_xdemitconf(
        contextLength: Long,
        interhunkContextLength: Long,
        flags: Long,
        findFunction: FindFunction?,
        findFunctionPrivate: Pointer?,
        hunkFunction: EmitHunkConsumeFunction?,
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

    // Diff Function
    @JvmStatic
    public external fun xdl_diff(
        mmfile1: mmfile_t,
        mmfile2: mmfile_t,
        xpparam: xpparam_t,
        xemitconf: xdemitconf_t,
        xdemitcb: xdemitcb_t,
    ): Int
}

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
