package com.campus.pinhaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_participant")
public class OrderParticipant {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("group_order_id")
    private Long groupOrderId;

    @TableField("user_id")
    private Long userId;

    @TableField("original_amount")
    private BigDecimal originalAmount;

    @TableField("discount_share_amount")
    private BigDecimal discountShareAmount;

    @TableField("payable_amount")
    private BigDecimal payableAmount;

    @TableField("rounding_adjustment_amount")
    private BigDecimal roundingAdjustmentAmount;

    @TableField("payment_status")
    private String paymentStatus;

    @TableField("paid_mark_time")
    private LocalDateTime paidMarkTime;

    @TableField("paid_confirm_time")
    private LocalDateTime paidConfirmTime;

    @TableField("join_time")
    private LocalDateTime joinTime;

    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
