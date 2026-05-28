package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentActionVO {

    private Long participantId;

    private String paymentStatus;

    private String paidMarkTime;

    private String paidConfirmTime;

    private PaymentRecordVO paymentRecord;
}
