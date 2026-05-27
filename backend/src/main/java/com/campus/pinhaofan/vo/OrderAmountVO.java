package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderAmountVO {

    private Integer participantCount;

    private BigDecimal originalTotalAmount;

    private BigDecimal discountThresholdAmount;

    private BigDecimal discountAmount;

    private BigDecimal actualDiscountAmount;

    private BigDecimal payableTotalAmount;

    private BigDecimal discountGapAmount;

    private Boolean discountReached;
}
