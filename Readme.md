# 配置项目环境变量

- 点击Run IDE with Plugin的下拉菜单
- 选择Edit Configurations
- 设置OPENROUTER_API_KEY=<你的openrouter api key>

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