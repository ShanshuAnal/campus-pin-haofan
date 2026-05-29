package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupOrderDetailVO {

    private GroupOrderVO order;

    private List<ParticipantVO> participants;

    private PickupRecordVO pickupRecord;

    private OrderAmountVO orderAmount;

    private String pickupStatus;

    private GroupOrderPermissionsVO permissions;
}
