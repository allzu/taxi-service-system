package com.taxi.entity;

public enum MedicalStatus {
    PENDING("Ожидает осмотра"),
    PASSED("Допущен"),
    FAILED("Не допущен");

    private final String description;

    MedicalStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}