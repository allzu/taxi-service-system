package com.taxi.entity;

public enum WaybillStatus {
    OPEN("Открыт"),
    CLOSED("Закрыт");

    private final String description;

    WaybillStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}