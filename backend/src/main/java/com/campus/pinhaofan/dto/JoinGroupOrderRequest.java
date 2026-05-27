package com.campus.pinhaofan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class JoinGroupOrderRequest {

    private String remark;

    @Valid
    @NotEmpty(message = "餐品不能为空")
    private List<MealItemRequest> mealItems;

    @Data
    public static class MealItemRequest {

        private String itemName;

        private Integer quantity;

        private BigDecimal unitPrice;

        private String remark;
    }
}
