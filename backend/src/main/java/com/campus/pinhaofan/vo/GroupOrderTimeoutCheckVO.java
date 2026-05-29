package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupOrderTimeoutCheckVO {

    private Long orderId;

    private Boolean expired;

    private String status;

    private String message;

    private String expireTime;
}
