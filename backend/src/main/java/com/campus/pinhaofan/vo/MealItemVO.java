package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealItemVO {

    private Long id;

    private Long groupOrderId;

    private Long participantId;

    private String itemName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotalAmount;

    private String remark;
}
