好的，作为需求分析工程师和建模专家，我将为您分析这份作业文档，并构建相应的需求模型。

### 1. 需求分析报告

**1.1 项目概述**
本项目要求开发一个IntelliJ IDEA插件，作为《Java企业级应用开发（荣誉）》课程的AI编程助教（TA）。核心目标是利用RAG（检索增强生成）技术，结合课程资料和LLM（大语言模型）能力，为学生提供智能化的编码辅助。

**1.2 核心需求分解**

*   **功能性需求 (Functional Requirements)**
    *   **F0： 基本功能**
        *   **FR1 - RAG问答聊天机器人：** 插件必须实现一个能够回答课程相关问题的聊天机器人。
            *   **FR1.1 - 知识库构建：** 系统能够预处理课程资料（如讲义幻灯片），将其分解为可搜索的知识块并建立索引。
            *   **FR1.2 - 相关性检索：** 当用户提问时，系统能从知识库中检索出最相关的知识块。
            *   **FR1.3 - 增强生成与引用：** 将检索到的知识块和原始问题一同发送给LLM服务，生成回答。回答中必须明确引用来源（文档名和页码/范围）。若未找到相关材料，需明确声明回答基于LLM的通用知识。
        *   **FR2 - 用户界面：** 在IntelliJ IDEA内提供一个用户友好的界面（如侧边栏或工具窗口）用于与TA交互。
        *   **FR3 - 上下文感知：** 插件需感知用户的编程上下文。至少需实现一个编辑器中的右键菜单操作，允许用户就选中的代码段快速提问。
    *   **F1： 扩展功能**
        *   **FR4 - AI驱动的源代码修改：** 提供根据用户指令执行重要代码修改任务的功能（如应用设计模式重构方法、修复bug）。
            *   **FR4.1 - 差异预览：** 修改源代码前，必须使用IntelliJ IDEA的内置差异查看器显示建议的更改，提供清晰的并排对比。
            *   **FR4.2 - 用户确认：** 允许用户审核并批准AI生成的建议后再应用更改。

*   **非功能性需求 (Non-Functional Requirements)**
    *   **NFR1 - 可用性：** 用户界面需友好、易于使用。
    *   **NFR2 - 集成性：** 插件必须与IntelliJ IDEA环境无缝集成。
    *   **NFR3 - 性能：** 问答和代码修改的响应时间应在合理范围内。
    *   **NFR4 - 可靠性：** 引用信息需准确，代码修改建议需安全（需用户确认）。

*   **约束条件 (Constraints)**
    *   **CON1 - 平台：** 必须为IntelliJ IDEA插件。
    *   **CON2 - 技术：** 鼓励使用免费的LLM服务（如阿里云百炼、DeepSeek），推荐使用LangChain4j、Apache Tika等技术支持RAG流程。
    *   **CON3 - 交付物：** 必须提交源代码、项目报告（PDF）和演示幻灯片（PDF）。

### 2. 需求模型

**2.1 用例图 (Use Case Diagram)**
此图描述了系统与外部用户（学生）之间的交互。

![](out/usecase/user-case.svg)

*   **参与者（Actor）：** 学生（Student）。

*   **用例（Use Cases）：**
    *   **询问课程相关问题 (Ask Course-Related Question):** 学生通过聊天界面提问。
    *   **就选中代码提问 (Ask Question on Selected Code):** 学生通过右键菜单针对特定代码提问（包含`<<include>>`关系，因为它是一种特定的提问方式）。
    *   **请求代码修改 (Request Code Modification):** 学生请求AI修改代码（F1功能）。
    *   **审核并应用更改 (Review & Apply Changes):** 学生在差异查看器中审核并确认应用AI的修改建议（F1功能，被`Request Code Modification`包含）。

**2.2 RAG核心业务流程流程图 (Flowchart)**
此图详细说明了F0功能中RAG问答的核心业务流程。

![](out/RAG-activity/RAG-Activity.svg)

**2.3 系统组件图 (Component Diagram)**

此图描述了插件的主要构成组件及其依赖关系。

![](system-componet.svg)

*   **用户界面组件 (User Interface Component):** 负责工具窗口、右键菜单的渲染和用户交互。
*   **核心控制器组件 (Core Controller Component):** 协调整个流程，处理用户请求，调用RAG服务和代码修改服务。

*   **上下文管理器组件 (Context Manager Component):** 获取并管理当前编辑器的上下文（选中的代码）。

* **LLM 框架(lang4j Library):** 与 `LLM Service` 通信。

*   **文档解析器 (Doc Parser):** 使用Apache Tika等库解析课程文档。
*   **向量数据库/索引 (Vector DB/Index):** 存储和处理知识块，支持相似性检索。
*   **LLM服务 (LLM Service):** 外部的大语言模型API。
