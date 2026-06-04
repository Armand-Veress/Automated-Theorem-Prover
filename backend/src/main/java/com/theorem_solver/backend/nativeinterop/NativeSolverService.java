package com.theorem_solver.backend.nativeinterop;

import org.springframework.stereotype.Service;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.*;

@Service
public class NativeSolverService {

    private static final MethodHandle solveExactCoverHandle;

    static {
        System.loadLibrary("dlx_solver");

        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = SymbolLookup.loaderLookup();

        MemorySegment funcPointer = lookup.find("solveExactCover").orElseThrow(
                () -> new RuntimeException(" solveExactCover function not found in dlx_solver!")
        );

        FunctionDescriptor descriptor = FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS
        );

        solveExactCoverHandle = linker.downcallHandle(funcPointer, descriptor);
    }

    public int[] solve(List<List<String>> clauses) {
        if (clauses == null || clauses.isEmpty()) {
            return new int[0];
        }

        int[] flatMatrix = createFlatMatrix(clauses);
        int maxSolutionRows = clauses.size();

        return executeNativeCall(flatMatrix, maxSolutionRows);
    }

    private int[] createFlatMatrix(List<List<String>> clauses) {
        Set<String> uniqueLiterals = new java.util.LinkedHashSet<>();
        for (List<String> clause : clauses) {
            uniqueLiterals.addAll(clause);
        }
        List<String> columns = new ArrayList<>(uniqueLiterals);

        int rows = clauses.size();
        int cols = columns.size();

        int[] flatMatrix = new int[2 + (rows * cols)];
        flatMatrix[0] = cols;
        flatMatrix[1] = rows;

        int idx = 2;
        for (int i = 0; i < rows; i++) {
            List<String> clause = clauses.get(i);
            for (int j = 0; j < cols; j++) {
                if (clause.contains(columns.get(j))) {
                    flatMatrix[idx++] = j;
                } else {
                    flatMatrix[idx++] = -1;
                }
            }
        }
        return flatMatrix;
    }

    private int[] executeNativeCall(int[] flatMatrix, int maxSolutionRows) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nativeMatrix = arena.allocateFrom(ValueLayout.JAVA_INT, flatMatrix);
            MemorySegment solutionBuffer = arena.allocate(ValueLayout.JAVA_INT, maxSolutionRows);
            MemorySegment solutionSize = arena.allocate(ValueLayout.JAVA_INT);

            solveExactCoverHandle.invokeExact(nativeMatrix, solutionBuffer, solutionSize);

            int actualSize = solutionSize.get(ValueLayout.JAVA_INT, 0);

            if (actualSize == -1) {
                return new int[] {-1};
            }
            if (actualSize == 0) {
                return new int[0];
            }

            return solutionBuffer.asSlice(0, (long) actualSize * Integer.BYTES)
                    .toArray(ValueLayout.JAVA_INT);

        } catch (Throwable e) {
            throw new RuntimeException("Error at Panama call", e);
        }
    }
}