package org.example.ta.actions;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

/**
 * Action that asks the TA to refactor the selected code and shows a diff preview.
 * NOTE: The actual refactor suggestions should be generated via LLM; here we show a placeholder flow.
 */
public class CodeRefactorAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (project == null || editor == null) return;
        Document doc = editor.getDocument();
        String sel = editor.getSelectionModel().getSelectedText();
        if (sel == null || sel.isBlank()) {
            Messages.showInfoMessage(project, "Select the method or code region you want refactored.", "No Selection");
            return;
        }
        String instruction = Messages.showInputDialog(project, "Describe how to refactor (e.g., apply Strategy pattern):", "Refactor Instruction", Messages.getQuestionIcon());
        if (instruction == null) return;

// Placeholder: in reality call DeepSeekClient.generateAnswer(prompt) and parse suggested code
        String suggested = "// Suggested refactor (placeholder)\n" + sel + "\n// End suggestion";

// Show diff: original vs suggested
        DocumentContent before = DiffContentFactory.getInstance().create(project, doc.getText());
        DocumentContent after = DiffContentFactory.getInstance().create(project, doc.getText().replace(sel, suggested));
        SimpleDiffRequest request = new SimpleDiffRequest("TA Refactor Preview", before, after, "Original", "Suggested");
        DiffManager.getInstance().showDiff(project, request);

// If the student accepts, we can apply suggested change programmatically. We'll ask user.
        int resp = Messages.showYesNoDialog(project, "Apply suggested change to the file?", "Apply Refactor", Messages.getQuestionIcon());
        if (resp == Messages.YES) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                int selStart = editor.getSelectionModel().getSelectionStart();
                int selEnd = editor.getSelectionModel().getSelectionEnd();
                doc.replaceString(selStart, selEnd, suggested);
            });
        }
    }
}
