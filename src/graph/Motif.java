package graph;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * A data class to hold all metadata for a specific graphlet (motif).
 */
public class Motif {
    public final String id;                 // e.g., "M1"
    public final String name;               // internal label, e.g. "NBNM"
    public final int index;                 // internal numbering, e.g. 52
    public final int coefficient;        // e.g., 9 // represent how many fourPath this motif includes
    private final BiFunction<FourPath, Graph, Long> counterFunction;

    public Motif(String id, String name, int index, int coefficient, BiFunction<FourPath, Graph, Long> counterFunction) {
        this.id = id;
        this.name = name;
        this.index = index;
        this.coefficient = coefficient;
        this.counterFunction = counterFunction;
    }

    public static final List<Motif> MOTIFS;

    // Static initializer block to populate the list of all supported motifs.
    static {
        MOTIFS = new ArrayList<>();

        MOTIFS.add(new Motif("M1", "NBNM", 52, 36, ExactCountNonInduced::fourPathM1Count));
        MOTIFS.add(new Motif("M2", "ADPM", 73, 24, ExactCountNonInduced::fourPathM2Count));
        MOTIFS.add(new Motif("M3", "FBNM", 74, 24, ExactCountNonInduced::fourPathM3Count));
        MOTIFS.add(new Motif("M4", "ABPM", 90, 15, ExactCountNonInduced::fourPathM4Count));
        MOTIFS.add(new Motif("M5", "BBNM", 91, 16, ExactCountNonInduced::fourPathM5Count));
        MOTIFS.add(new Motif("M6", "IGFK", 92, 14, ExactCountNonInduced::fourPathM6Count));
        MOTIFS.add(new Motif("M7", "AAPM", 101, 8, ExactCountNonInduced::fourPathM7Count));
        MOTIFS.add(new Motif("M8", "AGJG", 102, 9, ExactCountNonInduced::fourPathM8Count));
        MOTIFS.add(new Motif("M9", "ACNM", 103, 8, ExactCountNonInduced::fourPathM9Count));
        MOTIFS.add(new Motif("M10", "BBJM", 104, 8, ExactCountNonInduced::fourPathM10Count));

        MOTIFS.add(new Motif("M11", "MBEM", 106, 6, ExactCountNonInduced::fourPathM11Count));
        MOTIFS.add(new Motif("M12", "AADO", 107, 0, ExactCountNonInduced::fourPathM12Count));
        MOTIFS.add(new Motif("M13", "AAHK", 108, 3, ExactCountNonInduced::fourPathM13Count));
        MOTIFS.add(new Motif("M14", "ACFK", 109, 4, ExactCountNonInduced::fourPathM14Count));
        MOTIFS.add(new Motif("M15", "AIKG", 110, 4, ExactCountNonInduced::fourPathM15Count));
        MOTIFS.add(new Motif("M16", "AAPE", 111, 3, ExactCountNonInduced::fourPathM16Count));
        MOTIFS.add(new Motif("M17", "AGJE", 112, 3, ExactCountNonInduced::fourPathM17Count));
    }

    /**
     * This method provides a type-safe way to call the underlying BiFunction.
     * @param seed The seed object must be a graph.FourPath.
     * @param graph The graph.
     * @return The local count.
     */
    public long count(FourPath seed, Graph graph) {
        return counterFunction.apply(seed, graph);
    }
}
