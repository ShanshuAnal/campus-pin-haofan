package com.campus.pinhaofan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.pinhaofan.common.AuthTokenUtil;
import com.campus.pinhaofan.dto.CreateGroupOrderRequest;
import com.campus.pinhaofan.dto.JoinGroupOrderRequest;
import com.campus.pinhaofan.dto.LockGroupOrderRequest;
import com.campus.pinhaofan.dto.PaymentRequest;
import com.campus.pinhaofan.dto.PickupAssigneeRequest;
import com.campus.pinhaofan.dto.PickupStatusUpdateRequest;
import com.campus.pinhaofan.entity.GroupOrder;
import com.campus.pinhaofan.entity.MealItem;
import com.campus.pinhaofan.entity.OrderParticipant;
import com.campus.pinhaofan.entity.OrderStatusLog;
import com.campus.pinhaofan.entity.PaymentRecord;
import com.campus.pinhaofan.entity.PickupRecord;
import com.campus.pinhaofan.entity.User;
import com.campus.pinhaofan.enums.GroupOrderStatus;
import com.campus.pinhaofan.enums.PaymentStatus;
import com.campus.pinhaofan.enums.PickupStatus;
import com.campus.pinhaofan.exception.BusinessException;
import com.campus.pinhaofan.mapper.GroupOrderMapper;
import com.campus.pinhaofan.mapper.MealItemMapper;
import com.campus.pinhaofan.mapper.OrderParticipantMapper;
import com.campus.pinhaofan.mapper.OrderStatusLogMapper;
import com.campus.pinhaofan.mapper.PaymentRecordMapper;
import com.campus.pinhaofan.mapper.PickupRecordMapper;
import com.campus.pinhaofan.mapper.UserMapper;
import com.campus.pinhaofan.service.impl.GroupOrderServiceImpl;
import com.campus.pinhaofan.vo.GroupOrderDetailVO;
import com.campus.pinhaofan.vo.GroupOrderVO;
import com.campus.pinhaofan.vo.JoinGroupOrderVO;
import com.campus.pinhaofan.vo.LockAllocationVO;
import com.campus.pinhaofan.vo.LockGroupOrderVO;
import com.campus.pinhaofan.vo.DashboardSummaryVO;
import com.campus.pinhaofan.vo.MyGroupOrderVO;
import com.campus.pinhaofan.vo.PageResultVO;
import com.campus.pinhaofan.vo.PaymentActionVO;
import com.campus.pinhaofan.vo.PickupAssigneeVO;
import com.campus.pinhaofan.vo.PickupStatusUpdateVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupOrderServiceImplTest {

    private static final Long CREATOR_ID = 1001L;
    private static final Long MEMBER_ID = 1002L;
    private static final Long THIRD_USER_ID = 1003L;
    private static final Long ORDER_ID = 2001L;

    @Mock
    private UserMapper userMapper;
    @Mock
    private GroupOrderMapper groupOrderMapper;
    @Mock
    private OrderParticipantMapper orderParticipantMapper;
    @Mock
    private MealItemMapper mealItemMapper;
    @Mock
    private PickupRecordMapper pickupRecordMapper;
    @Mock
    private OrderStatusLogMapper orderStatusLogMapper;
    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    private AuthTokenUtil authTokenUtil;
    private GroupOrderService groupOrderService;

    @BeforeEach
    void setUp() {
        authTokenUtil = new AuthTokenUtil("unit-test-secret", 24);
        groupOrderService = new GroupOrderServiceImpl(
                authTokenUtil,
                userMapper,
                groupOrderMapper,
                orderParticipantMapper,
                mealItemMapper,
                pickupRecordMapper,
                orderStatusLogMapper,
                paymentRecordMapper
        );
    }

    @Test
    void listGroupOrdersReturnsPagedRecordsForAuthenticatedUser() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setCreateTime(LocalDateTime.of(2026, 5, 27, 18, 0));
        order.setUpdateTime(LocalDateTime.of(2026, 5, 27, 18, 1));

        Page<GroupOrder> page = Page.of(1, 10);
        page.setRecords(List.of(order));
        page.setTotal(1);

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectPage(any(), any())).thenReturn(page);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(creator));

        PageResultVO<GroupOrderVO> result = groupOrderService.listGroupOrders(
                authorization(CREATOR_ID),
                null,
                null,
                null,
                null,
                null
        );

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().getFirst().getId()).isEqualTo(ORDER_ID);
        assertThat(result.getRecords().getFirst().getStatus()).isEqualTo("CREATED");
        assertThat(result.getRecords().getFirst().getCreator().getUsername()).isEqualTo("20260001");

        ArgumentCaptor<LambdaQueryWrapper<GroupOrder>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(groupOrderMapper).selectPage(any(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getCustomSqlSegment())
                .contains("CASE WHEN status IN ('CREATED','LOCKED','ORDERED','DELIVERING','ARRIVED','PICKED_UP')")
                .contains("create_time DESC");
    }

    @Test
    void listGroupOrdersDefaultSortsUnfinishedOrdersFirstThenCreateTimeDesc() {
        when(userMapper.selectById(CREATOR_ID)).thenReturn(user(CREATOR_ID, "20260001", "小何"));
        Page<GroupOrder> page = Page.of(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(groupOrderMapper.selectPage(any(), any())).thenReturn(page);

        groupOrderService.listGroupOrders(
                authorization(CREATOR_ID),
                null,
                null,
                null,
                null,
                null
        );

        ArgumentCaptor<LambdaQueryWrapper<GroupOrder>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(groupOrderMapper).selectPage(any(), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getCustomSqlSegment();
        assertThat(sqlSegment)
                .contains("CASE WHEN status IN ('CREATED','LOCKED','ORDERED','DELIVERING','ARRIVED','PICKED_UP')")
                .contains("THEN 0 ELSE 1 END ASC")
                .contains("create_time DESC");
    }

    @Test
    void listGroupOrdersRejectsInvalidStatus() {
        when(userMapper.selectById(CREATOR_ID)).thenReturn(user(CREATOR_ID, "20260001", "小何"));

        assertThatThrownBy(() -> groupOrderService.listGroupOrders(
                authorization(CREATOR_ID),
                "UNKNOWN",
                null,
                null,
                1L,
                10L
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(400);
            assertThat(exception.getMessage()).isEqualTo("status 不合法");
        });
    }

    @Test
    void createGroupOrderUsesCurrentUserAndInitialCreatedState() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        doAnswer(invocation -> {
            GroupOrder order = invocation.getArgument(0);
            order.setId(ORDER_ID);
            return 1;
        }).when(groupOrderMapper).insert(any(GroupOrder.class));

        GroupOrderVO result = groupOrderService.createGroupOrder(
                authorization(CREATOR_ID),
                createOrderRequest("2099-05-27 18:30:00")
        );

        assertThat(result.getId()).isEqualTo(ORDER_ID);
        assertThat(result.getCreator().getId()).isEqualTo(CREATOR_ID);
        assertThat(result.getStatus()).isEqualTo("CREATED");
        assertThat(result.getParticipantCount()).isZero();
        assertThat(result.getOriginalTotalAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getPayableTotalAmount()).isEqualByComparingTo("0.00");

        ArgumentCaptor<GroupOrder> orderCaptor = ArgumentCaptor.forClass(GroupOrder.class);
        verify(groupOrderMapper).insert(orderCaptor.capture());
        GroupOrder insertedOrder = orderCaptor.getValue();
        assertThat(insertedOrder.getCreatorId()).isEqualTo(CREATOR_ID);
        assertThat(insertedOrder.getPickupLocation()).isEqualTo("一教大厅门口");
        assertThat(insertedOrder.getStatus()).isEqualTo(GroupOrderStatus.CREATED.getValue());

        ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
        verify(orderStatusLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getAfterStatus()).isEqualTo("CREATED");
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("CREATE_ORDER");
    }

    @Test
    void getGroupOrderDetailReturnsParticipantsMealsAndPickupStatus() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());
        order.setPickupUserId(MEMBER_ID);
        order.setParticipantCount(2);

        OrderParticipant creatorParticipant = participant(3001L, CREATOR_ID, "10.00");
        OrderParticipant memberParticipant = participant(3002L, MEMBER_ID, "20.00");
        MealItem creatorMeal = mealItem(4001L, 3001L, "饭团", "10.00");
        MealItem memberMeal = mealItem(4002L, 3002L, "奶茶", "20.00");
        PickupRecord pickupRecord = new PickupRecord();
        pickupRecord.setId(6001L);
        pickupRecord.setGroupOrderId(ORDER_ID);
        pickupRecord.setPickupUserId(MEMBER_ID);
        pickupRecord.setPickupLocation("一教大厅门口");
        pickupRecord.setPickupStatus("WAITING_ORDER");

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectList(any())).thenReturn(List.of(creatorParticipant, memberParticipant));
        when(mealItemMapper.selectList(any())).thenReturn(List.of(creatorMeal, memberMeal));
        when(pickupRecordMapper.selectOne(any())).thenReturn(pickupRecord);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(creator, member));

        GroupOrderDetailVO result = groupOrderService.getGroupOrderDetail(authorization(CREATOR_ID), ORDER_ID);

        assertThat(result.getOrder().getId()).isEqualTo(ORDER_ID);
        assertThat(result.getParticipants()).hasSize(2);
        assertThat(result.getParticipants().getFirst().getMealItems()).hasSize(1);
        assertThat(result.getParticipants().get(1).getUser().getUsername()).isEqualTo("20260002");
        assertThat(result.getPickupRecord().getPickupUser().getId()).isEqualTo(MEMBER_ID);
        assertThat(result.getPickupStatus()).isEqualTo("WAITING_ORDER");
    }

    @Test
    void joinGroupOrderCreatesParticipantMealAndUpdatesAmount() {
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();
        order.setParticipantCount(0);
        order.setMaxParticipants(2);

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            OrderParticipant participant = invocation.getArgument(0);
            participant.setId(3001L);
            return 1;
        }).when(orderParticipantMapper).insert(any(OrderParticipant.class));
        doAnswer(invocation -> {
            MealItem mealItem = invocation.getArgument(0);
            mealItem.setId(4001L);
            return 1;
        }).when(mealItemMapper).insert(any(MealItem.class));

        JoinGroupOrderVO result = groupOrderService.joinGroupOrder(
                authorization(MEMBER_ID),
                ORDER_ID,
                joinRequest("珍珠奶茶", 2, "22.00")
        );

        assertThat(result.getParticipant().getId()).isEqualTo(3001L);
        assertThat(result.getParticipant().getOriginalAmount()).isEqualByComparingTo("44.00");
        assertThat(result.getParticipant().getPayableAmount()).isEqualByComparingTo("44.00");
        assertThat(result.getParticipant().getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(result.getOrderAmount().getParticipantCount()).isEqualTo(1);
        assertThat(result.getOrderAmount().getOriginalTotalAmount()).isEqualByComparingTo("44.00");
        assertThat(result.getOrderAmount().getDiscountReached()).isFalse();
        assertThat(result.getOrderAmount().getDiscountGapAmount()).isEqualByComparingTo("16.00");

        ArgumentCaptor<GroupOrder> orderCaptor = ArgumentCaptor.forClass(GroupOrder.class);
        verify(groupOrderMapper).updateById(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getParticipantCount()).isEqualTo(1);
        assertThat(orderCaptor.getValue().getPayableTotalAmount()).isEqualByComparingTo("44.00");
    }

    @Test
    void joinGroupOrderRejectsDuplicateJoin() {
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> groupOrderService.joinGroupOrder(
                authorization(MEMBER_ID),
                ORDER_ID,
                joinRequest("珍珠奶茶", 1, "22.00")
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("不能重复加入同一拼单");
        });

        verify(orderParticipantMapper, never()).insert(any(OrderParticipant.class));
    }

    @Test
    void joinGroupOrderRejectsNonCreatedOrder() {
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        assertThatThrownBy(() -> groupOrderService.joinGroupOrder(
                authorization(MEMBER_ID),
                ORDER_ID,
                joinRequest("珍珠奶茶", 1, "22.00")
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(400);
            assertThat(exception.getMessage()).isEqualTo("拼单已锁定，不能继续加入");
        });
    }

    @Test
    void lockGroupOrderAllocatesDiscountAndCreatorRounding() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setDiscountThresholdAmount(new BigDecimal("30.00"));
        order.setDiscountAmount(new BigDecimal("10.00"));

        OrderParticipant creatorParticipant = participant(3001L, CREATOR_ID, "0.00");
        OrderParticipant memberParticipant = participant(3002L, MEMBER_ID, "0.00");
        OrderParticipant thirdParticipant = participant(3003L, 1003L, "0.00");
        List<OrderParticipant> participants = List.of(creatorParticipant, memberParticipant, thirdParticipant);
        List<MealItem> mealItems = List.of(
                mealItem(4001L, 3001L, "饭团", "10.00"),
                mealItem(4002L, 3002L, "奶茶", "10.00"),
                mealItem(4003L, 3003L, "夜宵", "10.00")
        );

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectList(any())).thenReturn(participants);
        when(mealItemMapper.selectList(any())).thenReturn(mealItems);

        LockGroupOrderRequest request = new LockGroupOrderRequest();
        request.setRemark("人数已够，准备下单");
        LockGroupOrderVO result = groupOrderService.lockGroupOrder(authorization(CREATOR_ID), ORDER_ID, request);

        assertThat(result.getOrder().getStatus()).isEqualTo("LOCKED");
        assertThat(result.getOrder().getOriginalTotalAmount()).isEqualByComparingTo("30.00");
        assertThat(result.getOrder().getActualDiscountAmount()).isEqualByComparingTo("10.00");
        assertThat(result.getOrder().getPayableTotalAmount()).isEqualByComparingTo("20.00");
        assertThat(result.getOrder().getRoundingAdjustmentAmount()).isEqualByComparingTo("-0.01");

        BigDecimal allocationSum = result.getAllocations().stream()
                .map(LockAllocationVO::getPayableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(allocationSum).isEqualByComparingTo("20.00");
        assertThat(result.getAllocations().getFirst().getPayableAmount()).isEqualByComparingTo("6.66");
        assertThat(result.getAllocations().getFirst().getRoundingAdjustmentAmount()).isEqualByComparingTo("-0.01");
        assertThat(result.getAllocations().get(1).getPayableAmount()).isEqualByComparingTo("6.67");
        assertThat(result.getAllocations().get(2).getPayableAmount()).isEqualByComparingTo("6.67");

        ArgumentCaptor<GroupOrder> orderCaptor = ArgumentCaptor.forClass(GroupOrder.class);
        verify(groupOrderMapper).updateById(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("LOCKED");
        assertThat(orderCaptor.getValue().getParticipantCount()).isEqualTo(3);
        assertThat(orderCaptor.getValue().getRoundingAdjustmentAmount()).isEqualByComparingTo("-0.01");

        ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
        verify(orderStatusLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getBeforeStatus()).isEqualTo("CREATED");
        assertThat(logCaptor.getValue().getAfterStatus()).isEqualTo("LOCKED");
        assertThat(logCaptor.getValue().getRemark()).isEqualTo("人数已够，准备下单");
    }

    @Test
    void lockGroupOrderKeepsDiscountZeroWhenThresholdNotReached() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setDiscountThresholdAmount(new BigDecimal("100.00"));
        order.setDiscountAmount(new BigDecimal("10.00"));

        OrderParticipant creatorParticipant = participant(3001L, CREATOR_ID, "0.00");
        OrderParticipant memberParticipant = participant(3002L, MEMBER_ID, "0.00");
        List<OrderParticipant> participants = List.of(creatorParticipant, memberParticipant);
        List<MealItem> mealItems = List.of(
                mealItem(4001L, 3001L, "饭团", "10.00"),
                mealItem(4002L, 3002L, "奶茶", "20.00")
        );

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectList(any())).thenReturn(participants);
        when(mealItemMapper.selectList(any())).thenReturn(mealItems);

        LockGroupOrderVO result = groupOrderService.lockGroupOrder(authorization(CREATOR_ID), ORDER_ID, null);

        assertThat(result.getOrder().getStatus()).isEqualTo("LOCKED");
        assertThat(result.getOrder().getOriginalTotalAmount()).isEqualByComparingTo("30.00");
        assertThat(result.getOrder().getActualDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getOrder().getPayableTotalAmount()).isEqualByComparingTo("30.00");
        assertThat(result.getOrder().getRoundingAdjustmentAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getAllocations().getFirst().getDiscountShareAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getAllocations().getFirst().getPayableAmount()).isEqualByComparingTo("10.00");
        assertThat(result.getAllocations().get(1).getDiscountShareAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getAllocations().get(1).getPayableAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void lockGroupOrderRejectsNonCreator() {
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        assertThatThrownBy(() -> groupOrderService.lockGroupOrder(
                authorization(MEMBER_ID),
                ORDER_ID,
                null
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(403);
            assertThat(exception.getMessage()).isEqualTo("只有发起人可以锁单");
        });
    }

    @Test
    void lockGroupOrderRejectsLockedState() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        assertThatThrownBy(() -> groupOrderService.lockGroupOrder(
                authorization(CREATOR_ID),
                ORDER_ID,
                null
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("当前状态不能锁单");
        });
    }

    @Test
    void lockGroupOrderRequiresCreatorParticipantForRounding() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectList(any()))
                .thenReturn(List.of(participant(3002L, MEMBER_ID, "20.00")));

        assertThatThrownBy(() -> groupOrderService.lockGroupOrder(
                authorization(CREATOR_ID),
                ORDER_ID,
                null
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(400);
            assertThat(exception.getMessage()).isEqualTo("发起人需先加入拼单以承接尾差");
        });
    }

    @Test
    void markParticipantPaidUpdatesStatusAndCreatesPaymentRecord() {
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());
        OrderParticipant participant = participant(3002L, MEMBER_ID, "18.76");

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectById(3002L)).thenReturn(participant);
        doAnswer(invocation -> {
            PaymentRecord record = invocation.getArgument(0);
            record.setId(5001L);
            return 1;
        }).when(paymentRecordMapper).insert(any(PaymentRecord.class));

        PaymentRequest request = new PaymentRequest();
        request.setRemark("已微信转账");
        PaymentActionVO result = groupOrderService.markParticipantPaid(
                authorization(MEMBER_ID),
                ORDER_ID,
                3002L,
                request
        );

        assertThat(result.getParticipantId()).isEqualTo(3002L);
        assertThat(result.getPaymentStatus()).isEqualTo("PAID");
        assertThat(result.getPaidMarkTime()).isNotNull();
        assertThat(result.getPaymentRecord().getId()).isEqualTo(5001L);
        assertThat(result.getPaymentRecord().getAmount()).isEqualByComparingTo("18.76");
        assertThat(result.getPaymentRecord().getPaymentStatus()).isEqualTo("PAID");

        ArgumentCaptor<OrderParticipant> participantCaptor = ArgumentCaptor.forClass(OrderParticipant.class);
        verify(orderParticipantMapper).updateById(participantCaptor.capture());
        assertThat(participantCaptor.getValue().getPaymentStatus()).isEqualTo("PAID");

        ArgumentCaptor<PaymentRecord> recordCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRecordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getUserId()).isEqualTo(MEMBER_ID);
        assertThat(recordCaptor.getValue().getConfirmUserId()).isNull();
    }

    @Test
    void markParticipantPaidRejectsOtherParticipant() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());
        OrderParticipant participant = participant(3002L, MEMBER_ID, "18.76");

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectById(3002L)).thenReturn(participant);

        assertThatThrownBy(() -> groupOrderService.markParticipantPaid(
                authorization(CREATOR_ID),
                ORDER_ID,
                3002L,
                null
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(403);
            assertThat(exception.getMessage()).isEqualTo("只能标记自己的付款");
        });

        verify(orderParticipantMapper, never()).updateById(any(OrderParticipant.class));
        verify(paymentRecordMapper, never()).insert(any(PaymentRecord.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CREATED", "CANCELLED", "FINISHED"})
    void markParticipantPaidRejectsInvalidOrderStatuses(String status) {
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();
        order.setStatus(status);

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        assertThatThrownBy(() -> groupOrderService.markParticipantPaid(
                authorization(MEMBER_ID),
                ORDER_ID,
                3002L,
                null
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(400);
            assertThat(exception.getMessage()).isEqualTo("拼单未锁定，暂不能标记付款");
        });

        verify(orderParticipantMapper, never()).selectById(3002L);
        verify(orderParticipantMapper, never()).updateById(any(OrderParticipant.class));
        verify(paymentRecordMapper, never()).insert(any(PaymentRecord.class));
    }

    @Test
    void markParticipantPaidRejectsDuplicatePaidWithoutDirtyRecord() {
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());
        OrderParticipant participant = participant(3002L, MEMBER_ID, "18.76");
        participant.setPaymentStatus(PaymentStatus.PAID.getValue());

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectById(3002L)).thenReturn(participant);

        assertThatThrownBy(() -> groupOrderService.markParticipantPaid(
                authorization(MEMBER_ID),
                ORDER_ID,
                3002L,
                null
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("已标记付款，不能重复标记");
        });

        verify(orderParticipantMapper, never()).updateById(any(OrderParticipant.class));
        verify(paymentRecordMapper, never()).insert(any(PaymentRecord.class));
    }

    @Test
    void confirmParticipantPaymentRequiresCreatorAndPaidStatus() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());
        OrderParticipant participant = participant(3002L, MEMBER_ID, "18.76");
        participant.setPaymentStatus(PaymentStatus.PAID.getValue());
        participant.setPaidMarkTime(LocalDateTime.now().minusMinutes(5));

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectById(3002L)).thenReturn(participant);
        doAnswer(invocation -> {
            PaymentRecord record = invocation.getArgument(0);
            record.setId(5002L);
            return 1;
        }).when(paymentRecordMapper).insert(any(PaymentRecord.class));

        PaymentRequest request = new PaymentRequest();
        request.setRemark("金额已核对");
        PaymentActionVO result = groupOrderService.confirmParticipantPayment(
                authorization(CREATOR_ID),
                ORDER_ID,
                3002L,
                request
        );

        assertThat(result.getParticipantId()).isEqualTo(3002L);
        assertThat(result.getPaymentStatus()).isEqualTo("CONFIRMED");
        assertThat(result.getPaidConfirmTime()).isNotNull();
        assertThat(result.getPaymentRecord().getId()).isEqualTo(5002L);
        assertThat(result.getPaymentRecord().getConfirmUserId()).isEqualTo(CREATOR_ID);
        assertThat(result.getPaymentRecord().getPaymentStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void confirmParticipantPaymentRejectsNonCreator() {
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());
        OrderParticipant participant = participant(3002L, MEMBER_ID, "18.76");
        participant.setPaymentStatus(PaymentStatus.PAID.getValue());
        participant.setPaidMarkTime(LocalDateTime.now().minusMinutes(5));

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectById(3002L)).thenReturn(participant);

        assertThatThrownBy(() -> groupOrderService.confirmParticipantPayment(
                authorization(MEMBER_ID),
                ORDER_ID,
                3002L,
                null
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(403);
            assertThat(exception.getMessage()).isEqualTo("只有发起人可以确认付款");
        });

        verify(orderParticipantMapper, never()).updateById(any(OrderParticipant.class));
        verify(paymentRecordMapper, never()).insert(any(PaymentRecord.class));
    }

    @Test
    void confirmParticipantPaymentRejectsUnpaidParticipant() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());
        OrderParticipant participant = participant(3002L, MEMBER_ID, "18.76");

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectById(3002L)).thenReturn(participant);

        assertThatThrownBy(() -> groupOrderService.confirmParticipantPayment(
                authorization(CREATOR_ID),
                ORDER_ID,
                3002L,
                null
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(400);
            assertThat(exception.getMessage()).isEqualTo("成员尚未标记付款");
        });

        verify(orderParticipantMapper, never()).updateById(any(OrderParticipant.class));
        verify(paymentRecordMapper, never()).insert(any(PaymentRecord.class));
    }

    @Test
    void confirmParticipantPaymentRejectsDuplicateConfirmedWithoutDirtyRecord() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());
        OrderParticipant participant = participant(3002L, MEMBER_ID, "18.76");
        participant.setPaymentStatus(PaymentStatus.CONFIRMED.getValue());
        participant.setPaidMarkTime(LocalDateTime.now().minusMinutes(10));
        participant.setPaidConfirmTime(LocalDateTime.now().minusMinutes(5));

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectById(3002L)).thenReturn(participant);

        assertThatThrownBy(() -> groupOrderService.confirmParticipantPayment(
                authorization(CREATOR_ID),
                ORDER_ID,
                3002L,
                null
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("付款已确认");
        });

        verify(orderParticipantMapper, never()).updateById(any(OrderParticipant.class));
        verify(paymentRecordMapper, never()).insert(any(PaymentRecord.class));
    }

    @Test
    void assignPickupUserRequiresCreatorAndMovesLockedOrderToOrdered() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectCount(any())).thenReturn(1L);
        when(pickupRecordMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            PickupRecord record = invocation.getArgument(0);
            record.setId(6001L);
            return 1;
        }).when(pickupRecordMapper).insert(any(PickupRecord.class));

        PickupAssigneeRequest request = new PickupAssigneeRequest();
        request.setPickupUserId(MEMBER_ID);
        request.setPickupLocation("宿舍楼下");
        request.setEstimatedArrivalTime("2099-05-27 19:10:00");
        request.setRemark("小林去取");

        PickupAssigneeVO result = groupOrderService.assignPickupUser(
                authorization(CREATOR_ID),
                ORDER_ID,
                request
        );

        assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(result.getPickupUserId()).isEqualTo(MEMBER_ID);
        assertThat(result.getPickupRecord().getPickupStatus()).isEqualTo("WAITING_ORDER");
        assertThat(result.getPickupRecord().getPickupUser().getId()).isEqualTo(MEMBER_ID);

        ArgumentCaptor<GroupOrder> orderCaptor = ArgumentCaptor.forClass(GroupOrder.class);
        verify(groupOrderMapper).updateById(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getPickupUserId()).isEqualTo(MEMBER_ID);
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("ORDERED");

        ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
        verify(orderStatusLogMapper, times(2)).insert(logCaptor.capture());
        assertThat(logCaptor.getAllValues())
                .extracting(OrderStatusLog::getActionType)
                .containsExactly("ASSIGN_PICKUP", "UPDATE_ORDER_STATUS");
    }

    @Test
    void assignPickupUserRejectsNonCreator() {
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        PickupAssigneeRequest request = new PickupAssigneeRequest();
        request.setPickupUserId(MEMBER_ID);

        assertThatThrownBy(() -> groupOrderService.assignPickupUser(
                authorization(MEMBER_ID),
                ORDER_ID,
                request
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(403);
            assertThat(exception.getMessage()).isEqualTo("只有发起人可以指定取餐人");
        });

        verify(pickupRecordMapper, never()).insert(any(PickupRecord.class));
        verify(pickupRecordMapper, never()).updateById(any(PickupRecord.class));
        verify(groupOrderMapper, never()).updateById(any(GroupOrder.class));
        verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
    }

    @Test
    void assignPickupUserRejectsUserOutsideOrder() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        User outsider = user(THIRD_USER_ID, "20260003", "小周");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(userMapper.selectById(THIRD_USER_ID)).thenReturn(outsider);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectCount(any())).thenReturn(0L);

        PickupAssigneeRequest request = new PickupAssigneeRequest();
        request.setPickupUserId(THIRD_USER_ID);

        assertThatThrownBy(() -> groupOrderService.assignPickupUser(
                authorization(CREATOR_ID),
                ORDER_ID,
                request
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(400);
            assertThat(exception.getMessage()).isEqualTo("取餐人必须是拼单参与者或发起人");
        });

        verify(pickupRecordMapper, never()).insert(any(PickupRecord.class));
        verify(pickupRecordMapper, never()).updateById(any(PickupRecord.class));
        verify(groupOrderMapper, never()).updateById(any(GroupOrder.class));
        verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
    }

    @Test
    void updatePickupStatusMovesInOrderAndSyncsGroupOrderStatus() {
        User member = user(MEMBER_ID, "20260002", "小林");
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.ORDERED.getValue());
        order.setPickupUserId(MEMBER_ID);
        PickupRecord pickupRecord = pickupRecord(PickupStatus.WAITING_ORDER.getValue());

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(pickupRecordMapper.selectOne(any())).thenReturn(pickupRecord);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(creator, member));

        PickupStatusUpdateRequest request = new PickupStatusUpdateRequest();
        request.setPickupStatus(PickupStatus.WAITING_DELIVERY.getValue());
        request.setRemark("已下单，等待配送");

        PickupStatusUpdateVO result = groupOrderService.updatePickupStatus(
                authorization(MEMBER_ID),
                ORDER_ID,
                request
        );

        assertThat(result.getPickupRecord().getPickupStatus()).isEqualTo("WAITING_DELIVERY");
        assertThat(result.getOrderStatus()).isEqualTo("DELIVERING");

        ArgumentCaptor<PickupRecord> pickupCaptor = ArgumentCaptor.forClass(PickupRecord.class);
        verify(pickupRecordMapper).updateById(pickupCaptor.capture());
        assertThat(pickupCaptor.getValue().getPickupStatus()).isEqualTo("WAITING_DELIVERY");

        ArgumentCaptor<GroupOrder> orderCaptor = ArgumentCaptor.forClass(GroupOrder.class);
        verify(groupOrderMapper).updateById(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("DELIVERING");

        ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
        verify(orderStatusLogMapper, times(2)).insert(logCaptor.capture());
        List<OrderStatusLog> logs = logCaptor.getAllValues();
        assertThat(logs.get(0).getActionType()).isEqualTo("UPDATE_PICKUP_STATUS");
        assertThat(logs.get(0).getBeforeStatus()).isEqualTo("WAITING_ORDER");
        assertThat(logs.get(0).getAfterStatus()).isEqualTo("WAITING_DELIVERY");
        assertThat(logs.get(1).getActionType()).isEqualTo("UPDATE_ORDER_STATUS");
        assertThat(logs.get(1).getBeforeStatus()).isEqualTo("ORDERED");
        assertThat(logs.get(1).getAfterStatus()).isEqualTo("DELIVERING");
    }

    @Test
    void updatePickupStatusAllowsCreatorAndSyncsArrivedStatus() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.DELIVERING.getValue());
        order.setPickupUserId(MEMBER_ID);
        PickupRecord pickupRecord = pickupRecord(PickupStatus.WAITING_DELIVERY.getValue());

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(pickupRecordMapper.selectOne(any())).thenReturn(pickupRecord);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(creator, member));

        PickupStatusUpdateRequest request = new PickupStatusUpdateRequest();
        request.setPickupStatus(PickupStatus.ARRIVED.getValue());
        request.setActualArrivalTime("2099-05-27 19:20:00");
        request.setRemark("已到达取餐点");

        PickupStatusUpdateVO result = groupOrderService.updatePickupStatus(
                authorization(CREATOR_ID),
                ORDER_ID,
                request
        );

        assertThat(result.getPickupRecord().getPickupStatus()).isEqualTo("ARRIVED");
        assertThat(result.getOrderStatus()).isEqualTo("ARRIVED");

        ArgumentCaptor<PickupRecord> pickupCaptor = ArgumentCaptor.forClass(PickupRecord.class);
        verify(pickupRecordMapper).updateById(pickupCaptor.capture());
        assertThat(pickupCaptor.getValue().getPickupStatus()).isEqualTo("ARRIVED");
        assertThat(pickupCaptor.getValue().getActualArrivalTime()).isEqualTo(LocalDateTime.of(2099, 5, 27, 19, 20));

        ArgumentCaptor<GroupOrder> orderCaptor = ArgumentCaptor.forClass(GroupOrder.class);
        verify(groupOrderMapper).updateById(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("ARRIVED");
    }

    @Test
    void updatePickupStatusRejectsNormalParticipant() {
        User thirdUser = user(THIRD_USER_ID, "20260003", "小周");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.ORDERED.getValue());
        order.setPickupUserId(MEMBER_ID);
        PickupRecord pickupRecord = pickupRecord(PickupStatus.WAITING_ORDER.getValue());

        when(userMapper.selectById(THIRD_USER_ID)).thenReturn(thirdUser);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(pickupRecordMapper.selectOne(any())).thenReturn(pickupRecord);

        PickupStatusUpdateRequest request = new PickupStatusUpdateRequest();
        request.setPickupStatus(PickupStatus.WAITING_DELIVERY.getValue());

        assertThatThrownBy(() -> groupOrderService.updatePickupStatus(
                authorization(THIRD_USER_ID),
                ORDER_ID,
                request
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(403);
            assertThat(exception.getMessage()).isEqualTo("无权更新取餐状态");
        });

        verify(pickupRecordMapper, never()).updateById(any(PickupRecord.class));
        verify(groupOrderMapper, never()).updateById(any(GroupOrder.class));
        verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
    }

    @Test
    void updatePickupStatusDistributedFinishesOrder() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.PICKED_UP.getValue());
        order.setPickupUserId(MEMBER_ID);
        PickupRecord pickupRecord = pickupRecord(PickupStatus.PICKED_UP.getValue());

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(pickupRecordMapper.selectOne(any())).thenReturn(pickupRecord);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(creator, member));

        PickupStatusUpdateRequest request = new PickupStatusUpdateRequest();
        request.setPickupStatus(PickupStatus.DISTRIBUTED.getValue());
        request.setRemark("已分发完成");

        PickupStatusUpdateVO result = groupOrderService.updatePickupStatus(
                authorization(CREATOR_ID),
                ORDER_ID,
                request
        );

        assertThat(result.getPickupRecord().getPickupStatus()).isEqualTo("DISTRIBUTED");
        assertThat(result.getOrderStatus()).isEqualTo("FINISHED");

        ArgumentCaptor<GroupOrder> orderCaptor = ArgumentCaptor.forClass(GroupOrder.class);
        verify(groupOrderMapper).updateById(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("FINISHED");
        assertThat(orderCaptor.getValue().getFinishTime()).isNotNull();

        ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
        verify(orderStatusLogMapper, times(2)).insert(logCaptor.capture());
        List<OrderStatusLog> logs = logCaptor.getAllValues();
        assertThat(logs.get(0).getActionType()).isEqualTo("UPDATE_PICKUP_STATUS");
        assertThat(logs.get(0).getBeforeStatus()).isEqualTo("PICKED_UP");
        assertThat(logs.get(0).getAfterStatus()).isEqualTo("DISTRIBUTED");
        assertThat(logs.get(1).getActionType()).isEqualTo("UPDATE_ORDER_STATUS");
        assertThat(logs.get(1).getBeforeStatus()).isEqualTo("PICKED_UP");
        assertThat(logs.get(1).getAfterStatus()).isEqualTo("FINISHED");
    }

    @Test
    void updatePickupStatusRejectsSkippedTransition() {
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.ORDERED.getValue());
        order.setPickupUserId(MEMBER_ID);
        PickupRecord pickupRecord = pickupRecord(PickupStatus.WAITING_ORDER.getValue());

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(pickupRecordMapper.selectOne(any())).thenReturn(pickupRecord);

        PickupStatusUpdateRequest request = new PickupStatusUpdateRequest();
        request.setPickupStatus(PickupStatus.ARRIVED.getValue());

        assertThatThrownBy(() -> groupOrderService.updatePickupStatus(
                authorization(MEMBER_ID),
                ORDER_ID,
                request
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(400);
            assertThat(exception.getMessage()).isEqualTo("非法取餐状态流转");
        });

        verify(pickupRecordMapper, never()).updateById(any(PickupRecord.class));
        verify(groupOrderMapper, never()).updateById(any(GroupOrder.class));
        verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
    }

    @Test
    void updatePickupStatusRejectsFinishedOrderWithoutDirtyWrite() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.FINISHED.getValue());
        order.setPickupUserId(MEMBER_ID);

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        PickupStatusUpdateRequest request = new PickupStatusUpdateRequest();
        request.setPickupStatus(PickupStatus.DISTRIBUTED.getValue());

        assertThatThrownBy(() -> groupOrderService.updatePickupStatus(
                authorization(CREATOR_ID),
                ORDER_ID,
                request
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("拼单已取消或已完成");
        });

        verify(pickupRecordMapper, never()).selectOne(any());
        verify(pickupRecordMapper, never()).updateById(any(PickupRecord.class));
        verify(groupOrderMapper, never()).updateById(any(GroupOrder.class));
        verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
    }

    @Test
    void listMyGroupOrdersReturnsPendingPaymentOrdersForCurrentUser() {
        User member = user(MEMBER_ID, "20260002", "小林");
        User creator = user(CREATOR_ID, "20260001", "小何");
        OrderParticipant participant = participant(3002L, MEMBER_ID, "18.76");
        participant.setPaymentStatus(PaymentStatus.UNPAID.getValue());

        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());
        order.setCreatorId(CREATOR_ID);
        order.setCreateTime(LocalDateTime.of(2026, 5, 28, 12, 0));

        Page<GroupOrder> page = Page.of(1, 10);
        page.setRecords(List.of(order));
        page.setTotal(1);

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(orderParticipantMapper.selectList(any())).thenReturn(List.of(participant));
        when(groupOrderMapper.selectPage(any(), any())).thenReturn(page);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(creator));
        when(pickupRecordMapper.selectList(any())).thenReturn(List.of());

        PageResultVO<MyGroupOrderVO> result = groupOrderService.listMyGroupOrders(
                authorization(MEMBER_ID),
                "PENDING_PAYMENT",
                null,
                null,
                null
        );

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().getFirst().getMyRole()).isEqualTo("PARTICIPANT");
        assertThat(result.getRecords().getFirst().getMyParticipantId()).isEqualTo(3002L);
        assertThat(result.getRecords().getFirst().getMyPaymentStatus()).isEqualTo("UNPAID");
        assertThat(result.getRecords().getFirst().getMyPayableAmount()).isEqualByComparingTo("18.76");
    }

    @Test
    void listMyGroupOrdersRejectsInvalidScope() {
        User member = user(MEMBER_ID, "20260002", "小林");
        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);

        assertThatThrownBy(() -> groupOrderService.listMyGroupOrders(
                authorization(MEMBER_ID),
                "UNKNOWN_SCOPE",
                null,
                null,
                null
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(400);
            assertThat(exception.getMessage()).isEqualTo("scope 不合法");
        });

        verify(groupOrderMapper, never()).selectPage(any(), any());
    }

    @Test
    void getDashboardSummaryAggregatesCurrentUserRelatedOrders() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        OrderParticipant myParticipant = participant(3001L, CREATOR_ID, "30.00");
        myParticipant.setPaymentStatus(PaymentStatus.CONFIRMED.getValue());

        GroupOrder todayFinished = baseOrder();
        todayFinished.setId(2001L);
        todayFinished.setStatus(GroupOrderStatus.FINISHED.getValue());
        todayFinished.setOrderType("MILK_TEA");
        todayFinished.setMerchantName("一号门奶茶");
        todayFinished.setParticipantCount(2);
        todayFinished.setOriginalTotalAmount(new BigDecimal("60.00"));
        todayFinished.setActualDiscountAmount(new BigDecimal("10.00"));
        todayFinished.setPayableTotalAmount(new BigDecimal("50.00"));
        todayFinished.setCreateTime(LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0));

        GroupOrder locked = baseOrder();
        locked.setId(2002L);
        locked.setStatus(GroupOrderStatus.LOCKED.getValue());
        locked.setOrderType("MILK_TEA");
        locked.setMerchantName("一号门奶茶");
        locked.setParticipantCount(1);
        locked.setOriginalTotalAmount(new BigDecimal("40.00"));
        locked.setActualDiscountAmount(new BigDecimal("5.00"));
        locked.setPayableTotalAmount(new BigDecimal("35.00"));
        locked.setCreateTime(LocalDateTime.now().minusDays(1));

        OrderParticipant paid = participant(3002L, MEMBER_ID, "20.00");
        paid.setGroupOrderId(2001L);
        paid.setPaymentStatus(PaymentStatus.PAID.getValue());
        OrderParticipant confirmed = participant(3003L, CREATOR_ID, "30.00");
        confirmed.setGroupOrderId(2002L);
        confirmed.setPaymentStatus(PaymentStatus.CONFIRMED.getValue());

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(orderParticipantMapper.selectList(any()))
                .thenReturn(List.of(myParticipant))
                .thenReturn(List.of(paid, confirmed));
        when(groupOrderMapper.selectList(any())).thenReturn(List.of(todayFinished, locked));

        DashboardSummaryVO result = groupOrderService.getDashboardSummary(
                authorization(CREATOR_ID),
                null,
                null,
                "MINE"
        );

        assertThat(result.getTodayOrderCount()).isEqualTo(1);
        assertThat(result.getSuccessOrderCount()).isEqualTo(1);
        assertThat(result.getOrderCount()).isEqualTo(2);
        assertThat(result.getLockedCount()).isEqualTo(1);
        assertThat(result.getFinishedCount()).isEqualTo(1);
        assertThat(result.getTotalSavedAmount()).isEqualByComparingTo("15.00");
        assertThat(result.getOriginalTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(result.getPayableTotalAmount()).isEqualByComparingTo("85.00");
        assertThat(result.getPaidParticipantCount()).isEqualTo(1);
        assertThat(result.getConfirmedParticipantCount()).isEqualTo(1);
        assertThat(result.getPopularTypes().getFirst().getName()).isEqualTo("MILK_TEA");
        assertThat(result.getPopularTypes().getFirst().getCount()).isEqualTo(2);
        assertThat(result.getPopularMerchants().getFirst().getName()).isEqualTo("一号门奶茶");
    }

    private String authorization(Long userId) {
        return "Bearer " + authTokenUtil.createToken(userId);
    }

    private User user(Long id, String account, String nickname) {
        User user = new User();
        user.setId(id);
        user.setAccount(account);
        user.setNickname(nickname);
        user.setStatus("ACTIVE");
        return user;
    }

    private GroupOrder baseOrder() {
        GroupOrder order = new GroupOrder();
        order.setId(ORDER_ID);
        order.setTitle("奶茶满 60 减 10");
        order.setOrderType("MILK_TEA");
        order.setMerchantName("一号门奶茶");
        order.setPickupLocation("一教大厅门口");
        order.setCreatorId(CREATOR_ID);
        order.setDeadlineTime(LocalDateTime.now().plusHours(2));
        order.setMaxParticipants(6);
        order.setParticipantCount(0);
        order.setDiscountThresholdAmount(new BigDecimal("60.00"));
        order.setDiscountAmount(new BigDecimal("10.00"));
        order.setOriginalTotalAmount(BigDecimal.ZERO);
        order.setActualDiscountAmount(BigDecimal.ZERO);
        order.setPayableTotalAmount(BigDecimal.ZERO);
        order.setRoundingAdjustmentAmount(BigDecimal.ZERO);
        order.setStatus(GroupOrderStatus.CREATED.getValue());
        order.setRemark("下课后一起取");
        return order;
    }

    private CreateGroupOrderRequest createOrderRequest(String deadlineTime) {
        CreateGroupOrderRequest request = new CreateGroupOrderRequest();
        request.setTitle("奶茶满 60 减 10");
        request.setOrderType("MILK_TEA");
        request.setMerchantName("一号门奶茶");
        request.setPickupLocation("一教大厅门口");
        request.setDeadlineTime(deadlineTime);
        request.setMaxParticipants(6);
        request.setDiscountThresholdAmount(new BigDecimal("60.00"));
        request.setDiscountAmount(new BigDecimal("10.00"));
        request.setRemark("下课后一起取");
        return request;
    }

    private JoinGroupOrderRequest joinRequest(String itemName, int quantity, String unitPrice) {
        JoinGroupOrderRequest.MealItemRequest item = new JoinGroupOrderRequest.MealItemRequest();
        item.setItemName(itemName);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setRemark("少冰");

        JoinGroupOrderRequest request = new JoinGroupOrderRequest();
        request.setRemark("少冰，不要吸管");
        request.setMealItems(List.of(item));
        return request;
    }

    private OrderParticipant participant(Long id, Long userId, String originalAmount) {
        OrderParticipant participant = new OrderParticipant();
        participant.setId(id);
        participant.setGroupOrderId(ORDER_ID);
        participant.setUserId(userId);
        participant.setOriginalAmount(new BigDecimal(originalAmount));
        participant.setDiscountShareAmount(BigDecimal.ZERO);
        participant.setPayableAmount(new BigDecimal(originalAmount));
        participant.setRoundingAdjustmentAmount(BigDecimal.ZERO);
        participant.setPaymentStatus(PaymentStatus.UNPAID.getValue());
        participant.setJoinTime(LocalDateTime.now().minusMinutes(10).plusSeconds(id));
        return participant;
    }

    private MealItem mealItem(Long id, Long participantId, String itemName, String subtotalAmount) {
        MealItem mealItem = new MealItem();
        mealItem.setId(id);
        mealItem.setGroupOrderId(ORDER_ID);
        mealItem.setParticipantId(participantId);
        mealItem.setItemName(itemName);
        mealItem.setQuantity(1);
        mealItem.setUnitPrice(new BigDecimal(subtotalAmount));
        mealItem.setSubtotalAmount(new BigDecimal(subtotalAmount));
        return mealItem;
    }

    private PickupRecord pickupRecord(String pickupStatus) {
        PickupRecord pickupRecord = new PickupRecord();
        pickupRecord.setId(6001L);
        pickupRecord.setGroupOrderId(ORDER_ID);
        pickupRecord.setPickupUserId(MEMBER_ID);
        pickupRecord.setPickupLocation("一教大厅门口");
        pickupRecord.setPickupStatus(pickupStatus);
        return pickupRecord;
    }
}
