package com.campus.pinhaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.pinhaofan.common.AuthTokenUtil;
import com.campus.pinhaofan.common.DateTimeUtil;
import com.campus.pinhaofan.dto.CreateGroupOrderRequest;
import com.campus.pinhaofan.entity.GroupOrder;
import com.campus.pinhaofan.entity.OrderStatusLog;
import com.campus.pinhaofan.entity.User;
import com.campus.pinhaofan.enums.GroupOrderStatus;
import com.campus.pinhaofan.enums.ResultCode;
import com.campus.pinhaofan.exception.BusinessException;
import com.campus.pinhaofan.mapper.GroupOrderMapper;
import com.campus.pinhaofan.mapper.OrderStatusLogMapper;
import com.campus.pinhaofan.mapper.UserMapper;
import com.campus.pinhaofan.service.GroupOrderService;
import com.campus.pinhaofan.vo.GroupOrderVO;
import com.campus.pinhaofan.vo.PageResultVO;
import com.campus.pinhaofan.vo.UserSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupOrderServiceImpl implements GroupOrderService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String DISABLED_STATUS = "DISABLED";
    private static final String TARGET_TYPE_GROUP_ORDER = "GROUP_ORDER";
    private static final String ACTION_TYPE_CREATE_ORDER = "CREATE_ORDER";
    private static final Set<String> ORDER_TYPES = Set.of("TAKEOUT", "CANTEEN", "MILK_TEA", "MIDNIGHT_SNACK");

    private final AuthTokenUtil authTokenUtil;
    private final UserMapper userMapper;
    private final GroupOrderMapper groupOrderMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;

    @Override
    public PageResultVO<GroupOrderVO> listGroupOrders(
            String authorization,
            String status,
            String orderType,
            String keyword,
            Long pageNum,
            Long pageSize) {
        requireCurrentUser(authorization);
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
                        .like(GroupOrder::getMerchantName, normalizedKeyword))
                .orderByDesc(GroupOrder::getCreateTime);

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
        insertCreateStatusLog(order, creator.getId());

        return toVO(order, Map.of(creator.getId(), creator));
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

    private GroupOrderVO toVO(GroupOrder order, Map<Long, User> users) {
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
                toUserSummary(users.get(order.getPickupUserId())),
                order.getRemark(),
                DateTimeUtil.format(order.getLockedTime()),
                DateTimeUtil.format(order.getFinishTime()),
                DateTimeUtil.format(order.getCancelTime()),
                DateTimeUtil.format(order.getCreateTime()),
                DateTimeUtil.format(order.getUpdateTime())
        );
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
