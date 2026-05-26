package com.campus.pinhaofan.common;

import com.campus.pinhaofan.enums.ResultCode;
import com.campus.pinhaofan.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateTimeUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtil() {
    }

    public static LocalDateTime parse(String value, String message) {
        try {
            return LocalDateTime.parse(value, FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
        }
    }

    public static String format(LocalDateTime value) {
        return value == null ? null : value.format(FORMATTER);
    }
}
