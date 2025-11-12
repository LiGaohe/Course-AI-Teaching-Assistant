package org.example.ta.index;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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
}
