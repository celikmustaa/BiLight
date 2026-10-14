# Tests — verifying the exact-count formulas

These tests check that the local counting patterns in
`src/graph/ExactCountNonInduced.java` are correct, on both bipartite and
general (non-bipartite) host graphs.

## What is verified

The estimator rests on one exact identity: for any host graph,

```
    sum over the sampler's seed space of  ext(M, P)   ==   coefficient(M) * N(M)
```

where `N(M)` is the true number of non-induced copies of motif `M`. The tests
enumerate the **entire** seed space of a small graph, sum the real
`ExactCountNonInduced` outputs, and compare against `coefficient(M) * N(M)` with
`N(M)` computed independently. They also check:

- **M12** (star `K1,5`, counted analytically, not sampled): `N(M12) == sum_v C(deg(v), 5)`.
- **the sampler weight**: `totalFourPathCount` equals the number of
  non-degenerate seed paths actually enumerated (this is the fix that removes
  triangle-induced degenerate seeds `v0 == v3` on general graphs).

## Two independent oracles (so the checker itself is checked)

1. `BruteForceCount.java` — a dependency-free ground truth. It counts
   non-induced copies straight from the definition (try every injective map of
   the motif's 6 vertices onto host vertices, keep the edge-preserving ones,
   divide by `|Aut(M)|`). It is deliberately "dumb" so it has no room for the
   subtle errors the clever formulas can have.

2. `verify_with_networkx.py` — recomputes the same `N(M)` with
   [networkx](https://networkx.org/)'s subgraph-monomorphism engine (code we did
   not write) and confirms it matches the counts the Java oracle produced. If
   networkx is not installed it falls back to a pure-python brute force
   (`--self`), so it always runs.

So: the **formulas** are checked against the **Java oracle**, and the **Java
oracle** is checked against **networkx**. Neither link is self-referential.

## Test graphs (`graphs/`)

- `anchor_k33`, `anchor_c6`, `anchor_p6` — tiny graphs with answers known by
  hand (K3,3 contains exactly one K3,3 and zero M9; C6 contains one C6; P6
  contains one P6). These confirm the oracle is calibrated correctly.
- `bipartite_small`, `bipartite_mid` — random bipartite graphs.
- `general_small`, `general_mid`, `general_dense` — random **non-bipartite**
  (triangle-containing) graphs, which exercise the general-host code paths.

Brute force is only tractable because these graphs are tiny; that is exactly
what is needed, since the formulas are graph-size-independent, so being exact on
every small graph is the property that matters.

## Running

```bash
# from the repo root
javac -d out $(find src -name '*.java')
javac -cp out -d out test/BruteForceCount.java test/ExactCountTest.java

# run the formula tests (exit code 0 = all pass); also writes test/expected_counts.csv
java -cp out:. ExactCountTest test/graphs/*.tsv

# independently audit the oracle's counts
python3 test/verify_with_networkx.py            # uses networkx if available
python3 test/verify_with_networkx.py --self      # pure-python, no dependencies
```

To add a graph, drop a two-column edge list (`u<TAB>v`, one edge per line) into
`graphs/` and re-run. Keep it small (≤ ~12 vertices) so brute force stays fast.
