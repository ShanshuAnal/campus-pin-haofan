package com.campus.pinhaofan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelGroupOrderRequest {

    @NotBlank(message = "cancelReason 不能为空")
    @Size(max = 255, message = "cancelReason 长度不能超过 255")
    private String cancelReason;
}
