package com.program.bookie.models;

import java.io.Serializable;

public class Quote implements Serializable {
    private static final long serialVersionUID = 1L;

    private String text;
    private String author;

    public Quote() {}

    public Quote(String text, String author) {
        this.text = text;
        this.author = author;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getFormattedQuote() {
        if (author != null && !author.trim().isEmpty()) {
            return "\"" + text + "\" - " + author;
        } else {
            return "\"" + text + "\"";
        }
    }

    @Override
    public String toString() {
        return "Quote{text='" + text + "', author='" + author + "'}";
    }
}