package org.example.ta.index;

/**
 * A chunk of text extracted from a course document.
 */
public class DocChunk {
    public final String sourceFile;
    public final int pageNumber;
    public final String text;

    public DocChunk(String sourceFile, int pageNumber, String text) {
        this.sourceFile = sourceFile;
        this.pageNumber = pageNumber;
        this.text = text;
    }
}