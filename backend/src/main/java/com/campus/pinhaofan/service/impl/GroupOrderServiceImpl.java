package com.campus.pinhaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.pinhaofan.common.AuthTokenUtil;
import com.campus.pinhaofan.common.DateTimeUtil;
import com.campus.pinhaofan.dto.CancelGroupOrderRequest;
import com.campus.pinhaofan.dto.CreateGroupOrderEventRequest;
import com.campus.pinhaofan.dto.CreateGroupOrderRequest;
import com.campus.pinhaofan.dto.JoinGroupOrderRequest;
import com.campus.pinhaofan.dto.LockGroupOrderRequest;
import com.campus.pinhaofan.dto.PaymentRequest;
import com.campus.pinhaofan.dto.PickupAssigneeRequest;
import com.campus.pinhaofan.dto.PickupStatusUpdateRequest;
import com.campus.pinhaofan.entity.GroupOrder;
import com.campus.pinhaofan.entity.GroupOrderEvent;
import com.campus.pinhaofan.entity.MealItem;
import com.campus.pinhaofan.entity.OrderParticipant;
import com.campus.pinhaofan.entity.OrderStatusLog;
import com.campus.pinhaofan.entity.PaymentRecord;
import com.campus.pinhaofan.entity.PickupRecord;
import com.campus.pinhaofan.entity.User;
import com.campus.pinhaofan.enums.GroupOrderEventLevel;
import com.campus.pinhaofan.enums.GroupOrderEventType;
import com.campus.pinhaofan.enums.GroupOrderStatus;
import com.campus.pinhaofan.enums.PaymentStatus;
import com.campus.pinhaofan.enums.PickupStatus;
import com.campus.pinhaofan.enums.ResultCode;
import com.campus.pinhaofan.exception.BusinessException;
import com.campus.pinhaofan.mapper.GroupOrderMapper;
import com.campus.pinhaofan.mapper.GroupOrderEventMapper;
import com.campus.pinhaofan.mapper.MealItemMapper;
import com.campus.pinhaofan.mapper.OrderParticipantMapper;
import com.campus.pinhaofan.mapper.OrderStatusLogMapper;
import com.campus.pinhaofan.mapper.PaymentRecordMapper;
import com.campus.pinhaofan.mapper.PickupRecordMapper;
import com.campus.pinhaofan.mapper.UserMapper;
import com.campus.pinhaofan.messaging.GroupOrderTimeoutMessagePublisher;
import com.campus.pinhaofan.service.GroupOrderService;
import com.campus.pinhaofan.vo.CancelGroupOrderVO;
import com.campus.pinhaofan.vo.DashboardRankItemVO;
import com.campus.pinhaofan.vo.DashboardSummaryVO;
import com.campus.pinhaofan.vo.GroupOrderDetailVO;
import com.campus.pinhaofan.vo.GroupOrderEventVO;
import com.campus.pinhaofan.vo.GroupOrderPermissionsVO;
import com.campus.pinhaofan.vo.GroupOrderTimeoutCheckVO;
import com.campus.pinhaofan.vo.GroupOrderVO;
import com.campus.pinhaofan.vo.JoinGroupOrderVO;
import com.campus.pinhaofan.vo.LockAllocationVO;
import com.campus.pinhaofan.vo.LockGroupOrderVO;
import com.campus.pinhaofan.vo.LockedGroupOrderVO;
import com.campus.pinhaofan.vo.MealItemVO;
import com.campus.pinhaofan.vo.MyGroupOrderVO;
import com.campus.pinhaofan.vo.OrderAmountVO;
import com.campus.pinhaofan.vo.PageResultVO;
import com.campus.pinhaofan.vo.ParticipantVO;
import com.campus.pinhaofan.vo.PaymentActionVO;
import com.campus.pinhaofan.vo.PaymentRecordVO;
import com.campus.pinhaofan.vo.PickupAssigneeVO;
import com.campus.pinhaofan.vo.PickupRecordVO;
import com.campus.pinhaofan.vo.PickupStatusUpdateVO;
import com.campus.pinhaofan.vo.UserSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupOrderServiceImpl implements GroupOrderService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String DISABLED_STATUS = "DISABLED";
    private static final String TARGET_TYPE_GROUP_ORDER = "GROUP_ORDER";
    private static final String TARGET_TYPE_PICKUP_RECORD = "PICKUP_RECORD";
    private static final String ACTION_TYPE_CREATE_ORDER = "CREATE_ORDER";
    private static final String ACTION_TYPE_LOCK_ORDER = "LOCK_ORDER";
    private static final String ACTION_TYPE_CANCEL_ORDER = "CANCEL_ORDER";
    private static final String ACTION_TYPE_EXPIRE_ORDER = "EXPIRE_ORDER";
    private static final String ACTION_TYPE_ASSIGN_PICKUP = "ASSIGN_PICKUP";
    private static final String ACTION_TYPE_UPDATE_PICKUP_STATUS = "UPDATE_PICKUP_STATUS";
    private static final String ACTION_TYPE_UPDATE_ORDER_STATUS = "UPDATE_ORDER_STATUS";
    private static final String MY_SCOPE_CREATED_BY_ME = "CREATED_BY_ME";
    private static final String MY_SCOPE_JOINED_BY_ME = "JOINED_BY_ME";
    private static final String MY_SCOPE_PICKUP_BY_ME = "PICKUP_BY_ME";
    private static final String MY_SCOPE_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String MY_SCOPE_HISTORY = "HISTORY";
    private static final String DASHBOARD_SCOPE_ALL = "ALL";
    private static final String DASHBOARD_SCOPE_MINE = "MINE";
    private static final String EVENT_TYPE_CANCELLED = "CANCELLED";
    private static final String EVENT_TYPE_EXPIRED = "EXPIRED";
    private static final String EVENT_LEVEL_INFO = "INFO";
    private static final String OPERATOR_ROLE_CREATOR = "CREATOR";
    private static final String OPERATOR_ROLE_PICKUP_USER = "PICKUP_USER";
    private static final String OPERATOR_ROLE_PARTICIPANT = "PARTICIPANT";
    private static final String OPERATOR_ROLE_SYSTEM = "SYSTEM";
    private static final Long SYSTEM_OPERATOR_ID = 0L;
    private static final Set<String> ORDER_TYPES = Set.of("TAKEOUT", "CANTEEN", "MILK_TEA", "MIDNIGHT_SNACK");
    private static final Set<String> MY_GROUP_ORDER_SCOPES = Set.of(
            MY_SCOPE_CREATED_BY_ME,
            MY_SCOPE_JOINED_BY_ME,
            MY_SCOPE_PICKUP_BY_ME,
            MY_SCOPE_PENDING_PAYMENT,
            MY_SCOPE_HISTORY
    );
    private static final Set<String> HISTORY_STATUSES = Set.of(
            GroupOrderStatus.FINISHED.getValue(),
            GroupOrderStatus.CANCELLED.getValue(),
            GroupOrderStatus.EXPIRED.getValue()
    );
    private static final Set<String> TERMINAL_STATUSES = Set.of(
            GroupOrderStatus.FINISHED.getValue(),
            GroupOrderStatus.CANCELLED.getValue(),
            GroupOrderStatus.EXPIRED.getValue()
    );
    private static final String DEFAULT_GROUP_ORDER_SORT =
            "ORDER BY CASE WHEN status IN ('CREATED','LOCKED','ORDERED','DELIVERING','ARRIVED','PICKED_UP') "
                    + "THEN 0 ELSE 1 END ASC, create_time DESC";
    private static final Set<String> PAYMENT_ALLOWED_ORDER_STATUSES = Set.of(
            GroupOrderStatus.LOCKED.getValue(),
            GroupOrderStatus.ORDERED.getValue(),
            GroupOrderStatus.DELIVERING.getValue(),
            GroupOrderStatus.ARRIVED.getValue()
    );

    private final AuthTokenUtil authTokenUtil;
    private final UserMapper userMapper;
    private final GroupOrderMapper groupOrderMapper;
    private final GroupOrderEventMapper groupOrderEventMapper;
    private final OrderParticipantMapper orderParticipantMapper;
    private final MealItemMapper mealItemMapper;
    private final PickupRecordMapper pickupRecordMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final GroupOrderTimeoutMessagePublisher groupOrderTimeoutMessagePublisher;

    @Override
    public PageResultVO<GroupOrderVO> listGroupOrders(
            String authorization,
            String status,
            String orderType,
            String keyword,
            Long pageNum,
            Long pageSize) {
        User currentUser = requireCurrentUser(authorization);
        String normalizedStatus = normalize(status);
        String normalizedOrderType = normalize(orderType);
        String normalizedKeyword = normalize(keyword);
        long current = normalizePageNum(pageNum);
        long size = normalizePageSize(pageSize);

        if (!normalizedStatus.isEmpty() && !isGroupOrderStatus(normalizedStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "status 不合法");
        }
        if (!normalizedOrderType.isEmpty() && !ORDER_TYPES.contains(normalizedOrderType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "orderType 不合法");
        }

        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<GroupOrder>()
                .eq(!normalizedStatus.isEmpty(), GroupOrder::getStatus, normalizedStatus)
                .eq(!normalizedOrderType.isEmpty(), GroupOrder::getOrderType, normalizedOrderType)
                .and(!normalizedKeyword.isEmpty(), query -> query
                        .like(GroupOrder::getTitle, normalizedKeyword)
                        .or()
                        .like(GroupOrder::getMerchantName, normalizedKeyword));
        applyHallVisibility(wrapper, currentUser.getId(), LocalDateTime.now());
        if (normalizedStatus.isEmpty()) {
            wrapper.last(DEFAULT_GROUP_ORDER_SORT);
        } else {
            wrapper.orderByDesc(GroupOrder::getCreateTime);
        }

        IPage<GroupOrder> page = groupOrderMapper.selectPage(Page.of(current, size), wrapper);
        Map<Long, User> users = loadUsers(page.getRecords());
        List<GroupOrderVO> records = page.getRecords().stream()
                .map(order -> toVO(order, users))
                .toList();

        return new PageResultVO<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    @Transactional
    public GroupOrderVO createGroupOrder(String authorization, CreateGroupOrderRequest request) {
        User creator = requireCurrentUser(authorization);
        validateCreateRequest(request);

        LocalDateTime deadlineTime = DateTimeUtil.parse(request.getDeadlineTime(), "deadlineTime 格式错误");
        if (!deadlineTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "deadlineTime 必须晚于当前时间");
        }

        BigDecimal discountAmount = request.getDiscountAmount() == null ? BigDecimal.ZERO : request.getDiscountAmount();
        GroupOrder order = new GroupOrder();
        order.setTitle(normalize(request.getTitle()));
        order.setOrderType(normalize(request.getOrderType()));
        order.setMerchantName(normalize(request.getMerchantName()));
        order.setPickupLocation(normalize(request.getPickupLocation()));
        order.setCreatorId(creator.getId());
        order.setDeadlineTime(deadlineTime);
        order.setMaxParticipants(request.getMaxParticipants());
        order.setParticipantCount(0);
        order.setDiscountThresholdAmount(request.getDiscountThresholdAmount());
        order.setDiscountAmount(discountAmount);
        order.setOriginalTotalAmount(BigDecimal.ZERO);
        order.setActualDiscountAmount(BigDecimal.ZERO);
        order.setPayableTotalAmount(BigDecimal.ZERO);
        order.setRoundingAdjustmentAmount(BigDecimal.ZERO);
        order.setStatus(GroupOrderStatus.CREATED.getValue());
        order.setRemark(request.getRemark());

        groupOrderMapper.insert(order);
        createCreatorParticipantIfNeeded(order, creator.getId(), request.getCreatorItems());
        insertCreateStatusLog(order, creator.getId());
        groupOrderTimeoutMessagePublisher.sendTimeoutMessage(order.getId(), deadlineTime);

        return toVO(order, Map.of(creator.getId(), creator));
    }

    @Override
    public GroupOrderDetailVO getGroupOrderDetail(String authorization, Long orderId) {
        User currentUser = requireCurrentUser(authorization);
        GroupOrder order = getExistingOrder(orderId);
        List<OrderParticipant> participants = orderParticipantMapper.selectList(
                new LambdaQueryWrapper<OrderParticipant>()
                        .eq(OrderParticipant::getGroupOrderId, orderId)
                        .orderByAsc(OrderParticipant::getJoinTime)
        );
        boolean relatedUser = isRelatedUser(order, participants, currentUser.getId());
        if (!GroupOrderStatus.CREATED.getValue().equals(order.getStatus()) && !relatedUser) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "No permission to view group order detail");
        }
        List<MealItem> mealItems = mealItemMapper.selectList(
                new LambdaQueryWrapper<MealItem>()
                        .eq(MealItem::getGroupOrderId, orderId)
                        .orderByAsc(MealItem::getCreateTime)
        );
        PickupRecord pickupRecord = pickupRecordMapper.selectOne(
                new LambdaQueryWrapper<PickupRecord>()
                        .eq(PickupRecord::getGroupOrderId, orderId)
                        .last("LIMIT 1")
        );

        Map<Long, List<MealItem>> mealItemsByParticipantId = mealItems.stream()
                .collect(Collectors.groupingBy(MealItem::getParticipantId));
        Map<Long, User> users = loadUsersForDetail(order, participants, pickupRecord);
        List<ParticipantVO> participantViews = participants.stream()
                .map(participant -> toParticipantVO(
                        participant,
                        users,
                        mealItemsByParticipantId.getOrDefault(participant.getId(), Collections.emptyList())
                ))
                .toList();
        PickupRecordVO pickupRecordView = toPickupRecordVO(pickupRecord, users);

        return new GroupOrderDetailVO(
                toVO(order, users),
                participantViews,
                pickupRecordView,
                toOrderAmountVO(order),
                pickupRecordView == null ? null : pickupRecordView.getPickupStatus(),
                buildPermissions(order, participants, pickupRecord, currentUser.getId())
        );
    }

    @Override
    @Transactional
    public JoinGroupOrderVO joinGroupOrder(String authorization, Long orderId, JoinGroupOrderRequest request) {
        User currentUser = requireCurrentUser(authorization);
        GroupOrder order = getExistingOrder(orderId);
        validateJoinOrder(order, currentUser.getId(), request);

        BigDecimal originalAmount = calculateOriginalAmount(request);
        LocalDateTime now = LocalDateTime.now();

        OrderParticipant participant = new OrderParticipant();
        participant.setGroupOrderId(orderId);
        participant.setUserId(currentUser.getId());
        participant.setOriginalAmount(originalAmount);
        participant.setDiscountShareAmount(BigDecimal.ZERO);
        participant.setPayableAmount(originalAmount);
        participant.setRoundingAdjustmentAmount(BigDecimal.ZERO);
        participant.setPaymentStatus(PaymentStatus.UNPAID.getValue());
        participant.setJoinTime(now);
        participant.setRemark(request.getRemark());

        try {
            orderParticipantMapper.insert(participant);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "不能重复加入同一拼单");
        }

        List<MealItem> mealItems = request.getMealItems().stream()
                .map(itemRequest -> toMealItem(orderId, participant.getId(), itemRequest))
                .toList();
        for (MealItem mealItem : mealItems) {
            mealItemMapper.insert(mealItem);
        }

        GroupOrder updatedOrder = updateOrderAmountAfterJoin(order, originalAmount);
        Map<Long, User> users = Map.of(currentUser.getId(), currentUser);
        return new JoinGroupOrderVO(
                toParticipantVO(participant, users, mealItems),
                toOrderAmountVO(updatedOrder)
        );
    }

    @Override
    @Transactional
    public LockGroupOrderVO lockGroupOrder(String authorization, Long orderId, LockGroupOrderRequest request) {
        User currentUser = requireCurrentUser(authorization);
        GroupOrder order = getExistingOrder(orderId);
        validateLockOrder(order, currentUser.getId());

        List<OrderParticipant> participants = orderParticipantMapper.selectList(
                new LambdaQueryWrapper<OrderParticipant>()
                        .eq(OrderParticipant::getGroupOrderId, orderId)
                        .orderByAsc(OrderParticipant::getJoinTime)
        );
        if (participants.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "拼单没有参与者，不能锁单");
        }

        OrderParticipant creatorParticipant = participants.stream()
                .filter(participant -> Objects.equals(participant.getUserId(), order.getCreatorId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ResultCode.BAD_REQUEST.getCode(),
                        "发起人需先加入拼单以承接尾差"
                ));

        List<MealItem> mealItems = mealItemMapper.selectList(
                new LambdaQueryWrapper<MealItem>()
                        .eq(MealItem::getGroupOrderId, orderId)
        );
        Map<Long, BigDecimal> originalAmountByParticipantId = mealItems.stream()
                .collect(Collectors.groupingBy(
                        MealItem::getParticipantId,
                        Collectors.mapping(
                                item -> amount(item.getSubtotalAmount()),
                                Collectors.reducing(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                        )
                ));

        BigDecimal originalTotalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (OrderParticipant participant : participants) {
            BigDecimal originalAmount = amount(originalAmountByParticipantId.get(participant.getId()));
            if (originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "餐品金额必须大于 0");
            }
            participant.setOriginalAmount(originalAmount);
            originalTotalAmount = originalTotalAmount.add(originalAmount).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal actualDiscountAmount = calculateActualDiscountAmount(order, originalTotalAmount);
        BigDecimal payableTotalAmount = originalTotalAmount.subtract(actualDiscountAmount)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal allocatedPayableTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (OrderParticipant participant : participants) {
            BigDecimal discountShareAmount = calculateDiscountShareAmount(
                    participant.getOriginalAmount(),
                    originalTotalAmount,
                    actualDiscountAmount
            );
            BigDecimal payableAmount = participant.getOriginalAmount()
                    .subtract(discountShareAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            participant.setDiscountShareAmount(discountShareAmount);
            participant.setPayableAmount(payableAmount);
            participant.setRoundingAdjustmentAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            allocatedPayableTotal = allocatedPayableTotal.add(payableAmount).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal roundingAdjustmentAmount = payableTotalAmount.subtract(allocatedPayableTotal)
                .setScale(2, RoundingMode.HALF_UP);
        if (roundingAdjustmentAmount.compareTo(BigDecimal.ZERO) != 0) {
            creatorParticipant.setRoundingAdjustmentAmount(roundingAdjustmentAmount);
            creatorParticipant.setPayableAmount(creatorParticipant.getPayableAmount()
                    .add(roundingAdjustmentAmount)
                    .setScale(2, RoundingMode.HALF_UP));
        }

        for (OrderParticipant participant : participants) {
            orderParticipantMapper.updateById(participant);
        }

        LocalDateTime lockedTime = LocalDateTime.now();
        order.setParticipantCount(participants.size());
        order.setOriginalTotalAmount(originalTotalAmount);
        order.setActualDiscountAmount(actualDiscountAmount);
        order.setPayableTotalAmount(payableTotalAmount);
        order.setRoundingAdjustmentAmount(roundingAdjustmentAmount);
        order.setStatus(GroupOrderStatus.LOCKED.getValue());
        order.setLockedTime(lockedTime);
        groupOrderMapper.updateById(order);

        insertLockStatusLog(order, currentUser.getId(), request == null ? null : request.getRemark());

        return new LockGroupOrderVO(
                toLockedGroupOrderVO(order),
                participants.stream().map(this::toLockAllocationVO).toList()
        );
    }

    @Override
    @Transactional
    public CancelGroupOrderVO cancelGroupOrder(
            String authorization,
            Long orderId,
            CancelGroupOrderRequest request) {
        User currentUser = requireCurrentUser(authorization);
        GroupOrder order = getExistingOrder(orderId);
        String cancelReason = normalize(request == null ? null : request.getCancelReason());
        if (cancelReason.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "cancelReason 不能为空");
        }
        if (cancelReason.length() > 255) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "cancelReason 长度不能超过 255");
        }

        validateCancelOrder(order, currentUser.getId());

        String beforeStatus = order.getStatus();
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(GroupOrderStatus.CANCELLED.getValue());
        order.setCancelUserId(currentUser.getId());
        order.setCancelReason(cancelReason);
        order.setCancelTime(now);
        order.setLastEventTime(now);
        groupOrderMapper.updateById(order);

        insertStatusLog(
                orderId,
                currentUser.getId(),
                TARGET_TYPE_GROUP_ORDER,
                orderId,
                ACTION_TYPE_CANCEL_ORDER,
                beforeStatus,
                GroupOrderStatus.CANCELLED.getValue(),
                cancelReason
        );
        insertGroupOrderEvent(
                order,
                EVENT_TYPE_CANCELLED,
                currentUser.getId(),
                OPERATOR_ROLE_CREATOR,
                "取消拼单",
                cancelReason,
                beforeStatus,
                GroupOrderStatus.CANCELLED.getValue(),
                now
        );

        return new CancelGroupOrderVO(
                order.getId(),
                order.getStatus(),
                order.getCancelReason(),
                DateTimeUtil.format(order.getCancelTime())
        );
    }

    @Override
    @Transactional
    public GroupOrderEventVO createGroupOrderEvent(
            String authorization,
            Long orderId,
            CreateGroupOrderEventRequest request) {
        User currentUser = requireCurrentUser(authorization);
        GroupOrder order = getExistingOrder(orderId);
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "request body cannot be empty");
        }
        if (!Objects.equals(order.getCreatorId(), currentUser.getId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Only creator can create order event");
        }

        String eventType = normalize(request == null ? null : request.getEventType());
        GroupOrderEventType type = GroupOrderEventType.fromValue(eventType)
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST.getCode(), "eventType is invalid"));
        if (!type.isManuallyCreatable()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "eventType cannot be created manually");
        }

        String eventLevel = normalize(request.getEventLevel());
        if (eventLevel.isEmpty()) {
            eventLevel = GroupOrderEventLevel.INFO.getValue();
        }
        String finalEventLevel = eventLevel;
        GroupOrderEventLevel.fromValue(finalEventLevel)
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST.getCode(), "eventLevel is invalid"));

        String eventTitle = normalize(request.getEventTitle());
        if (eventTitle.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "eventTitle cannot be blank");
        }
        if (eventTitle.length() > 100) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "eventTitle length cannot exceed 100");
        }
        String eventContent = normalize(request.getEventContent());
        if (eventContent.length() > 500) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "eventContent length cannot exceed 500");
        }

        LocalDateTime now = LocalDateTime.now();
        GroupOrderEvent event = new GroupOrderEvent();
        event.setGroupOrderId(order.getId());
        event.setEventType(type.getValue());
        event.setEventLevel(finalEventLevel);
        event.setOperatorId(currentUser.getId());
        event.setOperatorRole(OPERATOR_ROLE_CREATOR);
        event.setTitle(eventTitle);
        event.setContent(eventContent.isEmpty() ? null : eventContent);
        event.setBeforeStatus(order.getStatus());
        event.setAfterStatus(order.getStatus());
        event.setEventTime(now);
        event.setCreateTime(now);
        event.setUpdateTime(now);
        groupOrderEventMapper.insert(event);

        GroupOrder updateOrder = new GroupOrder();
        updateOrder.setId(order.getId());
        updateOrder.setLastEventTime(now);
        groupOrderMapper.updateById(updateOrder);

        return toGroupOrderEventVO(event);
    }

    @Override
    public List<GroupOrderEventVO> listGroupOrderEvents(String authorization, Long orderId) {
        User currentUser = requireCurrentUser(authorization);
        GroupOrder order = getExistingOrder(orderId);
        if (!canViewGroupOrderEvents(order, currentUser.getId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "No permission to view order events");
        }

        return groupOrderEventMapper.selectList(
                        new LambdaQueryWrapper<GroupOrderEvent>()
                                .eq(GroupOrderEvent::getGroupOrderId, orderId)
                                .orderByDesc(GroupOrderEvent::getEventTime)
                                .orderByDesc(GroupOrderEvent::getId)
                ).stream()
                .map(this::toGroupOrderEventVO)
                .toList();
    }

    @Override
    @Transactional
    public PaymentActionVO markParticipantPaid(
            String authorization,
            Long orderId,
            Long participantId,
            PaymentRequest request) {
        User currentUser = requireCurrentUser(authorization);
        GroupOrder order = getExistingOrder(orderId);
        validatePaymentOrderStatus(order);
        OrderParticipant participant = getExistingParticipant(orderId, participantId);

        if (!Objects.equals(participant.getUserId(), currentUser.getId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能标记自己的付款");
        }
        if (PaymentStatus.CONFIRMED.getValue().equals(participant.getPaymentStatus())) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "付款已确认，不能重复标记");
        }
        if (PaymentStatus.PAID.getValue().equals(participant.getPaymentStatus())) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "已标记付款，不能重复标记");
        }
        if (!PaymentStatus.UNPAID.getValue().equals(participant.getPaymentStatus())) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "当前付款状态不能标记付款");
        }

        LocalDateTime now = LocalDateTime.now();
        participant.setPaymentStatus(PaymentStatus.PAID.getValue());
        participant.setPaidMarkTime(now);
        orderParticipantMapper.updateById(participant);

        PaymentRecord paymentRecord = createPaymentRecord(
                orderId,
                participant,
                PaymentStatus.PAID.getValue(),
                now,
                null,
                null,
                request == null ? null : request.getRemark()
        );
        paymentRecordMapper.insert(paymentRecord);

        return toPaymentActionVO(participant, paymentRecord);
    }

    @Override
    @Transactional
    public PaymentActionVO confirmParticipantPayment(
            String authorization,
            Long orderId,
            Long participantId,
            PaymentRequest request) {
        User currentUser = requireCurrentUser(authorization);
        GroupOrder order = getExistingOrder(orderId);
        validatePaymentOrderStatus(order);
        OrderParticipant participant = getExistingParticipant(orderId, participantId);

        if (!Objects.equals(order.getCreatorId(), currentUser.getId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只有发起人可以确认付款");
        }
        if (PaymentStatus.CONFIRMED.getValue().equals(participant.getPaymentStatus())) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "付款已确认");
        }
        if (!PaymentStatus.PAID.getValue().equals(participant.getPaymentStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "成员尚未标记付款");
        }

        LocalDateTime now = LocalDateTime.now();
        participant.setPaymentStatus(PaymentStatus.CONFIRMED.getValue());
        participant.setPaidConfirmTime(now);
        orderParticipantMapper.updateById(participant);

        PaymentRecord paymentRecord = createPaymentRecord(
                orderId,
                participant,
                PaymentStatus.CONFIRMED.getValue(),
                participant.getPaidMarkTime(),
                currentUser.getId(),
                now,
                request == null ? null : request.getRemark()
        );
        paymentRecordMapper.insert(paymentRecord);

        return toPaymentActionVO(participant, paymentRecord);
    }

    @Override
    @Transactional
    public PickupAssigneeVO assignPickupUser(String authorization, Long orderId, PickupAssigneeRequest request) {
        User currentUser = requireCurrentUser(authorization);
        GroupOrder order = getExistingOrder(orderId);
        validatePickupMutable(order);
        if (!Objects.equals(order.getCreatorId(), currentUser.getId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只有发起人可以指定取餐人");
        }
        if (!GroupOrderStatus.LOCKED.getValue().equals(order.getStatus())
                && !GroupOrderStatus.ORDERED.getValue().equals(order.getStatus())
                && !GroupOrderStatus.DELIVERING.getValue().equals(order.getStatus())
                && !GroupOrderStatus.ARRIVED.getValue().equals(order.getStatus())
                && !GroupOrderStatus.PICKED_UP.getValue().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "拼单未锁定，不能指定取餐人");
        }

        Long pickupUserId = request.getPickupUserId();
        User pickupUser = userMapper.selectById(pickupUserId);
        if (pickupUser == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (!isCreatorOrParticipant(order, pickupUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "取餐人必须是拼单参与者或发起人");
        }

        PickupRecord pickupRecord = getPickupRecord(orderId);
        String beforePickupStatus = pickupRecord == null ? null : pickupRecord.getPickupStatus();
        String pickupStatus = beforePickupStatus == null ? PickupStatus.WAITING_ORDER.getValue() : beforePickupStatus;
        String pickupLocation = normalize(request.getPickupLocation()).isEmpty()
                ? order.getPickupLocation()
                : normalize(request.getPickupLocation());

        if (pickupRecord == null) {
            pickupRecord = new PickupRecord();
            pickupRecord.setGroupOrderId(orderId);
            pickupRecord.setPickupStatus(pickupStatus);
        }
        pickupRecord.setPickupUserId(pickupUserId);
        pickupRecord.setPickupLocation(pickupLocation);
        pickupRecord.setEstimatedArrivalTime(parseOptionalDateTime(request.getEstimatedArrivalTime(), "estimatedArrivalTime 格式错误"));
        pickupRecord.setRemark(request.getRemark());

        if (pickupRecord.getId() == null) {
            pickupRecordMapper.insert(pickupRecord);
        } else {
            pickupRecordMapper.updateById(pickupRecord);
        }

        String beforeOrderStatus = order.getStatus();
        order.setPickupUserId(pickupUserId);
        if (GroupOrderStatus.LOCKED.getValue().equals(order.getStatus())) {
            order.setStatus(GroupOrderStatus.ORDERED.getValue());
        }
        groupOrderMapper.updateById(order);

        insertStatusLog(
                orderId,
                currentUser.getId(),
                TARGET_TYPE_PICKUP_RECORD,
                pickupRecord.getId(),
                ACTION_TYPE_ASSIGN_PICKUP,
                beforePickupStatus,
                pickupStatus,
                normalize(request.getRemark()).isEmpty() ? "指定取餐人" : request.getRemark()
        );
        if (!Objects.equals(beforeOrderStatus, order.getStatus())) {
            insertStatusLog(
                    orderId,
                    currentUser.getId(),
                    TARGET_TYPE_GROUP_ORDER,
                    orderId,
                    ACTION_TYPE_UPDATE_ORDER_STATUS,
                    beforeOrderStatus,
                    order.getStatus(),
                    "指定取餐人后同步推进拼单状态"
            );
        }

        Map<Long, User> users = Map.of(pickupUser.getId(), pickupUser);
        return new PickupAssigneeVO(orderId, pickupUserId, toPickupRecordVO(pickupRecord, users));
    }

    @Override
    @Transactional
    public PickupStatusUpdateVO updatePickupStatus(String authorization, Long orderId, PickupStatusUpdateRequest request) {
        User currentUser = requireCurrentUser(authorization);
        GroupOrder order = getExistingOrder(orderId);
        validatePickupMutable(order);

        PickupRecord pickupRecord = getPickupRecord(orderId);
        if (pickupRecord == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未指定取餐人");
        }
        if (!Objects.equals(order.getCreatorId(), currentUser.getId())
                && !Objects.equals(pickupRecord.getPickupUserId(), currentUser.getId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权更新取餐状态");
        }

        String beforePickupStatus = pickupRecord.getPickupStatus();
        String targetPickupStatus = normalize(request.getPickupStatus());
        if (!isPickupStatus(targetPickupStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "pickupStatus 不合法");
        }
        validatePickupStatusTransition(beforePickupStatus, targetPickupStatus);

        String beforeOrderStatus = order.getStatus();
        String targetOrderStatus = resolveOrderStatusForPickup(targetPickupStatus);
        validateOrderStatusForPickup(beforeOrderStatus, targetOrderStatus);

        LocalDateTime now = LocalDateTime.now();
        pickupRecord.setPickupStatus(targetPickupStatus);
        if (!normalize(request.getPickupLocation()).isEmpty()) {
            pickupRecord.setPickupLocation(normalize(request.getPickupLocation()));
        }
        if (PickupStatus.ARRIVED.getValue().equals(targetPickupStatus)) {
            pickupRecord.setActualArrivalTime(parseOptionalDateTimeOrNow(
                    request.getActualArrivalTime(),
                    "actualArrivalTime 格式错误",
                    now
            ));
        } else if (PickupStatus.PICKED_UP.getValue().equals(targetPickupStatus)) {
            pickupRecord.setPickedUpTime(parseOptionalDateTimeOrNow(
                    request.getPickedUpTime(),
                    "pickedUpTime 格式错误",
                    now
            ));
        } else if (PickupStatus.DISTRIBUTED.getValue().equals(targetPickupStatus)) {
            pickupRecord.setDistributedTime(parseOptionalDateTimeOrNow(
                    request.getDistributedTime(),
                    "distributedTime 格式错误",
                    now
            ));
        }
        if (request.getRemark() != null) {
            pickupRecord.setRemark(request.getRemark());
        }
        pickupRecordMapper.updateById(pickupRecord);

        order.setStatus(targetOrderStatus);
        if (GroupOrderStatus.FINISHED.getValue().equals(targetOrderStatus)) {
            order.setFinishTime(now);
        }
        groupOrderMapper.updateById(order);

        insertStatusLog(
                orderId,
                currentUser.getId(),
                TARGET_TYPE_PICKUP_RECORD,
                pickupRecord.getId(),
                ACTION_TYPE_UPDATE_PICKUP_STATUS,
                beforePickupStatus,
                targetPickupStatus,
                normalize(request.getRemark()).isEmpty() ? "更新取餐状态" : request.getRemark()
        );
        if (!Objects.equals(beforeOrderStatus, targetOrderStatus)) {
            insertStatusLog(
                    orderId,
                    currentUser.getId(),
                    TARGET_TYPE_GROUP_ORDER,
                    orderId,
                    ACTION_TYPE_UPDATE_ORDER_STATUS,
                    beforeOrderStatus,
                    targetOrderStatus,
                    "取餐状态同步推进拼单状态"
            );
        }

        Map<Long, User> users = loadUsersForPickup(order, pickupRecord);
        return new PickupStatusUpdateVO(toPickupRecordVO(pickupRecord, users), order.getStatus());
    }

    @Override
    public PageResultVO<MyGroupOrderVO> listMyGroupOrders(
            String authorization,
            String scope,
            String status,
            Long pageNum,
            Long pageSize) {
        User currentUser = requireCurrentUser(authorization);
        String normalizedScope = normalize(scope);
        String normalizedStatus = normalize(status);
        long current = normalizePageNum(pageNum);
        long size = normalizePageSize(pageSize);

        if (!normalizedScope.isEmpty() && !MY_GROUP_ORDER_SCOPES.contains(normalizedScope)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "scope 不合法");
        }
        if (!normalizedStatus.isEmpty() && !isGroupOrderStatus(normalizedStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "status 不合法");
        }

        List<OrderParticipant> myParticipants = loadParticipantsByUser(currentUser.getId());
        List<OrderParticipant> pendingPaymentParticipants = myParticipants.stream()
                .filter(participant -> PaymentStatus.UNPAID.getValue().equals(participant.getPaymentStatus()))
                .toList();
        Set<Long> joinedOrderIds = myParticipants.stream()
                .map(OrderParticipant::getGroupOrderId)
                .collect(Collectors.toSet());
        Set<Long> pendingPaymentOrderIds = pendingPaymentParticipants.stream()
                .map(OrderParticipant::getGroupOrderId)
                .collect(Collectors.toSet());

        if (shouldReturnEmptyMyOrders(normalizedScope, joinedOrderIds, pendingPaymentOrderIds)) {
            return new PageResultVO<>(0L, current, size, Collections.emptyList());
        }

        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<>();
        applyMyGroupOrderScope(wrapper, normalizedScope, currentUser.getId(), joinedOrderIds, pendingPaymentOrderIds);
        wrapper.eq(!normalizedStatus.isEmpty(), GroupOrder::getStatus, normalizedStatus)
                .orderByDesc(GroupOrder::getCreateTime);

        IPage<GroupOrder> page = groupOrderMapper.selectPage(Page.of(current, size), wrapper);
        List<GroupOrder> orders = page.getRecords();
        Map<Long, User> users = loadUsers(orders);
        Map<Long, OrderParticipant> participantByOrderId = myParticipants.stream()
                .collect(Collectors.toMap(OrderParticipant::getGroupOrderId, Function.identity(), (left, right) -> left));
        Map<Long, PickupRecord> pickupRecordByOrderId = loadPickupRecordsByOrderIds(
                orders.stream().map(GroupOrder::getId).collect(Collectors.toSet())
        );

        List<MyGroupOrderVO> records = orders.stream()
                .map(order -> toMyGroupOrderVO(
                        order,
                        users,
                        participantByOrderId.get(order.getId()),
                        pickupRecordByOrderId.get(order.getId()),
                        currentUser.getId()
                ))
                .toList();
        return new PageResultVO<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public DashboardSummaryVO getDashboardSummary(String authorization, String startTime, String endTime, String scope) {
        User currentUser = requireCurrentUser(authorization);
        String normalizedScope = normalize(scope);
        if (!normalizedScope.isEmpty()
                && !DASHBOARD_SCOPE_ALL.equals(normalizedScope)
                && !DASHBOARD_SCOPE_MINE.equals(normalizedScope)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "scope 不合法");
        }

        LocalDateTime start = parseOptionalDateTime(startTime, "startTime 格式错误");
        LocalDateTime end = parseOptionalDateTime(endTime, "endTime 格式错误");
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "startTime 不能晚于 endTime");
        }

        List<OrderParticipant> myParticipants = loadParticipantsByUser(currentUser.getId());
        Set<Long> joinedOrderIds = myParticipants.stream()
                .map(OrderParticipant::getGroupOrderId)
                .collect(Collectors.toSet());

        LambdaQueryWrapper<GroupOrder> wrapper = new LambdaQueryWrapper<>();
        applyCurrentUserOrderScope(wrapper, currentUser.getId(), joinedOrderIds);
        wrapper.ge(start != null, GroupOrder::getCreateTime, start)
                .le(end != null, GroupOrder::getCreateTime, end)
                .orderByDesc(GroupOrder::getCreateTime);

        List<GroupOrder> orders = groupOrderMapper.selectList(wrapper);
        Set<Long> orderIds = orders.stream().map(GroupOrder::getId).collect(Collectors.toSet());
        List<OrderParticipant> participants = orderIds.isEmpty()
                ? Collections.emptyList()
                : orderParticipantMapper.selectList(
                new LambdaQueryWrapper<OrderParticipant>()
                        .in(OrderParticipant::getGroupOrderId, orderIds)
        );
        return toDashboardSummaryVO(orders, participants);
    }

    @Override
    @Transactional
    public GroupOrderTimeoutCheckVO expireGroupOrderIfTimeout(Long orderId) {
        GroupOrder order = groupOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "拼单不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!GroupOrderStatus.CREATED.getValue().equals(order.getStatus())) {
            log.info(
                    "Ignore timeout message because order status changed. orderId={}, status={}, deadlineTime={}",
                    orderId,
                    order.getStatus(),
                    order.getDeadlineTime()
            );
            return new GroupOrderTimeoutCheckVO(
                    orderId,
                    false,
                    order.getStatus(),
                    "当前状态无需超时关闭",
                    DateTimeUtil.format(order.getExpiredTime()),
                    DateTimeUtil.format(order.getDeadlineTime()),
                    false
            );
        }
        if (order.getDeadlineTime() == null) {
            log.warn("Timeout message ignored because deadline is missing. orderId={}", orderId);
            return new GroupOrderTimeoutCheckVO(
                    orderId,
                    false,
                    order.getStatus(),
                    "截止时间缺失",
                    null,
                    null,
                    false
            );
        }
        if (order.getDeadlineTime().isAfter(now)) {
            log.warn(
                    "Timeout message consumed before deadline. orderId={}, deadlineTime={}, now={}",
                    orderId,
                    order.getDeadlineTime(),
                    now
            );
            return new GroupOrderTimeoutCheckVO(
                    orderId,
                    false,
                    order.getStatus(),
                    "未到截止时间",
                    null,
                    DateTimeUtil.format(order.getDeadlineTime()),
                    true
            );
        }

        String expireReason = "超过加入截止时间，系统自动关闭";
        GroupOrder updateOrder = new GroupOrder();
        updateOrder.setStatus(GroupOrderStatus.EXPIRED.getValue());
        updateOrder.setExpiredTime(now);
        updateOrder.setExpireReason(expireReason);
        updateOrder.setLastEventTime(now);
        int updated = groupOrderMapper.update(
                updateOrder,
                new LambdaUpdateWrapper<GroupOrder>()
                        .eq(GroupOrder::getId, orderId)
                        .eq(GroupOrder::getStatus, GroupOrderStatus.CREATED.getValue())
                        .le(GroupOrder::getDeadlineTime, now)
        );
        if (updated <= 0) {
            GroupOrder latest = groupOrderMapper.selectById(orderId);
            log.info(
                    "Ignore timeout message because conditional expire update did not win. orderId={}, latestStatus={}",
                    orderId,
                    latest == null ? null : latest.getStatus()
            );
            return new GroupOrderTimeoutCheckVO(
                    orderId,
                    false,
                    latest == null ? null : latest.getStatus(),
                    "超时关闭已被其他操作处理",
                    latest == null ? null : DateTimeUtil.format(latest.getExpiredTime()),
                    latest == null ? null : DateTimeUtil.format(latest.getDeadlineTime()),
                    false
            );
        }

        order.setStatus(GroupOrderStatus.EXPIRED.getValue());
        order.setExpiredTime(now);
        order.setExpireReason(expireReason);
        order.setLastEventTime(now);
        insertStatusLog(
                orderId,
                SYSTEM_OPERATOR_ID,
                TARGET_TYPE_GROUP_ORDER,
                orderId,
                ACTION_TYPE_EXPIRE_ORDER,
                GroupOrderStatus.CREATED.getValue(),
                GroupOrderStatus.EXPIRED.getValue(),
                expireReason
        );
        insertGroupOrderEvent(
                order,
                EVENT_TYPE_EXPIRED,
                null,
                OPERATOR_ROLE_SYSTEM,
                "拼单超时关闭",
                expireReason,
                GroupOrderStatus.CREATED.getValue(),
                GroupOrderStatus.EXPIRED.getValue(),
                now
        );
        log.info(
                "Group order expired successfully. orderId={}, deadlineTime={}, expiredTime={}",
                orderId,
                order.getDeadlineTime(),
                now
        );

        return new GroupOrderTimeoutCheckVO(
                orderId,
                true,
                GroupOrderStatus.EXPIRED.getValue(),
                "拼单已超时关闭",
                DateTimeUtil.format(now),
                DateTimeUtil.format(order.getDeadlineTime()),
                false
        );
    }

    private List<OrderParticipant> loadParticipantsByUser(Long userId) {
        List<OrderParticipant> participants = orderParticipantMapper.selectList(
                new LambdaQueryWrapper<OrderParticipant>()
                        .eq(OrderParticipant::getUserId, userId)
                        .orderByDesc(OrderParticipant::getJoinTime)
        );
        return participants == null ? Collections.emptyList() : participants;
    }

    private void applyHallVisibility(
            LambdaQueryWrapper<GroupOrder> wrapper,
            Long currentUserId,
            LocalDateTime now) {
        Set<Long> joinedOrderIds = loadParticipantsByUser(currentUserId).stream()
                .map(OrderParticipant::getGroupOrderId)
                .collect(Collectors.toSet());
        wrapper.and(visibility -> {
            visibility.and(publicQuery -> publicQuery
                            .eq(GroupOrder::getStatus, GroupOrderStatus.CREATED.getValue())
                            .gt(GroupOrder::getDeadlineTime, now)
                            .and(capacity -> capacity
                                    .isNull(GroupOrder::getMaxParticipants)
                                    .or()
                                    .apply("participant_count < max_participants")))
                    .or()
                    .eq(GroupOrder::getCreatorId, currentUserId)
                    .or()
                    .eq(GroupOrder::getPickupUserId, currentUserId);
            if (!joinedOrderIds.isEmpty()) {
                visibility.or().in(GroupOrder::getId, joinedOrderIds);
            }
        });
    }

    private boolean shouldReturnEmptyMyOrders(
            String scope,
            Set<Long> joinedOrderIds,
            Set<Long> pendingPaymentOrderIds) {
        return MY_SCOPE_JOINED_BY_ME.equals(scope) && joinedOrderIds.isEmpty()
                || MY_SCOPE_PENDING_PAYMENT.equals(scope) && pendingPaymentOrderIds.isEmpty();
    }

    private void applyMyGroupOrderScope(
            LambdaQueryWrapper<GroupOrder> wrapper,
            String scope,
            Long currentUserId,
            Set<Long> joinedOrderIds,
            Set<Long> pendingPaymentOrderIds) {
        if (MY_SCOPE_CREATED_BY_ME.equals(scope)) {
            wrapper.eq(GroupOrder::getCreatorId, currentUserId);
            return;
        }
        if (MY_SCOPE_JOINED_BY_ME.equals(scope)) {
            wrapper.in(GroupOrder::getId, joinedOrderIds);
            return;
        }
        if (MY_SCOPE_PICKUP_BY_ME.equals(scope)) {
            wrapper.eq(GroupOrder::getPickupUserId, currentUserId);
            return;
        }
        if (MY_SCOPE_PENDING_PAYMENT.equals(scope)) {
            wrapper.in(GroupOrder::getId, pendingPaymentOrderIds)
                    .notIn(GroupOrder::getStatus, HISTORY_STATUSES);
            return;
        }
        if (MY_SCOPE_HISTORY.equals(scope)) {
            applyCurrentUserOrderScope(wrapper, currentUserId, joinedOrderIds);
            wrapper.in(GroupOrder::getStatus, HISTORY_STATUSES);
            return;
        }
        applyCurrentUserOrderScope(wrapper, currentUserId, joinedOrderIds);
    }

    private void applyCurrentUserOrderScope(
            LambdaQueryWrapper<GroupOrder> wrapper,
            Long currentUserId,
            Set<Long> joinedOrderIds) {
        wrapper.and(query -> {
            query.eq(GroupOrder::getCreatorId, currentUserId)
                    .or()
                    .eq(GroupOrder::getPickupUserId, currentUserId);
            if (!joinedOrderIds.isEmpty()) {
                query.or().in(GroupOrder::getId, joinedOrderIds);
            }
        });
    }

    private Map<Long, PickupRecord> loadPickupRecordsByOrderIds(Set<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return pickupRecordMapper.selectList(
                        new LambdaQueryWrapper<PickupRecord>()
                                .in(PickupRecord::getGroupOrderId, orderIds)
                )
                .stream()
                .collect(Collectors.toMap(PickupRecord::getGroupOrderId, Function.identity(), (left, right) -> left));
    }

    private MyGroupOrderVO toMyGroupOrderVO(
            GroupOrder order,
            Map<Long, User> users,
            OrderParticipant myParticipant,
            PickupRecord pickupRecord,
            Long currentUserId) {
        String myRole = resolveMyRole(order, myParticipant, currentUserId);
        return new MyGroupOrderVO(
                toVO(order, users),
                myRole,
                myParticipant == null ? null : myParticipant.getId(),
                myParticipant == null ? null : amount(myParticipant.getPayableAmount()),
                myParticipant == null ? null : myParticipant.getPaymentStatus(),
                pickupRecord == null ? null : pickupRecord.getPickupStatus()
        );
    }

    private String resolveMyRole(GroupOrder order, OrderParticipant myParticipant, Long currentUserId) {
        if (Objects.equals(order.getCreatorId(), currentUserId)) {
            return "CREATOR";
        }
        if (Objects.equals(order.getPickupUserId(), currentUserId)) {
            return "PICKUP";
        }
        if (myParticipant != null) {
            return "PARTICIPANT";
        }
        return "RELATED";
    }

    private DashboardSummaryVO toDashboardSummaryVO(
            List<GroupOrder> orders,
            List<OrderParticipant> participants) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();

        long todayOrderCount = orders.stream()
                .filter(order -> order.getCreateTime() != null
                        && !order.getCreateTime().isBefore(todayStart)
                        && order.getCreateTime().isBefore(tomorrowStart))
                .count();
        long successOrderCount = orders.stream()
                .filter(order -> GroupOrderStatus.FINISHED.getValue().equals(order.getStatus()))
                .count();
        long createdCount = countOrdersByStatus(orders, GroupOrderStatus.CREATED.getValue());
        long lockedCount = countOrdersByStatus(orders, GroupOrderStatus.LOCKED.getValue());
        long finishedCount = countOrdersByStatus(orders, GroupOrderStatus.FINISHED.getValue());
        long cancelledCount = countOrdersByStatus(orders, GroupOrderStatus.CANCELLED.getValue());

        BigDecimal originalTotalAmount = sumOrderAmount(orders, GroupOrder::getOriginalTotalAmount);
        BigDecimal actualDiscountAmount = sumOrderAmount(orders, GroupOrder::getActualDiscountAmount);
        BigDecimal payableTotalAmount = sumOrderAmount(orders, GroupOrder::getPayableTotalAmount);
        BigDecimal totalSavedAmount = orders.stream()
                .filter(order -> !GroupOrderStatus.CANCELLED.getValue().equals(order.getStatus()))
                .map(GroupOrder::getActualDiscountAmount)
                .map(this::amount)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        long paidParticipantCount = participants.stream()
                .filter(participant -> PaymentStatus.PAID.getValue().equals(participant.getPaymentStatus()))
                .count();
        long confirmedParticipantCount = participants.stream()
                .filter(participant -> PaymentStatus.CONFIRMED.getValue().equals(participant.getPaymentStatus()))
                .count();

        return new DashboardSummaryVO(
                todayOrderCount,
                successOrderCount,
                totalSavedAmount,
                (long) orders.size(),
                createdCount,
                lockedCount,
                finishedCount,
                cancelledCount,
                (long) participants.size(),
                originalTotalAmount,
                actualDiscountAmount,
                payableTotalAmount,
                paidParticipantCount,
                confirmedParticipantCount,
                rankOrders(orders, GroupOrder::getOrderType),
                rankOrders(orders, GroupOrder::getMerchantName)
        );
    }

    private long countOrdersByStatus(List<GroupOrder> orders, String status) {
        return orders.stream()
                .filter(order -> status.equals(order.getStatus()))
                .count();
    }

    private BigDecimal sumOrderAmount(
            List<GroupOrder> orders,
            Function<GroupOrder, BigDecimal> amountExtractor) {
        return orders.stream()
                .map(amountExtractor)
                .map(this::amount)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<DashboardRankItemVO> rankOrders(
            List<GroupOrder> orders,
            Function<GroupOrder, String> keyExtractor) {
        Map<String, Long> counts = orders.stream()
                .map(keyExtractor)
                .map(this::normalize)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .map(entry -> new DashboardRankItemVO(entry.getKey(), entry.getValue()))
                .toList();
    }

    private GroupOrder getExistingOrder(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "orderId 不合法");
        }
        GroupOrder order = groupOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "拼单不存在");
        }
        return order;
    }

    private OrderParticipant getExistingParticipant(Long orderId, Long participantId) {
        if (participantId == null || participantId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "participantId 不合法");
        }
        OrderParticipant participant = orderParticipantMapper.selectById(participantId);
        if (participant == null || !Objects.equals(participant.getGroupOrderId(), orderId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "参与记录不存在");
        }
        return participant;
    }

    private void validatePaymentOrderStatus(GroupOrder order) {
        if (!PAYMENT_ALLOWED_ORDER_STATUSES.contains(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "拼单未锁定，暂不能标记付款");
        }
    }

    private PaymentRecord createPaymentRecord(
            Long orderId,
            OrderParticipant participant,
            String paymentStatus,
            LocalDateTime markTime,
            Long confirmUserId,
            LocalDateTime confirmTime,
            String remark) {
        PaymentRecord record = new PaymentRecord();
        record.setGroupOrderId(orderId);
        record.setParticipantId(participant.getId());
        record.setUserId(participant.getUserId());
        record.setAmount(amount(participant.getPayableAmount()));
        record.setPaymentStatus(paymentStatus);
        record.setMarkTime(markTime);
        record.setConfirmUserId(confirmUserId);
        record.setConfirmTime(confirmTime);
        record.setRemark(remark);
        return record;
    }

    private PickupRecord getPickupRecord(Long orderId) {
        return pickupRecordMapper.selectOne(
                new LambdaQueryWrapper<PickupRecord>()
                        .eq(PickupRecord::getGroupOrderId, orderId)
                        .last("LIMIT 1")
        );
    }

    private void validatePickupMutable(GroupOrder order) {
        if (TERMINAL_STATUSES.contains(order.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "拼单已取消或已完成");
        }
    }

    private boolean isCreatorOrParticipant(GroupOrder order, Long userId) {
        if (Objects.equals(order.getCreatorId(), userId)) {
            return true;
        }
        Long count = orderParticipantMapper.selectCount(
                new LambdaQueryWrapper<OrderParticipant>()
                        .eq(OrderParticipant::getGroupOrderId, order.getId())
                        .eq(OrderParticipant::getUserId, userId)
        );
        return count != null && count > 0;
    }

    private boolean canViewGroupOrderEvents(GroupOrder order, Long userId) {
        if (Objects.equals(order.getCreatorId(), userId) || Objects.equals(order.getPickupUserId(), userId)) {
            return true;
        }
        Long count = orderParticipantMapper.selectCount(
                new LambdaQueryWrapper<OrderParticipant>()
                        .eq(OrderParticipant::getGroupOrderId, order.getId())
                        .eq(OrderParticipant::getUserId, userId)
        );
        return count != null && count > 0;
    }

    private boolean isRelatedUser(GroupOrder order, List<OrderParticipant> participants, Long userId) {
        return Objects.equals(order.getCreatorId(), userId)
                || Objects.equals(order.getPickupUserId(), userId)
                || participants.stream().anyMatch(participant -> Objects.equals(participant.getUserId(), userId));
    }

    private GroupOrderPermissionsVO buildPermissions(
            GroupOrder order,
            List<OrderParticipant> participants,
            PickupRecord pickupRecord,
            Long currentUserId) {
        OrderParticipant currentParticipant = participants.stream()
                .filter(participant -> Objects.equals(participant.getUserId(), currentUserId))
                .findFirst()
                .orElse(null);
        boolean isCreator = Objects.equals(order.getCreatorId(), currentUserId);
        boolean isPickupUser = Objects.equals(order.getPickupUserId(), currentUserId)
                || pickupRecord != null && Objects.equals(pickupRecord.getPickupUserId(), currentUserId);
        boolean isParticipant = currentParticipant != null;
        boolean terminal = TERMINAL_STATUSES.contains(order.getStatus());
        boolean paymentAllowed = PAYMENT_ALLOWED_ORDER_STATUSES.contains(order.getStatus());

        return new GroupOrderPermissionsVO(
                canJoin(order, isParticipant),
                isCreator && GroupOrderStatus.CREATED.getValue().equals(order.getStatus()),
                canCancel(order, participants, isCreator),
                paymentAllowed
                        && isParticipant
                        && PaymentStatus.UNPAID.getValue().equals(currentParticipant.getPaymentStatus()),
                paymentAllowed && isCreator,
                !terminal && (isCreator || isPickupUser),
                isCreator,
                isCreator || isParticipant || isPickupUser
        );
    }

    private boolean canJoin(GroupOrder order, boolean isParticipant) {
        int participantCount = order.getParticipantCount() == null ? 0 : order.getParticipantCount();
        boolean notFull = order.getMaxParticipants() == null || participantCount < order.getMaxParticipants();
        return GroupOrderStatus.CREATED.getValue().equals(order.getStatus())
                && order.getDeadlineTime() != null
                && order.getDeadlineTime().isAfter(LocalDateTime.now())
                && !isParticipant
                && notFull;
    }

    private boolean canCancel(GroupOrder order, List<OrderParticipant> participants, boolean isCreator) {
        if (!isCreator) {
            return false;
        }
        if (GroupOrderStatus.CREATED.getValue().equals(order.getStatus())) {
            return true;
        }
        if (!GroupOrderStatus.LOCKED.getValue().equals(order.getStatus())) {
            return false;
        }
        return participants.stream()
                .noneMatch(participant -> !PaymentStatus.UNPAID.getValue().equals(participant.getPaymentStatus()));
    }

    private void validatePickupStatusTransition(String beforeStatus, String targetStatus) {
        if (Objects.equals(beforeStatus, targetStatus)) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "取餐状态已是目标状态");
        }
        boolean allowed =
                PickupStatus.WAITING_ORDER.getValue().equals(beforeStatus)
                        && PickupStatus.WAITING_DELIVERY.getValue().equals(targetStatus)
                || PickupStatus.WAITING_DELIVERY.getValue().equals(beforeStatus)
                        && PickupStatus.ARRIVED.getValue().equals(targetStatus)
                || PickupStatus.ARRIVED.getValue().equals(beforeStatus)
                        && PickupStatus.PICKED_UP.getValue().equals(targetStatus)
                || PickupStatus.PICKED_UP.getValue().equals(beforeStatus)
                        && PickupStatus.DISTRIBUTED.getValue().equals(targetStatus);
        if (!allowed) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法取餐状态流转");
        }
    }

    private String resolveOrderStatusForPickup(String pickupStatus) {
        if (PickupStatus.WAITING_DELIVERY.getValue().equals(pickupStatus)) {
            return GroupOrderStatus.DELIVERING.getValue();
        }
        if (PickupStatus.ARRIVED.getValue().equals(pickupStatus)) {
            return GroupOrderStatus.ARRIVED.getValue();
        }
        if (PickupStatus.PICKED_UP.getValue().equals(pickupStatus)) {
            return GroupOrderStatus.PICKED_UP.getValue();
        }
        if (PickupStatus.DISTRIBUTED.getValue().equals(pickupStatus)) {
            return GroupOrderStatus.FINISHED.getValue();
        }
        return GroupOrderStatus.ORDERED.getValue();
    }

    private void validateOrderStatusForPickup(String beforeOrderStatus, String targetOrderStatus) {
        boolean allowed =
                GroupOrderStatus.ORDERED.getValue().equals(beforeOrderStatus)
                        && GroupOrderStatus.DELIVERING.getValue().equals(targetOrderStatus)
                || GroupOrderStatus.DELIVERING.getValue().equals(beforeOrderStatus)
                        && GroupOrderStatus.ARRIVED.getValue().equals(targetOrderStatus)
                || GroupOrderStatus.ARRIVED.getValue().equals(beforeOrderStatus)
                        && GroupOrderStatus.PICKED_UP.getValue().equals(targetOrderStatus)
                || GroupOrderStatus.PICKED_UP.getValue().equals(beforeOrderStatus)
                        && GroupOrderStatus.FINISHED.getValue().equals(targetOrderStatus);
        if (!allowed) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "取餐状态不能早于拼单状态");
        }
    }

    private boolean isPickupStatus(String value) {
        for (PickupStatus status : PickupStatus.values()) {
            if (status.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private LocalDateTime parseOptionalDateTime(String value, String errorMessage) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : DateTimeUtil.parse(normalized, errorMessage);
    }

    private LocalDateTime parseOptionalDateTimeOrNow(String value, String errorMessage, LocalDateTime now) {
        LocalDateTime parsed = parseOptionalDateTime(value, errorMessage);
        return parsed == null ? now : parsed;
    }

    private Map<Long, User> loadUsersForPickup(GroupOrder order, PickupRecord pickupRecord) {
        Set<Long> userIds = Stream.of(order.getCreatorId(), pickupRecord.getPickupUserId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private void validateJoinOrder(GroupOrder order, Long userId, JoinGroupOrderRequest request) {
        if (!GroupOrderStatus.CREATED.getValue().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "拼单已锁定，不能继续加入");
        }
        if (!order.getDeadlineTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "拼单已过截止时间");
        }
        Long joinedCount = orderParticipantMapper.selectCount(
                new LambdaQueryWrapper<OrderParticipant>()
                        .eq(OrderParticipant::getGroupOrderId, order.getId())
                        .eq(OrderParticipant::getUserId, userId)
        );
        if (joinedCount != null && joinedCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "不能重复加入同一拼单");
        }
        Integer maxParticipants = order.getMaxParticipants();
        int participantCount = order.getParticipantCount() == null ? 0 : order.getParticipantCount();
        if (maxParticipants != null && participantCount >= maxParticipants) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "拼单人数已满");
        }
        if (request.getMealItems() == null || request.getMealItems().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "餐品不能为空");
        }
        for (JoinGroupOrderRequest.MealItemRequest mealItem : request.getMealItems()) {
            validateMealItem(mealItem);
        }
    }

    private void validateMealItem(JoinGroupOrderRequest.MealItemRequest mealItem) {
        if (mealItem == null || normalize(mealItem.getItemName()).isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "餐品名称不能为空");
        }
        if (mealItem.getQuantity() == null || mealItem.getQuantity() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "餐品金额必须大于 0");
        }
        if (mealItem.getUnitPrice() == null || mealItem.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "餐品金额必须大于 0");
        }
        BigDecimal subtotalAmount = calculateSubtotalAmount(mealItem);
        if (subtotalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "餐品金额必须大于 0");
        }
    }

    private BigDecimal calculateOriginalAmount(JoinGroupOrderRequest request) {
        return calculateOriginalAmount(request.getMealItems());
    }

    private BigDecimal calculateOriginalAmount(List<JoinGroupOrderRequest.MealItemRequest> mealItems) {
        return mealItems.stream()
                .map(this::calculateSubtotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSubtotalAmount(JoinGroupOrderRequest.MealItemRequest mealItem) {
        BigDecimal unitPrice = mealItem.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
        return unitPrice.multiply(BigDecimal.valueOf(mealItem.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private MealItem toMealItem(
            Long orderId,
            Long participantId,
            JoinGroupOrderRequest.MealItemRequest request) {
        MealItem mealItem = new MealItem();
        mealItem.setGroupOrderId(orderId);
        mealItem.setParticipantId(participantId);
        mealItem.setItemName(normalize(request.getItemName()));
        mealItem.setQuantity(request.getQuantity());
        mealItem.setUnitPrice(request.getUnitPrice().setScale(2, RoundingMode.HALF_UP));
        mealItem.setSubtotalAmount(calculateSubtotalAmount(request));
        mealItem.setRemark(request.getRemark());
        return mealItem;
    }

    private GroupOrder updateOrderAmountAfterJoin(GroupOrder order, BigDecimal joinAmount) {
        int participantCount = order.getParticipantCount() == null ? 0 : order.getParticipantCount();
        BigDecimal originalTotalAmount = amount(order.getOriginalTotalAmount()).add(joinAmount)
                .setScale(2, RoundingMode.HALF_UP);

        order.setParticipantCount(participantCount + 1);
        order.setOriginalTotalAmount(originalTotalAmount);
        order.setActualDiscountAmount(BigDecimal.ZERO);
        order.setPayableTotalAmount(originalTotalAmount);
        order.setRoundingAdjustmentAmount(BigDecimal.ZERO);
        groupOrderMapper.updateById(order);
        return order;
    }

    private void validateLockOrder(GroupOrder order, Long userId) {
        if (!Objects.equals(order.getCreatorId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只有发起人可以锁单");
        }
        if (!GroupOrderStatus.CREATED.getValue().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "当前状态不能锁单");
        }
    }

    private void validateCancelOrder(GroupOrder order, Long userId) {
        if (!Objects.equals(order.getCreatorId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只有发起人可以取消拼单");
        }
        if (GroupOrderStatus.CREATED.getValue().equals(order.getStatus())) {
            return;
        }
        if (GroupOrderStatus.LOCKED.getValue().equals(order.getStatus())) {
            if (hasPaidParticipant(order.getId())) {
                throw new BusinessException(ResultCode.CONFLICT.getCode(), "已有成员付款，不能取消拼单");
            }
            return;
        }
        if (GroupOrderStatus.ORDERED.getValue().equals(order.getStatus())
                || GroupOrderStatus.DELIVERING.getValue().equals(order.getStatus())
                || GroupOrderStatus.ARRIVED.getValue().equals(order.getStatus())
                || GroupOrderStatus.PICKED_UP.getValue().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "当前状态不支持普通取消，请记录异常事件");
        }
        if (TERMINAL_STATUSES.contains(order.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "拼单已取消、已完成或已过期，不能取消");
        }
        throw new BusinessException(ResultCode.CONFLICT.getCode(), "当前状态不能取消拼单");
    }

    private boolean hasPaidParticipant(Long orderId) {
        Long count = orderParticipantMapper.selectCount(
                new LambdaQueryWrapper<OrderParticipant>()
                        .eq(OrderParticipant::getGroupOrderId, orderId)
                        .ne(OrderParticipant::getPaymentStatus, PaymentStatus.UNPAID.getValue())
        );
        return count != null && count > 0;
    }

    private BigDecimal calculateActualDiscountAmount(GroupOrder order, BigDecimal originalTotalAmount) {
        BigDecimal discountThresholdAmount = order.getDiscountThresholdAmount();
        BigDecimal discountAmount = amount(order.getDiscountAmount());
        if (discountThresholdAmount == null || originalTotalAmount.compareTo(discountThresholdAmount) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (discountAmount.compareTo(originalTotalAmount) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "优惠金额不能大于原始总金额");
        }
        return discountAmount;
    }

    private BigDecimal calculateDiscountShareAmount(
            BigDecimal participantOriginalAmount,
            BigDecimal originalTotalAmount,
            BigDecimal actualDiscountAmount) {
        if (actualDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return actualDiscountAmount
                .multiply(participantOriginalAmount)
                .divide(originalTotalAmount, 2, RoundingMode.HALF_UP);
    }

    private void validateCreateRequest(CreateGroupOrderRequest request) {
        if (normalize(request.getTitle()).isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "title 不能为空");
        }
        String orderType = normalize(request.getOrderType());
        if (orderType.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "orderType 不能为空");
        }
        if (!ORDER_TYPES.contains(orderType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "orderType 不合法");
        }
        if (normalize(request.getMerchantName()).isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "merchantName 不能为空");
        }
        if (normalize(request.getPickupLocation()).isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "pickupLocation 不能为空");
        }
        if (request.getMaxParticipants() != null && request.getMaxParticipants() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "maxParticipants 必须大于 0");
        }
        if (request.getDiscountThresholdAmount() != null
                && request.getDiscountThresholdAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "discountThresholdAmount 必须大于 0");
        }
        if (request.getDiscountAmount() != null && request.getDiscountAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "discountAmount 不能小于 0");
        }
        if (request.getDiscountAmount() != null
                && request.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                && request.getDiscountThresholdAmount() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "discountThresholdAmount 不能为空");
        }
        if (request.getCreatorItems() != null) {
            for (JoinGroupOrderRequest.MealItemRequest creatorItem : request.getCreatorItems()) {
                validateMealItem(creatorItem);
            }
        }
    }

    private void createCreatorParticipantIfNeeded(
            GroupOrder order,
            Long creatorId,
            List<JoinGroupOrderRequest.MealItemRequest> creatorItems) {
        if (creatorItems == null || creatorItems.isEmpty()) {
            return;
        }

        BigDecimal originalAmount = calculateOriginalAmount(creatorItems);
        LocalDateTime now = LocalDateTime.now();

        OrderParticipant participant = new OrderParticipant();
        participant.setGroupOrderId(order.getId());
        participant.setUserId(creatorId);
        participant.setOriginalAmount(originalAmount);
        participant.setDiscountShareAmount(BigDecimal.ZERO);
        participant.setPayableAmount(originalAmount);
        participant.setRoundingAdjustmentAmount(BigDecimal.ZERO);
        participant.setPaymentStatus(PaymentStatus.UNPAID.getValue());
        participant.setJoinTime(now);
        participant.setRemark("发起人自动加入");

        try {
            orderParticipantMapper.insert(participant);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "不能重复加入同一拼单");
        }

        List<MealItem> mealItems = creatorItems.stream()
                .map(itemRequest -> toMealItem(order.getId(), participant.getId(), itemRequest))
                .toList();
        for (MealItem mealItem : mealItems) {
            mealItemMapper.insert(mealItem);
        }

        updateOrderAmountAfterJoin(order, originalAmount);
    }

    private User requireCurrentUser(String authorization) {
        Long userId = authTokenUtil.parseUserIdFromAuthorization(authorization);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (DISABLED_STATUS.equals(user.getStatus()) || !ACTIVE_STATUS.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "用户已禁用");
        }
        return user;
    }

    private void insertCreateStatusLog(GroupOrder order, Long operatorId) {
        OrderStatusLog log = new OrderStatusLog();
        log.setGroupOrderId(order.getId());
        log.setOperatorId(operatorId);
        log.setTargetType(TARGET_TYPE_GROUP_ORDER);
        log.setTargetId(order.getId());
        log.setActionType(ACTION_TYPE_CREATE_ORDER);
        log.setAfterStatus(GroupOrderStatus.CREATED.getValue());
        log.setRemark("发起拼单");
        orderStatusLogMapper.insert(log);
    }

    private void insertLockStatusLog(GroupOrder order, Long operatorId, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setGroupOrderId(order.getId());
        log.setOperatorId(operatorId);
        log.setTargetType(TARGET_TYPE_GROUP_ORDER);
        log.setTargetId(order.getId());
        log.setActionType(ACTION_TYPE_LOCK_ORDER);
        log.setBeforeStatus(GroupOrderStatus.CREATED.getValue());
        log.setAfterStatus(GroupOrderStatus.LOCKED.getValue());
        log.setRemark(normalize(remark).isEmpty() ? "锁定拼单并生成优惠分摊" : remark);
        orderStatusLogMapper.insert(log);
    }

    private void insertStatusLog(
            Long orderId,
            Long operatorId,
            String targetType,
            Long targetId,
            String actionType,
            String beforeStatus,
            String afterStatus,
            String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setGroupOrderId(orderId);
        log.setOperatorId(operatorId);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setActionType(actionType);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setRemark(remark);
        orderStatusLogMapper.insert(log);
    }

    private void insertGroupOrderEvent(
            GroupOrder order,
            String eventType,
            Long operatorId,
            String operatorRole,
            String title,
            String content,
            String beforeStatus,
            String afterStatus,
            LocalDateTime eventTime) {
        GroupOrderEvent event = new GroupOrderEvent();
        event.setGroupOrderId(order.getId());
        event.setEventType(eventType);
        event.setEventLevel(EVENT_LEVEL_INFO);
        event.setOperatorId(operatorId);
        event.setOperatorRole(operatorRole);
        event.setTitle(title);
        event.setContent(content);
        event.setBeforeStatus(beforeStatus);
        event.setAfterStatus(afterStatus);
        event.setEventTime(eventTime);
        groupOrderEventMapper.insert(event);
    }

    private GroupOrderEventVO toGroupOrderEventVO(GroupOrderEvent event) {
        return new GroupOrderEventVO(
                event.getId(),
                event.getGroupOrderId(),
                event.getEventType(),
                event.getEventLevel(),
                event.getOperatorId(),
                event.getOperatorRole(),
                event.getTitle(),
                event.getContent(),
                event.getBeforeStatus(),
                event.getAfterStatus(),
                DateTimeUtil.format(event.getEventTime()),
                DateTimeUtil.format(event.getCreateTime())
        );
    }

    private Map<Long, User> loadUsers(List<GroupOrder> orders) {
        Set<Long> userIds = orders.stream()
                .flatMap(order -> java.util.stream.Stream.of(order.getCreatorId(), order.getPickupUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<Long, User> loadUsersForDetail(
            GroupOrder order,
            List<OrderParticipant> participants,
            PickupRecord pickupRecord) {
        Set<Long> userIds = Stream.concat(
                        Stream.of(
                                order.getCreatorId(),
                                order.getPickupUserId(),
                                pickupRecord == null ? null : pickupRecord.getPickupUserId()
                        ),
                        participants.stream().map(OrderParticipant::getUserId)
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private GroupOrderVO toVO(GroupOrder order, Map<Long, User> users) {
        UserSummaryVO pickupUser = order.getPickupUserId() == null
                ? null
                : toUserSummary(users.get(order.getPickupUserId()));
        return new GroupOrderVO(
                order.getId(),
                order.getTitle(),
                order.getOrderType(),
                order.getMerchantName(),
                order.getPickupLocation(),
                toUserSummary(users.get(order.getCreatorId())),
                DateTimeUtil.format(order.getDeadlineTime()),
                order.getMaxParticipants(),
                order.getParticipantCount(),
                order.getDiscountThresholdAmount(),
                order.getDiscountAmount(),
                order.getOriginalTotalAmount(),
                order.getActualDiscountAmount(),
                order.getPayableTotalAmount(),
                order.getRoundingAdjustmentAmount(),
                order.getStatus(),
                pickupUser,
                order.getRemark(),
                DateTimeUtil.format(order.getLockedTime()),
                DateTimeUtil.format(order.getFinishTime()),
                DateTimeUtil.format(order.getCancelTime()),
                DateTimeUtil.format(order.getCreateTime()),
                DateTimeUtil.format(order.getUpdateTime())
        );
    }

    private ParticipantVO toParticipantVO(
            OrderParticipant participant,
            Map<Long, User> users,
            List<MealItem> mealItems) {
        return new ParticipantVO(
                participant.getId(),
                participant.getGroupOrderId(),
                toUserSummary(users.get(participant.getUserId())),
                participant.getOriginalAmount(),
                participant.getDiscountShareAmount(),
                participant.getPayableAmount(),
                participant.getRoundingAdjustmentAmount(),
                participant.getPaymentStatus(),
                DateTimeUtil.format(participant.getPaidMarkTime()),
                DateTimeUtil.format(participant.getPaidConfirmTime()),
                DateTimeUtil.format(participant.getJoinTime()),
                participant.getRemark(),
                mealItems.stream().map(this::toMealItemVO).toList()
        );
    }

    private MealItemVO toMealItemVO(MealItem mealItem) {
        return new MealItemVO(
                mealItem.getId(),
                mealItem.getGroupOrderId(),
                mealItem.getParticipantId(),
                mealItem.getItemName(),
                mealItem.getQuantity(),
                mealItem.getUnitPrice(),
                mealItem.getSubtotalAmount(),
                mealItem.getRemark()
        );
    }

    private PickupRecordVO toPickupRecordVO(PickupRecord pickupRecord, Map<Long, User> users) {
        if (pickupRecord == null) {
            return null;
        }
        return new PickupRecordVO(
                pickupRecord.getId(),
                pickupRecord.getGroupOrderId(),
                toUserSummary(users.get(pickupRecord.getPickupUserId())),
                pickupRecord.getPickupLocation(),
                pickupRecord.getPickupStatus(),
                DateTimeUtil.format(pickupRecord.getEstimatedArrivalTime()),
                DateTimeUtil.format(pickupRecord.getActualArrivalTime()),
                DateTimeUtil.format(pickupRecord.getPickedUpTime()),
                DateTimeUtil.format(pickupRecord.getDistributedTime()),
                pickupRecord.getRemark()
        );
    }

    private LockedGroupOrderVO toLockedGroupOrderVO(GroupOrder order) {
        return new LockedGroupOrderVO(
                order.getId(),
                order.getStatus(),
                amount(order.getOriginalTotalAmount()),
                amount(order.getActualDiscountAmount()),
                amount(order.getPayableTotalAmount()),
                amount(order.getRoundingAdjustmentAmount()),
                DateTimeUtil.format(order.getLockedTime())
        );
    }

    private LockAllocationVO toLockAllocationVO(OrderParticipant participant) {
        return new LockAllocationVO(
                participant.getId(),
                participant.getUserId(),
                amount(participant.getOriginalAmount()),
                amount(participant.getDiscountShareAmount()),
                amount(participant.getPayableAmount()),
                amount(participant.getRoundingAdjustmentAmount()),
                participant.getPaymentStatus()
        );
    }

    private PaymentActionVO toPaymentActionVO(OrderParticipant participant, PaymentRecord paymentRecord) {
        return new PaymentActionVO(
                participant.getId(),
                participant.getPaymentStatus(),
                DateTimeUtil.format(participant.getPaidMarkTime()),
                DateTimeUtil.format(participant.getPaidConfirmTime()),
                toPaymentRecordVO(paymentRecord)
        );
    }

    private PaymentRecordVO toPaymentRecordVO(PaymentRecord paymentRecord) {
        return new PaymentRecordVO(
                paymentRecord.getId(),
                paymentRecord.getGroupOrderId(),
                paymentRecord.getParticipantId(),
                paymentRecord.getUserId(),
                amount(paymentRecord.getAmount()),
                paymentRecord.getPaymentStatus(),
                DateTimeUtil.format(paymentRecord.getMarkTime()),
                paymentRecord.getConfirmUserId(),
                DateTimeUtil.format(paymentRecord.getConfirmTime()),
                paymentRecord.getRemark()
        );
    }

    private OrderAmountVO toOrderAmountVO(GroupOrder order) {
        BigDecimal originalTotalAmount = amount(order.getOriginalTotalAmount());
        BigDecimal discountThresholdAmount = order.getDiscountThresholdAmount();
        boolean discountReached = discountThresholdAmount != null
                && originalTotalAmount.compareTo(discountThresholdAmount) >= 0;
        BigDecimal discountGapAmount = BigDecimal.ZERO;
        if (discountThresholdAmount != null && !discountReached) {
            discountGapAmount = discountThresholdAmount.subtract(originalTotalAmount)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return new OrderAmountVO(
                order.getParticipantCount(),
                originalTotalAmount,
                discountThresholdAmount,
                amount(order.getDiscountAmount()),
                amount(order.getActualDiscountAmount()),
                amount(order.getPayableTotalAmount()),
                discountGapAmount,
                discountReached
        );
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private UserSummaryVO toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryVO(user.getId(), user.getAccount(), user.getNickname(), user.getPhone(), user.getStatus());
    }

    private boolean isGroupOrderStatus(String value) {
        for (GroupOrderStatus status : GroupOrderStatus.values()) {
            if (status.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private long normalizePageNum(Long pageNum) {
        if (pageNum == null) {
            return 1L;
        }
        if (pageNum <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "pageNum 必须大于 0");
        }
        return pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null) {
            return 10L;
        }
        if (pageSize <= 0 || pageSize > 100) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "pageSize 必须在 1 到 100 之间");
        }
        return pageSize;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
