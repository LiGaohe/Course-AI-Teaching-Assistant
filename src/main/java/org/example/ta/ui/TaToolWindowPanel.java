package org.example.ta.ui;

import com.intellij.openapi.project.Project;

import javax.swing.*;
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
        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("Question:"), BorderLayout.NORTH);
        top.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        JPanel controls = new JPanel();
        controls.add(askBtn);
        controls.add(indexBtn);
        top.add(controls, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);

        outputArea.setEditable(false);
        panel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

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
