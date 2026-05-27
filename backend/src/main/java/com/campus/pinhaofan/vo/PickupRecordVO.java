package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PickupRecordVO {

    private Long id;

    private Long groupOrderId;

    private UserSummaryVO pickupUser;

    private String pickupLocation;

    private String pickupStatus;

    private String estimatedArrivalTime;

    private String actualArrivalTime;

    private String pickedUpTime;

    private String distributedTime;

    private String remark;
}
