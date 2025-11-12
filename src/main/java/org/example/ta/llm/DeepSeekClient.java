package org.example.ta.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Simple client wrapper for DeepSeek-like LLM API.
 * NOTE: You must configure your API key and real endpoint in application settings.
 */
public class DeepSeekClient {
    private final String apiKey;
    private final String endpoint; // e.g. https://api.deepseek.com/v1/generate
    private final HttpClient http = HttpClient.newHttpClient();

    public DeepSeekClient(String endpoint, String apiKey) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    public String generateAnswer(String prompt, Map<String, Object> options) throws IOException, InterruptedException {
// Build a JSON body. Keep minimal to be compatible with common LLM APIs.
        String body = "{\"prompt\": " + quoteJson(prompt) + ", \"max_tokens\": 512 }";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
// For robust code, you should parse JSON and extract the text field.
// Here we return the full response body and let the caller parse it if needed.
            return resp.body();
        } else {
            throw new IOException("LLM request failed: " + resp.statusCode() + " - " + resp.body());
        }
    }

    private String quoteJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}