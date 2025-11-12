package org.example.ta.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

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
}
