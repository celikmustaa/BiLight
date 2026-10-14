package graph;

import java.util.List;

public class Sample {

    /**
     * A generic sampling method that uses a 4-path seed for all motifs.
     *
     * @param k      The number of samples to take.
     * @param graph  The graph to sample from.
     * @param motifs  The graph.Motif objects containing all metadata for the estimation.
     * @return The estimated global count of the motif.
     */
    public static double[] estimate(int k, Graph graph, List<Motif> motifs) {
        double[] estimates = new double[motifs.size()];
        double[] final_results = new double[motifs.size()];

        for (int i = 0; i < k; i++) {
            FourPath fourPath = graph.getRandomFourPath();

            for (int m = 0; m < motifs.size(); m++) {
                Motif motif = motifs.get(m);
                final_results[m] += (double) motif.count(fourPath, graph);
            }
        }

        for (int i = 0; i < motifs.size(); i++) {
            Motif motif = motifs.get(i);
            estimates[i] = (final_results[i] / k) * graph.totalFourPathCount / motif.coefficient;
        }
        return estimates;
    }
}
