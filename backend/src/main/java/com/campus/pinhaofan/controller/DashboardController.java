package com.campus.pinhaofan.controller;

import com.campus.pinhaofan.common.Result;
import com.campus.pinhaofan.service.GroupOrderService;
import com.campus.pinhaofan.vo.DashboardSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard")
public class DashboardController {

    private final GroupOrderService groupOrderService;

    @GetMapping("/summary")
    public Result<DashboardSummaryVO> getDashboardSummary(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "scope", required = false) String scope) {
        return Result.success(groupOrderService.getDashboardSummary(authorization, startTime, endTime, scope));
    }
}
