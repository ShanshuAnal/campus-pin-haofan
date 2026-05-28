package com.campus.pinhaofan.controller;

import com.campus.pinhaofan.common.Result;
import com.campus.pinhaofan.dto.CreateGroupOrderRequest;
import com.campus.pinhaofan.dto.JoinGroupOrderRequest;
import com.campus.pinhaofan.dto.LockGroupOrderRequest;
import com.campus.pinhaofan.dto.PaymentRequest;
import com.campus.pinhaofan.dto.PickupAssigneeRequest;
import com.campus.pinhaofan.dto.PickupStatusUpdateRequest;
import com.campus.pinhaofan.service.GroupOrderService;
import com.campus.pinhaofan.vo.GroupOrderDetailVO;
import com.campus.pinhaofan.vo.GroupOrderVO;
import com.campus.pinhaofan.vo.JoinGroupOrderVO;
import com.campus.pinhaofan.vo.LockGroupOrderVO;
import com.campus.pinhaofan.vo.PageResultVO;
import com.campus.pinhaofan.vo.PaymentActionVO;
import com.campus.pinhaofan.vo.PickupAssigneeVO;
import com.campus.pinhaofan.vo.PickupStatusUpdateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group-orders")
public class GroupOrderController {

    private final GroupOrderService groupOrderService;

    @GetMapping
    public Result<PageResultVO<GroupOrderVO>> listGroupOrders(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "orderType", required = false) String orderType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        return Result.success(groupOrderService.listGroupOrders(
                authorization,
                status,
                orderType,
                keyword,
                pageNum,
                pageSize
        ));
    }

    @PostMapping
    public Result<GroupOrderVO> createGroupOrder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateGroupOrderRequest request) {
        return Result.success(groupOrderService.createGroupOrder(authorization, request));
    }

    @GetMapping("/{orderId}")
    public Result<GroupOrderDetailVO> getGroupOrderDetail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long orderId) {
        return Result.success(groupOrderService.getGroupOrderDetail(authorization, orderId));
    }

    @PostMapping("/{orderId}/participants")
    public Result<JoinGroupOrderVO> joinGroupOrder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long orderId,
            @Valid @RequestBody JoinGroupOrderRequest request) {
        return Result.success(groupOrderService.joinGroupOrder(authorization, orderId, request));
    }

    @PostMapping("/{orderId}/lock")
    public Result<LockGroupOrderVO> lockGroupOrder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long orderId,
            @RequestBody(required = false) LockGroupOrderRequest request) {
        return Result.success(groupOrderService.lockGroupOrder(authorization, orderId, request));
    }

    @PostMapping("/{orderId}/participants/{participantId}/payments/mark")
    public Result<PaymentActionVO> markParticipantPaid(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long orderId,
            @PathVariable Long participantId,
            @RequestBody(required = false) PaymentRequest request) {
        return Result.success(groupOrderService.markParticipantPaid(authorization, orderId, participantId, request));
    }

    @PostMapping("/{orderId}/participants/{participantId}/payments/confirm")
    public Result<PaymentActionVO> confirmParticipantPayment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long orderId,
            @PathVariable Long participantId,
            @RequestBody(required = false) PaymentRequest request) {
        return Result.success(groupOrderService.confirmParticipantPayment(authorization, orderId, participantId, request));
    }

    @PutMapping("/{orderId}/pickup-assignee")
    public Result<PickupAssigneeVO> assignPickupUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long orderId,
            @Valid @RequestBody PickupAssigneeRequest request) {
        return Result.success(groupOrderService.assignPickupUser(authorization, orderId, request));
    }

    @PatchMapping("/{orderId}/pickup-status")
    public Result<PickupStatusUpdateVO> updatePickupStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long orderId,
            @Valid @RequestBody PickupStatusUpdateRequest request) {
        return Result.success(groupOrderService.updatePickupStatus(authorization, orderId, request));
    }
}
