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
            "You are a strict FAANG interviewer. Always respond in valid JSON only.";

    public AIService(WebClient webClient, AIConfig config, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    // =========================
    // CALL AI
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
                .map(this::extractText)
                .doOnNext(res -> System.out.println("🔥 RAW AI RESPONSE: " + res))
                .doOnError(e -> System.out.println("🔥 AI ERROR: " + e.getMessage()));
    }

    // =========================
    // EXTRACT RESPONSE
    // =========================
    private String extractText(Map<?, ?> response) {

        try {
            List<?> choices = (List<?>) response.get("choices");

            if (choices == null || choices.isEmpty()) {
                return "";
            }

            Map<?, ?> first = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) first.get("message");

            return String.valueOf(message.get("content"));

        } catch (Exception e) {
            return "";
        }
    }

    // =========================
    // GENERATE QUESTION
    // =========================
    public Mono<String> generateQuestion(String role) {

        String prompt =
                SYSTEM_RULES +
                        "\nGenerate ONE interview question for: " + role +
                        "\nReturn ONLY the question text.";

        return callAI(prompt)
                .map(res -> res.replace("```", "").trim())
                .map(q -> q.isEmpty() ? "Unable to generate question" : q);
    }

    // =========================
    // EVALUATE ANSWERS (FIXED)
    // =========================
    public Mono<List<Map<String, Object>>> evaluateAnswers(List<AnswerRequest.QA> answers) {

        try {
            String input = objectMapper.writeValueAsString(answers);

            String prompt =
                    SYSTEM_RULES +
                            "\nReturn ONLY valid JSON array. No explanation." +
                            "\nFormat strictly like:" +
                            "\n[" +
                            "{\"question\":\"\",\"score\":8,\"good\":\"\",\"missing\":\"\",\"improvedAnswer\":\"\"}" +
                            "]" +
                            "\nDATA:\n" + input;

            return callAI(prompt)
                    .map(this::cleanJson)
                    .map(json -> {
                        System.out.println("🔥 FINAL JSON BEFORE PARSE: " + json);

                        try {
                            return objectMapper.readValue(
                                    json,
                                    new TypeReference<List<Map<String, Object>>>() {}
                            );
                        } catch (Exception e) {
                            System.out.println("🔥 JSON PARSE FAILED");
                            e.printStackTrace();

                            return List.of(fallback("JSON_PARSE_FAILED"));
                        }
                    });

        } catch (Exception e) {
            return Mono.just(List.of(fallback("SERIALIZATION_FAILED")));
        }
    }

    // =========================
    // CLEAN JSON
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
    // FALLBACK
    // =========================
    private Map<String, Object> fallback(String msg) {

        Map<String, Object> map = new HashMap<>();
        map.put("score", 0);
        map.put("good", "Error occurred");
        map.put("missing", "AI issue");
        map.put("improvedAnswer", msg);

        return map;
    }
}