package com.campus.pinhaofan.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupOrderEventVO {

    private Long id;

    private Long groupOrderId;

    private String eventType;

    private String eventLevel;

    private Long operatorId;

    private String operatorRole;

    @JsonProperty("title")
    private String eventTitle;

    @JsonProperty("content")
    private String eventContent;

    private String beforeStatus;

    private String afterStatus;

    private String eventTime;

    private String createTime;
}
