package org.example.ta.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.example.ta.index.DocChunk;
import org.example.ta.index.DocumentIndexer;
import org.example.ta.index.VectorStore;
import org.example.ta.llm.OpenRouterClient;
import org.example.ta.retrieval.SimpleRetriever;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple Swing panel for interacting with the TA.
 * Provides a text input, an area for results, and controls for indexing.
 */
public class TaToolWindowPanel {
    private final JPanel panel;
    private final JTextArea inputArea = new JTextArea(3, 40);
    private final JTextArea outputArea = new JTextArea(15, 40);
    private final JButton askBtn = new JButton("Ask TA");
    private final JButton indexBtn = new JButton("Index Documents");
    private final JButton askWithReasoningBtn = new JButton("Ask with Reasoning");
    private VectorStore vectorStore; // Store the vector store reference
    private SimpleRetriever retriever; // Store the retriever reference

    public TaToolWindowPanel(Project project) {
        panel = new JPanel(new BorderLayout());
        
        // 为输入框添加圆角深色边框
        Border roundedBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 1, true), // 圆角边框，深灰色
            BorderFactory.createEmptyBorder(5, 5, 5, 5) // 内边距
        );
        inputArea.setBorder(roundedBorder);
        
        // 为输出框添加与输入框一致的边框和间距
        outputArea.setBorder(roundedBorder);
        
        // 创建输入面板，包含输入区域和按钮
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 添加外边距
        inputPanel.add(new JLabel("Question:"), BorderLayout.NORTH);
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        
        // 创建按钮面板并放置在输入区域的右下角
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(askBtn);
        buttonPanel.add(askWithReasoningBtn);
        buttonPanel.add(indexBtn);
        inputPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // 将输出区域放在北部，输入面板放在南部
        outputArea.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 为输出区域添加外边距
        panel.add(outputScrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

// Hook actions (these call into services that should be implemented)
        askBtn.addActionListener(e -> {
            String q = inputArea.getText().trim();
            if (q.isEmpty()) return;
            outputArea.setText("Thinking...\n");
            
            // Perform the RAG process in a background thread
            new Thread(() -> {
                try {
                    String answer = performRAGProcess(q);
                    SwingUtilities.invokeLater(() -> outputArea.append(answer));
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        outputArea.append("Error: " + ex.getMessage() + "\n");
                        ex.printStackTrace();
                    });
                }
            }).start();
        });
        
        askWithReasoningBtn.addActionListener(e -> {
            String q = inputArea.getText().trim();
            if (q.isEmpty()) return;
            outputArea.setText("Thinking with reasoning...\n");
            
            // Perform the RAG process with reasoning in a background thread
            new Thread(() -> {
                try {
                    String answer = performRAGProcessWithReasoning(q);
                    SwingUtilities.invokeLater(() -> outputArea.append(answer));
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        outputArea.append("Error: " + ex.getMessage() + "\n");
                        ex.printStackTrace();
                    });
                }
            }).start();
        });

        indexBtn.addActionListener(e -> {
            String path = Messages.showInputDialog(panel, "Enter the path to the documents directory:", "Index Documents", Messages.getQuestionIcon());
            if (path == null || path.trim().isEmpty()) {
                return;
            }
            
            File dir = new File(path);
            if (!dir.exists() || !dir.isDirectory()) {
                Messages.showErrorDialog(panel, "Invalid directory path!", "Error");
                return;
            }
            
            outputArea.setText("Indexing started...\n");
            
            // Perform indexing in background thread to avoid freezing UI
            new Thread(() -> {
                try {
                    DocumentIndexer indexer = new DocumentIndexer();
                    List<DocChunk> chunks = indexer.indexDirectory(dir);
                    
                    // Create vector store and retriever
                    vectorStore = indexer.createVectorStore(chunks);
                    retriever = new SimpleRetriever(chunks);
                    
                    // Update UI on EDT
                    SwingUtilities.invokeLater(() -> {
                        outputArea.append("Indexing completed!\n");
                        outputArea.append("Indexed " + chunks.size() + " chunks.\n");
                        outputArea.append("Retriever and vector store created and ready for queries.\n");
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        outputArea.append("Indexing failed: " + ex.getMessage() + "\n");
                        ex.printStackTrace();
                    });
                }
            }).start();
        });
    }

    /**
     * Perform the full RAG process: retrieve relevant chunks and generate an answer
     *
     * @param question The user's question
     * @return The generated answer
     * @throws Exception If any error occurs during the process
     */
    private String performRAGProcess(String question) throws Exception {
        // Check if we have indexed documents
        if (retriever == null) {
            return "Please index documents first before asking questions.\n" +
                   "Note: this plugin example requires you to configure document path and OpenRouter API key in code or settings.\n";
        }
        
        // Retrieve relevant chunks
        List<SimpleRetriever.ScoredChunk> relevantChunks = retriever.retrieve(question, 3);
        
        // Extract the text content from the chunks
        List<String> contextTexts = relevantChunks.stream()
                .map(result -> String.format("[%s, page %d] %s", 
                        result.chunk.sourceFile, 
                        result.chunk.pageNumber, 
                        result.chunk.text))
                .collect(Collectors.toList());
        
        // Get API key (in a real implementation, this should come from settings)
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            // Fallback to DeepSeek for demonstration
            //return "To use the full RAG capabilities with OpenRouter, please set the OPENROUTER_API_KEY environment variable.\n" +
                   //"Using fallback demonstration mode.\n\n" +
                   //generateDemonstrationAnswer(question, contextTexts);
            apiKey = "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";
        }
        
        // Call OpenRouter API
        OpenRouterClient client = new OpenRouterClient(apiKey, "alibaba/tongyi-deepresearch-30b-a3b:free");
        return client.generateAnswer(question, contextTexts);
    }
    
    /**
     * Perform the full RAG process with reasoning: retrieve relevant chunks and generate an answer with reasoning
     *
     * @param question The user's question
     * @return The generated answer with reasoning
     * @throws Exception If any error occurs during the process
     */
    private String performRAGProcessWithReasoning(String question) throws Exception {
        // Check if we have indexed documents
        if (retriever == null) {
            return "Please index documents first before asking questions.\n" +
                   "Note: this plugin example requires you to configure document path and OpenRouter API key in code or settings.\n";
        }
        
        // Retrieve relevant chunks
        List<SimpleRetriever.ScoredChunk> relevantChunks = retriever.retrieve(question, 3);
        
        // Extract the text content from the chunks
        List<String> contextTexts = relevantChunks.stream()
                .map(result -> String.format("[%s, page %d] %s", 
                        result.chunk.sourceFile, 
                        result.chunk.pageNumber, 
                        result.chunk.text))
                .collect(Collectors.toList());
        
        // Get API key (in a real implementation, this should come from settings)
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            // Fallback to DeepSeek for demonstration
            return "To use the full RAG capabilities with OpenRouter, please set the OPENROUTER_API_KEY environment variable.\n" +
                   "Using fallback demonstration mode.\n\n" +
                   generateDemonstrationAnswer(question, contextTexts);
        }
        
        // Build context for the question
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("You are a helpful teaching assistant AI. ");
        contextBuilder.append("Answer the following question based on the provided course materials. ");
        contextBuilder.append("Always cite the source material and page number in your answer. ");
        contextBuilder.append("If the answer is only based on your general knowledge (not from the provided materials), ");
        contextBuilder.append("explicitly state that at the beginning of your response.\n\n");
        
        if (!contextTexts.isEmpty()) {
            contextBuilder.append("Relevant course materials:\n");
            for (int i = 0; i < contextTexts.size(); i++) {
                contextBuilder.append("[Source ").append(i + 1).append("] ")
                      .append(contextTexts.get(i)).append("\n\n");
            }
        }
        
        contextBuilder.append("Question: ").append(question).append("\n\n");
        contextBuilder.append("Answer:");
        
        // Call OpenRouter API with reasoning
        OpenRouterClient client = new OpenRouterClient(apiKey, "alibaba/tongyi-deepresearch-30b-a3b:free");
        OpenRouterClient.ReasoningResponse response = client.generateAnswerWithReasoning(contextBuilder.toString());
        
        // Continue reasoning with follow-up question
        java.util.List<OpenRouterClient.Message> messages = new java.util.ArrayList<>();
        messages.add(new OpenRouterClient.Message("user", contextBuilder.toString()));
        messages.add(new OpenRouterClient.Message("assistant", response.content, response.reasoningDetails));
        messages.add(new OpenRouterClient.Message("user", "Are you sure? Think carefully."));
        
        OpenRouterClient.ReasoningResponse response2 = client.continueReasoning(messages);
        
        // Format the output
        StringBuilder result = new StringBuilder();
        result.append("First response:\n");
        result.append(response.content).append("\n\n");
        result.append("Reasoning details:\n");
        result.append(response.reasoningDetails).append("\n\n");
        result.append("Second response (continued reasoning):\n");
        result.append(response2.content).append("\n\n");
        result.append("Reasoning details:\n");
        result.append(response2.reasoningDetails).append("\n");
        
        return result.toString();
    }
    
    /**
     * Generate a demonstration answer when no API key is available
     *
     * @param question The user's question
     * @param contextTexts The retrieved context texts
     * @return A demonstration answer
     */
    private String generateDemonstrationAnswer(String question, List<String> contextTexts) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(question).append("\n\n");
        
        if (!contextTexts.isEmpty()) {
            sb.append("Retrieved context:\n");
            for (int i = 0; i < contextTexts.size(); i++) {
                sb.append(i + 1).append(". ").append(contextTexts.get(i)).append("\n\n");
            }
            
            sb.append("Answer (demonstration):\n");
            sb.append("Based on the retrieved course materials, the answer to your question would be generated here.\n");
            sb.append("In the actual implementation with a valid API key, this would contain a detailed answer citing:\n");
            for (int i = 0; i < Math.min(3, contextTexts.size()); i++) {
                sb.append("- [Source ").append(i + 1).append("]\n");
            }
        } else {
            sb.append("No relevant course materials were found.\n");
            sb.append("Answer (demonstration):\n");
            sb.append("This answer is based on general knowledge as no relevant course materials were found.\n");
            sb.append("In the actual implementation with a valid API key, this would contain a detailed answer based on the LLM's general knowledge.\n");
        }
        
        return sb.toString();
    }

    public JComponent getComponent() { return panel; }
}