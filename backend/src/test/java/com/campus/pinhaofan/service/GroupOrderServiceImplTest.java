package com.campus.pinhaofan.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.pinhaofan.common.AuthTokenUtil;
import com.campus.pinhaofan.dto.CreateGroupOrderRequest;
import com.campus.pinhaofan.dto.JoinGroupOrderRequest;
import com.campus.pinhaofan.dto.LockGroupOrderRequest;
import com.campus.pinhaofan.entity.GroupOrder;
import com.campus.pinhaofan.entity.MealItem;
import com.campus.pinhaofan.entity.OrderParticipant;
import com.campus.pinhaofan.entity.OrderStatusLog;
import com.campus.pinhaofan.entity.PickupRecord;
import com.campus.pinhaofan.entity.User;
import com.campus.pinhaofan.enums.GroupOrderStatus;
import com.campus.pinhaofan.enums.PaymentStatus;
import com.campus.pinhaofan.exception.BusinessException;
import com.campus.pinhaofan.mapper.GroupOrderMapper;
import com.campus.pinhaofan.mapper.MealItemMapper;
import com.campus.pinhaofan.mapper.OrderParticipantMapper;
import com.campus.pinhaofan.mapper.OrderStatusLogMapper;
import com.campus.pinhaofan.mapper.PickupRecordMapper;
import com.campus.pinhaofan.mapper.UserMapper;
import com.campus.pinhaofan.service.impl.GroupOrderServiceImpl;
import com.campus.pinhaofan.vo.GroupOrderDetailVO;
import com.campus.pinhaofan.vo.GroupOrderVO;
import com.campus.pinhaofan.vo.JoinGroupOrderVO;
import com.campus.pinhaofan.vo.LockAllocationVO;
import com.campus.pinhaofan.vo.LockGroupOrderVO;
import com.campus.pinhaofan.vo.PageResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupOrderServiceImplTest {

    private static final Long CREATOR_ID = 1001L;
    private static final Long MEMBER_ID = 1002L;
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
                orderStatusLogMapper
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
}
