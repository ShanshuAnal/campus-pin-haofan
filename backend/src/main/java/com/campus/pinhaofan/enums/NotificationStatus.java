package com.campus.pinhaofan.enums;

import lombok.Getter;

@Getter
public enum NotificationStatus {

    UNREAD("UNREAD"),
    READ("READ");

    private final String value;

    NotificationStatus(String value) {
        this.value = value;
    }
}
