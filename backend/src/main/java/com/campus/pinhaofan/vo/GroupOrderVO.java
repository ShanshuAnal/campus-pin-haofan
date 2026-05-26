package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupOrderVO {

    private Long id;

    private String title;

    private String orderType;

    private String merchantName;

    private String pickupLocation;

    private UserSummaryVO creator;

    private String deadlineTime;

    private Integer maxParticipants;

    private Integer participantCount;

    private BigDecimal discountThresholdAmount;

    private BigDecimal discountAmount;

    private BigDecimal originalTotalAmount;

    private BigDecimal actualDiscountAmount;

    private BigDecimal payableTotalAmount;

    private BigDecimal roundingAdjustmentAmount;

    private String status;

    private UserSummaryVO pickupUser;

    private String remark;

    private String lockedTime;

    private String finishTime;

    private String cancelTime;

    private String createTime;

    private String updateTime;
}
