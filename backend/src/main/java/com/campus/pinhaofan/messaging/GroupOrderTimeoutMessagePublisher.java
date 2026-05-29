package com.campus.pinhaofan.messaging;

import java.time.LocalDateTime;

public interface GroupOrderTimeoutMessagePublisher {

    void sendTimeoutMessage(Long orderId, LocalDateTime deadlineTime);
}
