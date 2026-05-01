package com.nvdia.aiplatform.interview.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nvdia.aiplatform.interview.config.AIConfig;
import com.nvdia.aiplatform.interview.model.AnswerRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Service
public class AIService {

    private final WebClient webClient;
    private final AIConfig config;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_RULES =
            "You are a senior FAANG interviewer. Be strict but fair. No extra explanation.";

    public AIService(WebClient webClient, AIConfig config, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    // =========================
    // 🚀 CALL NVIDIA AI (FIXED)
    // =========================
    public Mono<String> callAI(String prompt) {

        Map<String, Object> body = Map.of(
                "model", config.getModel(),
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        return webClient.post()
                .uri(config.getApiUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(15))
                .doOnNext(resp -> System.out.println("🔥 AI RESPONSE: " + resp))
                .map(this::extractText)
                .onErrorResume(ex -> {
                    System.out.println("❌ AI ERROR: " + ex.getMessage());
                    return Mono.just("ERROR_CALLING_AI");
                });
    }

    // =========================
    // 🚀 SAFE RESPONSE PARSER
    // =========================
    private String extractText(Map<?, ?> response) {

        try {
            if (response == null) return "EMPTY_RESPONSE";

            Object choicesObj = response.get("choices");

            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                return "NO_CHOICES_RETURNED";
            }

            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);

            Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");

            if (message == null || message.get("content") == null) {
                return "NO_CONTENT";
            }

            return String.valueOf(message.get("content"));

        } catch (Exception e) {
            return "ERROR_PARSING_RESPONSE";
        }
    }

    // =========================
    // 🚀 GENERATE QUESTION
    // =========================
    public Mono<String> generateQuestion(String role) {

        String prompt =
                SYSTEM_RULES +
                        "\nGenerate ONE technical interview question for: " + role +
                        "\nReturn ONLY the question text.";

        return callAI(prompt)
                .map(this::cleanQuestion)
                .map(q -> {

                    if (q.contains("ERROR") || q.contains("NO_") || q.contains("EMPTY")) {
                        return "⚠ Unable to generate question. Please try again.";
                    }

                    return q;
                });
    }

    private String cleanQuestion(String response) {

        if (response == null) return "";

        return response
                .replace("```", "")
                .replaceAll("^\\d+\\.\\s*", "")
                .replaceAll("^Q\\d+:\\s*", "")
                .trim();
    }

    // =========================
    // 🚀 EVALUATE ANSWERS
    // =========================
    public Mono<List<Map<String, Object>>> evaluateAnswers(List<AnswerRequest.QA> answers) {

        try {
            String jsonAnswers = objectMapper.writeValueAsString(answers);

            String prompt =
                    SYSTEM_RULES +
                            "\nEvaluate answers fairly.\n" +
                            "Return ONLY JSON array:\n" +
                            "[{\"question\":\"\",\"score\":0,\"good\":\"\",\"missing\":\"\",\"improvedAnswer\":\"\"}]\n\n" +
                            "DATA:\n" + jsonAnswers;

            return callAI(prompt)
                    .map(this::cleanJson)
                    .map(json -> {
                        try {
                            return objectMapper.readValue(
                                    json,
                                    new TypeReference<List<Map<String, Object>>>() {}
                            );
                        } catch (Exception e) {
                            return List.of(fallback("JSON_PARSE_FAILED"));
                        }
                    });

        } catch (Exception e) {
            return Mono.just(List.of(fallback("SERIALIZATION_FAILED")));
        }
    }

    // =========================
    // 🚀 CLEAN JSON
    // =========================
    private String cleanJson(String response) {

        if (response == null) return "[]";

        response = response
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int start = response.indexOf("[");
        int end = response.lastIndexOf("]");

        if (start != -1 && end != -1) {
            return response.substring(start, end + 1);
        }

        return "[]";
    }

    // =========================
    // 🚀 FALLBACK
    // =========================
    private Map<String, Object> fallback(String msg) {

        Map<String, Object> map = new HashMap<>();
        map.put("score", 0);
        map.put("good", "Error occurred");
        map.put("missing", "AI response issue");
        map.put("improvedAnswer", msg);

        return map;
    }
}