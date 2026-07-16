package com.fanguide.model;

/**
 * Request payload for POST /api/chat.
 * {@code language} is optional — the controller defaults to "English" and
 * coerces any unrecognized value to a safe default before it reaches the
 * AI prompt.
 */
public class ChatRequest {
    private String question;
    private String language;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
