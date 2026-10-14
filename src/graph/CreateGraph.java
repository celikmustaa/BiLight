package graph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;


public class CreateGraph {

    /** Path to the input edge list. Set from the {@code -p} flag in {@link app.Main}. */
    public static String path;

    public static String separator = "\t";
    public static Graph graph = new Graph();

    /**
     * When true, the two columns are treated as separate vertex namespaces, so that the
     * same raw id appearing on the left and on the right becomes two distinct vertices.
     *
     * Default {@code false} reproduces the historical behaviour, in which a single map is
     * shared by both columns. That merges the sides on inputs which number both of them
     * from 1 (KONECT {@code out.*} files, MatrixMarket), and can make a bipartite input
     * load as a non-bipartite graph. Set with {@code -disjoint}.
     */
    public static boolean disjointSides = false;

    /**
     * Flickr-style side assignment: vertices with id {@code <= leftMaxId} are on the left,
     * everything else on the right, and edges whose endpoints fall on the same side are
     * dropped. This extracts the bipartite projection of a mixed graph. Negative disables.
     * Set with {@code -leftmax N}.
     */
    public static long leftMaxId = -1;


    public static Graph createGraph() {
        HashMap<String, Integer> idMap = new HashMap<>();
        int nextId = 0;
        long commentLines = 0;
        long sameSideDropped = 0;

        // Fail here rather than letting the run continue on an empty graph, which used to
        // look identical to a wrong -sep.
        java.io.File input = new java.io.File(path);
        if (!input.isFile()) {
            System.err.println("Input file not found: " + path);
            System.err.println("  (a relative path is resolved against " + System.getProperty("user.dir") + ")");
            return null;
        }

        try {
            BufferedReader br = new BufferedReader((new FileReader(path)));

            String line;
            long counter = 0;
            while ((line = br.readLine()) != null) {
                // '%' (MatrixMarket) and '#' (SNAP) mark comment headers. They are never
                // edges, and parsing them would silently create junk vertices.
                if (!line.isEmpty() && (line.charAt(0) == '%' || line.charAt(0) == '#')) {
                    commentLines++;
                    continue;
                }

                String[] splitted = line.split(separator);
                if (splitted.length < 2 || splitted[0].trim().isEmpty() || splitted[1].trim().isEmpty()) {
                    System.err.println("Skipping malformed line " + (counter + 1) + ": " + line);
                    continue;
                }

                String left_id_str = splitted[0].trim();
                String right_id_str = splitted[1].trim();

                if (leftMaxId >= 0) {
                    // Mixed input: keep only the edges that cross between the two sides.
                    long a, b;
                    try {
                        a = Long.parseLong(left_id_str);
                        b = Long.parseLong(right_id_str);
                    } catch (NumberFormatException e) {
                        System.err.println("Skipping non-numeric line " + (counter + 1) + ": " + line);
                        continue;
                    }
                    boolean aIsLeft = a <= leftMaxId;
                    boolean bIsLeft = b <= leftMaxId;
                    if (aIsLeft == bIsLeft) {
                        sameSideDropped++;
                        continue;
                    }
                    if (!aIsLeft) { // orient so the left endpoint comes first
                        String tmp = left_id_str;
                        left_id_str = right_id_str;
                        right_id_str = tmp;
                    }
                }

                // Keying the sides apart is what stops "1" on the left and "1" on the right
                // from collapsing into one vertex.
                String left_key = disjointSides ? "L:" + left_id_str : left_id_str;
                String right_key = disjointSides ? "R:" + right_id_str : right_id_str;

                if (!idMap.containsKey(left_key)) {
                    idMap.put(left_key, nextId++);
                }
                if (!idMap.containsKey(right_key)) {
                    idMap.put(right_key, nextId++);
                }

                int left_id = idMap.get(left_key);
                int right_id = idMap.get(right_key);
                graph.connect(left_id, right_id);
                counter += 1;
                if (counter % 100000 == 0){
                    System.out.println("Reading line "+ counter + " of the input file.");
                }
            }
            br.close();
        } catch (Exception e) {
            System.out.println("An error occurred in createGraph");
            e.printStackTrace();
        }

        if (commentLines > 0) {
            System.out.println("Skipped " + commentLines + " comment line(s) starting with % or #");
        }
        if (sameSideDropped > 0) {
            System.out.println("Dropped " + sameSideDropped + " same-side edge(s) (-leftmax " + leftMaxId + ")");
        }
        System.out.println("Vertex namespaces: " + (disjointSides ? "DISJOINT (-disjoint)" : "shared (historical default)"));

        graph.buildAdjacencyArrays();
        return graph;
    }




}
