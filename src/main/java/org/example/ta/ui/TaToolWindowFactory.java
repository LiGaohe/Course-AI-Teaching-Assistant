package org.example.ta.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

/**
 * Registers the Teaching Assistant (TA) tool window inside IntelliJ.
 * The window hosts the chat panel for interacting with the AI TA.
 */
public class TaToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        TaToolWindowPanel panel = new TaToolWindowPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel.getComponent(), "Course TA", false);

        toolWindow.getContentManager().addContent(content);
    }
}
