package org.example.ta.retrieval;

import org.example.ta.index.DocChunk;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A very simple retrieval mechanism using token overlap and TF scoring.
 * This avoids external embedding services and is deterministic.
 */
public class SimpleRetriever {
    private final List<DocChunk> chunks;
    private final List<Map<String, Integer>> chunkTermFreqs = new ArrayList<>();
    private final Map<String, Integer> df = new HashMap<>();
    private final int corpusSize;

    public SimpleRetriever(List<DocChunk> chunks) {
        this.chunks = chunks;
        for (DocChunk c : chunks) {
            Map<String, Integer> tf = termFreq(c.text);
            chunkTermFreqs.add(tf);
// update document frequency
            Set<String> seen = new HashSet<>(tf.keySet());
            for (String t : seen) df.put(t, df.getOrDefault(t, 0) + 1);
        }
        this.corpusSize = chunks.size();
    }

    private Map<String, Integer> termFreq(String text) {
        String[] toks = text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+");
        Map<String, Integer> tf = new HashMap<>();
        for (String t : toks) if (!t.isBlank()) tf.put(t, tf.getOrDefault(t, 0) + 1);
        return tf;
    }

    private double idf(String term) {
        return Math.log(1 + (double) corpusSize / (1 + df.getOrDefault(term, 0)));
    }

    private Map<String, Double> tfIdf(Map<String, Integer> tf) {
        Map<String, Double> m = new HashMap<>();
        for (Map.Entry<String, Integer> e : tf.entrySet()) m.put(e.getKey(), e.getValue() * idf(e.getKey()));
        return m;
    }

    private double dot(Map<String, Double> a, Map<String, Double> b) {
        double s = 0.0;
        for (Map.Entry<String, Double> e : a.entrySet()) s += e.getValue() * b.getOrDefault(e.getKey(), 0.0);
        return s;
    }

    private double norm(Map<String, Double> a) {
        double s = 0.0; for (double v : a.values()) s += v * v; return Math.sqrt(s);
    }

    public List<ScoredChunk> retrieve(String query, int k) {
        Map<String, Integer> qtf = termFreq(query);
        Map<String, Double> qvec = tfIdf(qtf);
        List<ScoredChunk> scored = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Double> cvec = tfIdf(chunkTermFreqs.get(i));
            double denom = norm(qvec) * norm(cvec);
            double sim = denom == 0 ? 0 : dot(qvec, cvec) / denom;
            scored.add(new ScoredChunk(chunks.get(i), sim));
        }
        return scored.stream().sorted(Comparator.comparingDouble((ScoredChunk sc) -> -sc.score)).limit(k).collect(Collectors.toList());
    }

    public static class ScoredChunk {
        public final DocChunk chunk;
        public final double score;
        public ScoredChunk(DocChunk chunk, double score) { this.chunk = chunk; this.score = score; }
    }
}