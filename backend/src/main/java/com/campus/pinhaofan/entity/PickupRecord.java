package com.campus.pinhaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pickup_record")
public class PickupRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("group_order_id")
    private Long groupOrderId;

    @TableField("pickup_user_id")
    private Long pickupUserId;

    @TableField("pickup_location")
    private String pickupLocation;

    @TableField("pickup_status")
    private String pickupStatus;

    @TableField("estimated_arrival_time")
    private LocalDateTime estimatedArrivalTime;

    @TableField("actual_arrival_time")
    private LocalDateTime actualArrivalTime;

    @TableField("picked_up_time")
    private LocalDateTime pickedUpTime;

    @TableField("distributed_time")
    private LocalDateTime distributedTime;

    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
