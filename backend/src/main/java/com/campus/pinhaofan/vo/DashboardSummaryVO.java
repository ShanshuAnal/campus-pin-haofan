package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class DashboardSummaryVO {

    private Long todayOrderCount;

    private Long successOrderCount;

    private BigDecimal totalSavedAmount;

    private Long orderCount;

    private Long createdCount;

    private Long lockedCount;

    private Long finishedCount;

    private Long cancelledCount;

    private Long participantCount;

    private BigDecimal originalTotalAmount;

    private BigDecimal actualDiscountAmount;

    private BigDecimal payableTotalAmount;

    private Long paidParticipantCount;

    private Long confirmedParticipantCount;

    private List<DashboardRankItemVO> popularTypes;

    private List<DashboardRankItemVO> popularMerchants;
}
