package org.example.ta.index;

import java.util.*;

/**
 * A simple vector store implementation for storing and retrieving document chunks with their vector representations.
 * This is a basic implementation that stores vectors in memory.
 */
public class VectorStore {
    private final Map<String, double[]> vectors = new HashMap<>();
    private final Map<String, DocChunk> chunks = new HashMap<>();
    private final List<String> ids = new ArrayList<>();

    /**
     * Add a document chunk with its vector representation to the store
     * 
     * @param id     Unique identifier for the chunk
     * @param chunk  The document chunk
     * @param vector Vector representation of the chunk
     */
    public void add(String id, DocChunk chunk, double[] vector) {
        ids.add(id);
        chunks.put(id, chunk);
        vectors.put(id, vector);
    }

    /**
     * Find the most similar chunks to the given query vector
     * 
     * @param queryVector The query vector
     * @param k           Number of results to return
     * @return List of scored chunks, sorted by similarity (highest first)
     */
    public List<ScoredChunk> search(double[] queryVector, int k) {
        List<ScoredChunk> results = new ArrayList<>();
        
        for (String id : ids) {
            double[] vector = vectors.get(id);
            double similarity = cosineSimilarity(queryVector, vector);
            results.add(new ScoredChunk(chunks.get(id), similarity, id));
        }
        
        // Sort by similarity (descending)
        results.sort((a, b) -> Double.compare(b.score, a.score));
        
        // Return top k results
        return results.subList(0, Math.min(k, results.size()));
    }

    /**
     * Calculate cosine similarity between two vectors
     * 
     * @param a First vector
     * @param b Second vector
     * @return Cosine similarity value between -1 and 1
     */
    private double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have the same dimensions");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        if (normA == 0 || normB == 0) {
            return 0; // Return 0 similarity if either vector is zero
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Represents a chunk with its similarity score and ID
     */
    public static class ScoredChunk {
        public final DocChunk chunk;
        public final double score;
        public final String id;

        public ScoredChunk(DocChunk chunk, double score, String id) {
            this.chunk = chunk;
            this.score = score;
            this.id = id;
        }
        
        public ScoredChunk(DocChunk chunk, double score) {
            this(chunk, score, null);
        }
    }
}