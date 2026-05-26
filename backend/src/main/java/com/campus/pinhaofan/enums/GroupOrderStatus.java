package com.campus.pinhaofan.enums;

import lombok.Getter;

@Getter
public enum GroupOrderStatus {

    CREATED("CREATED"),
    LOCKED("LOCKED"),
    ORDERED("ORDERED"),
    DELIVERING("DELIVERING"),
    ARRIVED("ARRIVED"),
    PICKED_UP("PICKED_UP"),
    FINISHED("FINISHED"),
    CANCELLED("CANCELLED");

    private final String value;

    GroupOrderStatus(String value) {
        this.value = value;
    }
}
