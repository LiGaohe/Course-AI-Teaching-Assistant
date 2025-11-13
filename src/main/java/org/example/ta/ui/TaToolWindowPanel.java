package org.example.ta.ui;

import com.intellij.openapi.ui.Messages;
import org.example.ta.index.DocChunk;
import org.example.ta.index.DocumentIndexer;
import org.example.ta.index.IndexFileManager;
import org.example.ta.llm.OpenRouterClient;
import org.example.ta.retrieval.SimpleRetriever;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.JBColor;
import java.awt.*;
import java.io.File;
import java.io.IOException;
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
    private SimpleRetriever retriever; // Store the retriever reference
    private final IndexFileManager indexFileManager = new IndexFileManager();
    
    // 添加静态实例引用，以便其他类可以访问
    private static TaToolWindowPanel instance;

    public TaToolWindowPanel() {
        instance = this;
        panel = new JPanel(new BorderLayout());
        
        // 为输入框添加圆角深色边框
        Border roundedBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.DARK_GRAY, 1, true), // 圆角边框，深灰色
            BorderFactory.createEmptyBorder(5, 5, 5, 5) // 内边距
        );
        inputArea.setBorder(roundedBorder);
        
        // 为输出框添加与输入框一致的边框和间距
        outputArea.setBorder(roundedBorder);
        
        // 设置文本区域的换行属性
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        
        // 创建输入面板，包含输入区域和按钮
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 添加外边距
        inputPanel.add(new JLabel("Question:"), BorderLayout.NORTH);
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        
        // 创建按钮面板并放置在输入区域的右下角
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(askBtn);
        JButton askWithReasoningBtn = new JButton("Ask with Reasoning");
        buttonPanel.add(askWithReasoningBtn);
        JButton indexBtn = new JButton("Index Documents");
        buttonPanel.add(indexBtn);
        inputPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // 将输出区域放在北部，输入面板放在南部
        outputArea.setEditable(false);
        JBScrollPane outputScrollPane = new JBScrollPane(outputArea);
        outputScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 为输出区域添加外边距
        // 设置滚动条策略
        outputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
            showIndexDocumentsDialog();
        });
    }
    
    /**
     * 显示索引文档对话框，包含知识库管理和添加新路径功能
     */
    private void showIndexDocumentsDialog() {
        // 创建对话框
        JDialog dialog = new JDialog((Frame) null, "Index Documents", true);
        dialog.setLayout(new BorderLayout());
        
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 创建知识库显示面板
        JComponent knowledgeBasePanel = createKnowledgeBasePanel();
        mainPanel.add(knowledgeBasePanel, BorderLayout.CENTER);
        
        // 创建底部按钮面板
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addBtn = new JButton("Add New Path");
        JButton deleteBtn = new JButton("Delete Selected");
        JButton reindexBtn = new JButton("Reindex All");
        JButton closeBtn = new JButton("Close");
        
        // 为删除按钮添加引用，以便在其他地方使用
        final JButton deleteButtonRef = deleteBtn;
        
        addBtn.addActionListener(e -> {
            addNewDocumentPath(dialog);
        });
        
        deleteBtn.addActionListener(e -> {
            deleteSelectedPath(dialog, knowledgeBasePanel, deleteButtonRef);
        });
        
        reindexBtn.addActionListener(e -> {
            reindexAllDocuments(dialog);
        });
        
        closeBtn.addActionListener(e -> {
            dialog.dispose();
        });
        
        bottomPanel.add(addBtn);
        bottomPanel.add(deleteBtn);
        bottomPanel.add(reindexBtn);
        bottomPanel.add(closeBtn);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(panel);
        dialog.setVisible(true);
    }
    
    /**
     * 创建知识库显示面板
     */
    private JComponent createKnowledgeBasePanel() {
        // 使用JTree来显示目录结构
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Knowledge Base");
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        
        // 加载已有的路径
        List<String> paths = indexFileManager.loadDocumentPaths();
        DocumentIndexer indexer = new DocumentIndexer();
        
        for (String path : paths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                DefaultMutableTreeNode pathNode = new DefaultMutableTreeNode(new FileInfo(dir.getName(), path, true));
                root.add(pathNode);
                
                try {
                    // 获取目录下的所有文件
                    List<File> files = indexer.listAllFilesRecursively(dir);
                    for (File file : files) {
                        // 获取相对于根目录的路径
                        String relativePath = dir.toPath().relativize(file.toPath()).toString();
                        pathNode.add(new DefaultMutableTreeNode(new FileInfo(relativePath, file.getAbsolutePath(), false)));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // 路径不存在或不是目录
                DefaultMutableTreeNode pathNode = new DefaultMutableTreeNode(new FileInfo(dir.getName() + " (Invalid)", path, true));
                root.add(pathNode);
            }
        }
        
        JTree tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.expandPath(new TreePath(root));
        
        // 展开所有路径节点
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
            tree.expandPath(new TreePath(child.getPath()));
        }
        
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Knowledge Base"));
        return scrollPane;
    }
    
    /**
     * 删除选中的路径
     */
    private void deleteSelectedPath(JDialog dialog, JComponent knowledgeBasePanel, JButton deleteButton) {
        if (knowledgeBasePanel instanceof JScrollPane scrollPane) {
            JViewport viewport = scrollPane.getViewport();
            if (viewport.getView() instanceof JTree tree) {
                DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
                DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
                
                TreePath[] selectedPaths = tree.getSelectionPaths();
                if (selectedPaths != null && selectedPaths.length > 0) {
                    // 获取选中的节点
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent();
                    Object userObject = selectedNode.getUserObject();
                    
                    if (userObject instanceof FileInfo fileInfo) {
                        if (fileInfo.isDirectory()) {
                            String path = fileInfo.getFullPath();
                            int result = Messages.showYesNoDialog(
                                dialog, 
                                "Are you sure you want to delete \"" + path + "\" from the knowledge base?", 
                                "Confirm Delete", 
                                Messages.getQuestionIcon()
                            );
                            
                            if (result == Messages.YES) {
                                // 从索引中删除选定的路径
                                List<String> currentPaths = indexFileManager.loadDocumentPaths();
                                currentPaths.remove(path);
                                indexFileManager.saveDocumentPaths(currentPaths);
                                
                                // 更新显示
                                root.remove(selectedNode);
                                treeModel.reload();
                                
                                outputArea.append("Deleted \"" + path + "\" from knowledge base.\n");
                            }
                        } else {
                            Messages.showInfoMessage(dialog, "Please select a directory path to delete, not a file.", "Delete Path");
                        }
                    }
                } else {
                    Messages.showInfoMessage(dialog, "Please select a knowledge base path to delete.", "No Selection");
                }
            }
        }
    }
    
    /**
     * 添加新文档路径
     */
    private void addNewDocumentPath(Component parent) {
        // Load previously saved paths
        List<String> savedPaths = indexFileManager.loadDocumentPaths();
        String defaultPath = savedPaths.isEmpty() ? "" : savedPaths.getLast();
        
        String path = Messages.showInputDialog(parent, "Enter the path to the documents directory:", "Index Documents", Messages.getQuestionIcon(), defaultPath, null);
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            Messages.showErrorDialog(parent, "Invalid directory path!", "Error");
            return;
        }
        
        // Save the new path
        indexFileManager.addDocumentPath(path);
        
        outputArea.setText("Indexing started...\n");
        outputArea.append("Index file location: " + indexFileManager.getIndexFilePath() + "\n");
        
        // Perform indexing in background thread to avoid freezing UI
        new Thread(() -> {
            try {
                DocumentIndexer indexer = new DocumentIndexer();
                List<DocChunk> chunks = indexer.indexDirectory(dir);
                
                // Create vector store and retriever
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
    }
    
    /**
     * 重新索引所有文档
     */
    private void reindexAllDocuments(Component parent) {
        List<String> paths = indexFileManager.loadDocumentPaths();
        if (paths.isEmpty()) {
            Messages.showInfoMessage(parent, "No document paths configured.", "Reindex");
            return;
        }
        
        outputArea.setText("Reindexing all documents...\n");
        
        new Thread(() -> {
            try {
                DocumentIndexer indexer = new DocumentIndexer();
                List<DocChunk> allChunks = new java.util.ArrayList<>();
                
                for (String path : paths) {
                    File dir = new File(path);
                    if (dir.exists() && dir.isDirectory()) {
                        SwingUtilities.invokeLater(() -> 
                            outputArea.append("Indexing: " + path + "\n"));
                        List<DocChunk> chunks = indexer.indexDirectory(dir);
                        allChunks.addAll(chunks);
                    }
                }
                
                // Create vector store and retriever
                retriever = new SimpleRetriever(allChunks);
                
                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    outputArea.append("Reindexing completed!\n");
                    outputArea.append("Total indexed chunks: " + allChunks.size() + "\n");
                    outputArea.append("Retriever and vector store updated.\n");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    outputArea.append("Reindexing failed: " + ex.getMessage() + "\n");
                    ex.printStackTrace();
                });
            }
        }).start();
    }
    
    // 提供公共方法获取实例
    public static TaToolWindowPanel getInstance() {
        return instance;
    }
    
    // 提供公共方法设置输入区域文本
    public void setInputText(String text) {
        inputArea.setText(text);
    }
    
    // 提供公共方法触发Ask按钮点击
    public void ask() {
        askBtn.doClick();
    }

    /**
     * Perform the full RAG process: retrieve relevant chunks and generate an answer
     *
     * @param question The user's question
     * @return The generated answer
     * @throws Exception If any error occurs during the process
     */
    private String performRAGProcess(String question) throws Exception {
        return performRAGProcessBase(question, false);
    }
    
    /**
     * Perform the full RAG process with reasoning: retrieve relevant chunks and generate an answer with reasoning
     *
     * @param question The user's question
     * @return The generated answer with reasoning
     * @throws Exception If any error occurs during the process
     */
    private String performRAGProcessWithReasoning(String question) throws Exception {
        return performRAGProcessBase(question, true);
    }
    
    /**
     * Base method for performing the RAG process
     *
     * @param question The user's question
     * @param withReasoning Whether to include reasoning in the response
     * @return The generated answer
     * @throws Exception If any error occurs during the process
     */
    private String performRAGProcessBase(String question, boolean withReasoning) throws Exception {
        // Check if we have indexed documents
        String validationError = validateRetriever();
        if (validationError != null) {
            return validationError;
        }
        
        // Retrieve relevant chunks
        List<SimpleRetriever.ScoredChunk> relevantChunks = retriever.retrieve(question, 3);
        
        // Extract the text content from the chunks
        List<String> contextTexts = relevantChunks.stream()
                .map(result -> String.format("[%s, page %d] %s", 
                        getFileName(result.chunk.sourceFile), 
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
        
        if (withReasoning) {
            // Build context for the question
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("You are a helpful teaching assistant AI. ");
            contextBuilder.append("Answer the following question based on the provided course materials. ");
            contextBuilder.append("Always cite the source material and page number in your answer. ");
            contextBuilder.append("If the answer is only based on your general knowledge (not from the provided materials), ");
            contextBuilder.append("explicitly state that at the beginning of your response.\n\n");
            
            if (!contextTexts.isEmpty()) {
                contextBuilder.append("Relevant course materials:\n");
                for (String contextText : contextTexts) {
                    contextBuilder.append(contextText).append("\n\n");
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
        } else {
            // Call OpenRouter API
            OpenRouterClient client = new OpenRouterClient(apiKey, "alibaba/tongyi-deepresearch-30b-a3b:free");
            return client.generateAnswer(question, contextTexts);
        }
    }
    
    /**
     * Validate if documents have been indexed
     *
     * @return Error message if validation fails, null otherwise
     */
    private String validateRetriever() {
        if (retriever == null) {
            return "Please index documents first before asking questions.\n" +
                   "Note: this plugin example requires you to configure document path and OpenRouter API key in code or settings.\n";
        }
        return null;
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
            sb.append("In the actual implementation with a valid API key, this would contain a detailed answer citing the sources.\n");
        } else {
            sb.append("No relevant course materials were found.\n");
            sb.append("Answer (demonstration):\n");
            sb.append("This answer is based on general knowledge as no relevant course materials were found.\n");
            sb.append("In the actual implementation with a valid API key, this would contain a detailed answer based on the LLM's general knowledge.\n");
        }
        
        return sb.toString();
    }

    /**
     * Extract file name from full path
     *
     * @param fullPath Full path to the file
     * @return File name with extension
     */
    private String getFileName(String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) {
            return "Unknown Source";
        }
        
        // Handle both Windows and Unix path separators
        String[] parts = fullPath.replace('\\', '/').split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return fullPath;
    }
    
    /**
     * 内部类用于存储文件信息
     */
    private static class FileInfo {
        private final String displayName;
        private final String fullPath;
        private final boolean isDirectory;
        
        public FileInfo(String displayName, String fullPath, boolean isDirectory) {
            this.displayName = displayName;
            this.fullPath = fullPath;
            this.isDirectory = isDirectory;
        }
        
        public String getFullPath() {
            return fullPath;
        }
        
        public boolean isDirectory() {
            return isDirectory;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }

    public JComponent getComponent() { return panel; }
}