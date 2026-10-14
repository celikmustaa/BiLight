#!/usr/bin/env python3
"""
Independent audit of the test's ground-truth oracle, using networkx
(a widely-used third-party library, not our code).

The Java test (ExactCountTest) checks the counting FORMULAS against its own
brute-force oracle (BruteForceCount.java) and writes the oracle's non-induced
counts to test/expected_counts.csv. This script recomputes those same counts
with networkx's subgraph-monomorphism engine and confirms they match -- so the
oracle judging the formulas is itself checked by independent code.

Usage:
    # 1) run the Java test first to (re)generate expected_counts.csv:
    #    javac -d out $(find src -name '*.java') && javac -cp out -d out test/*.java
    #    java -cp out:. ExactCountTest test/graphs/*.tsv
    # 2) then:
    python3 test/verify_with_networkx.py           # needs networkx
    python3 test/verify_with_networkx.py --self     # no networkx: use a pure-python
                                                    # brute force (definition) instead

Exit code 0 if every count agrees, 1 otherwise.
"""
import csv
import itertools
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
GRAPHS = os.path.join(HERE, "graphs")
EXPECTED = os.path.join(HERE, "expected_counts.csv")

# The 17 motif structures (must match BruteForceCount.edgesOf / graph.Motif order).
MOTIF_EDGES = {
    "M1":  [(0,3),(0,4),(0,5),(1,3),(1,4),(1,5),(2,3),(2,4),(2,5)],
    "M2":  [(0,2),(0,3),(0,4),(0,5),(1,2),(1,3),(1,4),(1,5)],
    "M3":  [(0,3),(0,4),(0,5),(1,3),(1,4),(1,5),(2,3),(2,4)],
    "M4":  [(0,2),(0,3),(0,4),(1,2),(1,3),(1,4),(1,5)],
    "M5":  [(0,1),(1,2),(2,3),(1,4),(3,4),(4,5),(2,5)],
    "M6":  [(0,3),(1,3),(2,4),(2,5),(0,4),(2,3),(1,5)],
    "M7":  [(0,2),(0,3),(1,2),(1,3),(1,4),(1,5)],
    "M8":  [(0,1),(1,2),(2,3),(1,4),(4,5),(5,2)],
    "M9":  [(0,1),(1,2),(2,3),(1,4),(3,4),(3,5)],
    "M10": [(0,1),(1,2),(2,3),(0,4),(4,5),(5,1)],
    "M11": [(0,1),(1,2),(2,3),(3,4),(4,5),(5,0)],
    "M12": [(0,1),(0,2),(0,3),(0,4),(0,5)],
    "M13": [(0,1),(1,4),(1,5),(1,2),(2,3)],
    "M14": [(0,1),(1,2),(2,3),(1,4),(2,5)],
    "M15": [(0,1),(0,2),(0,3),(1,4),(2,5)],
    "M16": [(0,1),(1,2),(2,3),(0,4),(0,5)],
    "M17": [(4,0),(0,1),(1,2),(2,3),(3,5)],
}


def load_graph(path):
    adj = {}
    with open(path) as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            a, b = line.split()
            a, b = int(a), int(b)
            adj.setdefault(a, set()).add(b)
            adj.setdefault(b, set()).add(a)
    return adj


def automorphisms(edges):
    eset = {frozenset(e) for e in edges}
    return sum(
        1 for p in itertools.permutations(range(6))
        if all(frozenset((p[u], p[v])) in eset for u, v in edges)
    )


def count_networkx(adj, edges):
    import networkx as nx
    from networkx.algorithms.isomorphism import GraphMatcher
    G = nx.Graph()
    G.add_nodes_from(adj)
    for u in adj:
        for v in adj[u]:
            if u < v:
                G.add_edge(u, v)
    M = nx.Graph(); M.add_nodes_from(range(6)); M.add_edges_from(edges)
    mono = sum(1 for _ in GraphMatcher(G, M).subgraph_monomorphisms_iter())
    return mono // automorphisms(edges)


def count_pure_python(adj, edges):
    nodes = list(adj)
    inj = 0
    for perm in itertools.permutations(nodes, 6):
        if all(perm[v] in adj[perm[u]] for u, v in edges):
            inj += 1
    return inj // automorphisms(edges)


def main():
    self_mode = "--self" in sys.argv
    counter = count_pure_python if self_mode else count_networkx
    oracle = "pure-python brute force" if self_mode else "networkx"
    if not self_mode:
        try:
            import networkx  # noqa: F401
        except ImportError:
            print("networkx not installed; falling back to --self (pure-python brute force).")
            print("  install with:  python3 -m pip install networkx")
            counter = count_pure_python
            oracle = "pure-python brute force"

    if not os.path.exists(EXPECTED):
        print(f"missing {EXPECTED}; run ExactCountTest first.")
        sys.exit(2)

    expected = {}
    with open(EXPECTED) as f:
        for row in csv.DictReader(f):
            expected[(row["graph"], row["motif"])] = int(row["N"])

    graphs = {}
    for (g, _m) in expected:
        if g not in graphs:
            graphs[g] = load_graph(os.path.join(GRAPHS, g))

    print(f"auditing {len(expected)} (graph, motif) counts against: {oracle}\n")
    mism = 0
    for (g, m), n_java in sorted(expected.items()):
        n_ref = counter(graphs[g], MOTIF_EDGES[m])
        if n_ref != n_java:
            mism += 1
            print(f"  MISMATCH {g} {m}: java_oracle={n_java} {oracle}={n_ref}")
    if mism == 0:
        print(f"ALL {len(expected)} COUNTS CONFIRMED by {oracle}.")
    else:
        print(f"\n{mism} mismatches.")
    sys.exit(1 if mism else 0)


if __name__ == "__main__":
    main()
