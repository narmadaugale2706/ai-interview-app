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
    // SIMPLE FAST AI CALL
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
    // GENERATE QUESTION (CACHE OPTIMIZED)
    // =========================
    public Mono<String> generateQuestion(String role) {

        String cacheKey = "question_" + role;

        return getFromCache("questions", cacheKey, String.class)
                .doOnNext(v -> log.info("🔥 Cache HIT for role: {}", role))
                .switchIfEmpty(
                        callAI("Ask 1 FAANG interview question for: " + role)
                                .map(this::cleanText)
                                .map(q -> q.isBlank() ? "Unable to generate question" : q)
                                .doOnNext(result -> putCache("questions", cacheKey, result))
                );
    }

    // =========================
    // EVALUATE ANSWERS (CACHE OPTIMIZED)
    // =========================
    public Mono<List<Map<String, Object>>> evaluateAnswers(List<AnswerRequest.QA> answers) {

        try {
            String input = objectMapper.writeValueAsString(answers);
            String cacheKey = "eval_" + input.hashCode();

            return getFromCache("evaluations", cacheKey, List.class)
                    .doOnNext(v -> log.info("🔥 Cache HIT for evaluation"))
                    .switchIfEmpty(
                            callAI(
                                    "Return ONLY JSON array." +
                                            " Format: [{question,score,good,missing,improvedAnswer}]" +
                                            " DATA: " + input
                            )
                                    .map(this::cleanJson)
                                    .map(json -> {
                                        try {
                                            return objectMapper.readValue(
                                                    json,
                                                    new TypeReference<List<Map<String, Object>>>() {}
                                            );
                                        } catch (Exception e) {
                                            log.error("JSON parse error", e);
                                            return List.of(fallback("PARSE_ERROR"));
                                        }
                                    })
                                    .doOnNext(result -> putCache("evaluations", cacheKey, result))
                    );

        } catch (Exception e) {
            return Mono.just(List.of(fallback("SERIALIZATION_ERROR")));
        }
    }

    // =========================
    // CACHE HELPERS (REACTIVE SAFE)
    // =========================
    private <T> Mono<T> getFromCache(String cacheName, String key, Class<T> type) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) return Mono.empty();

        T value = cache.get(key, type);
        return value != null ? Mono.just(value) : Mono.empty();
    }

    private void putCache(String cacheName, String key, Object value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    // =========================
    // RESPONSE PARSING
    // =========================
    private String extractText(Map<?, ?> response) {

        try {
            List<?> choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) return "";

            Map<?, ?> first = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) first.get("message");

            return String.valueOf(message.get("content"));

        } catch (Exception e) {
            return "";
        }
    }

    // =========================
    // CLEANERS
    // =========================
    private String cleanText(String text) {
        return text == null ? "" : text.replace("```", "").trim();
    }

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

    // =========================
    // FALLBACK
    // =========================
    private Map<String, Object> fallback(String msg) {

        Map<String, Object> map = new HashMap<>();
        map.put("score", 0);
        map.put("good", "Error occurred");
        map.put("missing", "System issue");
        map.put("improvedAnswer", msg);
        return map;
    }
}