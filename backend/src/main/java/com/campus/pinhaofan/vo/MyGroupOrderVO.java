package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MyGroupOrderVO {

    private GroupOrderVO order;

    private String myRole;

    private Long myParticipantId;

    private BigDecimal myPayableAmount;

    private String myPaymentStatus;

    private String pickupStatus;
}
