# BiLight

Unbiased estimation of **6-vertex bipartite graphlet frequencies** in large graphs, via weighted 4-path sampling.

Exact enumeration of 6-vertex motifs is infeasible on large networks. BiLight samples 4-paths using the alias
method (`O(1)` per sample), counts each sampled path's local extensions exactly, and rescales them into unbiased
global estimates for 17 non-isomorphic bipartite motifs.

This is the reference implementation accompanying the paper.

## Requirements

Java 11 or newer. **No external dependencies** — the estimator is pure JDK.

## Build

```bash
javac -d out -sourcepath src src/app/Main.java src/fileformatter/FileFormatter.java
```

## Run

```bash
java -cp out app.Main -p path/to/edgelist.txt [-sep SEPARATOR] [-samp SAMPLE_COUNT]
```

| Flag | Required | Default | Meaning |
|------|----------|---------|---------|
| `-p` | yes | — | Path to the input edge list |
| `-sep` | no | tab | Field separator (e.g. `,` for CSV, `" "` for space) |
| `-samp` | no | sweep | Single sample budget. If omitted, runs a sweep: 10; 50; 200; 500; 1 000; 5 000; 20 000; 50 000; 100 000 |

### Input format

One edge per line, two fields separated by `-sep`:

```
alice    book1
alice    book2
bob      book1
```

Vertex IDs may be integers or arbitrary strings — strings are mapped to internal integer IDs automatically.
Malformed lines are skipped with a warning. The graph is reduced to its **largest connected component**
before sampling.

### Output

Results are printed to stdout and appended to `results.txt` in the working directory: graph statistics
(vertices, edges, total 4-path count, setup time), then one row per sample budget with the 17 motif
estimates in scientific notation, the run time in milliseconds and the number of rejected
degenerate draws. The star `M12` (K1,5) contains no 4-path, so it is not sampled: its column holds
the exact count `sum_v C(deg(v), 5)`, the same in every row. The run time covers sampling only;
the one-time setup (loading, largest component, edge sampler, `M12`) is reported separately.

## Tests

The counting patterns are verified against an independent brute-force oracle. For every motif and
every test graph the suite enumerates the **entire** sampler seed space and checks the exact identity
the estimator rests on:

```
    sum over the seed space of ext(M, P)  ==  coefficient(M) * N(M)
```

where `N(M)` is counted straight from the definition (every injective edge-preserving map of the
motif's 6 vertices, divided by |Aut(M)|) -- no formulas, no inclusion-exclusion, so the oracle cannot
share a bug with the expressions it judges. The oracle is in turn audited against networkx's
subgraph-monomorphism engine.

```bash
javac -d out $(find src -name '*.java')
javac -cp out -d out test/BruteForceCount.java test/ExactCountTest.java
java -cp out:. ExactCountTest test/graphs/*.tsv     # exit 0 = all pass

python3 test/verify_with_networkx.py                # independent audit of the oracle
python3 test/verify_with_networkx.py --self         # pure-python fallback, no dependencies
```

The suite also checks the analytic `M12` formula (and that the value the estimator outputs for `M12` matches it) and that the sampler's `totalFourPathCount` equals
the enumerated non-degenerate seed count. See [test/README.md](test/README.md).

## Format conversion

`FileFormatter` converts an edge list into the input formats of the baselines used for comparison:

```bash
java -cp out fileformatter.FileFormatter motivo input.csv output.txt -sep ","
java -cp out fileformatter.FileFormatter pgd    input.csv output.txt -sep ","
```

## Layout

| Path | Role |
|------|------|
| `src/app/Main.java` | CLI entry point, sampling sweep, result reporting |
| `src/graph/Graph.java` | Adjacency storage, largest connected component, alias-method edge sampler |
| `src/graph/CreateGraph.java` | Edge-list parsing and vertex ID mapping |
| `src/graph/Sample.java` | Unbiased estimator |
| `src/graph/ExactCountNonInduced.java` | Exact local extension counts for a sampled 4-path |
| `src/graph/Motif.java` | The 17 motif definitions and their coefficients |
| `src/graph/FourPath.java`, `Node.java` | Supporting data structures |
| `src/fileformatter/FileFormatter.java` | Export to Motivo / PGD input formats |
| `test/` | Correctness suite: brute-force oracle, 9 host graphs, networkx audit |

Methodology, the estimator derivation, and the accuracy evaluation protocol are documented in
[PROJECT_CONTEXT.md](PROJECT_CONTEXT.md).

## License

[MIT](LICENSE)
