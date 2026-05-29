package com.campus.pinhaofan.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum GroupOrderEventType {

    CANCELLED("CANCELLED", false),
    EXPIRED("EXPIRED", false),
    DELAY_REPORTED("DELAY_REPORTED", true),
    MERCHANT_DELAY("MERCHANT_DELAY", true),
    DELIVERY_DELAY("DELIVERY_DELAY", true),
    PICKUP_EXCEPTION("PICKUP_EXCEPTION", true),
    ITEM_MISSING("ITEM_MISSING", true),
    CONTACT_FAILED("CONTACT_FAILED", true),
    PAYMENT_DISPUTE("PAYMENT_DISPUTE", true),
    NOTE("NOTE", true);

    private final String value;

    private final boolean manuallyCreatable;

    GroupOrderEventType(String value, boolean manuallyCreatable) {
        this.value = value;
        this.manuallyCreatable = manuallyCreatable;
    }

    public static Optional<GroupOrderEventType> fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst();
    }
}
