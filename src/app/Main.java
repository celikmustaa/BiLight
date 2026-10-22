package app;

import graph.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Main {
    public static void main(String[] args) {

        // --- Argument parsing ---
        int i = 0;
        String arg;
        String graph_path = "";
        String separator = "\t";
        // Sweep budgets, capped at 100K: that is the largest budget the article reports, and
        // ground truth is itself defined as the k=100K estimate, so a larger run would have
        // no reference to compare against. Runtime is linear in k, so the dropped 200K/500K/1M
        // would have multiplied the sweep cost by ~10.6x (hollywood: ~28 h -> ~300 h).
        // Use -samp N to run a single budget instead of the sweep.
        int[] k_sets = {10, 50, 200, 500, 1000, 5000, 20000, 50000, 100000};

        while (i < args.length && args[i].startsWith("-")) {
            arg = args[i++];
            switch (arg) {
                case "-p":
                    if (i < args.length) {
                        graph_path = args[i++];
                    } else {
                        System.err.println("-p requires a file path");
                    }
                    break;
                case "-sep":
                    if (i < args.length) {
                        separator = args[i++];
                    } else {
                        System.err.println("-sep requires a string value");
                    }
                    break;
                case "-samp":
                    if (i < args.length) {
                        k_sets = new int[]{Integer.parseInt(args[i++])};
                    } else {
                        System.err.println("-samp requires a sample count");
                    }
                    break;
                case "-disjoint":
                    // Treat the two columns as separate vertex namespaces.
                    CreateGraph.disjointSides = true;
                    break;
                case "-leftmax":
                    if (i < args.length) {
                        CreateGraph.leftMaxId = Long.parseLong(args[i++]);
                    } else {
                        System.err.println("-leftmax requires a maximum left-side id");
                    }
                    break;
            }
        }

        if (i != args.length || graph_path.isEmpty()) {
            System.err.println("Usage: app.Main -p graph_path [-sep separator (\\t)] [-samp sample_count]");
            System.err.println("                [-disjoint] [-leftmax max_left_id]");
            System.err.println("  -disjoint  keep the two columns in separate vertex namespaces, so that");
            System.err.println("             the same id on both sides does not collapse into one vertex");
            System.err.println("  -leftmax N vertices with id <= N are the left side; same-side edges are");
            System.err.println("             dropped (extracts the bipartite projection of a mixed graph)");
            return;
        } else {
            System.out.println("Arguments are parsed!");
        }

        // Open results file for append (null if it fails — we just skip file writes)
        PrintWriter fileOut = null;
        try {
            // Explicitly specifying UTF-8 to prevent encoding mismatches (Mojibake/Chinese characters)
            fileOut = new PrintWriter(new FileWriter("results.txt", StandardCharsets.UTF_8, true));
        } catch (IOException e) {
            System.err.println("Warning: could not open results.txt (" + e.getMessage() + ")");
        }

        // --- Graph setup and timing ---
        long totalSetupStartTime = System.currentTimeMillis();
        long stepStartTime;

        CreateGraph.path = graph_path;
        CreateGraph.separator = separator;

        // Graph Creation
        stepStartTime = System.currentTimeMillis();
        System.out.println("Creating graph from file...");
        Graph graph = CreateGraph.createGraph();
        if (graph == null || graph.map.isEmpty()) {
            System.err.println("No graph was loaded - stopping.");
            System.err.println("  Check the path, and check that -sep matches the file's separator");
            System.err.println("  (tab by default; use -sep \" \" for space, -sep \",\" for comma).");
            if (fileOut != null) fileOut.close();
            return;
        }
        System.out.println("Graph creation took: " + (System.currentTimeMillis() - stepStartTime) + " ms");

        // Filtering
        System.out.println("Graph size before filtering: " + graph.map.size() + " nodes, " + graph.edge_list.size() + " edges");
        stepStartTime = System.currentTimeMillis();
        System.out.println("Filtering to largest connected component...");
        graph.keepLargestConnectedComponent();
        System.out.println("Filtering took: " + (System.currentTimeMillis() - stepStartTime) + " ms");
        System.out.println("Graph size after filtering: " + graph.map.size() + " nodes, " + graph.edge_list.size() + " edges");

        // Edge Sampler
        stepStartTime = System.currentTimeMillis();
        System.out.println("Building edge sampler...");
        graph.buildEdgeSampler();
        System.out.println("Edge sampler construction took: " + (System.currentTimeMillis() - stepStartTime) + " ms");
        // Worth seeing in the log: a dataset that should be bipartite but loads as general
        // usually means the two columns collapsed into one namespace (see -disjoint).
        System.out.println("Host is bipartite: " + graph.hostIsBipartite);

        // Exact count of the star M12 (sum_v C(deg(v),5)): computed once here, so it is part of
        // the one-time setup cost and not of any sampling run's RUNTIME.
        System.out.println("Star M12 (exact): " + new DecimalFormat("0.####E0").format(graph.starCountM12()));

        long totalSetupEndTime = System.currentTimeMillis();
        long totalSetupRuntime = totalSetupEndTime - totalSetupStartTime;

        System.out.println("Total graph setup and pre-computation time: " + totalSetupRuntime + " ms");
        if (graph.totalFourPathCount == 0) {
            System.out.println("No four paths found in the graph. Can not sample");
            if (fileOut != null) fileOut.close();
            return;
        }
        System.out.println("Number of four paths: " + graph.totalFourPathCount);

        // --- Write graph info to file ---
        if (fileOut != null) {
            fileOut.println(graph_path);
            fileOut.println("--- Graph statistics ---");
            fileOut.println("Graph setup and pre-computation time: " + totalSetupRuntime + " ms");
            fileOut.println("Number of nodes: " + graph.map.size() + "\tNumber of edges: " + graph.edge_list.size());
            fileOut.println("Number of four paths: " + graph.totalFourPathCount);
            fileOut.flush();
        }

        // --- Dynamic Header Generation ---
        String[] numberWords = {"ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE", "TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN"};
        StringBuilder header = new StringBuilder();
        header.append(String.format("\n%15s", "sample"));
        for (int j = 0; j < Motif.MOTIFS.size(); j++) {
            Motif motif = Motif.MOTIFS.get(j);
            String motifHeader = String.format("%s-%s", numberWords[j], motif.name);
            header.append(String.format("%19s", motifHeader));
        }
        header.append(String.format("%19s", "RUNTIME(ms)"));
        header.append(String.format("%19s", "COLLISIONS"));
        String headerStr = header.toString();
        System.out.println(headerStr);
        System.out.flush();
        if (fileOut != null) {
            fileOut.println(headerStr);
            fileOut.flush();
        }

        // --- Sampling Loop ---
        NumberFormat numFormat = new DecimalFormat("0.####E0");
        for (int k : k_sets) {
            long startTime = System.currentTimeMillis();

            StringBuilder line = new StringBuilder();
            line.append(String.format("%15d", k));

            graph.collisionCount = 0; // count degenerate draws rejected during this run
            double[] estimates = Sample.estimate(k, graph, Motif.MOTIFS);
            for (double estimate : estimates) {
                line.append(String.format("%19s", numFormat.format(estimate)));
            }

            long endTime = System.currentTimeMillis();
            long runtime = endTime - startTime;
            line.append(String.format("%19s", runtime));
            line.append(String.format("%19d", graph.collisionCount));

            String lineStr = line.toString();
            System.out.println(lineStr);
            System.out.flush();
            if (fileOut != null) {
                fileOut.println(lineStr);
                fileOut.flush();
            }
        }

        if (fileOut != null) {
            fileOut.close();
        }
    } // main
} // app.Main