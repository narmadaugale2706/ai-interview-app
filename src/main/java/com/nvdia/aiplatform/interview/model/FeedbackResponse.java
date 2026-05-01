package com.nvdia.aiplatform.interview.model;

import lombok.Data;

import java.util.List;

@Data
public class FeedbackResponse {
    private List<Feedback> feedback;

    @Data
    public static class Feedback {
        private int score;
        private String good;
        private String missing;
        private String improvedAnswer;
    }
}