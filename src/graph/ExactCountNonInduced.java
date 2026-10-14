package graph;

/**
 * Local counting patterns ext(M, P) for the 17 bipartite 6-vertex motifs,
 * valid on ANY host graph (bipartite or general).
 *
 * Conventions (P = (v0, v1, v2, v3), central edge (v1, v2)):
 *   S         = {v0, v1, v2, v3}
 *   extDeg(u) = |Gamma(u) \ S|   (neighbors outside the path; works for path
 *               vertices and extension vertices alike)
 *   I_L       = Gamma(v1) ∩ Gamma(v3) \ {v0, v2}
 *   I_R       = Gamma(v0) ∩ Gamma(v2) \ {v1, v3}
 *   isCycle   = v0 ~ v3
 *
 * On general hosts, same-side chords (v0~v2, v1~v3) and triangles through
 * extension vertices are possible; every formula below accounts for them via
 * explicit path-exclusion (extDeg), adjacency indicators, and pairwise
 * intersection subtractions. On bipartite hosts each formula reduces exactly
 * to its bipartite specialization (indicators and intersection terms vanish).
 */
public class ExactCountNonInduced {

    /** n choose 2 */
    private static long comb2(long n) {
        return n <= 1 ? 0 : n * (n - 1) / 2;
    }

    // ------------------------------------------------------------------
    // M1: K_{3,3}. Cycle seed + two adjacent extensions completing the biclique.
    public static long fourPathM1Count(FourPath fourPath, Graph graph) {
        if (!fourPath.isCycle()) {
            return 0;
        }

        long count = 0;
        for (int id : fourPath.node_list[1].adjacencyArray) {
            if (!fourPath.ids.contains(id)) {
                for (int id2 : fourPath.node_list[0].adjacencyArray) {
                    if (!fourPath.ids.contains(id2) && graph.map.get(id).adjacency_list.contains(id2)) {
                        if (fourPath.node_list[3].adjacency_list.contains(id) && fourPath.node_list[2].adjacency_list.contains(id2)) {
                            count += 1;
                        }
                    }
                }
            }
        }

        return count;
    }

    // ------------------------------------------------------------------
    // M2: K_{2,4}. Cycle seed + two more common neighbors on one side.
    public static long fourPathM2Count(FourPath fourPath, Graph graph) {
        if (!fourPath.isCycle()) {
            return 0;
        }

        // 6 possible cycle choice
        return comb2(fourPath.left_intersection_filtered.length)
             + comb2(fourPath.right_intersection_filtered.length);
    }

    // ------------------------------------------------------------------
    // M3: K_{3,3} minus one edge.
    public static long fourPathM3Count(FourPath fourPath, Graph graph) {
        long count = 0;

        // case we don't use it as cycle (missing edge at the (v0,v3) position: p on the v1,v3 side, q on the v0,v2 side, p~q)
        for (int p : fourPath.left_intersection_filtered) {
            for (int q : fourPath.right_intersection_filtered) {
                if (graph.map.get(p).adjacency_list.contains(q)) count++;
            }
        }
        if (!fourPath.isCycle()) {
            return count;
        }

        // seed closes into C4; missing edge sits at one of the four (extension, path) positions
        for (int p : fourPath.left_intersection_filtered) {
            for (int w : graph.map.get(p).adjacencyArray) {
                if (fourPath.ids.contains(w)) continue;
                if (fourPath.node_list[0].adjacency_list.contains(w)) count++;
                if (fourPath.node_list[2].adjacency_list.contains(w)) count++;
            }
        }
        for (int q : fourPath.right_intersection_filtered) {
            for (int w : graph.map.get(q).adjacencyArray) {
                if (fourPath.ids.contains(w)) continue;
                if (fourPath.node_list[1].adjacency_list.contains(w)) count++;
                if (fourPath.node_list[3].adjacency_list.contains(w)) count++;
            }
        }
        // cases where the fourPath is the butterfly in the middle (missing edge between the two extensions p,q; must be distinct)
        count += (long) fourPath.left_intersection_filtered.length * fourPath.right_intersection_filtered.length
               - fourPath.getIlIrOverlap();

        return count;
    }

    // ------------------------------------------------------------------
    // M4: K_{2,3} + pendant on a hub.
    public static long fourPathM4Count(FourPath fourPath, Graph graph) {
        // case we don't use it as cycle
        long count = comb2(fourPath.left_intersection_filtered.length)
                   + comb2(fourPath.right_intersection_filtered.length);
        if (!fourPath.isCycle()) {
            return count;
        }

        // cycle seed: one more common neighbor u + pendant on a hub, excluding S and u itself
        count += (long) fourPath.left_intersection_filtered.length  * (fourPath.getExtDegree(1) + fourPath.getExtDegree(3) - 2);
        count += (long) fourPath.right_intersection_filtered.length * (fourPath.getExtDegree(0) + fourPath.getExtDegree(2) - 2);

        return count;
    }

    // ------------------------------------------------------------------
    // M5: K_{2,3} + pendant on a leaf.
    public static long fourPathM5Count(FourPath fourPath, Graph graph) {
        long count = 0;

        // case we don't use it as cycle (pendant-leaf is v1/v2: hub u in I_L, third leaf w ~ u and v2, outside S)
        for (int u : fourPath.left_intersection_filtered) {
            for (int w : graph.map.get(u).adjacencyArray) {
                if (!fourPath.ids.contains(w) && fourPath.node_list[2].adjacency_list.contains(w)) count++;
            }
        }
        for (int u : fourPath.right_intersection_filtered) {
            for (int w : graph.map.get(u).adjacencyArray) {
                if (!fourPath.ids.contains(w) && fourPath.node_list[1].adjacency_list.contains(w)) count++;
            }
        }
        if (!fourPath.isCycle()) {
            return count;
        }

        // If left cycle is chosen (pendant on the extra common-neighbour leaf u)
        count += fourPath.getOrSetLeftExtDegreeSum(graph.map);
        count += fourPath.getOrSetRightExtDegreeSum(graph.map);
        // If right or outer cycle is chosen (pendant on the v0/v2, resp. v1/v3, leaves;
        // exclude S and u itself when u is adjacent)
        count += (long) fourPath.left_intersection_filtered.length * (fourPath.getExtDegree(0) + fourPath.getExtDegree(2));
        for (int u : fourPath.left_intersection_filtered) {
            Node un = graph.map.get(u);
            if (un.adjacency_list.contains(fourPath.node_list[0].id)) count--;
            if (un.adjacency_list.contains(fourPath.node_list[2].id)) count--;
        }
        count += (long) fourPath.right_intersection_filtered.length * (fourPath.getExtDegree(1) + fourPath.getExtDegree(3));
        for (int u : fourPath.right_intersection_filtered) {
            Node un = graph.map.get(u);
            if (un.adjacency_list.contains(fourPath.node_list[1].id)) count--;
            if (un.adjacency_list.contains(fourPath.node_list[3].id)) count--;
        }

        return count;
    }

    // ------------------------------------------------------------------
    // M6: 2x3 grid.
    public static long fourPathM6Count(FourPath fourPath, Graph graph) {
        long count = 0;

        // case we don't use it as cycle
        // one corner case
        // a. corner is in 1st index (q in I_R plus w ~ q and v3)
        for (int q : fourPath.right_intersection_filtered) {
            for (int w : graph.map.get(q).adjacencyArray) {
                if (!fourPath.ids.contains(w) && fourPath.node_list[3].adjacency_list.contains(w)) count++;
            }
        }
        // b. corner is in 2nd index (mirrored: p in I_L plus w ~ p and v0)
        for (int p : fourPath.left_intersection_filtered) {
            for (int w : graph.map.get(p).adjacencyArray) {
                if (!fourPath.ids.contains(w) && fourPath.node_list[0].adjacency_list.contains(w)) count++;
            }
        }
        // two corners case (Z-shaped seed: one from I_L, one from I_R, distinct)
        count += (long) fourPath.left_intersection_filtered.length * fourPath.right_intersection_filtered.length
               - fourPath.getIlIrOverlap();
        if (!fourPath.isCycle()) {
            return count;
        }

        // cycle seed = one grid square; the remaining column attaches to one of its four edges
        int[][] pairs = {{1, 0}, {1, 2}, {3, 0}, {3, 2}};
        for (int[] jk : pairs) {
            Node vj = fourPath.node_list[jk[0]];
            Node vk = fourPath.node_list[jk[1]];
            for (int u : vj.adjacencyArray) {
                if (fourPath.ids.contains(u)) continue;
                for (int w : graph.map.get(u).adjacencyArray) {
                    if (!fourPath.ids.contains(w) && vk.adjacency_list.contains(w)) count++;
                }
            }
        }

        return count;
    }

    // ------------------------------------------------------------------
    // M7: C4 + two pendants on the same corner.
    public static long fourPathM7Count(FourPath fourPath, Graph graph) {
        // case we don't use it as cycle (u in I_L closes the cycle; second pendant on v1 avoids S and u)
        long count = (long) fourPath.left_intersection_filtered.length  * (fourPath.getExtDegree(1) - 1)
                   + (long) fourPath.right_intersection_filtered.length * (fourPath.getExtDegree(2) - 1);
        if (!fourPath.isCycle()) {
            return count;
        }

        for (int i = 0; i < 4; i++) {
            count += comb2(fourPath.getExtDegree(i));
        }

        return count;
    }

    // ------------------------------------------------------------------
    // M8: C4 + pendants on two adjacent corners.
    public static long fourPathM8Count(FourPath fourPath, Graph graph) {
        long count = 0;

        // case we don't use it as cycle
        // case we use both of the right nodes (pendant-A-B-pendant seed: remaining cycle edge via u ~ v1, w ~ u and v2)
        for (int u : fourPath.node_list[1].adjacencyArray) {
            if (fourPath.ids.contains(u)) continue;
            for (int w : graph.map.get(u).adjacencyArray) {
                if (!fourPath.ids.contains(w) && fourPath.node_list[2].adjacency_list.contains(w)) count++;
            }
        }
        // case we use only one of the right nodes
        // a. one corner case (pendant on the closing vertex u)
        count += fourPath.getOrSetLeftExtDegreeSum(graph.map);
        count += fourPath.getOrSetRightExtDegreeSum(graph.map);
        // b. two corners case (pendant on v2, resp. v1)
        count += (long) fourPath.left_intersection_filtered.length  * fourPath.getExtDegree(2);
        for (int u : fourPath.left_intersection_filtered) {
            if (graph.map.get(u).adjacency_list.contains(fourPath.node_list[2].id)) count--;
        }
        count += (long) fourPath.right_intersection_filtered.length * fourPath.getExtDegree(1);
        for (int u : fourPath.right_intersection_filtered) {
            if (graph.map.get(u).adjacency_list.contains(fourPath.node_list[1].id)) count--;
        }
        if (!fourPath.isCycle()) {
            return count;
        }

        // cycle seed: two distinct pendants on adjacent corners
        int[][] pairs = {{0, 1}, {1, 2}, {2, 3}, {3, 0}};
        for (int p = 0; p < 4; p++) {
            count += (long) fourPath.getExtDegree(pairs[p][0]) * fourPath.getExtDegree(pairs[p][1])
                   - fourPath.getAdjacentExtCommon(p);
        }

        return count;
    }

    // ------------------------------------------------------------------
    // M9: C4 + pendants on two opposite corners.
    public static long fourPathM9Count(FourPath fourPath, Graph graph) {
        // case we don't use it as cycle (u in I_L closes the cycle; pendant on v3 avoids S and u)
        long count = (long) fourPath.left_intersection_filtered.length  * (fourPath.getExtDegree(3) - 1)
                   + (long) fourPath.right_intersection_filtered.length * (fourPath.getExtDegree(0) - 1);
        if (!fourPath.isCycle()) {
            return count;
        }

        // cycle seed: distinct pendants on opposite corners; -|I| removes p1 == p3 collisions
        count += (long) fourPath.getExtDegree(1) * fourPath.getExtDegree(3) - fourPath.left_intersection_filtered.length;
        count += (long) fourPath.getExtDegree(0) * fourPath.getExtDegree(2) - fourPath.right_intersection_filtered.length;

        return count;
    }

    // ------------------------------------------------------------------
    // M10: C4 + a 2-edge tail.
    public static long fourPathM10Count(FourPath fourPath, Graph graph) {
        long count = 0;

        // case we don't use it as cycle
        // case the most left included (v0 is the tail middle: u in I_L closes the cycle, tail end avoids S and u)
        count += (long) fourPath.left_intersection_filtered.length * fourPath.getExtDegree(0);
        for (int u : fourPath.left_intersection_filtered) {
            if (graph.map.get(u).adjacency_list.contains(fourPath.node_list[0].id)) count--;
        }
        count += (long) fourPath.right_intersection_filtered.length * fourPath.getExtDegree(3);
        for (int u : fourPath.right_intersection_filtered) {
            if (graph.map.get(u).adjacency_list.contains(fourPath.node_list[3].id)) count--;
        }
        // case the most left is not included / whole right tail (seed = tail + first two cycle vertices: close the cycle with an adjacent pair)
        for (int u : fourPath.node_list[0].adjacencyArray) {
            if (fourPath.ids.contains(u)) continue;
            for (int w : graph.map.get(u).adjacencyArray) {
                if (!fourPath.ids.contains(w) && fourPath.node_list[1].adjacency_list.contains(w)) count++;
            }
        }
        for (int u : fourPath.node_list[3].adjacencyArray) {
            if (fourPath.ids.contains(u)) continue;
            for (int w : graph.map.get(u).adjacencyArray) {
                if (!fourPath.ids.contains(w) && fourPath.node_list[2].adjacency_list.contains(w)) count++;
            }
        }
        if (!fourPath.isCycle()) {
            return count;
        }

        // cycle seed: 2-edge tail hangs off any corner
        for (Node vj : fourPath.node_list) {
            for (int u : vj.adjacencyArray) {
                if (fourPath.ids.contains(u)) continue;
                count += fourPath.extDegreeOf(graph.map.get(u));
            }
        }

        return count;
    }

    // ------------------------------------------------------------------
    // M11: C6.
    public static long fourPathM11Count(FourPath fourPath, Graph graph) {
        long count = 0;

        for (int id0 : fourPath.node_list[0].adjacencyArray) {
            if (fourPath.ids.contains(id0)) continue;
            Node currNode = graph.map.get(id0);
            for (int id3 : fourPath.node_list[3].adjacencyArray) {
                if (fourPath.ids.contains(id3)) continue;
                if (currNode.adjacency_list.contains(id3)) {
                    count++;
                }
            }
        }

        return count;
    }

    // ------------------------------------------------------------------
    // M12: star K_{1,5}; computed analytically, not via path sampling.
    public static long fourPathM12Count(FourPath fourPath, Graph graph) {
        return 0L;
    }

    // ------------------------------------------------------------------
    // M13: center with 3 leaves + 2-edge tail.
    public static long fourPathM13Count(FourPath fourPath, Graph graph) {
        return comb2(fourPath.getExtDegree(1)) + comb2(fourPath.getExtDegree(2));
    }

    // ------------------------------------------------------------------
    // M14: double star; the two pendants must be distinct.
    public static long fourPathM14Count(FourPath fourPath, Graph graph) {
        return (long) fourPath.getExtDegree(1) * fourPath.getExtDegree(2)
             - fourPath.getAdjacentExtCommon(1);
    }

    // ------------------------------------------------------------------
    // M15: spider S(2,2,1).
    public static long fourPathM15Count(FourPath fourPath, Graph graph) {
        long count = 0;

        // seed = leg + center + leg-start: extend u (~v1 or ~v2) by one more edge outside S
        for (int u : fourPath.node_list[1].adjacencyArray) {
            if (fourPath.ids.contains(u)) continue;
            count += fourPath.extDegreeOf(graph.map.get(u));
        }
        for (int u : fourPath.node_list[2].adjacencyArray) {
            if (fourPath.ids.contains(u)) continue;
            count += fourPath.extDegreeOf(graph.map.get(u));
        }
        // seed = leg + center + pendant: distinct free neighbors of (v1,v0) resp. (v2,v3)
        count += (long) fourPath.getExtDegree(1) * fourPath.getExtDegree(0) - fourPath.getAdjacentExtCommon(0);
        count += (long) fourPath.getExtDegree(2) * fourPath.getExtDegree(3) - fourPath.getAdjacentExtCommon(2);

        return count;
    }

    // ------------------------------------------------------------------
    // M16: broom (P4 + two leaves at one end).
    public static long fourPathM16Count(FourPath fourPath, Graph graph) {
        long count = 0;
        // leaf on v1, tail end on v3 (and mirrored); -|I| removes leaf == tail collisions
        count += (long) fourPath.getExtDegree(1) * fourPath.getExtDegree(3) - fourPath.left_intersection_filtered.length;
        count += (long) fourPath.getExtDegree(2) * fourPath.getExtDegree(0) - fourPath.right_intersection_filtered.length;
        // both leaves at v0 (resp. v3)
        count += comb2(fourPath.getExtDegree(0));
        count += comb2(fourPath.getExtDegree(3));

        return count;
    }

    // ------------------------------------------------------------------
    // M17: P6.
    public static long fourPathM17Count(FourPath fourPath, Graph graph) {
        // both ends extended by one distinct vertex each
        long count = (long) fourPath.getExtDegree(0) * fourPath.getExtDegree(3) - fourPath.getAdjacentExtCommon(3);
        // 2-edge tail on one end
        for (int u : fourPath.node_list[0].adjacencyArray) {
            if (fourPath.ids.contains(u)) continue;
            count += fourPath.extDegreeOf(graph.map.get(u));
        }
        for (int u : fourPath.node_list[3].adjacencyArray) {
            if (fourPath.ids.contains(u)) continue;
            count += fourPath.extDegreeOf(graph.map.get(u));
        }

        return count;
    }
}
