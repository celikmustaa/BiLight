package graph;

import java.util.HashSet;

public class Node {
    public int id;
    public int degree;
    public HashSet<Integer> adjacency_list;
    public int[] adjacencyArray;

    public Node(int id) {
        this.id = id;
        this.degree = 0;
        this.adjacency_list = new HashSet<>();
    }

    public static int[] getIntersection(Node node1, Node node2) {
        // Use the smaller adjacency array for iteration
        int[] smaller;
        HashSet<Integer> largerSet;

        if (node1.adjacencyArray.length <= node2.adjacencyArray.length) {
            smaller = node1.adjacencyArray;
            largerSet = node2.adjacency_list;
        } else {
            smaller = node2.adjacencyArray;
            largerSet = node1.adjacency_list;
        }

        // Count matching elements first
        int count = 0;
        for (int id : smaller) {
            if (largerSet.contains(id)) count++;
        }

        // Fill the result array
        int[] intersection = new int[count];
        int idx = 0;
        for (int id : smaller) {
            if (largerSet.contains(id)) {
                intersection[idx++] = id;
            }
        }

        return intersection;
    }


    /**
     * Computes intersection of two nodes and excludes two specific node IDs.
     */
    public static int[] getIntersectionExcluding(Node nodeA, Node nodeB, int exclude1, int exclude2) {
        // Use smaller adjacency array for iteration
        int[] smaller;
        HashSet<Integer> largerSet;

        if (nodeA.adjacencyArray.length <= nodeB.adjacencyArray.length) {
            smaller = nodeA.adjacencyArray;
            largerSet = nodeB.adjacency_list;
        } else {
            smaller = nodeB.adjacencyArray;
            largerSet = nodeA.adjacency_list;
        }

        // Count matches excluding the two IDs
        int count = 0;
        for (int id : smaller) {
            if (id != exclude1 && id != exclude2 && largerSet.contains(id)) count++;
        }

        // Fill result array
        int[] intersection = new int[count];
        int idx = 0;
        for (int id : smaller) {
            if (id != exclude1 && id != exclude2 && largerSet.contains(id)) {
                intersection[idx++] = id;
            }
        }

        return intersection;
    }
}
