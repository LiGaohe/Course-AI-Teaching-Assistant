package org.example.ta.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Client for OpenRouter API with retry mechanism and proper error handling.
 * Supports free tier models and handles API rate limiting.
 */
public class OpenRouterClient {
    private final String apiKey;
    private final String endpoint = "https://openrouter.ai/api/v1/chat/completions";
    private final HttpClient http;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Retry configuration
    private final int maxRetries = 3;
    private final long retryDelayMs = 1000;
    
    // Model configuration
    private String model = "alibaba/tongyi-deepresearch-30b-a3b:free";

    public OpenRouterClient(String apiKey) {
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    public OpenRouterClient(String apiKey, String model) {
        this(apiKey);
        this.model = model;
    }

    /**
     * Generate an answer using OpenRouter API with RAG context
     *
     * @param question The user's original question
     * @param contextChunks Retrieved context chunks from the knowledge base
     * @return The generated answer with citations
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    public String generateAnswer(String question, List<String> contextChunks) 
            throws IOException, InterruptedException {
        
        String prompt = buildPrompt(question, contextChunks);
        return callApiWithRetry(prompt, false);
    }
    
    /**
     * Generate an answer with reasoning enabled
     *
     * @param question The user's original question
     * @return The API response with reasoning details
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    public ReasoningResponse generateAnswerWithReasoning(String question) 
            throws IOException, InterruptedException {
        return callApiWithReasoning(question, false);
    }
    
    /**
     * Continue reasoning with previous response
     *
     * @param messages The message history including previous reasoning
     * @return The API response with continued reasoning
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    public ReasoningResponse continueReasoning(List<Message> messages) 
            throws IOException, InterruptedException {
        return callApiWithReasoningMessages(messages, true);
    }

    /**
     * Build a prompt that includes the question and retrieved context
     *
     * @param question The user's original question
     * @param contextChunks Retrieved context chunks
     * @return Formatted prompt for the LLM
     */
    private String buildPrompt(String question, List<String> contextChunks) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a helpful teaching assistant AI. ");
        prompt.append("Answer the following question based on the provided course materials. ");
        prompt.append("Always cite the source material and page number in your answer. ");
        prompt.append("If the answer is only based on your general knowledge (not from the provided materials), ");
        prompt.append("explicitly state that at the beginning of your response.\n\n");
        
        if (!contextChunks.isEmpty()) {
            prompt.append("Relevant course materials:\n");
            for (int i = 0; i < contextChunks.size(); i++) {
                prompt.append("[Source ").append(i + 1).append("] ")
                      .append(contextChunks.get(i)).append("\n\n");
            }
        }
        
        prompt.append("Question: ").append(question).append("\n\n");
        prompt.append("Answer:");
        
        return prompt.toString();
    }

    /**
     * Call the OpenRouter API with retry mechanism
     *
     * @param prompt The prompt to send to the API
     * @param isReasoning Whether to enable reasoning
     * @return The API response
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    private String callApiWithRetry(String prompt, boolean isReasoning) throws IOException, InterruptedException {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String body = createRequestBody(prompt, isReasoning);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(60))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("HTTP-Referer", "https://github.com/ta-plugin") // Optional but recommended
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parseResponse(response.body());
                } else if (response.statusCode() == 429) {
                    // Rate limited - wait and retry
                    long delay = retryDelayMs * (1L << attempt); // Exponential backoff
                    Thread.sleep(delay);
                    continue;
                } else {
                    throw new IOException("OpenRouter API error: " + response.statusCode() + " - " + response.body());
                }
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    long delay = retryDelayMs * (1L << attempt); // Exponential backoff
                    Thread.sleep(delay);
                }
            }
        }
        
        throw new IOException("Failed to get response from OpenRouter API after " + maxRetries + " retries", lastException);
    }
    
    /**
     * Call the OpenRouter API with reasoning enabled
     *
     * @param prompt The prompt to send to the API
     * @param isContinue Whether this is a continuation of previous reasoning
     * @return The API response with reasoning details
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    private ReasoningResponse callApiWithReasoning(String prompt, boolean isContinue) throws IOException, InterruptedException {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String body = createReasoningRequestBody(prompt, isContinue);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(60))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("HTTP-Referer", "https://github.com/ta-plugin") // Optional but recommended
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parseReasoningResponse(response.body());
                } else if (response.statusCode() == 429) {
                    // Rate limited - wait and retry
                    long delay = retryDelayMs * (1L << attempt); // Exponential backoff
                    Thread.sleep(delay);
                    continue;
                } else {
                    throw new IOException("OpenRouter API error: " + response.statusCode() + " - " + response.body());
                }
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    long delay = retryDelayMs * (1L << attempt); // Exponential backoff
                    Thread.sleep(delay);
                }
            }
        }
        
        throw new IOException("Failed to get response from OpenRouter API after " + maxRetries + " retries", lastException);
    }
    
    /**
     * Call the OpenRouter API with reasoning enabled using message history
     *
     * @param messages The message history
     * @param isContinue Whether this is a continuation of previous reasoning
     * @return The API response with reasoning details
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    private ReasoningResponse callApiWithReasoningMessages(List<Message> messages, boolean isContinue) throws IOException, InterruptedException {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String body = createReasoningRequestBodyFromMessages(messages, isContinue);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(60))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("HTTP-Referer", "https://github.com/ta-plugin") // Optional but recommended
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parseReasoningResponse(response.body());
                } else if (response.statusCode() == 429) {
                    // Rate limited - wait and retry
                    long delay = retryDelayMs * (1L << attempt); // Exponential backoff
                    Thread.sleep(delay);
                    continue;
                } else {
                    throw new IOException("OpenRouter API error: " + response.statusCode() + " - " + response.body());
                }
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    long delay = retryDelayMs * (1L << attempt); // Exponential backoff
                    Thread.sleep(delay);
                }
            }
        }
        
        throw new IOException("Failed to get response from OpenRouter API after " + maxRetries + " retries", lastException);
    }

    /**
     * Create the request body for the OpenRouter API
     *
     * @param prompt The prompt to send
     * @param isReasoning Whether to enable reasoning
     * @return JSON string representing the request body
     */
    private String createRequestBody(String prompt, boolean isReasoning) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        
        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);
        
        if (isReasoning) {
            ObjectNode reasoning = requestBody.putObject("reasoning");
            reasoning.put("enabled", true);
        }
        
        return requestBody.toString();
    }
    
    /**
     * Create the request body for the OpenRouter API with reasoning enabled
     *
     * @param prompt The prompt to send
     * @param isContinue Whether this is a continuation of previous reasoning
     * @return JSON string representing the request body
     */
    private String createReasoningRequestBody(String prompt, boolean isContinue) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        
        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);
        
        ObjectNode reasoning = requestBody.putObject("reasoning");
        reasoning.put("enabled", true);
        
        return requestBody.toString();
    }
    
    /**
     * Create the request body for the OpenRouter API with reasoning enabled using message history
     *
     * @param messages The message history
     * @param isContinue Whether this is a continuation of previous reasoning
     * @return JSON string representing the request body
     */
    private String createReasoningRequestBodyFromMessages(List<Message> messages, boolean isContinue) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        
        ArrayNode requestMessages = requestBody.putArray("messages");
        for (Message message : messages) {
            ObjectNode msg = requestMessages.addObject();
            msg.put("role", message.role);
            msg.put("content", message.content);
            
            if (message.reasoningDetails != null && !message.reasoningDetails.isEmpty()) {
                try {
                    msg.set("reasoning_details", objectMapper.readTree(message.reasoningDetails));
                } catch (Exception e) {
                    // If parsing fails, skip reasoning_details
                }
            }
        }
        
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);
        
        ObjectNode reasoning = requestBody.putObject("reasoning");
        reasoning.put("enabled", true);
        
        return requestBody.toString();
    }

    /**
     * Parse the API response to extract the generated text
     *
     * @param responseBody The raw API response
     * @return The generated text
     * @throws IOException If parsing fails
     */
    private String parseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        
        if (choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).path("message");
            JsonNode content = message.path("content");
            
            if (content.isTextual()) {
                return content.asText();
            }
        }
        
        throw new IOException("Unexpected API response format: " + responseBody);
    }
    
    /**
     * Parse the API response to extract the generated text and reasoning details
     *
     * @param responseBody The raw API response
     * @return The response with content and reasoning details
     * @throws IOException If parsing fails
     */
    private ReasoningResponse parseReasoningResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        
        if (choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).path("message");
            JsonNode content = message.path("content");
            JsonNode reasoningDetails = message.path("reasoning_details");
            
            String contentText = content.isTextual() ? content.asText() : "";
            String reasoningText = reasoningDetails.isObject() ? reasoningDetails.toString() : "{}";
            
            return new ReasoningResponse(contentText, reasoningText, responseBody);
        }
        
        throw new IOException("Unexpected API response format: " + responseBody);
    }

    /**
     * Utility method to properly escape JSON strings
     *
     * @param s The string to escape
     * @return Properly escaped JSON string
     */
    private String quoteJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                     .replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
    
    /**
     * Represents a message in the conversation
     */
    public static class Message {
        public final String role;
        public final String content;
        public final String reasoningDetails;
        
        public Message(String role, String content) {
            this(role, content, null);
        }
        
        public Message(String role, String content, String reasoningDetails) {
            this.role = role;
            this.content = content;
            this.reasoningDetails = reasoningDetails;
        }
    }
    
    /**
     * Represents a response with reasoning details
     */
    public static class ReasoningResponse {
        public final String content;
        public final String reasoningDetails;
        public final String fullResponse;
        
        public ReasoningResponse(String content, String reasoningDetails, String fullResponse) {
            this.content = content;
            this.reasoningDetails = reasoningDetails;
            this.fullResponse = fullResponse;
        }
    }
}