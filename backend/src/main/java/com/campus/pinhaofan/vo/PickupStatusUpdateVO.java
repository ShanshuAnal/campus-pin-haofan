package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PickupStatusUpdateVO {

    private PickupRecordVO pickupRecord;

    private String orderStatus;
}
