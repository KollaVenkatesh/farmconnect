package com.klu.service;

public class RecipeAiException extends RuntimeException {
    public RecipeAiException(String message) {
        super(message);
    }

    public RecipeAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
