package com.taxi.entity;

public enum TechnicalStatus {
    UNKNOWN("Не проверен"),
    OK("Исправен"),
    NEEDS_REPAIR("Нужен ремонт"),
    NEEDS_INSPECTION("Нужен осмотр"),
    OUT_OF_SERVICE("В ремонте");

    private final String description;

    TechnicalStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
