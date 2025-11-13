package org.example.ta.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import org.example.ta.context.CodeContext;
import org.example.ta.context.ContextAwareProcessor;
import org.example.ta.ui.TaToolWindowPanel;

public class AskSelectedCodeAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        if (editor == null || project == null || psiFile == null) return;
        
        String sel = editor.getSelectionModel().getSelectedText();
        if (sel == null || sel.isBlank()) {
            Messages.showInfoMessage(project, "Please select a code fragment first.", "No Selection");
            return;
        }
        
        // Process context awareness
        ContextAwareProcessor contextProcessor = new ContextAwareProcessor();
        int selectionStart = editor.getSelectionModel().getSelectionStart();
        int selectionEnd = editor.getSelectionModel().getSelectionEnd();
        CodeContext codeContext = contextProcessor.analyzeContext(editor, psiFile, selectionStart, selectionEnd);
        
        // Use the selected code as the question directly
        String question = "Please explain the following code:\n\n" + codeContext.getSelectedCode();
        
        // Get the tool window
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CourseTA");
        if (toolWindow == null) {
            Messages.showErrorDialog(project, "Course TA tool window not found.", "Error");
            return;
        }
        
        // Show and activate the tool window
        toolWindow.show(() -> {
            // Get the tool window panel instance
            TaToolWindowPanel panel = TaToolWindowPanel.getInstance();
            if (panel == null) {
                Messages.showErrorDialog(project, "Failed to get tool window panel.", "Error");
                return;
            }
            
            // Set the question in the input area and simulate clicking the ask button
            panel.setInputText(question);
            panel.ask();
        });
    }
}