package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupOrderPermissionsVO {

    private Boolean canJoin;

    private Boolean canLock;

    private Boolean canCancel;

    private Boolean canMarkPayment;

    private Boolean canConfirmPayment;

    private Boolean canUpdatePickupStatus;

    private Boolean canCreateEvent;

    private Boolean canViewEvents;
}
