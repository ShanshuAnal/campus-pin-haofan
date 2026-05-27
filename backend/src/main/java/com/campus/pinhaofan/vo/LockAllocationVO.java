package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LockAllocationVO {

    private Long participantId;

    private Long userId;

    private BigDecimal originalAmount;

    private BigDecimal discountShareAmount;

    private BigDecimal payableAmount;

    private BigDecimal roundingAdjustmentAmount;

    private String paymentStatus;
}
