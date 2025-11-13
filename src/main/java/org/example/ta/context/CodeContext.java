package org.example.ta.context;

import java.util.List;

/**
 * Represents the context of a selected code fragment.
 * Contains information about the selected code, its surroundings, imports, and language.
 */
public class CodeContext {
    private final String selectedCode;
    private final String surroundingContext;
    private final List<String> imports;
    private final String language;
    
    public CodeContext(String selectedCode, String surroundingContext, List<String> imports, String language) {
        this.selectedCode = selectedCode;
        this.surroundingContext = surroundingContext;
        this.imports = imports;
        this.language = language;
    }
    
    /**
     * Gets the selected code fragment
     *
     * @return The selected code as a string
     */
    public String getSelectedCode() {
        return selectedCode;
    }
    
    /**
     * Gets the surrounding context of the selected code
     * This could include enclosing methods, classes, etc.
     *
     * @return The surrounding context as a string
     */
    public String getSurroundingContext() {
        return surroundingContext;
    }
    
    /**
     * Gets the list of imports in the file
     *
     * @return List of import statements
     */
    public List<String> getImports() {
        return imports;
    }
    
    /**
     * Gets the programming language of the code
     *
     * @return Language identifier
     */
    public String getLanguage() {
        return language;
    }
    
    @Override
    public String toString() {
        return "CodeContext{" +
                "language='" + language + '\'' +
                ", selectedCodeLength=" + (selectedCode != null ? selectedCode.length() : 0) +
                ", importsCount=" + (imports != null ? imports.size() : 0) +
                '}';
    }
}