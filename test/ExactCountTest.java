import graph.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

/**
 * Verifies the real local-counting formulas (graph.ExactCountNonInduced, via
 * graph.Motif) against an independent brute-force oracle (BruteForceCount).
 *
 * For every motif M and every test graph, it checks the estimator's exact
 * identity that underpins the whole method:
 *
 *     sum over the sampler's seed space of ext(M, P)  ==  coefficient(M) * N(M)
 *
 * where N(M) is the true non-induced count. It also checks:
 *   - the star M12 analytic formula: N(M12) == sum_v C(deg(v), 5)
 *   - the sampler weight: totalFourPathCount == number of non-degenerate seeds
 *
 * Works on any host graph (bipartite or general). Exit code 0 on success,
 * 1 on any mismatch, so it can be wired into CI.
 *
 * Usage:  java -cp <out>:. ExactCountTest test/graphs/*.tsv
 */
public class ExactCountTest {

    static Graph load(String path) throws Exception {
        Graph g = new Graph();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split("\\s+");
                if (p.length >= 2) g.connect(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
            }
        }
        g.buildAdjacencyArrays();
        g.buildEdgeSampler();
        return g;
    }

    static long combN5(int d) {                    // C(d,5)
        if (d < 5) return 0;
        long r = 1;
        for (int i = 0; i < 5; i++) r = r * (d - i);
        return r / 120L;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: ExactCountTest <graph.tsv> [more.tsv ...]");
            System.exit(2);
        }

        HashMap<String, Motif> byId = new HashMap<>();
        for (Motif m : Motif.MOTIFS) byId.put(m.id, m);

        int checks = 0, failures = 0;
        // ground-truth counts, emitted for the independent networkx audit
        PrintWriter truth = new PrintWriter("test/expected_counts.csv");
        truth.println("graph,motif,N");

        for (String path : args) {
            Graph g = load(path);
            String tag = path.substring(path.lastIndexOf('/') + 1);
            System.out.printf("%n=== %s  (%d nodes, %d edges, bipartite=%s, seedSpace=%d) ===%n",
                    tag, g.map.size(), g.edge_list.size(), g.hostIsBipartite, g.totalFourPathCount);

            // sum ext(M,P) over the sampler's seed space; also count seeds
            long[] formulaSum = new long[Motif.MOTIFS.size()];
            long seeds = 0;
            for (int[] e : g.edge_list) {
                Node v1 = g.map.get(e[0]);
                Node v2 = g.map.get(e[1]);
                for (int a : v1.adjacencyArray) {
                    if (a == v2.id) continue;
                    for (int b : v2.adjacencyArray) {
                        if (b == v1.id || b == a) continue;      // skip central edge and degenerate v0==v3
                        seeds++;
                        FourPath fp = new FourPath(g.map.get(a), v1, v2, g.map.get(b));
                        for (int mi = 0; mi < Motif.MOTIFS.size(); mi++) {
                            formulaSum[mi] += Motif.MOTIFS.get(mi).count(fp, g);
                        }
                    }
                }
            }

            // (0) sampler weight: total four-path count must equal the enumerated seed space
            checks++;
            boolean seedOk = (seeds == g.totalFourPathCount);
            if (!seedOk) failures++;
            System.out.printf("  %-5s seedSpace=%d  totalFourPathCount=%d  %s%n",
                    "P4", seeds, g.totalFourPathCount, seedOk ? "OK" : "*** MISMATCH ***");

            // (1) sampling motifs: sum ext == coefficient * N
            for (String id : BruteForceCount.samplingMotifIds()) {
                Motif m = byId.get(id);
                int mi = Motif.MOTIFS.indexOf(m);
                long truthN = BruteForceCount.countNonInduced(g, id);
                truth.printf("%s,%s,%d%n", tag, id, truthN);
                long expected = (long) m.coefficient * truthN;
                long got = formulaSum[mi];
                checks++;
                boolean ok = (got == expected);
                if (!ok) failures++;
                System.out.printf("  %-5s N=%-6d  coeff*N=%-8d  sum ext=%-8d  %s%n",
                        id, truthN, expected, got, ok ? "OK" : "*** MISMATCH ***");
            }

            // (2) star M12: analytic formula vs brute force
            long m12brute = BruteForceCount.countNonInduced(g, "M12");
            truth.printf("%s,%s,%d%n", tag, "M12", m12brute);
            long m12analytic = 0;
            for (Node n : g.map.values()) m12analytic += combN5(n.degree);
            checks++;
            boolean m12ok = (m12brute == m12analytic);
            if (!m12ok) failures++;
            System.out.printf("  %-5s N=%-6d  sum C(deg,5)=%-8d  %s%n",
                    "M12", m12brute, m12analytic, m12ok ? "OK" : "*** MISMATCH ***");

            // (2b) the value the estimator actually outputs for M12 (Graph.starCountM12,
            //      used by Sample.estimate) must equal the brute-force count too
            double m12graph = g.starCountM12();
            checks++;
            boolean m12graphOk = (m12graph == (double) m12brute);
            if (!m12graphOk) failures++;
            System.out.printf("  %-5s N=%-6d  Graph.starCountM12=%-8.0f  %s%n",
                    "M12", m12brute, m12graph, m12graphOk ? "OK" : "*** MISMATCH ***");
        }

        truth.close();
        System.out.printf("%n%d checks, %d failures%n", checks, failures);
        System.out.println("ground-truth counts written to test/expected_counts.csv (for verify_with_networkx.py)");
        System.out.println(failures == 0 ? "ALL TESTS PASSED" : "TESTS FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
