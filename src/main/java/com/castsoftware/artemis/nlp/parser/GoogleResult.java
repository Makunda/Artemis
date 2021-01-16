package com.castsoftware.artemis.nlp.parser;

public class GoogleResult {

    private String title;
    private String content;
    private int numberResult;
    private boolean blacklisted;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getNumberResult() {
        return numberResult;
    }

    public void setNumberResult(int numberResult) {
        this.numberResult = numberResult;
    }

    public boolean isBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

    public GoogleResult(String title, String content, int numberResult, boolean blacklisted) {
        this.title = title;
        this.content = content;
        this.numberResult = numberResult;
        this.blacklisted = blacklisted;
    }

    public GoogleResult() {
        this.title = "";
        this.content = "";
        this.numberResult = 0;
        this.blacklisted = false;
    }
}
