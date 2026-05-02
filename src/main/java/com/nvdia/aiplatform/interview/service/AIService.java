package com.nvdia.aiplatform.interview.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nvdia.aiplatform.interview.config.AIConfig;
import com.nvdia.aiplatform.interview.model.AnswerRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class AIService {

    private final WebClient webClient;
    private final AIConfig config;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    public AIService(WebClient webClient,
                     AIConfig config,
                     ObjectMapper objectMapper,
                     CacheManager cacheManager) {
        this.webClient = webClient;
        this.config = config;
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
    }

    // =========================
    // AI CALL
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
                .timeout(Duration.ofSeconds(10))
                .map(this::extractText);
    }

    // =========================
    // QUESTION (FIXED CACHE FLOW)
    // =========================
    public Mono<String> generateQuestion(String role) {

        String cacheKey = "question_" + role;

        Cache cache = cacheManager.getCache("questions");
        if (cache != null) {
            String cached = cache.get(cacheKey, String.class);
            if (cached != null) {
                return Mono.just(cached);
            }
        }

        String prompt = "You are FAANG interviewer. Ask 1 Java interview question. Return ONLY question.";

        return callAI(prompt)
                .map(this::cleanText)
                .map(q -> q.isBlank() ? "Unable to generate question" : q)
                .doOnNext(result -> {
                    if (cache != null) cache.put(cacheKey, result);
                });
    }

    // =========================
    // EVALUATION (FIXED + STRONG PROMPT)
    // =========================
    public Mono<List<Map<String, Object>>> evaluateAnswers(List<AnswerRequest.QA> answers) {

        try {
            String input = objectMapper.writeValueAsString(answers);
            String cacheKey = "eval_" + input.hashCode();

            Cache cache = cacheManager.getCache("evaluations");

            if (cache != null) {
                List<Map<String, Object>> cached = cache.get(cacheKey, List.class);
                if (cached != null) {
                    return Mono.just(cached);
                }
            }

            String prompt =
                    "You are FAANG interviewer evaluator.\n" +
                            "Return ONLY valid JSON ARRAY.\n" +
                            "NO explanation, NO text, NO markdown.\n" +
                            "Format strictly:\n" +
                            "[{\"question\":\"\",\"score\":8,\"good\":\"\",\"missing\":\"\",\"improvedAnswer\":\"\"}]\n" +
                            "DATA:\n" + input;

            return callAI(prompt)
                    .map(this::cleanJson)
                    .map(json -> {
                        try {
                            return objectMapper.readValue(
                                    json,
                                    new TypeReference<List<Map<String, Object>>>() {}
                            );
                        } catch (Exception e) {
                            return List.of(fallback("PARSE_ERROR"));
                        }
                    })
                    .doOnNext(result -> {
                        if (cache != null) cache.put(cacheKey, result);
                    });

        } catch (Exception e) {
            return Mono.just(List.of(fallback("SERIALIZATION_ERROR")));
        }
    }

    // =========================
    // FIXED TEXT EXTRACTION
    // =========================
    private String extractText(Map<?, ?> response) {

        try {
            List<?> choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) return "";

            Map<?, ?> first = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) first.get("message");

            return message.get("content").toString();

        } catch (Exception e) {
            return "";
        }
    }

    // =========================
    // IMPROVED JSON CLEANER (IMPORTANT FIX)
    // =========================
    private String cleanJson(String response) {

        if (response == null) return "[]";

        response = response
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int start = response.indexOf("[");
        int end = response.lastIndexOf("]");

        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }

        return "[]";
    }

    private String cleanText(String text) {
        return text == null ? "" : text.replace("```", "").trim();
    }

    private Map<String, Object> fallback(String msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("score", 0);
        map.put("good", "Error occurred");
        map.put("missing", "System issue");
        map.put("improvedAnswer", msg);
        return map;
    }
}