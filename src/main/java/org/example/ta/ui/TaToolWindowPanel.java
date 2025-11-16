package org.example.ta.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.components.FlatButton;
import com.formdev.flatlaf.extras.components.FlatTextArea;
import com.formdev.flatlaf.ui.FlatScrollBarUI;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.example.ta.index.DocChunk;
import org.example.ta.index.DocumentIndexer;
import org.example.ta.index.IndexFileManager;
import org.example.ta.llm.OpenRouterClient;
import org.example.ta.retrieval.SimpleRetriever;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
//import java.util.*;
import java.util.stream.Collectors;

/**
 * 全新 macOS / IntelliJ 风格的精美面板：
 * - 顶部标题栏 + 渐变 + 三色窗口按钮
 * - 左侧知识库卡片 + 状态
 * - 中间对话区气泡 + 顶部工具栏（清空/复制/导出）
 * - 底部输入区 + 动画按钮 + 字数统计 + Shift+Enter 换行
 *
 * 功能相关的方法（deleteSelectedPath / addNewDocumentPath / reindexAllDocuments / performRAGProcess* 等）
 * 只保留签名和调用位置，你可以把原有实现整体粘进去覆盖我标注的 TODO 块。
 */
public class TaToolWindowPanel {

    private final JPanel panel;
    // 换成 FlatTextArea，但兼容 JTextArea API
    private final FlatTextArea inputArea = new FlatTextArea();
    private final FlatTextArea outputArea = new FlatTextArea();
    private JButton askBtn = new JButton("Ask TA");
    private JButton askWithReasoningBtn = new JButton("Ask with Reasoning");
    private final JProgressBar loadingBar = new JProgressBar();
    private SimpleRetriever retriever;
    private final IndexFileManager indexFileManager = new IndexFileManager();

    // 颜色 & UI 常量
    private static final Color MAC_BG = new JBColor(new Color(246, 246, 248), new Color(24, 24, 26));
    private static final Color CARD_BG = new JBColor(new Color(255, 255, 255), new Color(35, 35, 37));
    private static final Color HEADER_GRADIENT_LEFT = new Color(97, 165, 255);
    private static final Color HEADER_GRADIENT_RIGHT = new Color(145, 120, 255);
    private static final Color MAC_ACCENT = new Color(0, 122, 255);
    private static final Color MAC_ACCENT_HOVER = new Color(10, 132, 255);
    private static final Color MAC_BORDER = new JBColor(new Color(220, 220, 220), new Color(70, 70, 70));
    private static final Color MAC_TEXT = new JBColor(new Color(28, 28, 30), new Color(230, 230, 234));
    private static final Color MAC_TEXT_SECONDARY = new JBColor(new Color(110, 110, 115), new Color(160, 160, 170));
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 30);

    // 动画
    private javax.swing.Timer headerGlowTimer;
    private float headerGlowPhase = 0f;
    private javax.swing.Timer askPulseTimer;
    private float askPulsePhase = 0f;

    // 状态
    private int indexedChunksCount = 0;
    private JLabel kbStatusLabel;
    private JLabel kbPathCountLabel;
    private JLabel messageCountLabel;
    private int messageCount = 0;

    private static TaToolWindowPanel instance;

    public TaToolWindowPanel() {
        instance = this;
        panel = new JPanel(new BorderLayout());
        panel.setBackground(MAC_BG);
        //panel.setPreferredSize(new Dimension(480, 820));
        setupBasicStyling();
        createUI();
        setupAnimations();
        
        // 在插件启动时自动进行一次重新索引
        autoReindexOnStartup();
    }

    /********************
     * 样式基础配置
     ********************/
    private void setupBasicStyling() {
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        inputArea.setColumns(50);
        inputArea.setRows(4);

        inputArea.setBackground(new JBColor(Color.WHITE, new Color(50, 50, 52)));
        inputArea.setForeground(MAC_TEXT);
        inputArea.setCaretColor(MAC_ACCENT);

        outputArea.setEditable(false);
        outputArea.setBackground(new JBColor(new Color(248, 248, 250), new Color(32, 32, 34)));
        outputArea.setForeground(MAC_TEXT);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setPreferredSize(new Dimension(loadingBar.getPreferredSize().width, 3));
        loadingBar.setBorderPainted(false);
        loadingBar.setForeground(MAC_ACCENT);
        loadingBar.setBackground(new Color(0, 0, 0, 0));

        // ⭐ 在这里安装占位提示（placeholder），注意要在设置完颜色之后调用
        installPlaceholderOnTextArea(
                inputArea,
                "Ask your course TA anything... (Enter = send, Shift+Enter = newline)"
        );

        // 输入区绑定快捷键：Enter 发送，Shift+Enter 换行
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (!e.isShiftDown()) {
                        e.consume();
                        askBtn.doClick();
                    }
                }
            }
        });
    }

    /**
     * 在任意 JTextArea 上实现“占位提示”效果（不依赖第三方 API）。
     */
    private void installPlaceholderOnTextArea(JTextArea textArea, String placeholder) {
        Color normalColor = MAC_TEXT; // 你原本的字体颜色
        Color placeholderColor = MAC_TEXT_SECONDARY; // 次要文字颜色，偏灰

        // 初始时如果为空，就显示 placeholder
        if (textArea.getText().isEmpty()) {
            textArea.setForeground(placeholderColor);
            textArea.setText(placeholder);
        }

        textArea.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (textArea.getText().equals(placeholder)) {
                    textArea.setText("");
                    textArea.setForeground(normalColor);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (textArea.getText().trim().isEmpty()) {
                    textArea.setForeground(placeholderColor);
                    textArea.setText(placeholder);
                }
            }
        });

        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void handleChange() {
                // 如果是占位状态就不改（避免和焦点逻辑打架）
                if (textArea.getText().equals(placeholder)) {
                    textArea.setForeground(placeholderColor);
                } else {
                    textArea.setForeground(normalColor);
                }
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                handleChange();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                handleChange();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                handleChange();
            }
        });
    }

    /********************
     * 主 UI 结构
     ********************/
    private void createUI() {
        // 外层 padding + 阴影卡片
        JPanel root = new JPanel(new GridBagLayout());
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        GradientShadowPanel mainCard = new GradientShadowPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                // 给一个“希望更高一点”的建议高度，比如 700
                if (d.height < 700) {
                    d.height = 700;
                }
                return d;
            }
        };
        mainCard.setLayout(new BorderLayout());
        mainCard.setBackground(CARD_BG);
        mainCard.setBorder(new EmptyBorder(8, 8, 8, 8));

        // 顶部 header
        JPanel header = createHeaderBar();
        mainCard.add(header, BorderLayout.NORTH);

        // 中间是左右结构：左边知识库卡片，右边对话区
        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setOpaque(false);

        JPanel leftSidebar = createLeftSidebar();
        JPanel conversationPanel = createConversationPanel();

        center.add(leftSidebar, BorderLayout.WEST);
        center.add(conversationPanel, BorderLayout.CENTER);

        mainCard.add(center, BorderLayout.CENTER);

        // 底部输入区
        JPanel bottomInput = createBottomInputBar();
        mainCard.add(bottomInput, BorderLayout.SOUTH);

        // 把 mainCard 放到 root 里，居中铺开
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        root.add(mainCard, gbc);

        panel.add(root, BorderLayout.CENTER);
    }


    /********************
     * 顶部 Header：渐变 + 三色点 + 模式标签
     ********************/
    private JPanel createHeaderBar() {
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // 渐变背景
                float glow = 0.25f + 0.15f * (float) Math.sin(headerGlowPhase);
                GradientPaint gp = new GradientPaint(0, 0,
                        blend(HEADER_GRADIENT_LEFT, Color.WHITE, glow),
                        w, h,
                        blend(HEADER_GRADIENT_RIGHT, Color.WHITE, glow * 0.7f));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(6, 10, 6, 10));
        header.setLayout(new BorderLayout());

        // 左边：三色圆点 + 标题
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        left.setOpaque(false);

        JPanel trafficLights = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int r = 10;
                int gap = 4;
                int x = 2;
                int y = 2;

                g2.setColor(new Color(255, 95, 86));
                g2.fillOval(x, y, r, r);
                x += r + gap;
                g2.setColor(new Color(255, 189, 46));
                g2.fillOval(x, y, r, r);
                x += r + gap;
                g2.setColor(new Color(39, 201, 63));
                g2.fillOval(x, y, r, r);

                g2.dispose();
            }
        };
        trafficLights.setPreferredSize(new Dimension(44, 16));
        trafficLights.setOpaque(false);

        JLabel title = new JLabel("Teaching Assistant");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));

        JLabel subtitle = new JLabel("AI-powered Java course helper");
        subtitle.setForeground(new Color(230, 230, 245, 210));
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 11f));

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        textBlock.add(title);
        textBlock.add(subtitle);

        //left.add(trafficLights);
        left.add(textBlock);

        // 右侧：模式标签 + loadingBar
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setOpaque(false);

        JLabel modeBadge = new JLabel("RAG · OpenRouter");
        modeBadge.setOpaque(true);
        modeBadge.setBackground(new Color(255, 255, 255, 40));
        modeBadge.setForeground(Color.WHITE);
        modeBadge.setFont(modeBadge.getFont().deriveFont(Font.BOLD, 11f));
        modeBadge.setBorder(new EmptyBorder(3, 8, 3, 8));

        loadingBar.setPreferredSize(new Dimension(110, 3));

        right.add(modeBadge);
        right.add(loadingBar);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    /********************
     * 左侧：知识库状态卡片
     ********************/
    private JPanel createLeftSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setOpaque(false);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(180, 0)); // 减小默认宽度从220到180

        // =============== 可折叠的知识库面板 ===============
        CollapsiblePanel kbPanel = new CollapsiblePanel("Knowledge Base", createKnowledgeBaseContent());
        kbPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(kbPanel);
        
        // KB面板和Tips面板之间增加一点外间距
        sidebar.add(Box.createVerticalStrut(8));

        // =============== 可折叠的提示面板 ===============
        CollapsiblePanel tipsPanel = new CollapsiblePanel("Tips", createTipsContent());
        tipsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(tipsPanel);
        
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }
    
    private JPanel createKnowledgeBaseContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        kbStatusLabel = new JLabel("Not indexed yet");
        kbStatusLabel.setForeground(MAC_TEXT_SECONDARY);
        kbStatusLabel.setFont(kbStatusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        kbStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        kbPathCountLabel = new JLabel("0 folders indexed");
        kbPathCountLabel.setForeground(MAC_TEXT_SECONDARY);
        kbPathCountLabel.setFont(kbPathCountLabel.getFont().deriveFont(Font.PLAIN, 11f));
        kbPathCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        messageCountLabel = new JLabel("0 messages in this session");
        messageCountLabel.setForeground(MAC_TEXT_SECONDARY);
        messageCountLabel.setFont(messageCountLabel.getFont().deriveFont(Font.PLAIN, 11f));
        messageCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel statsPanel = new JPanel();
        statsPanel.setOpaque(false);
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.add(kbPathCountLabel);
        statsPanel.add(Box.createVerticalStrut(2));
        statsPanel.add(messageCountLabel);

        // 按钮：Manage & Reindex 样式一致（都用描边按钮）
        JPanel kbButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0)); // 减小按钮间距
        kbButtons.setOpaque(false);
        kbButtons.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton manageBtn = createAccentOutlineButton("Manage");
        JButton reindexBtn = createAccentOutlineButton("Reindex");
        manageBtn.setFont(manageBtn.getFont().deriveFont(Font.PLAIN, 10f)); // 减小字体
        reindexBtn.setFont(reindexBtn.getFont().deriveFont(Font.PLAIN, 10f));

        manageBtn.addActionListener(e -> showIndexDocumentsDialog());
        reindexBtn.addActionListener(e -> reindexAllDocuments(panel));

        kbButtons.add(manageBtn);
        kbButtons.add(reindexBtn);

        // 顺序 + 间距
        content.add(kbStatusLabel);
        content.add(Box.createVerticalStrut(6));
        content.add(statsPanel);
        content.add(Box.createVerticalStrut(8));
        content.add(kbButtons);
        
        return content;
    }
    
    private JPanel createTipsContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        // 用 JTextArea 来做自动换行的多行 tip
        JTextArea t1 = new JTextArea("• Index your lecture slides");
        JTextArea t2 = new JTextArea("• Use \"Ask with Reasoning\" for harder questions");
        JTextArea t3 = new JTextArea("• You can reopen the index dialog anytime");

        java.util.List<JTextArea> tips = java.util.Arrays.asList(t1, t2, t3);
        for (JTextArea ta : tips) {
            ta.setEditable(false);
            ta.setOpaque(false);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setForeground(MAC_TEXT_SECONDARY);
            ta.setFont(ta.getFont().deriveFont(Font.PLAIN, 10f)); // 减小字体
            ta.setAlignmentX(Component.LEFT_ALIGNMENT);
            ta.setBorder(null);
        }

        content.add(t1);
        content.add(Box.createVerticalStrut(2)); // 减小间距
        content.add(t2);
        content.add(Box.createVerticalStrut(2));
        content.add(t3);
        
        return content;
    }

    /********************
     * 中间对话区：顶部工具栏 + 输出气泡 + 复制/清空/导出按钮
     ********************/
    private JPanel createConversationPanel() {
        JPanel container = new JPanel(new BorderLayout(0, 6));
        container.setOpaque(false);

        // 顶部工具条
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JLabel convTitle = new JLabel("Conversation");
        convTitle.setForeground(MAC_TEXT_SECONDARY);
        convTitle.setFont(convTitle.getFont().deriveFont(Font.BOLD, 12f));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);

        JButton clearBtn = createIconTextButton("Clear");
        JButton copyBtn = createIconTextButton("Copy all");
        JButton exportBtn = createIconTextButton("Export...");

        clearBtn.addActionListener(e -> {
            outputArea.setText("");
            messageCount = 0;
            updateMessageCount();
        });

        copyBtn.addActionListener(e -> {
            String text = outputArea.getText();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(text), null);
            showToast("Conversation copied to clipboard");
        });

        exportBtn.addActionListener(e -> exportConversation());

        actions.add(clearBtn);
        actions.add(copyBtn);
        actions.add(exportBtn);

        toolbar.add(convTitle, BorderLayout.WEST);
        toolbar.add(actions, BorderLayout.EAST);

        // 输出区：使用 JBScrollPane + 自定义 ScrollBarUI
        JBScrollPane scrollPane = new JBScrollPane(outputArea);
        scrollPane.setBorder(createRoundBorder());
        scrollPane.getVerticalScrollBar().setUI(new FlatScrollBarUI() {
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
        });

        container.add(toolbar, BorderLayout.NORTH);
        container.add(scrollPane, BorderLayout.CENTER);
        return container;
    }

    /********************
     * 底部输入区：文本 + 字数 + 两个按钮
     ********************/
    private JPanel createBottomInputBar() {
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(10, 0, 0, 0));

        // 输入文本 + 字数提示
        JPanel inputPanel = new RoundedPanel(14, CARD_BG);
        inputPanel.setLayout(new BorderLayout());
        inputPanel.setBorder(new EmptyBorder(8, 10, 6, 10));

        JBScrollPane inputScroll = new JBScrollPane(inputArea);
        inputScroll.setBorder(BorderFactory.createEmptyBorder());
        inputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.getVerticalScrollBar().setUI(new FlatScrollBarUI());

        JLabel charCountLabel = new JLabel("0 chars");
        charCountLabel.setForeground(MAC_TEXT_SECONDARY);
        charCountLabel.setFont(charCountLabel.getFont().deriveFont(Font.PLAIN, 10f));

        inputArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                int len = inputArea.getText().length();
                charCountLabel.setText(len + " chars");
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        JPanel countPanel = new JPanel(new BorderLayout());
        countPanel.setOpaque(false);
        countPanel.add(charCountLabel, BorderLayout.EAST);

        inputPanel.add(inputScroll, BorderLayout.CENTER);
        inputPanel.add(countPanel, BorderLayout.SOUTH);

        // 下方按钮排布
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.setOpaque(false);

        askWithReasoningBtn = createPrimaryButton("Ask with Reasoning");
        askBtn = createPrimaryButton("Ask TA");

        askWithReasoningBtn.addActionListener(e -> handleAskQuestion(true));
        askBtn.addActionListener(e -> handleAskQuestion(false));

        buttonRow.add(askWithReasoningBtn);
        buttonRow.add(askBtn);

        bottom.add(inputPanel, BorderLayout.CENTER);
        bottom.add(buttonRow, BorderLayout.SOUTH);
        return bottom;
    }

    /********************
     * 动画设置
     ********************/
    private void setupAnimations() {
        // Header 渐变“呼吸”动画
        headerGlowTimer = new javax.swing.Timer(40, e -> {
            headerGlowPhase += 0.04f;
            panel.repaint();
        });
        headerGlowTimer.start();

        // Ask 按钮轻微呼吸
        askPulseTimer = new javax.swing.Timer(40, e -> {
            askPulsePhase += 0.08f;
            askBtn.repaint();
            askWithReasoningBtn.repaint();
        });
        askPulseTimer.start();
    }

    /********************
     * 提问逻辑（UI 包装）
     ********************/
    private void handleAskQuestion(boolean withReasoning) {
        String q = inputArea.getText().trim();
        if (q.isEmpty()) return;

        messageCount++;
        updateMessageCount();

        startLoadingAnimation();
        appendSystemMessage("Thinking" + (withReasoning ? " with reasoning" : "") + "...");

        inputArea.setEnabled(false);
        askBtn.setEnabled(false);
        askWithReasoningBtn.setEnabled(false);

        new Thread(() -> {
            try {
                String answer = withReasoning ? performRAGProcessWithReasoning(q) : performRAGProcess(q);
                SwingUtilities.invokeLater(() -> {
                    appendUserMessage(q);
                    appendAssistantMessage(answer);
                    stopLoadingAnimation();
                    inputArea.setText("");
                    inputArea.setEnabled(true);
                    askBtn.setEnabled(true);
                    askWithReasoningBtn.setEnabled(true);
                    inputArea.requestFocusInWindow();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    appendSystemMessage("Error: " + ex.getMessage());
                    stopLoadingAnimation();
                    inputArea.setEnabled(true);
                    askBtn.setEnabled(true);
                    askWithReasoningBtn.setEnabled(true);
                    ex.printStackTrace();
                });
            }
        }).start();
    }

    private void startLoadingAnimation() {
        loadingBar.setVisible(true);
        FlatAnimatedLafChange.showSnapshot();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
    }

    private void stopLoadingAnimation() {
        loadingBar.setVisible(false);
    }

    /********************
     * 输出区：气泡样式追加
     ********************/
    private void appendSystemMessage(String text) {
        outputArea.append("[system] " + text + "\n\n");
        scrollOutputToBottom();
    }

    private void appendUserMessage(String text) {
        outputArea.append("You:\n" + text + "\n\n");
        scrollOutputToBottom();
    }

    private void appendAssistantMessage(String text) {
        // Format the assistant message to clearly indicate the source of information
        String formattedText = formatAssistantResponse(text);
        outputArea.append("TA:\n" + formattedText + "\n\n");
        scrollOutputToBottom();
    }

    /**
     * Format the assistant response to clearly indicate if the answer is based on knowledge base or general knowledge
     *
     * @param response The raw response from the assistant
     * @return Formatted response with clear source indication
     */
    private String formatAssistantResponse(String response) {
        // Check if response indicates it's based on knowledge base
        if (response.contains("[") && response.contains("page")) {
            // Looks like it has citations, wrap it in a clear section
            return "Based on course materials:\n" + response;
        } else if (response.contains("general knowledge")) {
            // Explicitly states it's based on general knowledge
            return "Based on general knowledge:\n" + response;
        } else if (response.startsWith("To use the full RAG capabilities")) {
            // Demonstration mode
            return response;
        } else if (response.contains("Based on the retrieved course materials")) {
            // Demonstration answer with course materials
            return "Based on course materials:\n" + response;
        } else if (response.contains("This answer is based on general knowledge")) {
            // Demonstration answer based on general knowledge
            return "Based on general knowledge:\n" + response;
        }
        // Default case - try to make it clearer
        return "Response:\n" + response;
    }

    private void scrollOutputToBottom() {
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void updateMessageCount() {
        if (messageCountLabel != null) {
            messageCountLabel.setText(messageCount + " messages in this session");
        }
    }

    /********************
     * 导出对话
     ********************/
    private void exportConversation() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Conversation");
        chooser.setSelectedFile(new File("ta-conversation-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".txt"));

        int result = chooser.showSaveDialog(panel);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(outputArea.getText());
                showToast("Conversation exported to " + file.getName());
            } catch (IOException ex) {
                Messages.showErrorDialog(panel, "Failed to export: " + ex.getMessage(), "Export Error");
            }
        }
    }

    /********************
     * Toast 小提示
     ********************/
    private void showToast(String message) {
        JWindow toast = new JWindow();
        toast.setBackground(new Color(0, 0, 0, 0));

        JPanel content = new RoundedPanel(18, new Color(30, 30, 30, 230));
        content.setBorder(new EmptyBorder(6, 14, 6, 14));
        content.setLayout(new BorderLayout());

        JLabel label = new JLabel(message);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));

        content.add(label, BorderLayout.CENTER);
        toast.add(content);
        toast.pack();

        Point loc = panel.getLocationOnScreen();
        int x = loc.x + panel.getWidth() - toast.getWidth() - 24;
        int y = loc.y + panel.getHeight() - toast.getHeight() - 24;
        toast.setLocation(x, y);
        toast.setAlwaysOnTop(true);
        toast.setVisible(true);

        new Timer(1800, e -> toast.dispose()).start();
    }

    /********************
     * 知识库管理对话框（UI 重做，只调用原逻辑）
     ********************/
    private void showIndexDocumentsDialog() {
        JDialog dialog = new JDialog((Frame) null, "Knowledge Base Management", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(780, 520);
        dialog.setLocationRelativeTo(panel);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(12, 12, 12, 12));
        main.setBackground(MAC_BG);

        // 顶部标题区域
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Knowledge Base Paths");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        title.setForeground(MAC_TEXT);

        JLabel desc = new JLabel("Add, remove or reindex the folders that the TA uses as context.");
        desc.setForeground(MAC_TEXT_SECONDARY);
        desc.setFont(desc.getFont().deriveFont(Font.PLAIN, 11f));

        JPanel topText = new JPanel();
        topText.setOpaque(false);
        topText.setLayout(new BoxLayout(topText, BoxLayout.Y_AXIS));
        topText.add(title);
        topText.add(desc);

        top.add(topText, BorderLayout.WEST);
        main.add(top, BorderLayout.NORTH);

        // 中部：JTree + border
        JComponent knowledgeBasePanel = createKnowledgeBasePanel();
        main.add(knowledgeBasePanel, BorderLayout.CENTER);

        // 底部按钮区域
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        bottom.setOpaque(false);

        JButton addBtn = createAccentOutlineButton("Add New Path");
        JButton deleteBtn = createMutedButton("Delete Selected");
        JButton reindexBtn = createPrimaryButton("Reindex All");
        JButton closeBtn = createMutedButton("Close");

        addBtn.addActionListener(e -> {
            addNewDocumentPath(dialog);
            // 更新右侧统计
            refreshKbStats();
            // 重建树结构
            dialog.dispose();
            showIndexDocumentsDialog();
        });

        deleteBtn.addActionListener(e -> {
            deleteSelectedPath(dialog, knowledgeBasePanel, deleteBtn);
            refreshKbStats();
        });

        reindexBtn.addActionListener(e -> {
            reindexAllDocuments(dialog);
            refreshKbStats();
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        bottom.add(addBtn);
        bottom.add(deleteBtn);
        bottom.add(reindexBtn);
        bottom.add(closeBtn);

        main.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(main);
        dialog.setVisible(true);
    }

    /**
     * 创建知识库面板（Tree）
     * 这里只是 UI + Tree 的创建逻辑，本身不改变你原来的 indexFileManager 行为
     */
    private JComponent createKnowledgeBasePanel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Knowledge Base");
        DefaultTreeModel treeModel = new DefaultTreeModel(root);

        List<String> paths = indexFileManager.loadDocumentPaths();
        DocumentIndexer indexer = new DocumentIndexer();

        for (String path : paths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                DefaultMutableTreeNode pathNode = new DefaultMutableTreeNode(new FileInfo(dir.getName(), path, true));
                root.add(pathNode);

                try {
                    List<File> files = indexer.listAllFilesRecursively(dir);
                    for (File file : files) {
                        String relativePath = dir.toPath().relativize(file.toPath()).toString();
                        pathNode.add(new DefaultMutableTreeNode(new FileInfo(relativePath, file.getAbsolutePath(), false)));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                DefaultMutableTreeNode pathNode = new DefaultMutableTreeNode(new FileInfo(dir.getName() + " (Invalid)", path, true));
                root.add(pathNode);
            }
        }

        JTree tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setBackground(new JBColor(Color.WHITE, new Color(45, 45, 47)));
        tree.setForeground(MAC_TEXT);
        tree.setBorder(new EmptyBorder(4, 4, 4, 4));

        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
            tree.expandPath(new TreePath(child.getPath()));
        }

        JBScrollPane scrollPane = new JBScrollPane(tree);
        scrollPane.setBorder(createRoundBorder());
        scrollPane.getVerticalScrollBar().setUI(new FlatScrollBarUI());
        return scrollPane;
    }

    private void refreshKbStats() {
        List<String> paths = indexFileManager.loadDocumentPaths();
        if (kbPathCountLabel != null) {
            kbPathCountLabel.setText(paths.size() + " folders indexed");
        }
        if (kbStatusLabel != null) {
            kbStatusLabel.setText(retriever == null ? "Index not ready" : "Ready for questions");
        }
    }

    /********************
     * 各种小组件工厂：按钮、圆角边框
     ********************/
    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                float pulse = 0.04f * (float) Math.sin(askPulsePhase);
                Color base = getModel().isRollover() ? MAC_ACCENT_HOVER : MAC_ACCENT;
                Color c = blend(base, Color.WHITE, pulse);

                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);

                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(getText());
                int th = fm.getAscent();
                g2.drawString(getText(), (getWidth() - tw) / 2, (getHeight() + th) / 2 - 2);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(new EmptyBorder(6, 18, 6, 18));
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 12f));
        return btn;
    }

    private JButton createAccentOutlineButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                g2.setColor(MAC_ACCENT);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(new EmptyBorder(4, 12, 4, 12));
        btn.setFocusPainted(false);
        btn.setForeground(MAC_ACCENT);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 11f));
        return btn;
    }

    private JButton createMutedButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(4, 10, 4, 10));
        btn.setContentAreaFilled(true);
        btn.setBackground(new JBColor(new Color(240, 240, 242), new Color(55, 55, 57)));
        btn.setForeground(MAC_TEXT);
        btn.setFont(btn.getFont().deriveFont(Font.PLAIN, 11f));
        return btn;
    }

    private JButton createIconTextButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(new EmptyBorder(3, 6, 3, 6));
        btn.setForeground(MAC_TEXT_SECONDARY);
        btn.setFont(btn.getFont().deriveFont(Font.PLAIN, 11f));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(MAC_ACCENT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(MAC_TEXT_SECONDARY);
            }
        });
        return btn;
    }

    private Border createRoundBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAC_BORDER, 1, true),
                new EmptyBorder(4, 6, 4, 6)
        );
    }

    /********************
     * 一些辅助类：圆角面板、渐变阴影面板
     ********************/
    private static class RoundedPanel extends JPanel {
        private final int arc;
        private final Color bg;

        public RoundedPanel(int arc, Color bg) {
            this.arc = arc;
            this.bg = bg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class GradientShadowPanel extends JPanel {
        public GradientShadowPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // 阴影
            g2.setColor(SHADOW_COLOR);
            g2.fillRoundRect(8, 10, w - 16, h - 16, 18, 18);

            // 主体背景
            g2.setColor(CARD_BG);
            g2.fillRoundRect(0, 0, w - 8, h - 8, 18, 18);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /********************
     * 可折叠面板组件
     ********************/
    private static class CollapsiblePanel extends JPanel {
        private final String title;
        private final JPanel contentPanel;
        private final JLabel toggleButton;
        private boolean expanded = true;

        public CollapsiblePanel(String title, JPanel content) {
            this.title = title;
            this.contentPanel = content;
            
            setOpaque(false);
            setLayout(new BorderLayout());
            
            // 创建标题栏
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            
            JLabel titleLabel = new JLabel(title);
            titleLabel.setForeground(MAC_TEXT);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
            
            toggleButton = new JLabel("▼");
            toggleButton.setFont(toggleButton.getFont().deriveFont(10f));
            toggleButton.setForeground(MAC_TEXT_SECONDARY);
            
            header.add(titleLabel, BorderLayout.WEST);
            header.add(toggleButton, BorderLayout.EAST);
            
            // 添加点击事件处理
            header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            header.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toggle();
                }
            });
            
            // 创建内容面板
            contentPanel.setOpaque(false);
            contentPanel.setBorder(new EmptyBorder(8, 0, 8, 0));
            
            // 创建主容器
            JPanel mainPanel = new RoundedPanel(14, CARD_BG);
            mainPanel.setBorder(new EmptyBorder(10, 12, 10, 12)); // 减小边距
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.add(header);
            mainPanel.add(Box.createVerticalStrut(6));
            mainPanel.add(contentPanel);
            
            add(mainPanel, BorderLayout.NORTH);
        }
        
        private void toggle() {
            expanded = !expanded;
            contentPanel.setVisible(expanded);
            toggleButton.setText(expanded ? "▼" : "▶");
            revalidate();
            repaint();
        }
    }

    private static Color blend(Color c1, Color c2, float ratio) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
        int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
        int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
        int a = (int) (c1.getAlpha() * (1 - ratio) + c2.getAlpha() * ratio);
        return new Color(r, g, b, a);
    }

    /********************
     * ===== 下面是你原来的功能函数 =====
     * 注意：签名完全不改，只在方法体里写了 TODO。
     * 你直接把你消息里那坨完整实现粘进来覆盖 TODO 即可。
     ********************/

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

    /**
     * 在插件启动时自动重新索引所有已配置的文档路径
     */
    private void autoReindexOnStartup() {
        // 使用SwingUtilities.invokeLater确保UI完全初始化后再执行
        SwingUtilities.invokeLater(() -> {
            List<String> paths = indexFileManager.loadDocumentPaths();
            if (!paths.isEmpty()) {
                appendSystemMessage("Auto-reindexing all documents on startup...");
                reindexAllDocuments(panel);
            }
        });
    }

    public JComponent getComponent() { return panel; }
    public static TaToolWindowPanel getInstance() { return instance; }
    public void setInputText(String text) { inputArea.setText(text); }
    public void ask() { askBtn.doClick(); }
}
