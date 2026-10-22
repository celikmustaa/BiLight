package graph;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An undirected simple graph (no self-loops, no multi-edges).
 *
 * The host may be bipartite or general: bipartiteness is detected in
 * {@link #buildEdgeSampler()} and exposed as {@link #hostIsBipartite}, rather
 * than assumed. It is used only to skip work that cannot apply on a bipartite
 * host, never to change a result.
 */
public class Graph {

    // Left nodes have positive id, Right nodes have negative id.
    public HashMap<Integer, Node> map;    // all nodes
    public ArrayList<int[]> edge_list;    // [[n1, -n2],[n3, -n4] ...]

    // alias method tables for edge sampling
    private int[] alias;
    private double[] prob;
    private int totalEdges;
    public long totalFourPathCount;

    /** Running count of rejected degenerate draws (node1 == node4, i.e. a triangle
     *  through the central edge). Always 0 on bipartite hosts. Reset by the caller
     *  per run to report collisions for that run. */
    public long collisionCount = 0;

    /** Cached exact count of the star motif M12 (K_{1,5}); see {@link #starCountM12()}. */
    private Double starCountM12 = null;

    /**
     * Exact non-induced count of the star K_{1,5} (motif M12): sum over v of C(deg(v), 5).
     * Every copy is a centre plus 5 of its neighbours, on any host graph, so no sampling is
     * needed (M12 contains no P4, so the path sampler could not estimate it anyway).
     * Computed exactly with BigInteger, since C(d,5) overflows a long once d exceeds ~6000,
     * and returned as a double. Call after keepLargestConnectedComponent(); the result is cached.
     */
    public double starCountM12() {
        if (starCountM12 == null) {
            java.math.BigInteger sum = java.math.BigInteger.ZERO;
            for (Node node : map.values()) {
                long d = node.degree;
                if (d < 5) continue;
                java.math.BigInteger c = java.math.BigInteger.ONE;
                for (int i = 0; i < 5; i++) c = c.multiply(java.math.BigInteger.valueOf(d - i));
                sum = sum.add(c.divide(java.math.BigInteger.valueOf(120)));
            }
            starCountM12 = sum.doubleValue();
        }
        return starCountM12;
    }

    public Graph() {
        this.map = new HashMap<>();
        this.edge_list = new ArrayList<>();
    }

    public void addNode(int id) {
        map.computeIfAbsent(id, Node::new);
    }


    public void connect(int left_id, int right_id) {
        if (left_id == right_id) return; // Prevent self-loops

        this.addNode(left_id);
        this.addNode(right_id);
        Node left = this.map.get(left_id);
        Node right = this.map.get(right_id);

        if (!(right.adjacency_list.contains(left.id))) { // no multiple edges
            left.adjacency_list.add(right_id);
            right.adjacency_list.add(left_id);

            left.degree++;
            right.degree++;

            this.edge_list.add(new int[]{left_id, right_id});
        }
    }

    /** Build adjacency arrays (one-time preprocessing). */
    public void buildAdjacencyArrays() {
        for (Node node : map.values()) {
            // Convert the adjacency_set to an int[] once
            node.adjacencyArray = node.adjacency_list.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    public void keepLargestConnectedComponent() {
        if (map.isEmpty()) {
            return;
        }

        HashSet<Integer> visited = new HashSet<>();
        List<Integer> largestComponent = new ArrayList<>();

        for (int nodeId : map.keySet()) {
            if (!visited.contains(nodeId)) {
                List<Integer> currentComponent = new ArrayList<>();
                Queue<Integer> queue = new LinkedList<>();

                queue.add(nodeId);
                visited.add(nodeId);
                currentComponent.add(nodeId);

                while (!queue.isEmpty()) {
                    int u = queue.poll();
                    for (int v : map.get(u).adjacency_list) {
                        if (!visited.contains(v)) {
                            visited.add(v);
                            queue.add(v);
                            currentComponent.add(v);
                        }
                    }
                }

                if (currentComponent.size() > largestComponent.size()) {
                    largestComponent = currentComponent;
                }
            }
        }

        HashSet<Integer> largestComponentNodes = new HashSet<>(largestComponent);

        map.keySet().retainAll(largestComponentNodes);

        edge_list.removeIf(edge -> !largestComponentNodes.contains(edge[0]) || !largestComponentNodes.contains(edge[1]));

        for (Node node : map.values()) {
            node.adjacency_list.retainAll(largestComponentNodes);
            node.degree = node.adjacency_list.size();
        }
    }


    public long countEdgesBetweenNodeSets(int[] setA, int[] setB) {
        // Always iterate over the smaller set
        int[] smaller, larger;
        if (setA.length <= setB.length) {
            smaller = setA;
            larger  = setB;
        } else {
            smaller = setB;
            larger  = setA;
        }

        long count = 0;
        for (int id : smaller) {
            HashSet<Integer> neighbors = this.map.get(id).adjacency_list;
            for (int id2 : larger) {
                if (neighbors.contains(id2)) {
                    count++;
                }
            }
        }

        return count;
    }

    /** True when the host graph is 2-colorable; detected in buildEdgeSampler. */
    public boolean hostIsBipartite;

    /** 2-coloring BFS over all components. */
    private boolean checkBipartite() {
        HashMap<Integer, Integer> color = new HashMap<>();
        for (int start : map.keySet()) {
            if (color.containsKey(start)) continue;
            color.put(start, 0);
            Queue<Integer> queue = new LinkedList<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                int cu = color.get(u);
                for (int v : map.get(u).adjacency_list) {
                    Integer cv = color.get(v);
                    if (cv == null) {
                        color.put(v, 1 - cu);
                        queue.add(v);
                    } else if (cv == cu) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** |Gamma(u) ∩ Gamma(v)|: common neighbors of an edge's endpoints (= triangles through it). */
    private long commonNeighborCount(Node u, Node v) {
        Node smaller = (u.degree <= v.degree) ? u : v;
        Node larger  = (u.degree <= v.degree) ? v : u;
        long c = 0;
        for (int w : smaller.adjacency_list) {
            if (larger.adjacency_list.contains(w)) c++;
        }
        return c;
    }

    /** Build alias sampler for weighted edge selection. */
    public void buildEdgeSampler() {
        int m = edge_list.size();
        if (m == 0) return;

        prob = new double[m];
        alias = new int[m];
        totalEdges = m;

        hostIsBipartite = checkBipartite();

        double[] normalizedWeights = new double[m];
        long sum = 0;
        long[] weights = new long[m];
        for (int i = 0; i < m; i++) {
            int[] e = edge_list.get(i);
            long w = ((long) (map.get(e[0]).degree - 1)) * (map.get(e[1]).degree - 1);
            if (!hostIsBipartite) {
                // (d(u)-1)(d(v)-1) also counts degenerate walks with v0 == v3, one per
                // common neighbor of the endpoints (3 per triangle). Subtract them so
                // the weight equals the exact number of P4s with this central edge.
                // On bipartite hosts there are no triangles and no correction is needed.
                w -= commonNeighborCount(map.get(e[0]), map.get(e[1]));
            }
            if (w < 0) w = 0; // safeguard
            weights[i] = w;
            sum += w;
        }
        totalFourPathCount = sum;
        if (sum == 0) return;

        // normalize
        for (int i = 0; i < m; i++) {
            normalizedWeights[i] = (double) weights[i] * m / sum;
        }

        Deque<Integer> small = new ArrayDeque<>();
        Deque<Integer> large = new ArrayDeque<>();
        for (int i = 0; i < m; i++) {
            if (normalizedWeights[i] < 1.0) small.add(i);
            else large.add(i);
        }

        while (!small.isEmpty() && !large.isEmpty()) {
            int s = small.removeLast();
            int l = large.removeLast();
            prob[s] = normalizedWeights[s];
            alias[s] = l;
            normalizedWeights[l] = (normalizedWeights[l] + normalizedWeights[s]) - 1.0;
            if (normalizedWeights[l] < 1.0) small.add(l);
            else large.add(l);
        }
        while (!large.isEmpty()) prob[large.removeLast()] = 1.0;
        while (!small.isEmpty()) prob[small.removeLast()] = 1.0;
    }

    /** graph.Sample a random edge index using alias method. */
    private int sampleEdge() {
        int i = ThreadLocalRandom.current().nextInt(totalEdges);
        double r = ThreadLocalRandom.current().nextDouble();
        return (r < prob[i]) ? i : alias[i];
    }

    public FourPath getRandomFourPath() {
        if (totalFourPathCount == 0) return null;

        int[] edge = edge_list.get(sampleEdge());
        Node node2 = map.get(edge[0]);
        Node node3 = map.get(edge[1]);

        if (node2.degree <= 1 || node3.degree <= 1) {
            return null; // Cannot extend the path
        }

        // Draw the two endpoints; on non-bipartite hosts a common neighbor of the
        // central edge can be drawn on both sides (v0 == v3, a triangle, not a P4).
        // Redraw the whole pair until the endpoints differ (rejection sampling keeps
        // the accepted path uniform over the valid pairs, matching the edge weight
        // which already excludes these degenerate pairs). Any positive-weight edge has
        // at least one valid pair, so this terminates; the expected number of retries
        // is (#triangles through the edge) / ((d2-1)(d3-1)), i.e. ~0 in practice.
        int node_id1;
        int node_id4;
        while (true) {
            // Select neighbor of node2 (excluding node3) in O(1)
            int rand_idx1 = ThreadLocalRandom.current().nextInt(node2.degree - 1);
            node_id1 = node2.adjacencyArray[rand_idx1];
            if (node_id1 == node3.id) {
                node_id1 = node2.adjacencyArray[node2.degree - 1];
            }

            // Select neighbor of node3 (excluding node2) in O(1)
            int rand_idx2 = ThreadLocalRandom.current().nextInt(node3.degree - 1);
            node_id4 = node3.adjacencyArray[rand_idx2];
            if (node_id4 == node2.id) {
                node_id4 = node3.adjacencyArray[node3.degree - 1];
            }

            if (node_id1 != node_id4) break;
            collisionCount++; // degenerate draw (triangle); reject and redraw both
        }

        Node node1 = map.get(node_id1);
        Node node4 = map.get(node_id4);

        return new FourPath(node1, node2, node3, node4);
    }
}
