package com.campus.pinhaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("meal_item")
public class MealItem {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("group_order_id")
    private Long groupOrderId;

    @TableField("participant_id")
    private Long participantId;

    @TableField("item_name")
    private String itemName;

    @TableField("quantity")
    private Integer quantity;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("subtotal_amount")
    private BigDecimal subtotalAmount;

    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
