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
package io.airlift.compress.snappy;

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

public final class SnappyNative
{
    private final MemorySegment lengthBuffer = MemorySegment.ofArray(new long[1]);

    public static long maxCompressedLength(long inputLength)
    {
        return maxCompressedLengthInternal(inputLength);
    }

    public long compress(MemorySegment input, long inputLength, MemorySegment compressed, long compressedLength)
    {
        lengthBuffer.set(JAVA_LONG, 0, compressedLength);
        compressInternal(input, inputLength, compressed, lengthBuffer);
        return lengthBuffer.get(JAVA_LONG, 0);
    }

    public long decompress(MemorySegment compressed, long compressedLength, MemorySegment uncompressed, long uncompressedLength)
    {
        lengthBuffer.set(JAVA_LONG, 0, uncompressedLength);
        decompressInternal(compressed, compressedLength, uncompressed, lengthBuffer);
        return lengthBuffer.get(JAVA_LONG, 0);
    }

    public long decompressedLength(MemorySegment compressed, long compressedLength)
    {
        lengthBuffer.set(JAVA_LONG, 0, 0);
        decompressedLengthInternal(compressed, compressedLength, lengthBuffer);
        return lengthBuffer.get(JAVA_LONG, 0);
    }

    public static boolean validate(MemorySegment compressed, long compressedLength)
    {
        return validateInternal(compressed, compressedLength);
    }

    //
    // FFI stuff
    //

    private static final AddressLayout C_POINTER = ValueLayout.ADDRESS
            .withTargetLayout(MemoryLayout.sequenceLayout(Long.MAX_VALUE, JAVA_BYTE));

    private static final int SNAPPY_OK = 0;
    private static final int SNAPPY_INVALID_INPUT = 1;
    private static final int SNAPPY_BUFFER_TOO_SMALL = 2;

    // TODO how do we want to handle loading native libraries?
    private static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup
            .libraryLookup("/opt/homebrew/lib/libsnappy.dylib", Arena.ofAuto())
            .or(SymbolLookup.loaderLookup())
            .or(Linker.nativeLinker().defaultLookup());

    private static MethodHandle lookupMethod(String name, FunctionDescriptor functionDescriptor)
    {
        return Linker.nativeLinker().downcallHandle(
                SYMBOL_LOOKUP.find(name)
                        .orElseThrow(() -> new UnsatisfiedLinkError("unresolved symbol: " + name)),
                functionDescriptor,
                Linker.Option.critical(true));
    }

    private static final MethodHandle COMPRESS_METHOD = lookupMethod("snappy_compress", FunctionDescriptor.of(JAVA_INT, C_POINTER, JAVA_LONG, C_POINTER, C_POINTER));

    private static void compressInternal(MemorySegment input, long inputLength, MemorySegment compressed, MemorySegment compressedLength)
    {
        int result;
        try {
            result = (int) COMPRESS_METHOD.invokeExact(input, inputLength, compressed, compressedLength);
        }
        catch (Throwable t) {
            throw new AssertionError("should not reach here", t);
        }

        // verify result
        if (result == SNAPPY_BUFFER_TOO_SMALL) {
            throw new IllegalArgumentException("Output buffer is too small");
        }
        if (result != SNAPPY_OK) {
            throw new IllegalArgumentException("Unknown error occurred during compression: result=" + result);
        }
    }

    private static final MethodHandle DECOMPRESS_METHOD = lookupMethod("snappy_uncompress", FunctionDescriptor.of(JAVA_INT, C_POINTER, JAVA_LONG, C_POINTER, C_POINTER));

    private static void decompressInternal(MemorySegment compressed, long compressedLength, MemorySegment output, MemorySegment outputLength)
    {
        int result;
        try {
            result = (int) DECOMPRESS_METHOD.invokeExact(compressed, compressedLength, output, outputLength);
        }
        catch (Throwable t) {
            throw new AssertionError("should not reach here", t);
        }
        if (result == SNAPPY_INVALID_INPUT) {
            // todo we need a more specific exception, but MalformedInputException requires a position
            throw new IllegalArgumentException("Invalid input");
        }
        if (result == SNAPPY_BUFFER_TOO_SMALL) {
            throw new IllegalArgumentException("Output buffer is too small");
        }
        if (result != SNAPPY_OK) {
            throw new IllegalArgumentException("Unknown error occurred during decompression: result=" + result);
        }
    }

    private static final MethodHandle MAX_COMPRESSED_LENGTH_METHOD = lookupMethod("snappy_max_compressed_length", FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));

    private static long maxCompressedLengthInternal(long inputLength)
    {
        try {
            return (long) MAX_COMPRESSED_LENGTH_METHOD.invokeExact(inputLength);
        }
        catch (Throwable t) {
            throw new AssertionError("should not reach here", t);
        }
    }

    private static final MethodHandle UNCOMPRESSED_LENGTH_METHOD = lookupMethod("snappy_uncompressed_length", FunctionDescriptor.of(JAVA_INT, C_POINTER, JAVA_LONG, C_POINTER));

    private static void decompressedLengthInternal(MemorySegment compressed, long compressedLength, MemorySegment decompressedLength)
    {
        int result;
        try {
            result = (int) UNCOMPRESSED_LENGTH_METHOD.invokeExact(compressed, compressedLength, decompressedLength);
        }
        catch (Throwable t) {
            throw new AssertionError("should not reach here", t);
        }
        if (result == SNAPPY_INVALID_INPUT) {
            throw new IllegalArgumentException("Invalid input");
        }
        if (result != SNAPPY_OK) {
            throw new IllegalArgumentException("Unknown error occurred during decompressed length calculation: result=" + result);
        }
    }

    private static final MethodHandle VALIDATE_METHOD = lookupMethod("snappy_validate_compressed_buffer", FunctionDescriptor.of(JAVA_INT, C_POINTER, JAVA_LONG));

    private static boolean validateInternal(MemorySegment compressed, long compressedLength)
    {
        try {
            int result = (int) VALIDATE_METHOD.invokeExact(compressed, compressedLength);
            return result == SNAPPY_OK;
        }
        catch (Throwable t) {
            throw new AssertionError("should not reach here", t);
        }
    }
}
