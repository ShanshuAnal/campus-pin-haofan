package com.campus.pinhaofan.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@Deprecated
@ConditionalOnProperty(prefix = "haofan.rocketmq", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopGroupOrderTimeoutMessagePublisher implements GroupOrderTimeoutMessagePublisher {

    @Override
    public void sendTimeoutMessage(Long orderId, LocalDateTime deadlineTime) {
        log.info("RocketMQ disabled, skip timeout message. orderId={}, deadlineTime={}", orderId, deadlineTime);
    }
}
