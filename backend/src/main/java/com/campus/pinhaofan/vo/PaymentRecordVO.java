package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecordVO {

    private Long id;

    private Long groupOrderId;

    private Long participantId;

    private Long userId;

    private BigDecimal amount;

    private String paymentStatus;

    private String markTime;

    private Long confirmUserId;

    private String confirmTime;

    private String remark;
}
