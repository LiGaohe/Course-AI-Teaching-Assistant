package org.example.ta.actions;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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

import java.io.BufferedReader;
import java.io.InputStream;
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

        // Messages.showInfoMessage(project, "正在请求 AI 进行代码重构，请稍候……", "AI 正在处理");

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
                String errorMsg = ex.getMessage();
                if (errorMsg.contains("402")) {
                    showError(project, "调用 AI 失败：API配额已用完或者需要付费。请检查您的账户配额或升级付费计划。");
                } else {
                    showError(project, "调用 AI 失败：" + errorMsg);
                }
            }
        }).start();
    }

    /**
     * 调用 OpenRouter API 获取重构建议
     */
    private String requestAIRefactor(String apiKey, String code, String instruction) throws Exception {
        String prompt = "请根据以下要求重构这段代码，并仅输出重构后的完整代码：" +
                "\n要求：" + instruction + "\n代码：\n```java\n" + code + "\n```";

        // 从资源文件中读取模型配置
        String modelConfigJson = readModelConfig();
        JSONObject modelConfig = JSON.parseObject(modelConfigJson);
        String modelName = modelConfig.getString("refactorModel");

        JSONObject payload = new JSONObject();
        payload.put("model", modelName);
        com.alibaba.fastjson.JSONArray messages = new com.alibaba.fastjson.JSONArray();
        messages.add(new JSONObject().fluentPut("role", "system").fluentPut("content", "你是一名专业的Java重构专家。"));
        messages.add(new JSONObject().fluentPut("role", "user").fluentPut("content", prompt));
        payload.put("messages", messages);

        URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toJSONString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        
        // 根据响应码选择输入流或错误流
        BufferedReader br;
        if (responseCode >= 200 && responseCode < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }
        
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }

        // 检查HTTP状态码
        if (responseCode != 200) {
            throw new RuntimeException("Server returned HTTP response code: " + responseCode + " for URL: " + url.toString() + ", Response: " + response.toString());
        }

        JSONObject obj = JSON.parseObject(response.toString());
        return obj.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
    }

    /**
     * 读取模型配置文件
     * 
     * @return 配置文件内容
     * @throws Exception 读取异常
     */
    private String readModelConfig() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("model-config.json")) {
            if (is == null) {
                throw new RuntimeException("找不到模型配置文件: model-config.json");
            }
            
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }
            return content.toString();
        }
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