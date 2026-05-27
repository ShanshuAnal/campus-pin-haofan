package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantVO {

    private Long id;

    private Long groupOrderId;

    private UserSummaryVO user;

    private BigDecimal originalAmount;

    private BigDecimal discountShareAmount;

    private BigDecimal payableAmount;

    private BigDecimal roundingAdjustmentAmount;

    private String paymentStatus;

    private String paidMarkTime;

    private String paidConfirmTime;

    private String joinTime;

    private String remark;

    private List<MealItemVO> mealItems;
}
