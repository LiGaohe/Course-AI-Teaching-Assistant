package org.example.ta.ui;

import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

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

    public TaToolWindowPanel(Project project) {
        panel = new JPanel(new BorderLayout());
        
        // 为输入框添加圆角深色边框
        Border roundedBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 1, true), // 圆角边框，深灰色
            BorderFactory.createEmptyBorder(5, 5, 5, 5) // 内边距
        );
        inputArea.setBorder(roundedBorder);
        
        // 创建输入面板，包含输入区域和按钮
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 添加外边距
        inputPanel.add(new JLabel("Question:"), BorderLayout.NORTH);
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        
        // 创建按钮面板并放置在输入区域的右下角
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(askBtn);
        buttonPanel.add(indexBtn);
        inputPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // 将输出区域放在北部，输入面板放在南部
        outputArea.setEditable(false);
        panel.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

// Hook actions (these call into services that should be implemented)
        askBtn.addActionListener(e -> {
            String q = inputArea.getText().trim();
            if (q.isEmpty()) return;
            outputArea.setText("Thinking...\n");
// This should call your retrieval + LLM pipeline. For simplicity we show a placeholder.
            outputArea.append("(Placeholder) Answer will appear here.\n");
            outputArea.append("Note: this plugin example requires you to configure document path and DeepSeek API key in code or settings.");
        });

        indexBtn.addActionListener(e -> JOptionPane.showMessageDialog(panel, "Indexing started (placeholder). Implement DocumentIndexer.indexDirectory() and wire it here."));
    }

    public JComponent getComponent() { return panel; }
}