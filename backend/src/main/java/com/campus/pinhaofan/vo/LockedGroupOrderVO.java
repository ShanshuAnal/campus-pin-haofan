package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LockedGroupOrderVO {

    private Long id;

    private String status;

    private BigDecimal originalTotalAmount;

    private BigDecimal actualDiscountAmount;

    private BigDecimal payableTotalAmount;

    private BigDecimal roundingAdjustmentAmount;

    private String lockedTime;
}
