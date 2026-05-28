package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PickupAssigneeVO {

    private Long orderId;

    private Long pickupUserId;

    private PickupRecordVO pickupRecord;
}
