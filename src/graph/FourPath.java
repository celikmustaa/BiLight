package graph;

import java.util.HashMap;
import java.util.HashSet;

/**
 * A sampled seed path P = (v0, v1, v2, v3) with central edge (v1, v2), plus
 * lazily cached quantities shared by the local counting patterns. All caches
 * are valid on ANY host graph (bipartite or general): exclusions are explicit
 * rather than inferred from side-parity.
 */
public class FourPath {
    public Node[] node_list;
    public HashSet<Integer> ids;
    public int[] left_intersection_filtered; // Gamma(v1) ∩ Gamma(v3) \ {v0, v2}
    public int[] right_intersection_filtered; // Gamma(v0) ∩ Gamma(v2) \ {v1, v3}

    // caches (computed on first use)
    private int[] path_ext_degree = null;         // extDeg(v_i) = |Gamma(v_i) \ S|
    private long left_ext_degree_sum = -1;        // sum of extDeg(u) over u in I_L
    private long right_ext_degree_sum = -1;       // sum of extDeg(u) over u in I_R
    private long[] adjacent_ext_common = null;    // |Gamma'(v_j) ∩ Gamma'(v_k)| for (j,k)=(0,1),(1,2),(2,3),(3,0)
    private long il_ir_overlap = -1;              // |I_L ∩ I_R|

    public FourPath(Node node1, Node node2, Node node3, Node node4) {
        this.node_list = new Node[]{node1, node2, node3, node4};
        this.ids = new HashSet<>();
        for (Node node : node_list) ids.add(node.id);
        left_intersection_filtered = Node.getIntersectionExcluding(node_list[1], node_list[3], node_list[0].id, node_list[2].id);
        right_intersection_filtered = Node.getIntersectionExcluding(node_list[0], node_list[2], node_list[1].id, node_list[3].id);

    }

    // Checks if the 4-path forms a 4-cycle.
    public boolean isCycle() {
        return node_list[0].adjacency_list.contains(node_list[3].id);
    }

    /** |Gamma(u) \ {v0,v1,v2,v3}| for an arbitrary vertex u. */
    public int extDegreeOf(Node u) {
        int d = u.degree;
        for (Node p : node_list) {
            if (u.adjacency_list.contains(p.id)) d--;
        }
        return d;
    }

    /** extDeg(v_i): neighbors of the i-th path vertex outside the path. */
    public int getExtDegree(int i) {
        if (path_ext_degree == null) {
            path_ext_degree = new int[4];
            for (int j = 0; j < 4; j++) {
                path_ext_degree[j] = extDegreeOf(node_list[j]);
            }
        }
        return path_ext_degree[i];
    }

    /** sum of extDeg(u) over u in I_L. */
    public long getOrSetLeftExtDegreeSum(HashMap<Integer, Node> map) {
        if (left_ext_degree_sum == -1) {
            left_ext_degree_sum = 0;
            for (int id : left_intersection_filtered) {
                left_ext_degree_sum += extDegreeOf(map.get(id));
            }
        }
        return left_ext_degree_sum;
    }

    /** sum of extDeg(u) over u in I_R. */
    public long getOrSetRightExtDegreeSum(HashMap<Integer, Node> map) {
        if (right_ext_degree_sum == -1) {
            right_ext_degree_sum = 0;
            for (int id : right_intersection_filtered) {
                right_ext_degree_sum += extDegreeOf(map.get(id));
            }
        }
        return right_ext_degree_sum;
    }

    /**
     * |Gamma'(v_j) ∩ Gamma'(v_k)| for the four adjacent pairs of the seed,
     * indexed 0:(v0,v1), 1:(v1,v2), 2:(v2,v3), 3:(v3,v0). Zero on bipartite
     * hosts (the pairs sit on opposite sides).
     */
    public long getAdjacentExtCommon(int pairIndex) {
        if (adjacent_ext_common == null) {
            adjacent_ext_common = new long[4];
            int[][] pairs = {{0, 1}, {1, 2}, {2, 3}, {3, 0}};
            for (int p = 0; p < 4; p++) {
                Node a = node_list[pairs[p][0]];
                Node b = node_list[pairs[p][1]];
                Node smaller = (a.degree <= b.degree) ? a : b;
                Node larger  = (a.degree <= b.degree) ? b : a;
                long c = 0;
                for (int w : smaller.adjacencyArray) {
                    if (!ids.contains(w) && larger.adjacency_list.contains(w)) c++;
                }
                adjacent_ext_common[p] = c;
            }
        }
        return adjacent_ext_common[pairIndex];
    }

    /** |I_L ∩ I_R| (a vertex adjacent to all four path vertices; impossible on bipartite hosts). */
    public long getIlIrOverlap() {
        if (il_ir_overlap == -1) {
            il_ir_overlap = 0;
            HashSet<Integer> ir = new HashSet<>();
            for (int q : right_intersection_filtered) ir.add(q);
            for (int p : left_intersection_filtered) {
                if (ir.contains(p)) il_ir_overlap++;
            }
        }
        return il_ir_overlap;
    }
}
