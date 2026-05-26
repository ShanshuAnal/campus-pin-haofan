package com.campus.pinhaofan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateGroupOrderRequest {

    @NotBlank(message = "title 不能为空")
    private String title;

    @NotBlank(message = "orderType 不能为空")
    private String orderType;

    @NotBlank(message = "merchantName 不能为空")
    private String merchantName;

    @NotBlank(message = "pickupLocation 不能为空")
    private String pickupLocation;

    @NotBlank(message = "deadlineTime 不能为空")
    private String deadlineTime;

    private Integer maxParticipants;

    private BigDecimal discountThresholdAmount;

    private BigDecimal discountAmount;

    private String remark;
}
