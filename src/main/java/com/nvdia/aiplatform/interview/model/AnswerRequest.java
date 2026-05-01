package com.nvdia.aiplatform.interview.model;

import lombok.Data;

import java.util.List;

@Data
public class AnswerRequest {

    private String role;
    private List<QA> answers;

    @Data
    public static class QA {
        private String question;
        private String answer;
    }
}