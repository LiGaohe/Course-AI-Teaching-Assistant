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

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * AI-powered code refactor action using OpenRouter API.
 * Steps:
 * 1. Get selected code and user instruction
 * 2. Ask OpenRouter (e.g. GPT-4) for a refactored version
 * 3. Show diff window
 * 4. Allow user to apply the change
 */
public class CodeRefactorAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (project == null || editor == null) return;

        Document doc = editor.getDocument();
        String selected = editor.getSelectionModel().getSelectedText();

        if (selected == null || selected.isBlank()) {
            Messages.showInfoMessage(project, "请先选中需要重构的代码片段。", "提示");
            return;
        }

        String instruction = Messages.showInputDialog(
                project,
                "请输入重构要求（例如：优化命名、提取方法、使用设计模式...）：",
                "AI 代码重构",
                Messages.getQuestionIcon()
        );
        if (instruction == null || instruction.isBlank()) return;

        String apiKey = System.getenv("OPENROUTER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            Messages.showErrorDialog(project, "未检测到 OPENROUTER_API_KEY 环境变量，请在 Run Configurations 中设置。", "错误");
            return;
        }

        Messages.showInfoMessage(project, "正在请求 AI 进行代码重构，请稍候……", "AI 正在处理");

        // 异步执行网络请求
        new Thread(() -> {
            try {
                String suggestion = requestAIRefactor(apiKey, selected, instruction);

                if (suggestion == null || suggestion.isBlank()) {
                    showError(project, "AI 返回为空，请检查 API Key 或网络。");
                    return;
                }

                showDiffAndApply(project, editor, doc, selected, suggestion);

            } catch (Exception ex) {
                ex.printStackTrace();
                showError(project, "调用 AI 失败：" + ex.getMessage());
            }
        }).start();
    }

    /**
     * 调用 OpenRouter API 获取重构建议
     */
    private String requestAIRefactor(String apiKey, String code, String instruction) throws Exception {
        String prompt = "请根据以下要求重构这段代码，并仅输出重构后的完整代码：" +
                "\n要求：" + instruction + "\n代码：\n```java\n" + code + "\n```";

        JSONObject payload = new JSONObject();
        payload.put("model", "gpt-4o-mini");  // 也可用 "gpt-4o" 或其他模型
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", "你是一名专业的Java重构专家。"));
        messages.put(new JSONObject().put("role", "user").put("content", prompt));
        payload.put("messages", messages);

        URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JSONObject obj = new JSONObject(response.toString());
        return obj
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
    }

    /**
     * 在 IntelliJ 中显示差异对比，并允许用户应用修改
     */
    private void showDiffAndApply(Project project, Editor editor, Document doc, String oldCode, String newCode) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                DocumentContent before = DiffContentFactory.getInstance().create(project, doc.getText());
                DocumentContent after = DiffContentFactory.getInstance().create(
                        project, doc.getText().replace(oldCode, newCode));

                SimpleDiffRequest request = new SimpleDiffRequest(
                        "AI 重构预览",
                        before,
                        after,
                        "原始代码",
                        "AI 重构后的代码"
                );

                DiffManager.getInstance().showDiff(project, request);

                int resp = Messages.showYesNoDialog(
                        project,
                        "是否将 AI 的重构结果应用到文件中？",
                        "应用重构",
                        Messages.getQuestionIcon()
                );
                if (resp == Messages.YES) {
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        int start = editor.getSelectionModel().getSelectionStart();
                        int end = editor.getSelectionModel().getSelectionEnd();
                        doc.replaceString(start, end, newCode);
                    });
                }
            } catch (Exception ex) {
                showError(project, "展示 Diff 失败：" + ex.getMessage());
            }
        });
    }

    private void showError(Project project, String msg) {
        javax.swing.SwingUtilities.invokeLater(() ->
                Messages.showErrorDialog(project, msg, "错误"));
    }
}
