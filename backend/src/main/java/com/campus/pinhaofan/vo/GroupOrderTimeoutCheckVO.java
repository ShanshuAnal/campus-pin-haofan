package com.campus.pinhaofan.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GroupOrderTimeoutCheckVO {

    private Long orderId;

    private Boolean expired;

    private String status;

    private String message;

    private String expireTime;

    private String deadlineTime;

    private Boolean retryRequired;

    public GroupOrderTimeoutCheckVO(
            Long orderId,
            Boolean expired,
            String status,
            String message,
            String expireTime) {
        this(orderId, expired, status, message, expireTime, null, false);
    }

    public GroupOrderTimeoutCheckVO(
            Long orderId,
            Boolean expired,
            String status,
            String message,
            String expireTime,
            String deadlineTime,
            Boolean retryRequired) {
        this.orderId = orderId;
        this.expired = expired;
        this.status = status;
        this.message = message;
        this.expireTime = expireTime;
        this.deadlineTime = deadlineTime;
        this.retryRequired = retryRequired;
    }
}
