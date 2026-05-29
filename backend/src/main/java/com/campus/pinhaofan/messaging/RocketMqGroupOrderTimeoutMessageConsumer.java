package com.campus.pinhaofan.messaging;

import com.campus.pinhaofan.config.RocketMqProperties;
import com.campus.pinhaofan.service.GroupOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "haofan.rocketmq", name = "enabled", havingValue = "true")
public class RocketMqGroupOrderTimeoutMessageConsumer implements InitializingBean, DisposableBean {

    private final RocketMqProperties properties;
    private final GroupOrderService groupOrderService;

    private DefaultMQPushConsumer consumer;

    @Override
    public void afterPropertiesSet() throws Exception {
        validateProperties();
        consumer = new DefaultMQPushConsumer(null, properties.getConsumerGroup(), createAclHook());
        consumer.setNamesrvAddr(properties.getNameServer());
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.subscribe(properties.getTopic(), properties.getTag());
        consumer.registerMessageListener((MessageListenerConcurrently) (messages, context) -> {
            for (MessageExt message : messages) {
                try {
                    consumeMessage(message);
                } catch (Exception ex) {
                    log.error(
                            "Consume RocketMQ timeout message failed. msgId={}, topic={}, tags={}",
                            message.getMsgId(),
                            message.getTopic(),
                            message.getTags(),
                            ex
                    );
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        log.info(
                "RocketMQ timeout consumer started. nameServer={}, consumerGroup={}, topic={}, tag={}",
                properties.getNameServer(),
                properties.getConsumerGroup(),
                properties.getTopic(),
                properties.getTag()
        );
    }

    public void consumeTimeoutMessage(Long orderId) {
        log.info("Consume group order timeout message. orderId={}", orderId);
        groupOrderService.expireGroupOrderIfTimeout(orderId);
    }

    @Override
    public void destroy() {
        if (consumer != null) {
            consumer.shutdown();
            log.info("RocketMQ timeout consumer shutdown.");
        }
    }

    private void consumeMessage(MessageExt message) {
        Long orderId = parseOrderId(message);
        if (orderId == null) {
            log.warn("Skip invalid RocketMQ timeout message. msgId={}", message.getMsgId());
            return;
        }
        consumeTimeoutMessage(orderId);
    }

    private Long parseOrderId(MessageExt message) {
        String orderIdProperty = message.getUserProperty("orderId");
        String rawOrderId = StringUtils.hasText(orderIdProperty)
                ? orderIdProperty
                : new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            return Long.valueOf(rawOrderId.trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private AclClientRPCHook createAclHook() {
        return new AclClientRPCHook(new SessionCredentials(properties.getAccessKey(), properties.getSecretKey()));
    }

    private void validateProperties() {
        requireText(properties.getNameServer(), "haofan.rocketmq.name-server");
        requireText(properties.getAccessKey(), "haofan.rocketmq.access-key");
        requireText(properties.getSecretKey(), "haofan.rocketmq.secret-key");
        requireText(properties.getConsumerGroup(), "haofan.rocketmq.consumer-group");
        requireText(properties.getTopic(), "haofan.rocketmq.topic");
        requireText(properties.getTag(), "haofan.rocketmq.tag");
    }

    private void requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(propertyName + " must be configured when RocketMQ is enabled");
        }
    }
}
