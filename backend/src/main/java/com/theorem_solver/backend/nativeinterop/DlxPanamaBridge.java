package com.theorem_solver.backend.nativeinterop;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class DlxPanamaBridge {
    private static final MethodHandle solveExactCoverHandle;

    static {
        System.loadLibrary("dlx_solver");

        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = SymbolLookup.loaderLookup();

        MemorySegment funcPointer = lookup.find("solveExactCover").orElseThrow(
                () -> new RuntimeException(" solveExactCover() not found in the library! ")
        );

        FunctionDescriptor descriptor = FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS, // const int* flat_matrix
                ValueLayout.ADDRESS, // int* solution_buffer
                ValueLayout.ADDRESS  // int* solution_size
        );

        solveExactCoverHandle = linker.downcallHandle(funcPointer, descriptor);
    }

    public int[] solve(int[] flatMatrix, int maxSolutionRows) {
        try (Arena arena = Arena.ofConfined()) {
            // Allocate space for the flatMatrix
            MemorySegment nativeMatrix = arena.allocateFrom(ValueLayout.JAVA_INT, flatMatrix);

            // Allocate space for solution's buffer
            MemorySegment solutionBuffer = arena.allocate(ValueLayout.JAVA_INT, maxSolutionRows);

            // Allocate space for solution's size (int)
            MemorySegment solutionSize = arena.allocate(ValueLayout.JAVA_INT);

            // Call C++ function
            solveExactCoverHandle.invokeExact(nativeMatrix, solutionBuffer, solutionSize);

            int actualSize = solutionSize.get(ValueLayout.JAVA_INT, 0);
            return solutionBuffer.asSlice(0, (long) actualSize * Integer.BYTES)
                    .toArray(ValueLayout.JAVA_INT);

        } catch (Throwable e) {
            throw new RuntimeException("Fatal error: DLX Solver call by Panama", e);
        }
    }
}