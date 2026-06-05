# Automated Theorem Prover
---

## 1. Architecture description
The project is a hybrid application designed to evaluate propositional logic formulas. <br>
It uses a **Java Spring Boot** backend to parse theorems and reduce them into Exact Cover / CNF flat matrices, 
which are then solved via **JNI** using a **C++** implementation of **Knuth's DLX (Dancing Links X algorithm)**.
The results are displayed through a responsive Angular interface.

[image]

## How to run

**Prerequisites:** Docker <br>
Clone repository, navigate to the root directory and run the following command:
```
    docker-compose up --build -d
```
The graphical interface is exposed on **http://localhost:4200**. The Spring Boot REST API binds to port **8080**.

---

## 2. Technical specifications

### 2.1. Formal Context and Problem Space

The application functions as a refutation-based theorem prover. It determines whether a specific logical theorem is a logical consequence of a set of given axioms by attempting to find a contradiction. <br>
The system operates strictly within Propositional Logic (Zero-order logic), **reducing a Boolean Satisfiability Problem (SAT) to an Exact Cover problem**. Both SAT and Exact Cover belong to the NP-complete complexity class, meaning the worst-case execution time grows exponentially, bounded by **$O(2^n)$**. <br>
While the input syntax accepts First-Order Logic quantifiers ($\forall$, $\exists$), the system does not evaluate infinite domains. It expands these quantifiers over finite, predefined domains, reducing them to standard propositional logic before execution.

---

### 2.2. System Architecture and Interoperability

* **Frontend (Angular):** The graphical interface
* **Backend (Java Spring Boot):** Receives the input, parses the logical syntax, applies mathematical rewrite rules, and builds the Exact Cover matrix.
* **Native Module (C++ / CMake):** High performance DLX implementation that solves the exact cover problem for the flat matrix built.
* **Infrastructure:** Orchestrated into containers with docker-compose

**Java-C++ Memory Interoperability (NativeInterop):**
Transferring complex objects (like Java Abstract Syntax Trees or 2D arrays) to C++ incurs severe serialization overhead. To achieve minimal-copy overhead, the Java backend flattens the 2D Exact Cover matrix into a 1D primitive `int` array. <br>

Unlike alternative DLX implementations that use `-1` delimiters to separate rows in a 1D sequence, this architecture relies on a strictly rectangular, fixed-width grid (passing the column count explicitly alongside the array). This approach guarantees predictable memory allocation, eliminates conditional branching during C++ parsing, and maximizes CPU cache alignment. <br>

Using the Java Native Interface (JNI), the JVM passes a direct memory pointer to this array. The C++ engine reads the matrix structure directly from this shared memory space, constructs its internal structures, and writes the output back, avoiding costly garbage collection events.

---

### 2.3. The Execution Pipeline

1. **Lexical and Syntactic Analysis:**
    * The input string is broken down into discrete tokens (Lexer).
    * The tokens are evaluated against grammar rules to build an Abstract Syntax Tree (AST) representing the logical hierarchy (Parser).
2. **Grounding:**
    * The system extracts constants and expands quantified nodes (`FORALL`, `EXISTS`) into finite sets of `AND` and `OR` operations.
3. **CNF Transformation:**
    * The AST is flattened into Conjunctive Normal Form (CNF) by eliminating implications, applying De Morgan's laws (pushing negations down), and distributing disjunctions over conjunctions. The result is a flat list of clauses.
4. **Unit Propagation:**
    * Before generating the matrix, the system scans the CNF for unit clauses (clauses with only one literal). It forces these assignments and mathematically simplifies the remaining clauses.
5. **Matrix Generation:**
    * The CNF list is mapped into an Exact Cover matrix. Variables and clauses are mapped to columns. Truth assignments are mapped to rows. A `1` is placed where a specific assignment satisfies a specific constraint.
6. **Native Execution and Interpretation:**
    * The C++ module algorithm searches for a subset of rows that satisfy all constraints.

---

### 2.4. Native C++ Engine: Donald Knuth's DLX

**Matrix Format and Data Structure:**
The 1D array received from Java is reconstructed in C++ into a highly sparse, toroidal doubly linked list. Every node in the matrix possesses four pointers (`up`, `down`, `left`, `right`). 
* Columns represent constraints (variables to be assigned and clauses to be satisfied).
* Rows represent the possible state assignments.

**Algorithm Dynamics:**
The solver uses Depth-First Search (DFS). When a row is selected, the corresponding columns and conflicting rows are "covered" (removed from the matrix). DLX achieves this by unlinking nodes through simple pointer reassignment in $O(1)$ constant time. When backtracking, nodes are "uncovered" by restoring the pointers. No dynamic memory allocation or deletion occurs during the recursive search.

**Heuristics and Kill Flags:**
The algorithm employs the Minimum Remaining Values (MRV) heuristic, always choosing the column with the smallest number of 1's, optimizing considerably the backtracking process. <br>
Due to the NP-complete nature of the problem, highly symmetric inputs cause deterministic combinatorial explosions. The C++ engine continually polls the execution time during the search, so that when the time limit of 2500 ms is exceeded, the search is stopped and a timeout state is returned to the JVM.

---

## 3. Execution Trace: Quantifier Duality

**Theorem:** $\exists x P(x) \implies \neg \forall y \neg P(y)$
**Domain:** $D = \{a, b\}$

### 3.1. Refutation Setup
To prove validity, the system negates the theorem to search for a contradiction. Let $F$ be the evaluation formula:
$$F = \neg (\exists x P(x) \implies \neg \forall y \neg P(y))$$

**AST Construction:**
```
    NOT
     |
    IMPLIES
     ├── EXISTS (x) -> P(x)
     └── NOT
          |
         FORALL (y) -> NOT -> P(y)
```
### 3.2. Grounding (Domain Expansion)
First-Order constructs are eliminated by instantiating variables over $D$:
* $\exists x P(x) \longrightarrow P(a) \lor P(b)$
* $\forall y \neg P(y) \longrightarrow \neg P(a) \land \neg P(b)$

**Substituting:** Let $A = P(a)$ and $B = P(b)$.
$$F = \neg ((A \lor B) \implies \neg (\neg A \land \neg B))$$

### 3.3. CNF Transformation
The formula is flattened using strict rewrite rules:

1. **Implication ($X \implies Y \equiv \neg X \lor Y$):**
   $$F = \neg (\neg (A \lor B) \lor \neg (\neg A \land \neg B))$$
2. **De Morgan & Double Negation:**
   $$F = \neg \neg (A \lor B) \land \neg \neg (\neg A \land \neg B)$$
   $$F = (A \lor B) \land (\neg A \land \neg B)$$
3. **Clause Extraction:**
   $C_1: A \lor B$
   $C_2: \neg A$
   $C_3: \neg B$

### 3.4. Exact Cover Matrix
Clauses map to a binary grid. Columns = Constraints. Rows = State assignments.
```
    Variables: A, B  |  Clauses: C1, C2, C3
    Columns:    [A] [B] [C1] [C2] [C3]
    ------------------------------------
    R1 (A=T):    1   0   1    0    0
    R2 (A=F):    1   0   0    1    0
    R3 (B=T):    0   1   1    0    0
    R4 (B=F):    0   1   0    0    1
```
Array passed to C++: [1,0,1,0,0, 1,0,0,1,0, 0,1,0,1,0, 0,1,0,0,1]

### 3.5. Native DLX Search
Algorithm X searches for a subset of rows covering every column exactly once.

* **Iteration 1 (Target: C2):** MRV selects column [C2] (only 1 option).
  -> Select R2 (A=F).
  -> Cover [A], [C2]. Conflicting R1 is eliminated.
* **Iteration 2 (Target: C3):** MRV selects column [C3] (only 1 option).
  -> Select R4 (B=F).
  -> Cover [B], [C3]. Conflicting R3 is eliminated.
* **Iteration 3 (Target: C1):** Column [C1] requires R1 or R3. 
  -> Both R1 and R3 are already eliminated. 
  -> Available rows for [C1] = 0.

**Conclusion:** DFS hits a deterministic dead end. Matrix has no Exact Cover. 
$F$ is unsatisfiable $\implies$ The original theorem is valid.
