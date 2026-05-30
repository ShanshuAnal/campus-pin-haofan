package com.campus.pinhaofan.enums;

import lombok.Getter;

@Getter
public enum PaymentStatus {

    UNPAID("UNPAID"),
    ESCROWED("ESCROWED"),
    CONFIRMED("CONFIRMED"),
    SETTLED("SETTLED"),
    REFUNDED("REFUNDED");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }
}
