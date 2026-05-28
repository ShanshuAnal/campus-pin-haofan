package com.campus.pinhaofan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PickupAssigneeRequest {

    @NotNull(message = "pickupUserId 不能为空")
    private Long pickupUserId;

    private String pickupLocation;

    private String estimatedArrivalTime;

    private String remark;
}
