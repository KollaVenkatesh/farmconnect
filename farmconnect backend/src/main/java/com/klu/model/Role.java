package com.klu.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {
    USER, BUYER, FARMER, ADMIN;

    @JsonCreator
    public static Role fromValue(String value) {
        if (value == null || value.isBlank()) return USER;
        return Role.valueOf(value.trim().toUpperCase());
    }
}
