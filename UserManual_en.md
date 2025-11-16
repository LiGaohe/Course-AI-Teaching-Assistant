# Course TA Assistant Plugin User Manual

## Introduction

Course TA Assistant is a plugin designed for IntelliJ IDEA, aiming to provide AI-powered teaching assistant features for programming learners. The plugin leverages Retrieval-Augmented Generation (RAG) technology to answer questions based on user-provided course materials, and offers code explanation and refactoring functionalities.

## Main Features

1. **Course Material-Based Q&A**: Uses RAG to answer questions based on indexed course materials.
2. **Code Explanation**: Quickly explains selected code snippets via the right-click menu.
3. **AI Code Refactoring**: Suggests code refactoring with AI and provides diff comparison.
4. **Knowledge Base Management**: Visual management of indexed course materials for Q&A.

## Installation and Configuration

### Environment Variable Setup

Before using the plugin, set your OpenRouter API key:

1. Click the "Run IDE with Plugin" dropdown menu.
2. Select "Edit Configurations".
3. Add the following to environment variables: `OPENROUTER_API_KEY=<your openrouter api key>`

### Model Configuration

The model used by the plugin can be configured in `resources/model-config.json`.

## User Interface Overview

### Main Interface

The main interface is located in the right tool window of IntelliJ IDEA, titled "Course TA".

The interface consists of:

1. **Top Title Bar**:
   - Displays the plugin name "Teaching Assistant"
   - Shows the subtitle "AI-powered Java course helper"
   - Shows the current mode "RAG · OpenRouter"

2. **Left Knowledge Base Panel**:
   - Shows knowledge base status (indexed or not)
   - Displays the number of indexed folders and messages in the session
   - Provides "Manage..." and "Reindex All" buttons for knowledge base management

3. **Central Conversation Area**:
   - Displays conversation history, including user questions and TA answers
   - Top toolbar provides "Clear", "Copy all", and "Export..." functions
   - Supports scrolling through conversation history

4. **Bottom Input Area**:
   - Text box for entering questions
   - Shows character count
   - Provides two ask buttons:
     - "Ask TA": Normal question
     - "Ask with Reasoning": Question with reasoning process

### Knowledge Base Management Dialog

Click the "Manage..." button in the left knowledge base panel to open the management dialog.

1. **Top Info Bar**:
   - Title "Knowledge Base Paths"
   - Description "Add, remove or reindex the folders that the TA uses as context"

2. Shows the list of currently indexed folders and files

3. **Bottom Action Buttons**:
   - "Add New Path": Add a new document path
   - "Delete Selected": Delete selected path(s)
   - "Reindex All": Reindex all documents
   - "Close": Close the dialog

## Usage Guide

### 1. Index Course Materials

To use course material-based Q&A, first index the relevant documents:

1. Click the "Manage..." button in the left knowledge base panel
2. Click "Add New Path"
3. Enter the folder path containing course materials
4. Click "Reindex All" to ensure all documents are indexed

### 2. Ask Questions

Enter your question in the bottom input area, then click "Ask TA" or "Ask with Reasoning".

### 3. Explain Code

Select the code snippet you want to explain in the editor, then:

1. Right-click the selected code
2. Choose "Ask Course TA" from the menu
3. The plugin will send the code to the TA for explanation

### 4. Code Refactoring

Select the code snippet you want to refactor in the editor, then:

1. Right-click the selected code
2. Choose "Refactor with TA" from the menu
3. Enter your refactoring requirements in the dialog
4. Wait for the AI to generate refactoring suggestions
5. View the suggested changes in the diff window
6. Choose whether to apply the refactoring

## Troubleshooting

### Common Issues

1. **API Key Error**:
   - Ensure the `OPENROUTER_API_KEY` environment variable is set correctly
   - Check if the key is valid and has sufficient quota

2. **Unable to Index Documents**:
   - Check if the provided folder path is correct
   - Ensure the folder contains readable document files

3. **Q&A Not Working**:
   - Confirm that documents have been successfully indexed
   - Check your network connection

### Error Messages

- "Please index documents first before asking questions": You need to index documents first
- "未检测到 OPENROUTER_API_KEY 环境变量": API key not set
- "调用 AI 失败：API配额已用完或者需要付费": API quota exhausted or payment required

## Other Features

### Custom Models

You can use different AI models by modifying the code:

1. Change the default model in `OpenRouterClient.java`
2. Specify the model name when creating an `OpenRouterClient` instance

### Export Conversation

Use the "Export..." button in the conversation area toolbar to export the current conversation as a text file.

## Version Information

Current version supports the following IntelliJ IDEA versions:
- IntelliJ IDEA Community Edition 2025.1.4.1 or later