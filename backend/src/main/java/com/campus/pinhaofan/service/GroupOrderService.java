package com.campus.pinhaofan.service;

import com.campus.pinhaofan.dto.CreateGroupOrderRequest;
import com.campus.pinhaofan.dto.JoinGroupOrderRequest;
import com.campus.pinhaofan.dto.LockGroupOrderRequest;
import com.campus.pinhaofan.dto.PaymentRequest;
import com.campus.pinhaofan.dto.PickupAssigneeRequest;
import com.campus.pinhaofan.dto.PickupStatusUpdateRequest;
import com.campus.pinhaofan.vo.DashboardSummaryVO;
import com.campus.pinhaofan.vo.GroupOrderDetailVO;
import com.campus.pinhaofan.vo.GroupOrderVO;
import com.campus.pinhaofan.vo.JoinGroupOrderVO;
import com.campus.pinhaofan.vo.LockGroupOrderVO;
import com.campus.pinhaofan.vo.MyGroupOrderVO;
import com.campus.pinhaofan.vo.PageResultVO;
import com.campus.pinhaofan.vo.PaymentActionVO;
import com.campus.pinhaofan.vo.PickupAssigneeVO;
import com.campus.pinhaofan.vo.PickupStatusUpdateVO;

public interface GroupOrderService {

    PageResultVO<GroupOrderVO> listGroupOrders(
            String authorization,
            String status,
            String orderType,
            String keyword,
            Long pageNum,
            Long pageSize
    );

    GroupOrderVO createGroupOrder(String authorization, CreateGroupOrderRequest request);

    GroupOrderDetailVO getGroupOrderDetail(String authorization, Long orderId);

    JoinGroupOrderVO joinGroupOrder(String authorization, Long orderId, JoinGroupOrderRequest request);

    LockGroupOrderVO lockGroupOrder(String authorization, Long orderId, LockGroupOrderRequest request);

    PaymentActionVO markParticipantPaid(String authorization, Long orderId, Long participantId, PaymentRequest request);

    PaymentActionVO confirmParticipantPayment(String authorization, Long orderId, Long participantId, PaymentRequest request);

    PickupAssigneeVO assignPickupUser(String authorization, Long orderId, PickupAssigneeRequest request);

    PickupStatusUpdateVO updatePickupStatus(String authorization, Long orderId, PickupStatusUpdateRequest request);

    PageResultVO<MyGroupOrderVO> listMyGroupOrders(
            String authorization,
            String scope,
            String status,
            Long pageNum,
            Long pageSize
    );

    DashboardSummaryVO getDashboardSummary(String authorization, String startTime, String endTime, String scope);
}
