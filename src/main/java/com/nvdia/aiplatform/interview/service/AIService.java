package com.nvdia.aiplatform.interview.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nvdia.aiplatform.interview.config.AIConfig;
import com.nvdia.aiplatform.interview.model.AnswerRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AIService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private AIConfig config;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SYSTEM_RULES =
            "You are a senior FAANG interviewer. Be strict but fair. No extra explanation.";

    public Mono<String> callAI(String prompt) {

        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModel());

        body.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));

        return webClient.post()
                .uri(config.getApiUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractText)
                .onErrorReturn("ERROR_CALLING_AI");
    }

    private String extractText(Map response) {
        try {
            List choices = (List) response.get("choices");
            Map choice = (Map) choices.get(0);
            Map message = (Map) choice.get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            return "ERROR_PARSING_RESPONSE";
        }
    }

    public Mono<String> generateQuestion(String role) {

        String prompt =
                SYSTEM_RULES +
                        "\nGenerate ONE technical interview question for: " + role +
                        "\nReturn ONLY question text.";

        return callAI(prompt)
                .map(this::cleanQuestion);
    }

    private String cleanQuestion(String response) {

        if (response == null) return "";

        return response
                .replace("```", "")
                .replaceAll("^\\d+\\.\\s*", "")
                .replaceAll("^Q\\d+:\\s*", "")
                .trim();
    }

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
                            return List.of(fallback(json));
                        }
                    });

        } catch (Exception e) {
            return Mono.just(List.of(fallback("Serialization failed")));
        }
    }

    private Map<String, Object> fallback(String msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("score", 0);
        map.put("good", "Error occurred");
        map.put("missing", "AI response issue");
        map.put("improvedAnswer", msg);
        return map;
    }

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
}