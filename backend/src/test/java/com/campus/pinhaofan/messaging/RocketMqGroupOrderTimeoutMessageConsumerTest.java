package com.campus.pinhaofan.messaging;

import com.campus.pinhaofan.config.RocketMqProperties;
import com.campus.pinhaofan.service.GroupOrderService;
import com.campus.pinhaofan.vo.GroupOrderTimeoutCheckVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RocketMqGroupOrderTimeoutMessageConsumerTest {

    @Test
    void consumeTimeoutMessageRepublishesWhenMessageArrivesBeforeDeadline() {
        GroupOrderService groupOrderService = mock(GroupOrderService.class);
        GroupOrderTimeoutMessagePublisher publisher = mock(GroupOrderTimeoutMessagePublisher.class);
        RocketMqGroupOrderTimeoutMessageConsumer consumer = new RocketMqGroupOrderTimeoutMessageConsumer(
                new RocketMqProperties(),
                groupOrderService,
                publisher
        );

        when(groupOrderService.expireGroupOrderIfTimeout(2006L)).thenReturn(new GroupOrderTimeoutCheckVO(
                2006L,
                false,
                "CREATED",
                "未到截止时间",
                null,
                "2026-05-30 11:26:00",
                true
        ));

        consumer.consumeTimeoutMessage(2006L);

        verify(publisher).sendTimeoutMessage(2006L, LocalDateTime.of(2026, 5, 30, 11, 26));
    }

    @Test
    void consumeTimeoutMessageDoesNotRepublishWhenOrderStatusChanged() {
        GroupOrderService groupOrderService = mock(GroupOrderService.class);
        GroupOrderTimeoutMessagePublisher publisher = mock(GroupOrderTimeoutMessagePublisher.class);
        RocketMqGroupOrderTimeoutMessageConsumer consumer = new RocketMqGroupOrderTimeoutMessageConsumer(
                new RocketMqProperties(),
                groupOrderService,
                publisher
        );

        when(groupOrderService.expireGroupOrderIfTimeout(2006L)).thenReturn(new GroupOrderTimeoutCheckVO(
                2006L,
                false,
                "LOCKED",
                "当前状态无需超时关闭",
                null
        ));

        consumer.consumeTimeoutMessage(2006L);

        verify(publisher, never()).sendTimeoutMessage(2006L, LocalDateTime.of(2026, 5, 30, 11, 26));
    }
}
