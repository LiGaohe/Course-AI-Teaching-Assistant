# 配置项目环境变量

- 点击Run IDE with Plugin的下拉菜单
- 选择Edit Configurations
- 设置OPENROUTER_API_KEY=<你的openrouter api key>
- 注意检查key的有效性

# 修改api key

- 如何更改Model
  - 如果您想使用不同的模型，有两种方法：
    1. 直接修改默认值： 修改OpenRouterClient.java中的默认model值:
       ```java
        private String model = "your-desired-model-name";
       ```
    2. 在创建客户端时指定： 修改TaToolWindowPanel.java中创建OpenRouterClient的地方，传入想要的模型名称：
       ```java
          OpenRouterClient client = new OpenRouterClient("your-api-key", "your-desired-model-name");
       ```

# 测试插件的IDEA选择

- 可以使用默认设置, 这样无需激活IDEA

- 或者您已有激活的IDEA, 可在`build.gradle.kts`中, 修改:
```kotlin
intellijPlatform {
        create("IC", "2025.1.4.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        bundledPlugin("com.intellij.java")
    }
```

改为:
```kotlin
intellijPlatform {
        local("E:/intellj idea/IntelliJ IDEA 2025.2.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        bundledPlugin("com.intellij.java")
}
```
# 开始

## 使用命令行运行:

```bash
.\gradlew clean
.\gradlew build
set OPENROUTER_API_KEY=your-api-key-here
.\gradlew runIde
   
```

## 点击运行

```bash
.\gradlew clean
.\gradlew build

# 然后点击Run IDE with Plugin
   
```
