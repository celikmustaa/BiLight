import graph.Graph;
import graph.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free ground-truth oracle for the tests.
 *
 * It counts non-induced copies of a motif directly from the definition:
 *   N(M) = (number of injective, edge-preserving maps V(M) -> V(host)) / |Aut(M)|
 * via straightforward backtracking. No formulas, no inclusion-exclusion --
 * the whole point is that this code is obviously correct so it can judge the
 * clever formulas in ExactCountNonInduced. (Cross-checked against networkx by
 * verify_with_networkx.py.)
 *
 * The 17 motif structures (6 vertices, id order matching graph.Motif) are the
 * single source of truth for the test graphs.
 */
public class BruteForceCount {

    /** Motif id -> undirected edges on vertices 0..5. */
    public static int[][] edgesOf(String id) {
        switch (id) {
            case "M1":  return new int[][]{{0,3},{0,4},{0,5},{1,3},{1,4},{1,5},{2,3},{2,4},{2,5}}; // K3,3
            case "M2":  return new int[][]{{0,2},{0,3},{0,4},{0,5},{1,2},{1,3},{1,4},{1,5}};       // K2,4
            case "M3":  return new int[][]{{0,3},{0,4},{0,5},{1,3},{1,4},{1,5},{2,3},{2,4}};       // K3,3 - e
            case "M4":  return new int[][]{{0,2},{0,3},{0,4},{1,2},{1,3},{1,4},{1,5}};             // K2,3 + pendant on hub
            case "M5":  return new int[][]{{0,1},{1,2},{2,3},{1,4},{3,4},{4,5},{2,5}};             // K2,3 + pendant on leaf
            case "M6":  return new int[][]{{0,3},{1,3},{2,4},{2,5},{0,4},{2,3},{1,5}};             // 2x3 grid
            case "M7":  return new int[][]{{0,2},{0,3},{1,2},{1,3},{1,4},{1,5}};                   // C4 + 2 pendants same corner
            case "M8":  return new int[][]{{0,1},{1,2},{2,3},{1,4},{4,5},{5,2}};                   // C4 + pendants adjacent corners
            case "M9":  return new int[][]{{0,1},{1,2},{2,3},{1,4},{3,4},{3,5}};                   // C4 + pendants opposite corners
            case "M10": return new int[][]{{0,1},{1,2},{2,3},{0,4},{4,5},{5,1}};                   // C4 + 2-edge tail
            case "M11": return new int[][]{{0,1},{1,2},{2,3},{3,4},{4,5},{5,0}};                   // C6
            case "M12": return new int[][]{{0,1},{0,2},{0,3},{0,4},{0,5}};                         // star K1,5
            case "M13": return new int[][]{{0,1},{1,4},{1,5},{1,2},{2,3}};                         // center 3 leaves + tail
            case "M14": return new int[][]{{0,1},{1,2},{2,3},{1,4},{2,5}};                         // double star
            case "M15": return new int[][]{{0,1},{0,2},{0,3},{1,4},{2,5}};                         // spider S(2,2,1)
            case "M16": return new int[][]{{0,1},{1,2},{2,3},{0,4},{0,5}};                         // broom
            case "M17": return new int[][]{{4,0},{0,1},{1,2},{2,3},{3,5}};                         // P6
            default: throw new IllegalArgumentException("unknown motif " + id);
        }
    }

    private static boolean[][] adjMatrix(int[][] edges) {
        boolean[][] a = new boolean[6][6];
        for (int[] e : edges) { a[e[0]][e[1]] = true; a[e[1]][e[0]] = true; }
        return a;
    }

    /** |Aut(M)|: permutations of {0..5} preserving the edge set. */
    public static long automorphisms(String id) {
        boolean[][] a = adjMatrix(edgesOf(id));
        return permCount(a, new int[6], new boolean[6], 0);
    }

    private static long permCount(boolean[][] a, int[] p, boolean[] used, int k) {
        if (k == 6) {
            for (int i = 0; i < 6; i++)
                for (int j = i + 1; j < 6; j++)
                    if (a[i][j] != a[p[i]][p[j]]) return 0;
            return 1;
        }
        long c = 0;
        for (int v = 0; v < 6; v++) {
            if (used[v]) continue;
            used[v] = true; p[k] = v;
            c += permCount(a, p, used, k + 1);
            used[v] = false;
        }
        return c;
    }

    /** Non-induced count of motif id in the host graph. */
    public static long countNonInduced(Graph g, String id) {
        boolean[][] m = adjMatrix(edgesOf(id));
        int[] hostIds = new int[g.map.size()];
        int idx = 0;
        for (int nid : g.map.keySet()) hostIds[idx++] = nid;

        long injective = backtrack(g, m, hostIds, new int[6], new boolean[hostIds.length], 0);
        long aut = automorphisms(id);
        if (injective % aut != 0) {
            throw new IllegalStateException("injective=" + injective + " not divisible by aut=" + aut + " for " + id);
        }
        return injective / aut;
    }

    /** Count injective edge-preserving maps of motif vertices 0..5 onto host vertices. */
    private static long backtrack(Graph g, boolean[][] m, int[] hostIds,
                                  int[] assign, boolean[] usedHost, int k) {
        if (k == 6) return 1;
        long c = 0;
        for (int hi = 0; hi < hostIds.length; hi++) {
            if (usedHost[hi]) continue;
            int cand = hostIds[hi];
            boolean ok = true;
            for (int j = 0; j < k; j++) {                       // check every motif edge (k,j) already placed
                if (m[k][j] && !g.map.get(cand).adjacency_list.contains(assign[j])) { ok = false; break; }
            }
            if (!ok) continue;
            assign[k] = cand; usedHost[hi] = true;
            c += backtrack(g, m, hostIds, assign, usedHost, k + 1);
            usedHost[hi] = false;
        }
        return c;
    }

    public static List<String> samplingMotifIds() {   // all except the star M12 (counted analytically)
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= 17; i++) if (i != 12) ids.add("M" + i);
        return ids;
    }
}
