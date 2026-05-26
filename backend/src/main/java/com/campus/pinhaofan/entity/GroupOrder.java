package com.campus.pinhaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("group_order")
public class GroupOrder {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("title")
    private String title;

    @TableField("order_type")
    private String orderType;

    @TableField("merchant_name")
    private String merchantName;

    @TableField("pickup_location")
    private String pickupLocation;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("deadline_time")
    private LocalDateTime deadlineTime;

    @TableField("max_participants")
    private Integer maxParticipants;

    @TableField("participant_count")
    private Integer participantCount;

    @TableField("discount_threshold_amount")
    private BigDecimal discountThresholdAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("original_total_amount")
    private BigDecimal originalTotalAmount;

    @TableField("actual_discount_amount")
    private BigDecimal actualDiscountAmount;

    @TableField("payable_total_amount")
    private BigDecimal payableTotalAmount;

    @TableField("rounding_adjustment_amount")
    private BigDecimal roundingAdjustmentAmount;

    @TableField("status")
    private String status;

    @TableField("pickup_user_id")
    private Long pickupUserId;

    @TableField("remark")
    private String remark;

    @TableField("locked_time")
    private LocalDateTime lockedTime;

    @TableField("finish_time")
    private LocalDateTime finishTime;

    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
