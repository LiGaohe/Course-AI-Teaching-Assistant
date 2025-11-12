package org.example.ta.index;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Simple document indexer that uses Apache Tika to extract text and split it into chunks.
 * This is used to preprocess course materials for RAG retrieval.
 */
public class DocumentIndexer {

    private final Tika tika = new Tika();
    private final int chunkSize = 800; // characters per chunk (configurable)

    /**
     * Recursively index all files in the given directory.
     */
    public List<DocChunk> indexDirectory(File dir) throws IOException {
        List<DocChunk> chunks = new ArrayList<>();
        Files.walk(dir.toPath())
                .filter(Files::isRegularFile)
                .forEach(p -> {
                    try {
                        indexFile(p.toFile(), chunks);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        return chunks;
    }

    /**
     * Parse a file using Apache Tika and split its text content into chunks.
     */
    private void indexFile(File file, List<DocChunk> out) throws IOException {
        String text = "";
        try {
            text = tika.parseToString(file);
        } catch (TikaException e) {
            System.err.println("[Tika] Failed to parse file: " + file.getName());
            e.printStackTrace();
            return;
        }

        // Naive page tracking: split by form feed or heuristics
        String[] pages = text.split("\f");
        for (int p = 0; p < pages.length; p++) {
            String pageText = pages[p].trim();
            for (int i = 0; i < pageText.length(); i += chunkSize) {
                int end = Math.min(i + chunkSize, pageText.length());
                String chunk = pageText.substring(i, end).trim();
                if (!chunk.isEmpty()) {
                    DocChunk dc = new DocChunk(file.getName(), p + 1, chunk);
                    out.add(dc);
                }
            }
        }
    }
    
    /**
     * Convert document chunks to vectors and store them in a VectorStore
     * 
     * @param chunks The document chunks to vectorize
     * @return A VectorStore containing the chunk vectors
     */
    public VectorStore createVectorStore(List<DocChunk> chunks) {
        VectorStore vectorStore = new VectorStore();
        
        for (DocChunk chunk : chunks) {
            // Generate a simple vector representation based on term frequency
            double[] vector = createSimpleVector(chunk.text);
            
            // Create a unique ID for this chunk
            String id = UUID.randomUUID().toString();
            
            // Add to vector store
            vectorStore.add(id, chunk, vector);
        }
        
        return vectorStore;
    }
    
    /**
     * Create a simple vector representation of text based on character frequencies
     * This is a basic approach for demonstration purposes
     * 
     * @param text The text to convert to a vector
     * @return A vector representation of the text
     */
    public double[] createSimpleVector(String text) {  // Changed from private to public
        // Using a simple approach - character frequency vector for ASCII characters
        double[] vector = new double[128]; // ASCII characters
        
        String lowerText = text.toLowerCase();
        for (int i = 0; i < lowerText.length(); i++) {
            char c = lowerText.charAt(i);
            if (c < 128) { // Only consider ASCII characters
                vector[c] += 1.0;
            }
        }
        
        // Normalize the vector
        double magnitude = 0.0;
        for (int i = 0; i < vector.length; i++) {
            magnitude += vector[i] * vector[i];
        }
        magnitude = Math.sqrt(magnitude);
        
        // Avoid division by zero
        if (magnitude > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= magnitude;
            }
        }
        
        return vector;
    }
}