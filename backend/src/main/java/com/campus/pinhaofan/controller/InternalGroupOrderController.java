package com.campus.pinhaofan.controller;

import com.campus.pinhaofan.common.Result;
import com.campus.pinhaofan.service.GroupOrderService;
import com.campus.pinhaofan.vo.GroupOrderTimeoutCheckVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/group-orders")
public class InternalGroupOrderController {

    private final GroupOrderService groupOrderService;

    @PostMapping("/{orderId}/expire-check")
    public Result<GroupOrderTimeoutCheckVO> expireCheck(@PathVariable Long orderId) {
        return Result.success(groupOrderService.expireGroupOrderIfTimeout(orderId));
    }
}
