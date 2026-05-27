package com.campus.pinhaofan.service;

import com.campus.pinhaofan.dto.CreateGroupOrderRequest;
import com.campus.pinhaofan.dto.JoinGroupOrderRequest;
import com.campus.pinhaofan.vo.GroupOrderDetailVO;
import com.campus.pinhaofan.vo.GroupOrderVO;
import com.campus.pinhaofan.vo.JoinGroupOrderVO;
import com.campus.pinhaofan.vo.PageResultVO;

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
}
