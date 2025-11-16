# Course TA Assistant 插件用户手册

## 简介

Course TA Assistant 是一个为 IntelliJ IDEA 设计的插件，旨在为编程学习者提供课程专属的 AI 助教功能。该插件结合了检索增强生成（RAG）技术，能够基于用户提供的课程材料回答问题，并提供代码解释和重构功能。

## 主要功能

1. **基于课程材料的问答**：使用 RAG 技术，基于索引的课程材料回答问题
2. **代码解释**：右键菜单选项可快速解释选中的代码片段
3. **AI 代码重构**：通过 AI 建议重构选中的代码，并提供差异对比
4. **知识库管理**：可视化管理用于问答的课程材料索引

## 安装和配置

### 环境变量设置

在使用插件前，需要设置 OpenRouter API 密钥：

1. 点击"Run IDE with Plugin"下拉菜单
2. 选择"Edit Configurations"
3. 在环境变量中添加：`OPENROUTER_API_KEY=<你的openrouter api key>`

### 模型配置

插件使用的模型可以在`resources/model-config.json`中配置

## 用户界面说明

### 主界面


插件主界面位于 IntelliJ IDEA 的右侧工具窗口中，标题为"Course TA"。

![](assets/插件主页面.png)

界面主要分为以下几个部分：

1. **顶部标题栏**：
   - 显示插件名称"Teaching Assistant"
   - 显示副标题"AI-powered Java course helper"
   - 显示当前模式"RAG · OpenRouter"

2. **左侧知识库面板**：
   - 显示知识库状态（是否已索引）
   - 显示索引的文件夹数量和会话中的消息数
   - 提供"Manage..."和"Reindex All"按钮来管理知识库

3. **中央对话区域**：
   - 显示对话历史，包括用户提问和 TA 回答
   - 顶部工具栏提供"Clear"、"Copy all"和"Export..."功能
   - 支持滚动查看历史对话

4. **底部输入区域**：
   - 文本输入框用于输入问题
   - 显示输入字符数
   - 提供两个提问按钮：
     - "Ask TA"：普通提问
     - "Ask with Reasoning"：带推理过程的提问

### 知识库管理对话框

![](assets/插件主页面点击知识库.png)

通过点击左侧知识库面板中的"Manage..."按钮可以打开知识库管理对话框：

![](assets/知识库页面.png)

1. **顶部信息栏**：
   - 标题"Knowledge Base Paths"
   - 描述"Add, remove or reindex the folders that the TA uses as context"

2. 显示当前已索引的文件夹和文件列表

3. **底部操作按钮**：
   - "Add New Path"：添加新的文档路径
   - "Delete Selected"：删除选中的路径
   - "Reindex All"：重新索引所有文档
   - "Close"：关闭对话框

## 使用指南

### 1. 索引课程材料

要使用基于课程材料的问答功能，首先需要索引相关文档：

1. 点击左侧知识库面板的"Manage..."按钮

![](assets/插件主页面点击知识库.png)

2. 点击"Add New Path"按钮

![](assets/知识库页面点添加.png)

3. 输入包含课程材料的文件夹路径

![](assets/添加知识库索引.png)

4. 点击"Reindex All"按钮确保所有文档都被索引

![](assets/知识库页面点检索.png)

### 2. 进行问答

在底部输入区域输入问题，然后点击"Ask TA"或"Ask with Reasoning"按钮：

![](assets/问问题.png)

### 3. 解释代码

![](assets/问.png)

在编辑器中选中需要解释的代码片段，然后：

1. 右键点击选中代码
2. 选择"Ask Course TA"菜单项
3. 插件会自动将代码发送给 TA 进行解释

### 4. 代码重构

![](assets/重构.png)

在编辑器中选中需要重构的代码片段，然后：

1. 右键点击选中代码
2. 选择"Refactor with TA"菜单项
3. 在弹出的对话框中输入重构要求
4. 等待 AI 生成重构建议
5. 在差异对比窗口中查看建议的更改
6. 选择是否应用重构

## 故障排除

### 常见问题

1. **API 密钥错误**：
   - 确保已正确设置 OPENROUTER_API_KEY 环境变量
   - 检查密钥是否有效且有足够配额

2. **无法索引文档**：
   - 检查提供的文件夹路径是否正确
   - 确保文件夹中包含可读取的文档文件

3. **问答功能不工作**：
   - 确认已成功索引文档
   - 检查网络连接是否正常

### 错误信息

- "Please index documents first before asking questions"：需要先索引文档
- "未检测到 OPENROUTER_API_KEY 环境变量"：未设置 API 密钥
- "调用 AI 失败：API配额已用完或者需要付费"：API 配额不足

## 其他功能

### 自定义模型

用户可以通过修改代码来使用不同的 AI 模型：

1. 修改 OpenRouterClient.java 中的默认模型
2. 在创建 OpenRouterClient 实例时指定模型名称

### 导出对话

使用对话区域顶部工具栏的"Export..."按钮可以将当前对话导出为文本文件。

## 版本信息

当前版本支持以下 IntelliJ IDEA 版本：
- IntelliJ IDEA Community Edition 2025.1.4.1 或更高版本

