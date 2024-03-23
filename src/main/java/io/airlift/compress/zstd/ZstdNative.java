/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.airlift.compress.zstd;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

public final class ZstdNative
{
    private static final AddressLayout C_POINTER = ValueLayout.ADDRESS
            .withTargetLayout(MemoryLayout.sequenceLayout(Long.MAX_VALUE, JAVA_BYTE));

    private static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup
            .libraryLookup("/opt/homebrew/lib/libzstd.dylib", Arena.ofAuto())
            .or(SymbolLookup.loaderLookup())
            .or(Linker.nativeLinker().defaultLookup());

    // TODO should we just hardcode this to 3?
    public static final int DEFAULT_COMPRESSION_LEVEL;

    static {
        MethodHandle defaultCompressionLevelMethod = lookupMethod("ZSTD_defaultCLevel", FunctionDescriptor.of(JAVA_INT));
        try {
            DEFAULT_COMPRESSION_LEVEL = (int) defaultCompressionLevelMethod.invokeExact();
        }
        catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }
    }

    private ZstdNative() {}

    private static MethodHandle lookupMethod(String name, FunctionDescriptor functionDescriptor)
    {
        return Linker.nativeLinker().downcallHandle(
                SYMBOL_LOOKUP.find(name)
                        .orElseThrow(() -> new UnsatisfiedLinkError("unresolved symbol: " + name)),
                functionDescriptor,
                Linker.Option.critical(true));
    }

    private static final MethodHandle MAX_COMPRESSED_LENGTH_METHOD = lookupMethod("ZSTD_compressBound", FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));

    public static long maxCompressedLength(long inputLength)
    {
        long result;
        try {
            result = (long) MAX_COMPRESSED_LENGTH_METHOD.invokeExact(inputLength);
        }
        catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }
        if (isError(result)) {
            throw new IllegalArgumentException("Error occurred during compression: " + getErrorName(result));
        }
        return result;
    }

    private static final MethodHandle COMPRESS_METHOD = lookupMethod("ZSTD_compress", FunctionDescriptor.of(JAVA_LONG, C_POINTER, JAVA_LONG, C_POINTER, JAVA_LONG, JAVA_INT));

    public static long compress(MemorySegment input, long inputLength, MemorySegment compressed, long compressedLength, int compressionLevel)
    {
        long result;
        try {
            result = (long) COMPRESS_METHOD.invokeExact(compressed, compressedLength, input, inputLength, compressionLevel);
        }
        catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }

        if (isError(result)) {
            throw new IllegalArgumentException("Error occurred during compression: " + getErrorName(result));
        }
        return result;
    }

    private static final MethodHandle DECOMPRESS_METHOD = lookupMethod("ZSTD_decompress", FunctionDescriptor.of(JAVA_LONG, C_POINTER, JAVA_LONG, C_POINTER, JAVA_LONG));

    public static long decompress(MemorySegment compressed, long compressedLength, MemorySegment output, long outputLength)
    {
        long result;
        try {
            result = (long) DECOMPRESS_METHOD.invokeExact(output, outputLength, compressed, compressedLength);
        }
        catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }

        if (isError(result)) {
            throw new IllegalArgumentException("Error occurred during decompression: " + getErrorName(result));
        }
        return result;
    }

    private static final MethodHandle UNCOMPRESSED_LENGTH_METHOD = lookupMethod("ZSTD_getFrameContentSize", FunctionDescriptor.of(JAVA_LONG, C_POINTER, JAVA_LONG));

    public static long decompressedLength(MemorySegment compressed, long compressedLength)
    {
        long result;
        try {
            result = (long) UNCOMPRESSED_LENGTH_METHOD.invokeExact(compressed, compressedLength);
        }
        catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }

        if (isError(result)) {
            throw new IllegalArgumentException("Error occurred during decompression: " + getErrorName(result));
        }
        return result;
    }

    private static final MethodHandle IS_ERROR_METHOD = lookupMethod("ZSTD_isError", FunctionDescriptor.of(JAVA_INT, JAVA_LONG));

    private static boolean isError(long code)
    {
        try {
            return (int) IS_ERROR_METHOD.invokeExact(code) != 0;
        }
        catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }
    }

    private static final MethodHandle GET_ERROR_NAME_METHOD = lookupMethod("ZSTD_getErrorName", FunctionDescriptor.of(C_POINTER, JAVA_LONG));

    private static String getErrorName(long code)
    {
        try {
            MemorySegment name = (MemorySegment) GET_ERROR_NAME_METHOD.invokeExact(code);
            return name.getString(0);
        }
        catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }
    }
}
