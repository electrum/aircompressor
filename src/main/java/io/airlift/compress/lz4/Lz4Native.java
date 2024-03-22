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
package io.airlift.compress.lz4;

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

public final class Lz4Native
{
    private static final AddressLayout C_POINTER = ValueLayout.ADDRESS
            .withTargetLayout(MemoryLayout.sequenceLayout(Long.MAX_VALUE, JAVA_BYTE));

    private static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup
            .libraryLookup("/opt/homebrew/lib/liblz4.dylib", Arena.ofAuto())
            .or(SymbolLookup.loaderLookup())
            .or(Linker.nativeLinker().defaultLookup());

    private Lz4Native() {}

    private static MethodHandle lookupMethod(String name, FunctionDescriptor functionDescriptor)
    {
        return Linker.nativeLinker().downcallHandle(
                SYMBOL_LOOKUP.find(name)
                        .orElseThrow(() -> new UnsatisfiedLinkError("unresolved symbol: " + name)),
                functionDescriptor,
                Linker.Option.critical(true));
    }

    private static final MethodHandle MAX_COMPRESSED_LENGTH_METHOD = lookupMethod("LZ4_compressBound", FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    public static int maxCompressedLength(int inputLength)
    {
        try {
            return (int) MAX_COMPRESSED_LENGTH_METHOD.invokeExact(inputLength);
        }
        catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }
    }

    private static final MethodHandle COMPRESS_METHOD = lookupMethod("LZ4_compress_default", FunctionDescriptor.of(JAVA_INT, C_POINTER, C_POINTER, JAVA_INT, JAVA_INT));

    public static int compress(MemorySegment input, int inputLength, MemorySegment compressed, int compressedLength)
    {
        int result;
        try {
            result = (int) COMPRESS_METHOD.invokeExact(input, compressed, inputLength, compressedLength);
        }
        catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }

        // LZ4_compress_default returns 0 on error, but disallow negative values also
        if (result <= 0) {
            throw new IllegalArgumentException("Unknown error occurred during compression: result=" + result);
        }
        return result;
    }

    private static final MethodHandle DECOMPRESS_METHOD = lookupMethod("LZ4_decompress_safe", FunctionDescriptor.of(JAVA_INT, C_POINTER, C_POINTER, JAVA_INT, JAVA_INT));

    public static int decompress(MemorySegment compressed, int compressedLength, MemorySegment output, int outputLength)
    {
        int result;
        try {
            result = (int) DECOMPRESS_METHOD.invokeExact(compressed, output, compressedLength, outputLength);
        }
        catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }

        // negative return values indicate errors
        if (result < 0) {
            throw new IllegalArgumentException("Unknown error occurred during decompression: result=" + result);
        }
        return result;
    }
}
