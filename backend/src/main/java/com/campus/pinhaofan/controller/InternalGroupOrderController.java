package com.campus.pinhaofan.controller;

import com.campus.pinhaofan.common.OperationLog;
import com.campus.pinhaofan.common.Result;
import com.campus.pinhaofan.service.GroupOrderService;
import com.campus.pinhaofan.vo.GroupOrderTimeoutCheckVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/group-orders")
public class InternalGroupOrderController {

    private final GroupOrderService groupOrderService;

    @PostMapping("/{orderId}/expire-check")
    @OperationLog("手动触发超时关闭")
    public Result<GroupOrderTimeoutCheckVO> expireCheck(@PathVariable Long orderId) {
        return Result.success(groupOrderService.expireGroupOrderIfTimeout(orderId));
    }

    @PostMapping("/expire-scan")
    @OperationLog("手动触发超时补偿扫描")
    public Result<List<GroupOrderTimeoutCheckVO>> expireScan() {
        return Result.success(groupOrderService.scanAndExpireTimeoutGroupOrders());
    }
}
