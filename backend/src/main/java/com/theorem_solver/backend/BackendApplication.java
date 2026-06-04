package com.theorem_solver.backend;

import com.theorem_solver.backend.compiler.ProverResult;
import com.theorem_solver.backend.compiler.TheoremProverService;
import com.theorem_solver.backend.compiler.ast.LogicNode;
import com.theorem_solver.backend.compiler.lexer.*;
import com.theorem_solver.backend.compiler.parser.*;
import com.theorem_solver.backend.compiler.reducer.*;
import com.theorem_solver.backend.nativeinterop.NativeSolverService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.*;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}
}
