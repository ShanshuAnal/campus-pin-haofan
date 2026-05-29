package com.campus.pinhaofan.controller;

import com.campus.pinhaofan.common.RedisConcurrencyGuard;
import com.campus.pinhaofan.common.Result;
import com.campus.pinhaofan.dto.CancelGroupOrderRequest;
import com.campus.pinhaofan.dto.CreateGroupOrderEventRequest;
import com.campus.pinhaofan.dto.CreateGroupOrderRequest;
import com.campus.pinhaofan.dto.JoinGroupOrderRequest;
import com.campus.pinhaofan.dto.LockGroupOrderRequest;
import com.campus.pinhaofan.dto.PaymentRequest;
import com.campus.pinhaofan.dto.PickupAssigneeRequest;
import com.campus.pinhaofan.dto.PickupStatusUpdateRequest;
import com.campus.pinhaofan.service.GroupOrderService;
import com.campus.pinhaofan.vo.CancelGroupOrderVO;
import com.campus.pinhaofan.vo.GroupOrderDetailVO;
import com.campus.pinhaofan.vo.GroupOrderEventVO;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group-orders")
public class GroupOrderController {

    private final GroupOrderService groupOrderService;
    private final RedisConcurrencyGuard redisConcurrencyGuard;

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
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateGroupOrderRequest request) {
        return redisConcurrencyGuard.runIdempotent(
                "group-order:create",
                authorization,
                idempotencyKey,
                () -> Result.success(groupOrderService.createGroupOrder(authorization, request))
        );
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
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable Long orderId,
            @Valid @RequestBody JoinGroupOrderRequest request) {
        return redisConcurrencyGuard.runIdempotentWithOrderWriteLock(
                "group-order:join:" + orderId,
                authorization,
                idempotencyKey,
                orderId,
                () -> Result.success(groupOrderService.joinGroupOrder(authorization, orderId, request))
        );
    }

    @PostMapping("/{orderId}/lock")
    public Result<LockGroupOrderVO> lockGroupOrder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable Long orderId,
            @RequestBody(required = false) LockGroupOrderRequest request) {
        return redisConcurrencyGuard.runIdempotentWithOrderWriteLock(
                "group-order:lock:" + orderId,
                authorization,
                idempotencyKey,
                orderId,
                () -> Result.success(groupOrderService.lockGroupOrder(authorization, orderId, request))
        );
    }

    @PostMapping("/{orderId}/cancel")
    public Result<CancelGroupOrderVO> cancelGroupOrder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable Long orderId,
            @Valid @RequestBody CancelGroupOrderRequest request) {
        return redisConcurrencyGuard.runIdempotentWithOrderWriteLock(
                "group-order:cancel:" + orderId,
                authorization,
                idempotencyKey,
                orderId,
                () -> Result.success(groupOrderService.cancelGroupOrder(authorization, orderId, request))
        );
    }

    @PostMapping("/{orderId}/events")
    public Result<GroupOrderEventVO> createGroupOrderEvent(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable Long orderId,
            @Valid @RequestBody CreateGroupOrderEventRequest request) {
        return redisConcurrencyGuard.runIdempotent(
                "group-order:event:" + orderId,
                authorization,
                idempotencyKey,
                () -> Result.success(groupOrderService.createGroupOrderEvent(authorization, orderId, request))
        );
    }

    @GetMapping("/{orderId}/events")
    public Result<List<GroupOrderEventVO>> listGroupOrderEvents(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long orderId) {
        return Result.success(groupOrderService.listGroupOrderEvents(authorization, orderId));
    }

    @PostMapping("/{orderId}/participants/{participantId}/payments/mark")
    public Result<PaymentActionVO> markParticipantPaid(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable Long orderId,
            @PathVariable Long participantId,
            @RequestBody(required = false) PaymentRequest request) {
        return redisConcurrencyGuard.runIdempotent(
                "group-order:payment-mark:" + orderId + ":" + participantId,
                authorization,
                idempotencyKey,
                () -> Result.success(groupOrderService.markParticipantPaid(authorization, orderId, participantId, request))
        );
    }

    @PostMapping("/{orderId}/participants/{participantId}/payments/confirm")
    public Result<PaymentActionVO> confirmParticipantPayment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable Long orderId,
            @PathVariable Long participantId,
            @RequestBody(required = false) PaymentRequest request) {
        return redisConcurrencyGuard.runIdempotent(
                "group-order:payment-confirm:" + orderId + ":" + participantId,
                authorization,
                idempotencyKey,
                () -> Result.success(groupOrderService.confirmParticipantPayment(authorization, orderId, participantId, request))
        );
    }

    @PutMapping("/{orderId}/pickup-assignee")
    public Result<PickupAssigneeVO> assignPickupUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable Long orderId,
            @Valid @RequestBody PickupAssigneeRequest request) {
        return redisConcurrencyGuard.runIdempotentWithOrderWriteLock(
                "group-order:pickup-assignee:" + orderId,
                authorization,
                idempotencyKey,
                orderId,
                () -> Result.success(groupOrderService.assignPickupUser(authorization, orderId, request))
        );
    }

    @PatchMapping("/{orderId}/pickup-status")
    public Result<PickupStatusUpdateVO> updatePickupStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable Long orderId,
            @Valid @RequestBody PickupStatusUpdateRequest request) {
        return redisConcurrencyGuard.runIdempotentWithOrderWriteLock(
                "group-order:pickup-status:" + orderId,
                authorization,
                idempotencyKey,
                orderId,
                () -> Result.success(groupOrderService.updatePickupStatus(authorization, orderId, request))
        );
    }
}
