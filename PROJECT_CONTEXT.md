# Project Context: BiLight

**Navigation:** [README.md](README.md) (build, CLI, input/output formats)

---

## 1. Project Overview
**Name:** BiLight  
**Language:** Java

**Goal:**  
The project is designed to efficiently estimate the counts of specific **6-node bipartite graph motifs** (subgraphs) in large-scale networks. Exact enumeration is computationally infeasible due to motif size and combinatorial explosion. Therefore, the project relies on **randomized path sampling** and **unbiased estimators**.

This project is inspired by and methodologically aligned with prior work on motif estimation, particularly:
- *Motif Counting Beyond Five Nodes* (Bressan et al., 2018)
- Recent work on bipartite motif estimation (e.g., butterfly motifs)

---

## 2. Core Methodology: Path Sampling & Estimation

The central idea is to sample **4-paths** (paths of length 4) from a graph and use them to estimate global counts of larger (6-node) motifs.

**Generalizability:**  
While the project currently focuses on **bipartite motifs**, the underlying **path sampling methodology is general**. The `Graph` class and the sampling logic do not enforce bipartite constraints; they operate on any undirected graph. If one were to implement counting logic for non-bipartite motifs (e.g., triangles, cliques), the same sampling framework would apply.

### Estimation Formula

Implemented in `src/graph/Sample.java`.

For a set of $k$ sampled 4-paths, the estimated count for a motif $M$ is:

$$
\hat{N}(M) = \frac{1}{k} \sum_{i=1}^{k}
\frac{count(M \mid P_i) \times N_{\text{total\_4paths}}}{C_M}
$$

Where:
- $P_i$ is the $i$-th sampled 4-path.
- $count(M \mid P_i)$ is the number of extensions of path $P_i$ into motif $M$  
  (computed in `ExactCountNonInduced.java`).
- $N_{\text{total\_4paths}}$ is the total number of 4-paths in the graph.
- $C_M$ is the **motif coefficient**, i.e., the number of distinct 4-paths contained in one instance of motif $M$.

This estimator is **unbiased**, following the same theoretical principles as Motivo.

---

## 3. Sampling Strategy

### Edge Sampling
- Each edge $(u,v)$ carries weight $w_e = (d(u)-1)(d(v)-1) - |\Gamma(u) \cap \Gamma(v)|$, the exact
  number of 4-paths centred on it. The common-neighbour term discounts the triangles through $e$, and
  is zero on a bipartite host.
- Edges are then sampled proportionally to $w_e$ using the **Alias Method** for $O(1)$ sampling.
- Implemented in `Graph.java`.

### Path Extension
- A sampled edge is extended by randomly choosing one neighbour on each side to form a 4-path.
- Draws where the two endpoints coincide ($v_0 = v_3$, possible only via a triangle) are rejected and
  the pair redrawn, which keeps the resulting 4-path uniform on any host graph.

---

## 4. Data Processing & Graph Construction

### Input Handling
- Supports CSV, TSV, and generic delimited text files.
- Separator configurable via `-sep` argument.
- Node IDs may be integers or strings.

### Graph Construction Logic

Implemented in both `CreateGraph.java` and `FileFormatter.java`.

1. **ID Parsing & Mapping:**
   - The system attempts to parse node IDs as integers.
   - If IDs are strings (e.g., "A123", "B456"), they are mapped to unique internal integers using a `HashMap`.

2. **Graph Structure:**
   - The graph is stored as an adjacency list.
   - Edges are undirected.
   - The input is treated as a valid edge list where connections define the structure; the graph is then reduced to its largest connected component before sampling.

---

## 5. Motif Definitions

The project currently supports **17 non-isomorphic 6-node bipartite motifs** (M1–M17).

Each motif has:
- An ID
- A symbolic name
- A coefficient ($C_M$)
- A motif-specific extension counting function

Defined in `src/graph/Motif.java`.

| Motif | Name | Coefficient | Logic Source |
|------|------|-------------|--------------|
| M1 | NBNM | 36 | `fourPathM1Count` |
| M2 | ADPM | 24 | `fourPathM2Count` |
| ... | ... | ... | ... |
| M17 | AGJE | 3 | `fourPathM17Count` |

**Note on M12:** Motif M12 is excluded from the estimation process because its exact count can be easily computed analytically or via simpler methods, making estimation unnecessary.

---

## 6. Accuracy & Error Measurement (Very Important)

### Motivation

Exact motif counts are **not computable** for large graphs and 6-node motifs. Therefore, following **Bressan et al. (Motivo)**, accuracy is evaluated by comparing estimates against **high-sample reference runs**, not exact ground truth.

This is standard practice in motif estimation research.

---

### Reference (Ground Truth Approximation)

- The estimator is run with a very large sampling budget (e.g., 100,000 samples).
- If multiple runs exist, results are averaged.
- This reference vector is treated as the **ground truth distribution**.

---

### Per-Run Error Calculation

For a given run at a smaller sample size:

1. **Relative Error per Motif**
   $$
   \text{RE}_m = \frac{|\hat{N}_m - N^{\text{ref}}_m|}{N^{\text{ref}}_m}
   $$

2. **1-Norm Accuracy / Average Distance Error**
   $$
   \text{ErrorScore} = \frac{1}{|\mathcal{M}|} \sum_{m \in \mathcal{M}} \text{RE}_m
   $$

This corresponds exactly to what **Motivo calls “1-norm accuracy”**.

> Note: The term *Average Distance Error* is mathematically equivalent; the paper uses *1-norm accuracy* terminology.

---

### Aggregation Across Multiple Runs

For each sample size:
- **Dot (Mean):** Average of `ErrorScore` across independent runs.
- **Bar (Std Dev):** Standard deviation of `ErrorScore` across runs.

This measures:
- Accuracy (mean convergence)
- Stability (variance due to randomness)

---

## 7. Visualization Philosophy

Plots mirror those in *Motif Counting Beyond Five Nodes*:

- X-axis: Number of samples
- Y-axis: 1-norm accuracy (average relative error)
- Error bars: Standard deviation across runs

Additionally:
- Convergence plots may show estimated motif distributions approaching the reference distribution, similar to Fig. 2 in Motivo.

---

## 8. Relation to Prior Work

### Motif Counting Beyond Five Nodes (Bressan et al., 2018)
- Focuses on **general graphs**, not bipartite only.
- Uses **color-coding** and **Monte Carlo sampling**.
- Evaluates accuracy using **relative error averaged across motifs** (1-norm).
- Uses **high-sample estimates as ground truth**.

This project follows the **same evaluation philosophy**, adapted to bipartite graphs and path sampling.

---

### Butterfly Counting Papers
- Butterfly motifs are 4-node structures.
- Exact counting is feasible due to special combinatorial properties.
- Accuracy is often measured against exact counts.

**Important distinction:**  
Exact counting is *not feasible* for 6-node bipartite motifs, which justifies the use of approximate ground truth in this project and in Motivo.

---

## 9. Project Structure & Key Classes

### `src/graph`
- `Graph.java`
- `CreateGraph.java`
- `Sample.java`
- `ExactCountNonInduced.java`
- `Motif.java`
- `FourPath.java`
- `Node.java`

### `src/fileformatter`
- `FileFormatter.java`

### `src/app`
- `Main.java`

---

## 10. Compilation & Execution

### Compilation
```bash
javac -d out -sourcepath src src/app/Main.java src/fileformatter/FileFormatter.java
```

### File Formatting
```bash
java -cp out fileformatter.FileFormatter motivo "input.csv" "output.txt" -sep ","
```

### Main Execution
```bash
java -cp out app.Main -p "input.txt" -sep "," -samp 100000
```

See [README.md](README.md) for the full CLI reference.

## 11. Key Conceptual Takeaways (For AI & Humans)

- Estimators are unbiased
- Exact counting is infeasible for large 6-node motifs
- Accuracy evaluation follows Motivo
- 1-norm accuracy ≡ average relative error
- Error bars represent run-to-run variability, not motif variability