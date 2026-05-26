package com.campus.pinhaofan.enums;

import lombok.Getter;

@Getter
public enum PaymentStatus {

    UNPAID("UNPAID"),
    PAID("PAID"),
    CONFIRMED("CONFIRMED"),
    REFUNDED("REFUNDED");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }
}
