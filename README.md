# Automated Theorem Prover

---
1. [Project description](#1-project-description)
   - [HowToRun](#how-to-run)
2. [Technical specifications](#2-technical-specifications)
3. [Example: Quantifier Duality](#3-example-quantifier-duality)
---

## 1. Project description
The project is a hybrid application designed to evaluate propositional logic formulas. <br>
It uses a **Java Spring Boot** backend to parse theorems and reduce them into Exact Cover / CNF flat matrices, 
which are then solved via **JNI** using a **C++** implementation of **Knuth's DLX (Dancing Links X algorithm)**.
The results are displayed through a responsive Angular interface.

<img width="957" height="500" alt="image" src="https://github.com/user-attachments/assets/4b02f4c1-d31a-43a3-a352-33cd0e7efa8c" />


## How to run

**Prerequisites:** Docker <br>
Clone repository, navigate to the root directory and run the following command:

    docker-compose up --build -d

The graphical interface is exposed on **http://localhost:4200**. The Spring Boot REST API binds to port **8080**.

---

## 2. Technical specifications

### 2.1. Formal Context and Problem Space

The application functions as a refutation-based theorem prover. It determines whether a specific logical theorem is a logical consequence of a set of given axioms by attempting to find a contradiction. <br>
The system operates strictly within Propositional Logic (Zero-order logic), **reducing a Boolean Satisfiability Problem (SAT) to an Exact Cover problem**. Since both SAT and Exact Cover belong to the NP-complete complexity class, yhis means the worst-case execution time grows exponentially, bounded by **$O(2^n)$**. <br>
While the input syntax accepts First-Order Logic quantifiers ($\forall$, $\exists$), the system utilizes Skolemization and dynamic domain instantiation to reduce first-order logic into standard propositional logic before execution.

---

### 2.2. System Architecture and Interoperability

* **Frontend (Angular):** The graphical interface
* **Backend (Java Spring Boot):** Receives the input, parses the logical syntax, applies mathematical rewrite rules, and builds the Exact Cover matrix.
* **Native Module (C++ / CMake):** High performance DLX implementation that solves the exact cover problem for the flat matrix built.
* **Infrastructure:** Orchestrated into containers with docker-compose

**Java-C++ Memory Interoperability (NativeInterop):**
Transferring complex objects (like Java Abstract Syntax Trees or 2D arrays) to C++ can cause significant serialization overhead. To achieve minimal-copy overhead, the Java backend flattens the 2D Exact Cover matrix into a 1D primitive `int` array. <br>

Unlike alternative DLX implementations that use `-1` delimiters to separate only the active nodes in a 1D sequence, this architecture relies on a strictly rectangular, fixed-width grid padded with zeros. This approach ensures the horizontal integrity of the matrix—the `0`s act as rigid spatial constraints, ensuring that row elements are well-aligned and linked without altering their sequential order. Furthermore, this rigid format guarantees predictable memory allocation, eliminates conditional branching during C++ parsing, and maximizes CPU cache alignment. <br>

Using the Java Native Interface (JNI), the JVM passes a direct memory pointer to this array. The C++ engine reads the matrix structure directly from this shared memory space, constructs its internal structures, and writes the output back, avoiding costly garbage collection events.

---

### 2.3. The Execution Pipeline

1. **Lexical and Syntactic Analysis:**
    * The input string is broken down into discrete tokens (Lexer).
    * The tokens are evaluated against grammar rules to build an Abstract Syntax Tree (AST) representing the logical hierarchy (Parser).
2. **NNF Transformation & Grounding:**
    * The AST is first converted to Negation Normal Form (NNF) by eliminating implications and applying De Morgan's laws (pushing negations down). Existential quantifiers (`EXISTS`) are Skolemized to generate new constants dynamically, and universal quantifiers (`FORALL`) are expanded over this dynamic domain.
3. **CNF Transformation:**
    * Disjunctions are distributed over conjunctions. The result is a flat list of clauses.
4. **Unit Propagation:**
    * Before generating the matrix, the system scans the CNF for unit clauses (clauses with only one literal). It forces these assignments and mathematically simplifies the remaining clauses, potentially short-circuiting trivial contradictions without calling the native engine.
5. **Matrix Generation:**
    * The CNF list is mapped into an Exact Cover matrix. Variables and clauses are mapped to columns. Truth assignments are mapped to rows. A `1` is placed where a specific assignment satisfies a specific constraint.
6. **Native Execution and Interpretation:**
    * The C++ module algorithm backtrack-searches for a subset of rows that satisfy all constraints.

---

## 3. Example: Quantifier Duality

**Theorem:** $\exists x P(x) \implies \neg \forall y \neg P(y)$

### 3.1. Refutation Setup & NNF
To prove validity, the system negates the theorem to search for a contradiction. Let $F$ be the evaluation formula:
$$F = \neg (\exists x P(x) \implies \neg \forall y \neg P(y))$$

Before grounding, the parser applies implication elimination and De Morgan's laws to push the negation downwards (Negation Normal Form). This flips the initial quantifiers:
$$F = \exists x P(x) \land \forall y \neg P(y)$$

### 3.2. Grounding (Skolemization & Dynamic Domain)
First-Order constructs are eliminated dynamically:
* **Existential Instantiation:** $\exists x P(x)$ generates a new Skolem constant `sk_x_1`, populating the dynamic domain $D = \{sk_{x_1}\}$. The node reduces to $P(sk_{x_1})$.
* **Universal Instantiation:** $\forall y \neg P(y)$ expands over the new domain $D$, reducing to $\neg P(sk_{x_1})$.

**Substituting:** Let $A = P(sk_{x_1})$.
$$F = A \land \neg A$$

### 3.3. CNF Transformation & Unit Propagation
The formula is already a pure contradiction in CNF:
   $C_1: A$
   $C_2: \neg A$

*Note: The Java Unit Propagator immediately detects this contradiction. It identifies $C_1$ as a unit clause and forces $A = \text{True}$. Substituting this assignment into $C_2$ leaves an empty clause, proving mathematical impossibility. The Optimizer safely short-circuits, returning a 0x0 matrix and halting execution. To illustrate the C++ Native Engine mechanics, the theoretical Exact Cover matrix is mapped below.*

### 3.4. Exact Cover Matrix
Clauses map to a binary grid. Columns = Constraints. Rows = State assignments.

    Variables: A  |  Clauses: C1, C2
    Columns:    [A] [C1] [C2]
    -------------------------
    R1 (A=T):    1   1    0
    R2 (A=F):    1   0    1

Array passed to C++: [1,1,0, 1,0,1]

### 3.5. Native DLX Search
Algorithm X searches for a subset of rows covering every column exactly once.

* **Iteration 1 (Target: C1):** MRV selects column [C1] (only 1 option).
  -> Select R1 (A=T).
  -> Cover [A], [C1]. Conflicting R2 is eliminated.
* **Iteration 2 (Target: C2):** Column [C2] requires R2. 
  -> R2 is already eliminated. 
  -> Available rows for [C2] = 0.

**Conclusion:** DFS hits a deterministic dead end. Matrix has no Exact Cover. 
$F$ is unsatisfiable $\implies$ The original theorem is valid.

<img width="959" height="501" alt="image" src="https://github.com/user-attachments/assets/d0b15861-3748-4c88-91a2-ee68b50e78e7" />

