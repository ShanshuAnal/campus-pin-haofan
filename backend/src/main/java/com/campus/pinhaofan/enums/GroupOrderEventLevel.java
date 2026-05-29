package com.campus.pinhaofan.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum GroupOrderEventLevel {

    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR");

    private final String value;

    GroupOrderEventLevel(String value) {
        this.value = value;
    }

    public static Optional<GroupOrderEventLevel> fromValue(String value) {
        return Arrays.stream(values())
                .filter(level -> level.value.equals(value))
                .findFirst();
    }
}
