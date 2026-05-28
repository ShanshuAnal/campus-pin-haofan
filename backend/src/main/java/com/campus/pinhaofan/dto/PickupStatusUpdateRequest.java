package com.campus.pinhaofan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PickupStatusUpdateRequest {

    @NotBlank(message = "pickupStatus 不能为空")
    private String pickupStatus;

    private String pickupLocation;

    private String actualArrivalTime;

    private String pickedUpTime;

    private String distributedTime;

    private String remark;
}
