package com.campus.pinhaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_order_event")
public class GroupOrderEvent {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("group_order_id")
    private Long groupOrderId;

    @TableField("event_type")
    private String eventType;

    @TableField("event_level")
    private String eventLevel;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("operator_role")
    private String operatorRole;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("before_status")
    private String beforeStatus;

    @TableField("after_status")
    private String afterStatus;

    @TableField("event_time")
    private LocalDateTime eventTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
