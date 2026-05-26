package com.campus.pinhaofan.enums;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "登录已失效"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "数据不存在"),
    CONFLICT(409, "数据冲突"),
    INTERNAL_ERROR(500, "系统异常");

    private final Integer code;

    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
