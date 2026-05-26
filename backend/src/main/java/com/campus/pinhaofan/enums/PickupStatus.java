package com.campus.pinhaofan.enums;

import lombok.Getter;

@Getter
public enum PickupStatus {

    WAITING_ORDER("WAITING_ORDER"),
    WAITING_DELIVERY("WAITING_DELIVERY"),
    ARRIVED("ARRIVED"),
    PICKED_UP("PICKED_UP"),
    DISTRIBUTED("DISTRIBUTED");

    private final String value;

    PickupStatus(String value) {
        this.value = value;
    }
}
