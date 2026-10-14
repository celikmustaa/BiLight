package fileformatter;

import graph.Graph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class FileFormatter {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java fileformatter.FileFormatter <method> <input_path> [output_path] [-sep <separator>]");
            System.err.println("Methods: motivo, pgd");
            System.err.println("Default output_path: out.txt");
            System.err.println("Default separator: tab");
            return;
        }

        String method = args[0];
        String inputPath = args[1];
        String outputPath = "out.txt";
        String separator = "\t";

        // A simple parser for optional arguments
        // Assumes output path is the first optional arg if it doesn't start with a hyphen
        if (args.length > 2 && !args[2].startsWith("-")) {
            outputPath = args[2];
        }
        // Look for -sep flag
        for (int i = 2; i < args.length; i++) {
            if ("-sep".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                separator = args[i+1];
                break;
            }
        }


        try {
            System.out.println("Starting file formatting...");
            System.out.printf("Method: %s, Input: %s, Output: %s, Separator: [%s]%n", method, inputPath, outputPath, separator);

            switch (method.toLowerCase()) {
                case "motivo":
                    toMotivo(inputPath, outputPath, separator);
                    break;
                case "pgd":
                    toPGD(inputPath, outputPath, separator);
                    break;
                default:
                    System.err.println("Unknown method: " + method + ". Available methods: motivo, pgd");
                    break;
            }
            System.out.println("Formatting complete.");
        } catch (Exception e) {
            System.err.println("An error occurred during file formatting.");
            e.printStackTrace();
        }
    }

    private static Graph readGraphFromFile(String inputPath, String separator) throws IOException {
        Graph graph = new Graph();
        HashMap<String, Integer> idMap = new HashMap<>();
        int nextId = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(inputPath))) {
            String line;
            int lineCounter = 0;
            while ((line = br.readLine()) != null) {
                lineCounter++;
                String[] splitted = line.split(separator);
                if (splitted.length < 2 || splitted[0].trim().isEmpty() || splitted[1].trim().isEmpty()) {
                    System.err.println("Skipping malformed line " + lineCounter + ": " + line);
                    continue;
                }
                
                String left_id_str = splitted[0].trim();
                String right_id_str = splitted[1].trim();

                if (!idMap.containsKey(left_id_str)) {
                    idMap.put(left_id_str, nextId++);
                }
                if (!idMap.containsKey(right_id_str)) {
                    idMap.put(right_id_str, nextId++);
                }

                int left_id = idMap.get(left_id_str);
                int right_id = idMap.get(right_id_str);
                graph.connect(left_id, right_id);

                if (lineCounter % 10000 == 0) {
                    System.out.println("Processed " + lineCounter + " lines.");
                }
            }
            System.out.println("Finished reading " + lineCounter + " lines from input file.");
        }
        return graph;
    }




    public static void toMotivo(String inputPath, String outputPath, String separator) throws IOException {
        Graph graph = readGraphFromFile(inputPath, separator);

        System.out.println("Mapping node IDs for Motivo format...");
        int motivoIdCounter = 0;
        HashMap<Integer, Integer> map_to_motivo = new HashMap<>();
        for (int map_id : graph.map.keySet()) {
            if (!map_to_motivo.containsKey(map_id)) {
                map_to_motivo.put(map_id, motivoIdCounter++);
            }
        }

        System.out.println("Writing to output file...");
        try (FileWriter myWriter = new FileWriter(outputPath)) {
            myWriter.write(graph.map.size() + " " + graph.edge_list.size() + "\n");
            for(int i = 0; i < graph.edge_list.size() - 1; i++){
                int[] edge = graph.edge_list.get(i);
                myWriter.write(map_to_motivo.get(edge[0]) + " " + map_to_motivo.get(edge[1]) + "\n");
                myWriter.write(map_to_motivo.get(edge[1]) + " " + map_to_motivo.get(edge[0]) + "\n");
            }
            int[] edge = graph.edge_list.get(graph.edge_list.size() - 1);
            myWriter.write(map_to_motivo.get(edge[0]) + " " + map_to_motivo.get(edge[1]) + "\n");
            myWriter.write(map_to_motivo.get(edge[1]) + " " + map_to_motivo.get(edge[0]) );
        }
        System.out.println("Successfully formatted file to: " + outputPath);
    }

    public static void toPGD(String inputPath, String outputPath, String separator) throws IOException {
        Graph graph = readGraphFromFile(inputPath, separator);

        System.out.println("Mapping node IDs for PGD format...");
        int pgdIdCounter = 0;
        HashMap<Integer, Integer> map_to_pgd = new HashMap<>();
        for (int map_id : graph.map.keySet()) {
            if (!map_to_pgd.containsKey(map_id)) {
                map_to_pgd.put(map_id, pgdIdCounter++);
            }
        }

        System.out.println("Writing to output file...");
        try (FileWriter myWriter = new FileWriter(outputPath)) {
            myWriter.write(graph.map.size() + " " + graph.map.size() + " " + graph.edge_list.size() + "\n");
            if (graph.edge_list.isEmpty()) {
                System.out.println("No edges found in the graph. Output file will contain only the header.");
                return;
            }
            for(int i = 0; i < graph.edge_list.size() - 1; i++){
                int[] edge = graph.edge_list.get(i);
                myWriter.write(map_to_pgd.get(edge[0]) + " " + map_to_pgd.get(edge[1]) + "\n");
            }
            int[] edge = graph.edge_list.get(graph.edge_list.size() - 1);
            myWriter.write(map_to_pgd.get(edge[0]) + " " + map_to_pgd.get(edge[1]) + "\n");
        }
        System.out.println("Successfully formatted file to: " + outputPath);
    }
}
