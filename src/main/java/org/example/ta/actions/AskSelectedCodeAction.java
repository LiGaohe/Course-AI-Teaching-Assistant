package org.example.ta.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.example.ta.index.DocChunk;
import org.example.ta.index.DocumentIndexer;
import org.example.ta.index.VectorStore;

import java.util.List;

public class AskSelectedCodeAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null || project == null) return;
        String sel = editor.getSelectionModel().getSelectedText();
        if (sel == null || sel.isBlank()) {
            Messages.showInfoMessage(project, "Please select a code fragment first.", "No Selection");
            return;
        }
        // For demo, show a dialog and the selected code. In a real implementation, call retriever + LLM.
        String q = Messages.showInputDialog(project, "Ask a question about the selected code:", "Ask TA", Messages.getQuestionIcon());
        if (q == null || q.isBlank()) return;
        // Build a prompt including the selected code and ask DeepSeek via DeepSeekClient
        Messages.showInfoMessage(project, "(Placeholder) Would ask LLM with selected code and display RAG results.", "Ask TA");
    }
    
    /**
     * Example method showing how to use the indexing and vector store functionality
     * In a real implementation, this would be integrated with the UI or other components
     */
    private void demonstrateIndexingAndSearch() {
        // This is just a demonstration of how the components work together
        // In practice, indexing would be done when documents are added/updated
        
        // 1. Index documents
        // DocumentIndexer indexer = new DocumentIndexer();
        // List<DocChunk> chunks = indexer.indexDirectory(new File("path/to/documents"));
        
        // 2. Create vector store
        // VectorStore vectorStore = indexer.createVectorStore(chunks);
        
        // 3. Search for similar content
        // double[] queryVector = indexer.createSimpleVector("query text");
        // List<VectorStore.ScoredChunk> results = vectorStore.search(queryVector, 5);
        
        // 4. Use results for RAG (Retrieval-Augmented Generation)
    }
}