package com.campus.pinhaofan.controller;

import com.campus.pinhaofan.common.Result;
import com.campus.pinhaofan.service.GroupOrderService;
import com.campus.pinhaofan.vo.MyGroupOrderVO;
import com.campus.pinhaofan.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/my/group-orders")
public class MyGroupOrderController {

    private final GroupOrderService groupOrderService;

    @GetMapping
    public Result<PageResultVO<MyGroupOrderVO>> listMyGroupOrders(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        return Result.success(groupOrderService.listMyGroupOrders(authorization, scope, status, pageNum, pageSize));
    }
}
