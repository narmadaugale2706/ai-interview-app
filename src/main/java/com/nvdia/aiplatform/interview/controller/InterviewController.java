package com.nvdia.aiplatform.interview.controller;

import com.nvdia.aiplatform.interview.model.AnswerRequest;
import com.nvdia.aiplatform.interview.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;


@RestController
@CrossOrigin("*")
public class InterviewController {

    @Autowired
    private AIService aiService;

    @PostMapping("/generate-question")
    public Mono<Map<String, String>> generate(@RequestBody Map<String, String> req) {

        String role = req.get("role");

        return aiService.generateQuestion(role)
                .map(q -> Map.of("question", q))
                .doOnError(e -> System.out.println("🔥 CONTROLLER ERROR: " + e.getMessage()))
                .onErrorReturn(Map.of("question", "⚠ Backend error"));
    }

    @PostMapping("/evaluate-answers")
    public Mono<Map<String, Object>> evaluate(@RequestBody AnswerRequest request) {

        System.out.println("🔥 REQUEST RECEIVED: " + request);

        return aiService.evaluateAnswers(request.getAnswers())
                .map(f -> Map.of("feedback", f))
                .doOnError(e -> System.out.println("🔥 EVALUATION ERROR: " + e.getMessage()));
    }

    @GetMapping("/")
    public String home() {
        return "AI Interview App Running 🚀";
    }
}