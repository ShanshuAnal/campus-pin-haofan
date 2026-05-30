package com.campus.pinhaofan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.pinhaofan.common.AuthTokenUtil;
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
import com.campus.pinhaofan.enums.GroupOrderStatus;
import com.campus.pinhaofan.enums.PaymentStatus;
import com.campus.pinhaofan.enums.PickupStatus;
import com.campus.pinhaofan.exception.BusinessException;
import com.campus.pinhaofan.mapper.GroupOrderEventMapper;
import com.campus.pinhaofan.mapper.GroupOrderMapper;
import com.campus.pinhaofan.mapper.MealItemMapper;
import com.campus.pinhaofan.mapper.OrderParticipantMapper;
import com.campus.pinhaofan.mapper.OrderStatusLogMapper;
import com.campus.pinhaofan.mapper.PaymentRecordMapper;
import com.campus.pinhaofan.mapper.PickupRecordMapper;
import com.campus.pinhaofan.mapper.UserMapper;
import com.campus.pinhaofan.messaging.GroupOrderTimeoutMessagePublisher;
import com.campus.pinhaofan.service.impl.GroupOrderServiceImpl;
import com.campus.pinhaofan.vo.CancelGroupOrderVO;
import com.campus.pinhaofan.vo.GroupOrderDetailVO;
import com.campus.pinhaofan.vo.GroupOrderEventVO;
import com.campus.pinhaofan.vo.GroupOrderTimeoutCheckVO;
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
    private GroupOrderEventMapper groupOrderEventMapper;
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
    @Mock
    private GroupOrderTimeoutMessagePublisher groupOrderTimeoutMessagePublisher;

    private AuthTokenUtil authTokenUtil;
    private GroupOrderService groupOrderService;

    @BeforeEach
    void setUp() {
        authTokenUtil = new AuthTokenUtil("unit-test-secret", 24);
        groupOrderService = new GroupOrderServiceImpl(
                authTokenUtil,
                userMapper,
                groupOrderMapper,
                groupOrderEventMapper,
                orderParticipantMapper,
                mealItemMapper,
                pickupRecordMapper,
                orderStatusLogMapper,
                paymentRecordMapper,
                groupOrderTimeoutMessagePublisher
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
                .contains("status =")
                .contains("deadline_time")
                .contains("participant_count < max_participants")
                .contains("creator_id")
                .contains("pickup_user_id")
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
                .contains("status =")
                .contains("deadline_time")
                .contains("participant_count < max_participants")
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
        verify(groupOrderTimeoutMessagePublisher).sendTimeoutMessage(any(), any());
    }

    @Test
    void createGroupOrderWithCreatorItemsCreatesCreatorParticipantMealsAndUpdatesAmount() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        doAnswer(invocation -> {
            GroupOrder order = invocation.getArgument(0);
            order.setId(ORDER_ID);
            return 1;
        }).when(groupOrderMapper).insert(any(GroupOrder.class));
        doAnswer(invocation -> {
            OrderParticipant participant = invocation.getArgument(0);
            participant.setId(3001L);
            return 1;
        }).when(orderParticipantMapper).insert(any(OrderParticipant.class));
        doAnswer(invocation -> {
            MealItem mealItem = invocation.getArgument(0);
            mealItem.setId(mealItem.getItemName().equals("牛肉饭") ? 4001L : 4002L);
            return 1;
        }).when(mealItemMapper).insert(any(MealItem.class));

        CreateGroupOrderRequest request = createOrderRequest("2099-05-27 18:30:00");
        request.setCreatorItems(List.of(
                mealItemRequest("牛肉饭", 1, "28.00"),
                mealItemRequest("柠檬茶", 2, "12.50")
        ));

        GroupOrderVO result = groupOrderService.createGroupOrder(authorization(CREATOR_ID), request);

        assertThat(result.getParticipantCount()).isEqualTo(1);
        assertThat(result.getOriginalTotalAmount()).isEqualByComparingTo("53.00");
        assertThat(result.getPayableTotalAmount()).isEqualByComparingTo("53.00");
        assertThat(result.getActualDiscountAmount()).isEqualByComparingTo("0.00");

        ArgumentCaptor<OrderParticipant> participantCaptor = ArgumentCaptor.forClass(OrderParticipant.class);
        verify(orderParticipantMapper).insert(participantCaptor.capture());
        OrderParticipant participant = participantCaptor.getValue();
        assertThat(participant.getGroupOrderId()).isEqualTo(ORDER_ID);
        assertThat(participant.getUserId()).isEqualTo(CREATOR_ID);
        assertThat(participant.getOriginalAmount()).isEqualByComparingTo("53.00");
        assertThat(participant.getPayableAmount()).isEqualByComparingTo("53.00");
        assertThat(participant.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID.getValue());

        ArgumentCaptor<MealItem> mealCaptor = ArgumentCaptor.forClass(MealItem.class);
        verify(mealItemMapper, times(2)).insert(mealCaptor.capture());
        assertThat(mealCaptor.getAllValues())
                .extracting(MealItem::getItemName)
                .containsExactly("牛肉饭", "柠檬茶");
        assertThat(mealCaptor.getAllValues())
                .extracting(MealItem::getSubtotalAmount)
                .containsExactly(new BigDecimal("28.00"), new BigDecimal("25.00"));

        ArgumentCaptor<GroupOrder> orderCaptor = ArgumentCaptor.forClass(GroupOrder.class);
        verify(groupOrderMapper).updateById(orderCaptor.capture());
        GroupOrder updatedOrder = orderCaptor.getValue();
        assertThat(updatedOrder.getParticipantCount()).isEqualTo(1);
        assertThat(updatedOrder.getOriginalTotalAmount()).isEqualByComparingTo("53.00");
        assertThat(updatedOrder.getPayableTotalAmount()).isEqualByComparingTo("53.00");
    }

    @Test
    void createGroupOrderAllowsEmptyCreatorItems() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        doAnswer(invocation -> {
            GroupOrder order = invocation.getArgument(0);
            order.setId(ORDER_ID);
            return 1;
        }).when(groupOrderMapper).insert(any(GroupOrder.class));

        CreateGroupOrderRequest request = createOrderRequest("2099-05-27 18:30:00");
        request.setCreatorItems(List.of());

        GroupOrderVO result = groupOrderService.createGroupOrder(authorization(CREATOR_ID), request);

        assertThat(result.getParticipantCount()).isZero();
        assertThat(result.getOriginalTotalAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getPayableTotalAmount()).isEqualByComparingTo("0.00");
        verify(orderParticipantMapper, never()).insert(any(OrderParticipant.class));
        verify(mealItemMapper, never()).insert(any(MealItem.class));
        verify(groupOrderMapper, never()).updateById(any(GroupOrder.class));
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
        assertThat(result.getPermissions().getCanJoin()).isFalse();
        assertThat(result.getPermissions().getCanLock()).isFalse();
        assertThat(result.getPermissions().getCanCancel()).isTrue();
        assertThat(result.getPermissions().getCanCreateEvent()).isTrue();
        assertThat(result.getPermissions().getCanViewEvents()).isTrue();
    }

    @Test
    void getGroupOrderDetailAllowsUnrelatedUserToViewCreatedOrderWithLimitedPermissions() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        User outsider = user(THIRD_USER_ID, "20260003", "小周");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.CREATED.getValue());
        order.setParticipantCount(0);
        order.setMaxParticipants(2);

        when(userMapper.selectById(THIRD_USER_ID)).thenReturn(outsider);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectList(any())).thenReturn(List.of());
        when(mealItemMapper.selectList(any())).thenReturn(List.of());
        when(pickupRecordMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(creator));

        GroupOrderDetailVO result = groupOrderService.getGroupOrderDetail(authorization(THIRD_USER_ID), ORDER_ID);

        assertThat(result.getOrder().getId()).isEqualTo(ORDER_ID);
        assertThat(result.getParticipants()).isEmpty();
        assertThat(result.getPermissions().getCanJoin()).isTrue();
        assertThat(result.getPermissions().getCanViewEvents()).isFalse();
        assertThat(result.getPermissions().getCanCreateEvent()).isFalse();
        assertThat(result.getPermissions().getCanCancel()).isFalse();
    }

    @Test
    void getGroupOrderDetailRejectsUnrelatedUserAfterLocked() {
        User outsider = user(THIRD_USER_ID, "20260003", "小周");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());
        OrderParticipant memberParticipant = participant(3002L, MEMBER_ID, "20.00");

        when(userMapper.selectById(THIRD_USER_ID)).thenReturn(outsider);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectList(any())).thenReturn(List.of(memberParticipant));

        assertThatThrownBy(() -> groupOrderService.getGroupOrderDetail(authorization(THIRD_USER_ID), ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(403);
                    assertThat(exception.getMessage()).isEqualTo("No permission to view group order detail");
                });

        verify(mealItemMapper, never()).selectList(any());
        verify(pickupRecordMapper, never()).selectOne(any());
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
    void creatorAutoJoinedThenJoinAgainReturnsConflict() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setParticipantCount(1);

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> groupOrderService.joinGroupOrder(
                authorization(CREATOR_ID),
                ORDER_ID,
                joinRequest("珍珠奶茶", 1, "22.00")
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("不能重复加入同一拼单");
        });

        verify(orderParticipantMapper, never()).insert(any(OrderParticipant.class));
        verify(mealItemMapper, never()).insert(any(MealItem.class));
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
    void cancelCreatedOrderUpdatesStatusAndWritesLogAndEvent() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        CancelGroupOrderVO result = groupOrderService.cancelGroupOrder(
                authorization(CREATOR_ID),
                ORDER_ID,
                cancelRequest("人数不够，先取消")
        );

        assertThat(result.getId()).isEqualTo(ORDER_ID);
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getCancelReason()).isEqualTo("人数不够，先取消");
        assertThat(result.getCancelTime()).isNotBlank();

        ArgumentCaptor<GroupOrder> orderCaptor = ArgumentCaptor.forClass(GroupOrder.class);
        verify(groupOrderMapper).updateById(orderCaptor.capture());
        GroupOrder updatedOrder = orderCaptor.getValue();
        assertThat(updatedOrder.getStatus()).isEqualTo("CANCELLED");
        assertThat(updatedOrder.getCancelUserId()).isEqualTo(CREATOR_ID);
        assertThat(updatedOrder.getCancelReason()).isEqualTo("人数不够，先取消");
        assertThat(updatedOrder.getCancelTime()).isNotNull();
        assertThat(updatedOrder.getLastEventTime()).isEqualTo(updatedOrder.getCancelTime());

        ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
        verify(orderStatusLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("CANCEL_ORDER");
        assertThat(logCaptor.getValue().getBeforeStatus()).isEqualTo("CREATED");
        assertThat(logCaptor.getValue().getAfterStatus()).isEqualTo("CANCELLED");
        assertThat(logCaptor.getValue().getRemark()).isEqualTo("人数不够，先取消");

        ArgumentCaptor<GroupOrderEvent> eventCaptor = ArgumentCaptor.forClass(GroupOrderEvent.class);
        verify(groupOrderEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("CANCELLED");
        assertThat(eventCaptor.getValue().getOperatorId()).isEqualTo(CREATOR_ID);
        assertThat(eventCaptor.getValue().getOperatorRole()).isEqualTo("CREATOR");
        assertThat(eventCaptor.getValue().getBeforeStatus()).isEqualTo("CREATED");
        assertThat(eventCaptor.getValue().getAfterStatus()).isEqualTo("CANCELLED");
        assertThat(eventCaptor.getValue().getContent()).isEqualTo("人数不够，先取消");
    }

    @Test
    void cancelLockedOrderRefundsEscrowedAndConfirmedPayments() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());
        OrderParticipant escrowed = participant(3002L, MEMBER_ID, "18.76");
        escrowed.setPaymentStatus(PaymentStatus.ESCROWED.getValue());
        OrderParticipant confirmed = participant(3003L, THIRD_USER_ID, "12.00");
        confirmed.setPaymentStatus(PaymentStatus.CONFIRMED.getValue());

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectList(any())).thenReturn(List.of(escrowed, confirmed));

        CancelGroupOrderVO result = groupOrderService.cancelGroupOrder(
                authorization(CREATOR_ID),
                ORDER_ID,
                cancelRequest("有人已托管，取消并退款")
        );

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        verify(groupOrderMapper).updateById(any(GroupOrder.class));

        ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
        verify(orderStatusLogMapper, times(3)).insert(logCaptor.capture());
        assertThat(logCaptor.getAllValues())
                .extracting(OrderStatusLog::getActionType)
                .containsExactlyInAnyOrder("CANCEL_ORDER", "REFUND_PAYMENT", "REFUND_PAYMENT");
        assertThat(logCaptor.getAllValues())
                .extracting(OrderStatusLog::getTargetType)
                .containsExactlyInAnyOrder("GROUP_ORDER", "PAYMENT", "PAYMENT");

        ArgumentCaptor<OrderParticipant> participantCaptor = ArgumentCaptor.forClass(OrderParticipant.class);
        verify(orderParticipantMapper, times(2)).updateById(participantCaptor.capture());
        assertThat(participantCaptor.getAllValues())
                .extracting(OrderParticipant::getPaymentStatus)
                .containsExactlyInAnyOrder("REFUNDED", "REFUNDED");

        verify(paymentRecordMapper, times(2)).insert(any(PaymentRecord.class));
        ArgumentCaptor<GroupOrderEvent> eventCaptor = ArgumentCaptor.forClass(GroupOrderEvent.class);
        verify(groupOrderEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("CANCELLED");
        assertThat(eventCaptor.getValue().getOperatorId()).isEqualTo(CREATOR_ID);
        assertThat(eventCaptor.getValue().getOperatorRole()).isEqualTo("CREATOR");
    }

    @Test
    void cancelLockedOrderAllowsWhenAllParticipantsUnpaid() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.LOCKED.getValue());

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectList(any())).thenReturn(List.of());

        CancelGroupOrderVO result = groupOrderService.cancelGroupOrder(
                authorization(CREATOR_ID),
                ORDER_ID,
                cancelRequest("锁单后无人付款，取消")
        );

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        verify(groupOrderMapper).updateById(any(GroupOrder.class));

        ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
        verify(orderStatusLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("CANCEL_ORDER");
        assertThat(logCaptor.getValue().getBeforeStatus()).isEqualTo("LOCKED");
        assertThat(logCaptor.getValue().getAfterStatus()).isEqualTo("CANCELLED");
        assertThat(logCaptor.getValue().getRemark()).isEqualTo("锁单后无人付款，取消");

        ArgumentCaptor<GroupOrderEvent> eventCaptor = ArgumentCaptor.forClass(GroupOrderEvent.class);
        verify(groupOrderEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("CANCELLED");
        assertThat(eventCaptor.getValue().getOperatorId()).isEqualTo(CREATOR_ID);
        assertThat(eventCaptor.getValue().getOperatorRole()).isEqualTo("CREATOR");
        assertThat(eventCaptor.getValue().getBeforeStatus()).isEqualTo("LOCKED");
        assertThat(eventCaptor.getValue().getAfterStatus()).isEqualTo("CANCELLED");
        assertThat(eventCaptor.getValue().getContent()).isEqualTo("锁单后无人付款，取消");
    }

    @Test
    void cancelOrderRejectsNonCreator() {
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        assertThatThrownBy(() -> groupOrderService.cancelGroupOrder(
                authorization(MEMBER_ID),
                ORDER_ID,
                cancelRequest("非发起人取消")
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(403);
            assertThat(exception.getMessage()).isEqualTo("只有发起人可以取消拼单");
        });

        verify(groupOrderMapper, never()).updateById(any(GroupOrder.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ORDERED", "DELIVERING", "ARRIVED", "PICKED_UP"})
    void cancelOrderRejectsFulfillmentStatuses(String status) {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setStatus(status);

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        assertThatThrownBy(() -> groupOrderService.cancelGroupOrder(
                authorization(CREATOR_ID),
                ORDER_ID,
                cancelRequest("履约中取消")
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("当前状态不支持普通取消，请记录异常事件");
        });

        verify(groupOrderMapper, never()).updateById(any(GroupOrder.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"FINISHED", "CANCELLED", "EXPIRED"})
    void cancelOrderRejectsTerminalStatuses(String status) {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setStatus(status);

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        assertThatThrownBy(() -> groupOrderService.cancelGroupOrder(
                authorization(CREATOR_ID),
                ORDER_ID,
                cancelRequest("终态取消")
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("拼单已取消、已完成或已过期，不能取消");
        });

        verify(groupOrderMapper, never()).updateById(any(GroupOrder.class));
    }

    @Test
    void expireGroupOrderIfTimeoutUpdatesCreatedExpiredOrderAndWritesLogAndEvent() {
        GroupOrder order = baseOrder();
        order.setDeadlineTime(LocalDateTime.now().minusMinutes(1));
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(groupOrderMapper.update(any(GroupOrder.class), any())).thenReturn(1);

        GroupOrderTimeoutCheckVO result = groupOrderService.expireGroupOrderIfTimeout(ORDER_ID);

        assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(result.getExpired()).isTrue();
        assertThat(result.getStatus()).isEqualTo("EXPIRED");
        assertThat(result.getExpireTime()).isNotBlank();

        ArgumentCaptor<GroupOrder> orderCaptor = ArgumentCaptor.forClass(GroupOrder.class);
        verify(groupOrderMapper).update(orderCaptor.capture(), any());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("EXPIRED");
        assertThat(orderCaptor.getValue().getExpiredTime()).isNotNull();
        assertThat(orderCaptor.getValue().getExpireReason()).isEqualTo("超过加入截止时间，系统自动关闭");
        assertThat(orderCaptor.getValue().getLastEventTime()).isEqualTo(orderCaptor.getValue().getExpiredTime());

        ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
        verify(orderStatusLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getOperatorId()).isEqualTo(0L);
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("EXPIRE_ORDER");
        assertThat(logCaptor.getValue().getBeforeStatus()).isEqualTo("CREATED");
        assertThat(logCaptor.getValue().getAfterStatus()).isEqualTo("EXPIRED");

        ArgumentCaptor<GroupOrderEvent> eventCaptor = ArgumentCaptor.forClass(GroupOrderEvent.class);
        verify(groupOrderEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("EXPIRED");
        assertThat(eventCaptor.getValue().getOperatorId()).isNull();
        assertThat(eventCaptor.getValue().getOperatorRole()).isEqualTo("SYSTEM");
        assertThat(eventCaptor.getValue().getTitle()).isEqualTo("拼单超时关闭");
    }

    @Test
    void expireGroupOrderIfTimeoutSkipsWhenDeadlineNotReached() {
        GroupOrder order = baseOrder();
        order.setDeadlineTime(LocalDateTime.now().plusMinutes(5));
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        GroupOrderTimeoutCheckVO result = groupOrderService.expireGroupOrderIfTimeout(ORDER_ID);

        assertThat(result.getExpired()).isFalse();
        assertThat(result.getStatus()).isEqualTo("CREATED");
        assertThat(result.getMessage()).isEqualTo("未到截止时间");
        verify(groupOrderMapper, never()).update(any(GroupOrder.class), any());
        verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        verify(groupOrderEventMapper, never()).insert(any(GroupOrderEvent.class));
    }

    @Test
    void expireGroupOrderIfTimeoutIsIdempotentForNonCreatedStatus() {
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.EXPIRED.getValue());
        order.setExpiredTime(LocalDateTime.now().minusMinutes(1));
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        GroupOrderTimeoutCheckVO result = groupOrderService.expireGroupOrderIfTimeout(ORDER_ID);

        assertThat(result.getExpired()).isFalse();
        assertThat(result.getStatus()).isEqualTo("EXPIRED");
        assertThat(result.getMessage()).isEqualTo("当前状态无需超时关闭");
        verify(groupOrderMapper, never()).update(any(GroupOrder.class), any());
        verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        verify(groupOrderEventMapper, never()).insert(any(GroupOrderEvent.class));
    }

    @Test
    void expireGroupOrderIfTimeoutReturnsFalseWhenConcurrentUpdateWins() {
        GroupOrder order = baseOrder();
        order.setDeadlineTime(LocalDateTime.now().minusMinutes(1));
        GroupOrder latest = baseOrder();
        latest.setStatus(GroupOrderStatus.LOCKED.getValue());
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order, latest);
        when(groupOrderMapper.update(any(GroupOrder.class), any())).thenReturn(0);

        GroupOrderTimeoutCheckVO result = groupOrderService.expireGroupOrderIfTimeout(ORDER_ID);

        assertThat(result.getExpired()).isFalse();
        assertThat(result.getStatus()).isEqualTo("LOCKED");
        assertThat(result.getMessage()).isEqualTo("超时关闭已被其他操作处理");
        verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        verify(groupOrderEventMapper, never()).insert(any(GroupOrderEvent.class));
    }

    @Test
    void scanAndExpireTimeoutGroupOrdersExpiresCreatedTimeoutOrdersThroughHandler() {
        GroupOrder timeoutOrder = baseOrder();
        timeoutOrder.setDeadlineTime(LocalDateTime.now().minusMinutes(5));
        when(groupOrderMapper.selectList(any())).thenReturn(List.of(timeoutOrder));
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(timeoutOrder);
        when(groupOrderMapper.update(any(GroupOrder.class), any())).thenReturn(1);

        List<GroupOrderTimeoutCheckVO> result = groupOrderService.scanAndExpireTimeoutGroupOrders();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getOrderId()).isEqualTo(ORDER_ID);
        assertThat(result.getFirst().getExpired()).isTrue();
        assertThat(result.getFirst().getStatus()).isEqualTo(GroupOrderStatus.EXPIRED.getValue());

        ArgumentCaptor<LambdaQueryWrapper<GroupOrder>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(groupOrderMapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment())
                .contains("status")
                .contains("deadline_time")
                .containsIgnoringCase("ORDER BY deadline_time ASC,id ASC");
        verify(groupOrderMapper).update(any(GroupOrder.class), any());
        verify(orderStatusLogMapper).insert(any(OrderStatusLog.class));
        verify(groupOrderEventMapper).insert(any(GroupOrderEvent.class));
    }

    @Test
    void scanAndExpireTimeoutGroupOrdersReturnsEmptyWhenNoCandidate() {
        when(groupOrderMapper.selectList(any())).thenReturn(List.of());

        List<GroupOrderTimeoutCheckVO> result = groupOrderService.scanAndExpireTimeoutGroupOrders();

        assertThat(result).isEmpty();
        verify(groupOrderMapper, never()).selectById(any());
        verify(groupOrderMapper, never()).update(any(GroupOrder.class), any());
        verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        verify(groupOrderEventMapper, never()).insert(any(GroupOrderEvent.class));
    }

    @Test
    void createGroupOrderEventAllowsCreatorAndUpdatesLastEventTime() {
        User creator = user(CREATOR_ID, "20260001", "灏忎綍");
        GroupOrder order = baseOrder();
        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        doAnswer(invocation -> {
            GroupOrderEvent event = invocation.getArgument(0);
            event.setId(9001L);
            return 1;
        }).when(groupOrderEventMapper).insert(any(GroupOrderEvent.class));

        GroupOrderEventVO result = groupOrderService.createGroupOrderEvent(
                authorization(CREATOR_ID),
                ORDER_ID,
                eventRequest("MERCHANT_DELAY", "WARN", "商家出餐延迟", "预计晚到 10 分钟")
        );

        assertThat(result.getId()).isEqualTo(9001L);
        assertThat(result.getGroupOrderId()).isEqualTo(ORDER_ID);
        assertThat(result.getEventType()).isEqualTo("MERCHANT_DELAY");
        assertThat(result.getEventLevel()).isEqualTo("WARN");
        assertThat(result.getOperatorId()).isEqualTo(CREATOR_ID);
        assertThat(result.getOperatorRole()).isEqualTo("CREATOR");
        assertThat(result.getEventTitle()).isEqualTo("商家出餐延迟");
        assertThat(result.getEventContent()).isEqualTo("预计晚到 10 分钟");
        assertThat(result.getBeforeStatus()).isEqualTo(order.getStatus());
        assertThat(result.getAfterStatus()).isEqualTo(order.getStatus());

        ArgumentCaptor<GroupOrderEvent> eventCaptor = ArgumentCaptor.forClass(GroupOrderEvent.class);
        verify(groupOrderEventMapper).insert(eventCaptor.capture());
        GroupOrderEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("MERCHANT_DELAY");
        assertThat(event.getEventLevel()).isEqualTo("WARN");
        assertThat(event.getBeforeStatus()).isEqualTo(order.getStatus());
        assertThat(event.getAfterStatus()).isEqualTo(order.getStatus());
        assertThat(event.getEventTime()).isNotNull();
        assertThat(event.getCreateTime()).isNotNull();

        ArgumentCaptor<GroupOrder> orderCaptor = ArgumentCaptor.forClass(GroupOrder.class);
        verify(groupOrderMapper).updateById(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getId()).isEqualTo(ORDER_ID);
        assertThat(orderCaptor.getValue().getLastEventTime()).isNotNull();
        assertThat(orderCaptor.getValue().getStatus()).isNull();
    }

    @Test
    void createGroupOrderEventRejectsNonCreator() {
        User member = user(MEMBER_ID, "20260002", "灏忔灄");
        GroupOrder order = baseOrder();
        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        assertThatThrownBy(() -> groupOrderService.createGroupOrderEvent(
                authorization(MEMBER_ID),
                ORDER_ID,
                eventRequest("ITEM_MISSING", "ERROR", "缺餐", "少了一份鸡腿饭")
        )).isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getCode()).isEqualTo(403));

        verify(groupOrderEventMapper, never()).insert(any(GroupOrderEvent.class));
        verify(groupOrderMapper, never()).updateById(any(GroupOrder.class));
    }

    @Test
    void createGroupOrderEventRejectsLifecycleEventType() {
        User creator = user(CREATOR_ID, "20260001", "灏忎綍");
        GroupOrder order = baseOrder();
        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        assertThatThrownBy(() -> groupOrderService.createGroupOrderEvent(
                authorization(CREATOR_ID),
                ORDER_ID,
                eventRequest("EXPIRED", "INFO", "超时关闭", "不允许手动创建")
        )).isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getCode()).isEqualTo(400));

        verify(groupOrderEventMapper, never()).insert(any(GroupOrderEvent.class));
        verify(groupOrderMapper, never()).updateById(any(GroupOrder.class));
    }

    @Test
    void listGroupOrderEventsAllowsParticipant() {
        User member = user(MEMBER_ID, "20260002", "灏忔灄");
        GroupOrder order = baseOrder();
        GroupOrderEvent event = groupOrderEvent("PAYMENT_DISPUTE", "WARN", "付款争议", "成员反馈金额有误");
        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectCount(any())).thenReturn(1L);
        when(groupOrderEventMapper.selectList(any())).thenReturn(List.of(event));

        List<GroupOrderEventVO> result = groupOrderService.listGroupOrderEvents(authorization(MEMBER_ID), ORDER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getEventType()).isEqualTo("PAYMENT_DISPUTE");
        assertThat(result.getFirst().getEventTitle()).isEqualTo("付款争议");
        assertThat(result.getFirst().getOperatorId()).isEqualTo(CREATOR_ID);
        verify(groupOrderEventMapper).selectList(any());
    }

    @Test
    void listGroupOrderEventsQueriesAllEventsByEventTimeAndIdDesc() {
        User member = user(MEMBER_ID, "20260002", "member");
        GroupOrder order = baseOrder();
        GroupOrderEvent latestEvent = groupOrderEvent("PAYMENT_DISPUTE", "WARN", "Payment issue", "Amount mismatch");
        latestEvent.setId(9003L);
        latestEvent.setEventTime(LocalDateTime.of(2026, 5, 29, 18, 45));
        GroupOrderEvent sameTimeOlderEvent = groupOrderEvent("PICKUP_EXCEPTION", "WARN", "Pickup issue", "Queue too long");
        sameTimeOlderEvent.setId(9002L);
        sameTimeOlderEvent.setEventTime(LocalDateTime.of(2026, 5, 29, 18, 45));
        GroupOrderEvent olderEvent = groupOrderEvent("DELAY_REPORTED", "INFO", "Delay", "Ten minutes late");
        olderEvent.setId(9001L);
        olderEvent.setEventTime(LocalDateTime.of(2026, 5, 29, 18, 30));
        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectCount(any())).thenReturn(1L);
        when(groupOrderEventMapper.selectList(any())).thenReturn(List.of(latestEvent, sameTimeOlderEvent, olderEvent));

        List<GroupOrderEventVO> result = groupOrderService.listGroupOrderEvents(authorization(MEMBER_ID), ORDER_ID);

        assertThat(result).extracting(GroupOrderEventVO::getId).containsExactly(9003L, 9002L, 9001L);
        ArgumentCaptor<LambdaQueryWrapper> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(groupOrderEventMapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment())
                .contains("group_order_id")
                .containsIgnoringCase("ORDER BY event_time DESC,id DESC");
    }

    @Test
    void listGroupOrderEventsRejectsUnrelatedUser() {
        User outsider = user(THIRD_USER_ID, "20260003", "小周");
        GroupOrder order = baseOrder();
        when(userMapper.selectById(THIRD_USER_ID)).thenReturn(outsider);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(orderParticipantMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> groupOrderService.listGroupOrderEvents(authorization(THIRD_USER_ID), ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getCode()).isEqualTo(403));

        verify(groupOrderEventMapper, never()).selectList(any());
    }

    @Test
    void cancelledOrderRejectsJoinLockPaymentAndPickupProgress() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        User member = user(MEMBER_ID, "20260002", "小林");
        GroupOrder order = baseOrder();
        order.setStatus(GroupOrderStatus.CANCELLED.getValue());
        order.setPickupUserId(MEMBER_ID);

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
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

        assertThatThrownBy(() -> groupOrderService.lockGroupOrder(
                authorization(CREATOR_ID),
                ORDER_ID,
                null
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("当前状态不能锁单");
        });

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setRemark("取消后尝试付款");
        assertThatThrownBy(() -> groupOrderService.markParticipantPaid(
                authorization(MEMBER_ID),
                ORDER_ID,
                3002L,
                paymentRequest
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(400);
            assertThat(exception.getMessage()).isEqualTo("拼单未锁定，暂不能标记付款");
        });

        PickupStatusUpdateRequest pickupRequest = new PickupStatusUpdateRequest();
        pickupRequest.setPickupStatus(PickupStatus.WAITING_DELIVERY.getValue());
        assertThatThrownBy(() -> groupOrderService.updatePickupStatus(
                authorization(CREATOR_ID),
                ORDER_ID,
                pickupRequest
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("拼单已取消或已完成");
        });

        verify(orderParticipantMapper, never()).insert(any(OrderParticipant.class));
        verify(mealItemMapper, never()).insert(any(MealItem.class));
        verify(groupOrderMapper, never()).updateById(any(GroupOrder.class));
        verify(paymentRecordMapper, never()).insert(any(PaymentRecord.class));
        verify(pickupRecordMapper, never()).updateById(any(PickupRecord.class));
        verify(orderStatusLogMapper, never()).insert(any(OrderStatusLog.class));
        verify(groupOrderEventMapper, never()).insert(any(GroupOrderEvent.class));
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
        assertThat(result.getPaymentStatus()).isEqualTo("ESCROWED");
        assertThat(result.getPaidMarkTime()).isNotNull();
        assertThat(result.getPaymentRecord().getId()).isEqualTo(5001L);
        assertThat(result.getPaymentRecord().getAmount()).isEqualByComparingTo("18.76");
        assertThat(result.getPaymentRecord().getPaymentStatus()).isEqualTo("ESCROWED");

        ArgumentCaptor<OrderParticipant> participantCaptor = ArgumentCaptor.forClass(OrderParticipant.class);
        verify(orderParticipantMapper).updateById(participantCaptor.capture());
        assertThat(participantCaptor.getValue().getPaymentStatus()).isEqualTo("ESCROWED");

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
        participant.setPaymentStatus(PaymentStatus.ESCROWED.getValue());

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
        participant.setPaymentStatus(PaymentStatus.ESCROWED.getValue());
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
        participant.setPaymentStatus(PaymentStatus.ESCROWED.getValue());
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
        OrderParticipant confirmed = participant(3002L, MEMBER_ID, "18.76");
        confirmed.setPaymentStatus(PaymentStatus.CONFIRMED.getValue());

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(groupOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(pickupRecordMapper.selectOne(any())).thenReturn(pickupRecord);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(creator, member));
        when(orderParticipantMapper.selectList(any())).thenReturn(List.of(confirmed));

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

        ArgumentCaptor<OrderParticipant> participantCaptor = ArgumentCaptor.forClass(OrderParticipant.class);
        verify(orderParticipantMapper).updateById(participantCaptor.capture());
        assertThat(participantCaptor.getValue().getPaymentStatus()).isEqualTo("SETTLED");

        ArgumentCaptor<PaymentRecord> paymentCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRecordMapper).insert(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getPaymentStatus()).isEqualTo("SETTLED");
        assertThat(paymentCaptor.getValue().getConfirmUserId()).isEqualTo(CREATOR_ID);

        ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
        verify(orderStatusLogMapper, times(3)).insert(logCaptor.capture());
        List<OrderStatusLog> logs = logCaptor.getAllValues();
        assertThat(logs.get(0).getActionType()).isEqualTo("UPDATE_PICKUP_STATUS");
        assertThat(logs.get(0).getBeforeStatus()).isEqualTo("PICKED_UP");
        assertThat(logs.get(0).getAfterStatus()).isEqualTo("DISTRIBUTED");
        assertThat(logs.get(1).getActionType()).isEqualTo("UPDATE_ORDER_STATUS");
        assertThat(logs.get(1).getBeforeStatus()).isEqualTo("PICKED_UP");
        assertThat(logs.get(1).getAfterStatus()).isEqualTo("FINISHED");
        assertThat(logs.get(2).getActionType()).isEqualTo("SETTLE_PAYMENT");
        assertThat(logs.get(2).getTargetType()).isEqualTo("PAYMENT");
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
    void listMyGroupOrdersReturnsCreatedByMeOrders() {
        User creator = user(CREATOR_ID, "20260001", "小何");
        GroupOrder order = baseOrder();
        order.setCreatorId(CREATOR_ID);
        order.setStatus(GroupOrderStatus.CREATED.getValue());
        order.setCreateTime(LocalDateTime.of(2026, 5, 28, 13, 0));

        Page<GroupOrder> page = Page.of(1, 10);
        page.setRecords(List.of(order));
        page.setTotal(1);

        when(userMapper.selectById(CREATOR_ID)).thenReturn(creator);
        when(orderParticipantMapper.selectList(any())).thenReturn(List.of());
        when(groupOrderMapper.selectPage(any(), any())).thenReturn(page);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(creator));
        when(pickupRecordMapper.selectList(any())).thenReturn(List.of());

        PageResultVO<MyGroupOrderVO> result = groupOrderService.listMyGroupOrders(
                authorization(CREATOR_ID),
                "CREATED_BY_ME",
                null,
                null,
                null
        );

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().getFirst().getOrder().getId()).isEqualTo(ORDER_ID);
        assertThat(result.getRecords().getFirst().getOrder().getCreator().getId()).isEqualTo(CREATOR_ID);
        assertThat(result.getRecords().getFirst().getMyRole()).isEqualTo("CREATOR");
        assertThat(result.getRecords().getFirst().getMyParticipantId()).isNull();
        assertThat(result.getRecords().getFirst().getMyPaymentStatus()).isNull();

        ArgumentCaptor<LambdaQueryWrapper<GroupOrder>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(groupOrderMapper).selectPage(any(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getCustomSqlSegment())
                .contains("creator_id")
                .doesNotContain("pickup_user_id");
    }

    @Test
    void listMyGroupOrdersReturnsJoinedByMeOrders() {
        User member = user(MEMBER_ID, "20260002", "小林");
        User creator = user(CREATOR_ID, "20260001", "小何");
        OrderParticipant participant = participant(3002L, MEMBER_ID, "18.76");
        participant.setPaymentStatus(PaymentStatus.ESCROWED.getValue());

        GroupOrder order = baseOrder();
        order.setCreatorId(CREATOR_ID);
        order.setStatus(GroupOrderStatus.LOCKED.getValue());
        order.setCreateTime(LocalDateTime.of(2026, 5, 28, 12, 30));

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
                "JOINED_BY_ME",
                null,
                null,
                null
        );

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().getFirst().getMyRole()).isEqualTo("PARTICIPANT");
        assertThat(result.getRecords().getFirst().getMyParticipantId()).isEqualTo(3002L);
        assertThat(result.getRecords().getFirst().getMyPaymentStatus()).isEqualTo("ESCROWED");
        assertThat(result.getRecords().getFirst().getMyPayableAmount()).isEqualByComparingTo("18.76");

        ArgumentCaptor<LambdaQueryWrapper<GroupOrder>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(groupOrderMapper).selectPage(any(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getCustomSqlSegment())
                .contains("id IN")
                .doesNotContain("creator_id");
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

        ArgumentCaptor<LambdaQueryWrapper<GroupOrder>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(groupOrderMapper).selectPage(any(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getCustomSqlSegment())
                .contains("id IN")
                .contains("status NOT IN");
    }

    @Test
    void listMyGroupOrdersReturnsHistoryOrdersForCurrentUser() {
        User member = user(MEMBER_ID, "20260002", "小林");
        User creator = user(CREATOR_ID, "20260001", "小何");
        OrderParticipant finishedParticipant = participant(3002L, MEMBER_ID, "18.76");
        finishedParticipant.setGroupOrderId(2001L);
        finishedParticipant.setPaymentStatus(PaymentStatus.CONFIRMED.getValue());
        OrderParticipant cancelledParticipant = participant(3003L, MEMBER_ID, "22.00");
        cancelledParticipant.setGroupOrderId(2002L);

        GroupOrder finished = baseOrder();
        finished.setId(2001L);
        finished.setCreatorId(CREATOR_ID);
        finished.setStatus(GroupOrderStatus.FINISHED.getValue());
        finished.setCreateTime(LocalDateTime.of(2026, 5, 27, 18, 0));
        GroupOrder cancelled = baseOrder();
        cancelled.setId(2002L);
        cancelled.setCreatorId(CREATOR_ID);
        cancelled.setStatus(GroupOrderStatus.CANCELLED.getValue());
        cancelled.setCreateTime(LocalDateTime.of(2026, 5, 26, 18, 0));

        Page<GroupOrder> page = Page.of(1, 10);
        page.setRecords(List.of(finished, cancelled));
        page.setTotal(2);

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(orderParticipantMapper.selectList(any())).thenReturn(List.of(finishedParticipant, cancelledParticipant));
        when(groupOrderMapper.selectPage(any(), any())).thenReturn(page);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(creator));
        when(pickupRecordMapper.selectList(any())).thenReturn(List.of());

        PageResultVO<MyGroupOrderVO> result = groupOrderService.listMyGroupOrders(
                authorization(MEMBER_ID),
                "HISTORY",
                null,
                null,
                null
        );

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getRecords()).extracting(record -> record.getOrder().getStatus())
                .containsExactly("FINISHED", "CANCELLED");
        assertThat(result.getRecords()).extracting(MyGroupOrderVO::getMyRole)
                .containsExactly("PARTICIPANT", "PARTICIPANT");
        assertThat(result.getRecords().getFirst().getMyPaymentStatus()).isEqualTo("CONFIRMED");

        ArgumentCaptor<LambdaQueryWrapper<GroupOrder>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(groupOrderMapper).selectPage(any(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getCustomSqlSegment())
                .contains("creator_id")
                .contains("pickup_user_id")
                .contains("id IN")
                .contains("status IN");
    }

    @Test
    void listMyGroupOrdersWithoutScopeLimitsQueryToCurrentUserRelatedOrders() {
        User member = user(MEMBER_ID, "20260002", "小林");
        OrderParticipant participant = participant(3002L, MEMBER_ID, "18.76");
        participant.setGroupOrderId(2002L);

        Page<GroupOrder> page = Page.of(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);

        when(userMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(orderParticipantMapper.selectList(any())).thenReturn(List.of(participant));
        when(groupOrderMapper.selectPage(any(), any())).thenReturn(page);

        PageResultVO<MyGroupOrderVO> result = groupOrderService.listMyGroupOrders(
                authorization(MEMBER_ID),
                null,
                null,
                1L,
                10L
        );

        assertThat(result.getTotal()).isZero();
        assertThat(result.getRecords()).isEmpty();

        ArgumentCaptor<LambdaQueryWrapper<GroupOrder>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(groupOrderMapper).selectPage(any(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getCustomSqlSegment())
                .contains("creator_id")
                .contains("pickup_user_id")
                .contains("id IN")
                .contains("ORDER BY create_time DESC");
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
        paid.setPaymentStatus(PaymentStatus.ESCROWED.getValue());
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

        ArgumentCaptor<LambdaQueryWrapper<GroupOrder>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(groupOrderMapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getCustomSqlSegment())
                .contains("creator_id")
                .contains("pickup_user_id")
                .contains("id IN")
                .contains("ORDER BY create_time DESC");
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
        JoinGroupOrderRequest.MealItemRequest item = mealItemRequest(itemName, quantity, unitPrice);
        item.setRemark("少冰");

        JoinGroupOrderRequest request = new JoinGroupOrderRequest();
        request.setRemark("少冰，不要吸管");
        request.setMealItems(List.of(item));
        return request;
    }

    private JoinGroupOrderRequest.MealItemRequest mealItemRequest(String itemName, int quantity, String unitPrice) {
        JoinGroupOrderRequest.MealItemRequest item = new JoinGroupOrderRequest.MealItemRequest();
        item.setItemName(itemName);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        return item;
    }

    private CancelGroupOrderRequest cancelRequest(String cancelReason) {
        CancelGroupOrderRequest request = new CancelGroupOrderRequest();
        request.setCancelReason(cancelReason);
        return request;
    }

    private CreateGroupOrderEventRequest eventRequest(
            String eventType,
            String eventLevel,
            String eventTitle,
            String eventContent) {
        CreateGroupOrderEventRequest request = new CreateGroupOrderEventRequest();
        request.setEventType(eventType);
        request.setEventLevel(eventLevel);
        request.setEventTitle(eventTitle);
        request.setEventContent(eventContent);
        return request;
    }

    private GroupOrderEvent groupOrderEvent(String eventType, String eventLevel, String title, String content) {
        GroupOrderEvent event = new GroupOrderEvent();
        event.setId(9001L);
        event.setGroupOrderId(ORDER_ID);
        event.setEventType(eventType);
        event.setEventLevel(eventLevel);
        event.setOperatorId(CREATOR_ID);
        event.setOperatorRole("CREATOR");
        event.setTitle(title);
        event.setContent(content);
        event.setBeforeStatus(GroupOrderStatus.LOCKED.getValue());
        event.setAfterStatus(GroupOrderStatus.LOCKED.getValue());
        event.setEventTime(LocalDateTime.of(2026, 5, 29, 18, 45));
        event.setCreateTime(LocalDateTime.of(2026, 5, 29, 18, 45));
        return event;
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
