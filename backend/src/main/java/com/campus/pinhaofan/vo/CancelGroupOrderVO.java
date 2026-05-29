package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelGroupOrderVO {

    private Long id;

    private String status;

    private String cancelReason;

    private String cancelTime;
}
