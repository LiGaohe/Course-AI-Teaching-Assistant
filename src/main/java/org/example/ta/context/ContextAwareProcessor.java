package org.example.ta.context;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.example.ta.index.DocumentIndexer;

import java.util.*;

/**
 * Context-aware processor that analyzes code context for selected code fragments.
 * This module enables the plugin to understand the programming context of user selections.
 */
public class ContextAwareProcessor {
    
    private final DocumentIndexer documentIndexer;
    
    public ContextAwareProcessor() {
        this.documentIndexer = new DocumentIndexer();
    }
    
    /**
     * Analyzes the context of selected code in an editor
     *
     * @param editor The editor containing the selection
     * @param psiFile The PSI file representation
     * @param selectionStart Start offset of selection
     * @param selectionEnd End offset of selection
     * @return Context information about the selected code
     */
    public CodeContext analyzeContext(Editor editor, PsiFile psiFile, int selectionStart, int selectionEnd) {
        String selectedText = editor.getDocument().getText(new TextRange(selectionStart, selectionEnd));
        
        // Get the PSI element at the start of the selection
        PsiElement elementAtCursor = psiFile.findElementAt(selectionStart);
        
        // Find the most relevant parent context (method, class, etc.)
        String surroundingContext = extractSurroundingContext(elementAtCursor);
        
        // Identify imported classes and packages
        List<String> imports = extractImports(psiFile);
        
        // Detect programming language
        String language = detectLanguage(psiFile);
        
        return new CodeContext(selectedText, surroundingContext, imports, language);
    }
    
    /**
     * Extracts the surrounding context of the selected code
     * Tries to identify enclosing methods, classes, or other structural elements
     *
     * @param element The PSI element at the selection point
     * @return A string representation of the surrounding context
     */
    private String extractSurroundingContext(PsiElement element) {
        if (element == null) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        
        // Walk up the PSI tree to collect context
        PsiElement current = element;
        while (current != null) {
            // Add information about the current element if it's a major construct
            String elementInfo = getElementInfo(current);
            if (!elementInfo.isEmpty()) {
                if (context.length() > 0) {
                    context.insert(0, elementInfo + "\n");
                } else {
                    context.append(elementInfo);
                }
            }
            current = current.getParent();
        }
        
        return context.toString();
    }
    
    /**
     * Gets information about a PSI element for context
     *
     * @param element The PSI element
     * @return Information string about the element
     */
    private String getElementInfo(PsiElement element) {
        // This is a simplified implementation
        // In a full implementation, this would be language-specific
        String elementType = element.getClass().getSimpleName();
        
        // For common structural elements, include their names
        if (elementType.contains("Method") || elementType.contains("Class") || 
            elementType.contains("Function") || elementType.contains("Interface")) {
            return elementType + ": " + element.getTextRange();
        }
        
        return ""; // Return empty for non-structural elements
    }
    
    /**
     * Extracts import statements from the file
     *
     * @param psiFile The PSI file
     * @return List of imported classes/packages
     */
    private List<String> extractImports(PsiFile psiFile) {
        List<String> imports = new ArrayList<>();
        
        // Find all import statements in the file
        PsiElement[] children = psiFile.getChildren();
        for (PsiElement child : children) {
            if (isImportStatement(child)) {
                imports.add(child.getText());
            }
        }
        
        return imports;
    }
    
    /**
     * Checks if a PSI element represents an import statement
     *
     * @param element The PSI element to check
     * @return True if the element is an import statement
     */
    private boolean isImportStatement(PsiElement element) {
        // Simplified check - in a real implementation this would be language-specific
        String text = element.getText().trim().toLowerCase();
        return text.startsWith("import ") || text.startsWith("using ");
    }
    
    /**
     * Detects the programming language of the file
     *
     * @param psiFile The PSI file
     * @return Language identifier
     */
    private String detectLanguage(PsiFile psiFile) {
        String fileName = psiFile.getName();
        if (fileName.endsWith(".java")) {
            return "Java";
        } else if (fileName.endsWith(".kt")) {
            return "Kotlin";
        } else if (fileName.endsWith(".py")) {
            return "Python";
        } else if (fileName.endsWith(".js")) {
            return "JavaScript";
        } else if (fileName.endsWith(".ts")) {
            return "TypeScript";
        } else {
            return "Unknown";
        }
    }
    
    /**
     * Creates a vector representation of the code context for similarity search
     *
     * @param context The code context to vectorize
     * @return Vector representation of the context
     */
    public double[] createContextVector(CodeContext context) {
        // Combine all context elements into a single text for vectorization
        StringBuilder contextText = new StringBuilder();
        contextText.append(context.getSelectedCode());
        contextText.append(" ");
        contextText.append(context.getSurroundingContext());
        
        for (String imp : context.getImports()) {
            contextText.append(" ").append(imp);
        }
        
        return documentIndexer.createSimpleVector(contextText.toString());
    }
}