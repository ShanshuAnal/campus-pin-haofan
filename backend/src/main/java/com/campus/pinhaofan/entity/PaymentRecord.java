package com.campus.pinhaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_record")
public class PaymentRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("group_order_id")
    private Long groupOrderId;

    @TableField("participant_id")
    private Long participantId;

    @TableField("user_id")
    private Long userId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("payment_status")
    private String paymentStatus;

    @TableField("mark_time")
    private LocalDateTime markTime;

    @TableField("confirm_user_id")
    private Long confirmUserId;

    @TableField("confirm_time")
    private LocalDateTime confirmTime;

    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
