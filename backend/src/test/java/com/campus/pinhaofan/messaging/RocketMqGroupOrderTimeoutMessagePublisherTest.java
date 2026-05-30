package com.campus.pinhaofan.messaging;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RocketMqGroupOrderTimeoutMessagePublisherTest {

    @Test
    void calculateDeliveryTimeAddsFiveSecondBuffer() {
        LocalDateTime deadlineTime = LocalDateTime.of(2026, 5, 30, 11, 26);

        LocalDateTime deliveryTime = RocketMqGroupOrderTimeoutMessagePublisher.calculateDeliveryTime(deadlineTime);

        assertThat(deliveryTime).isEqualTo(LocalDateTime.of(2026, 5, 30, 11, 26, 5));
    }
}
