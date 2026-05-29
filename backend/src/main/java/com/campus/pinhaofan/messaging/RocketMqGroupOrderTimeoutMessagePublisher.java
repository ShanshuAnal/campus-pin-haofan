package com.campus.pinhaofan.messaging;

import com.campus.pinhaofan.config.RocketMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "haofan.rocketmq", name = "enabled", havingValue = "true")
public class RocketMqGroupOrderTimeoutMessagePublisher
        implements GroupOrderTimeoutMessagePublisher, InitializingBean, DisposableBean {

    private static final String START_DELIVER_TIME_PROPERTY = "__STARTDELIVERTIME";

    private final RocketMqProperties properties;

    private DefaultMQProducer producer;

    @Override
    public void afterPropertiesSet() throws Exception {
        validateProperties();
        producer = new DefaultMQProducer(properties.getProducerGroup(), createAclHook());
        producer.setNamesrvAddr(properties.getNameServer());
        producer.start();
        log.info(
                "RocketMQ timeout producer started. nameServer={}, producerGroup={}, topic={}",
                properties.getNameServer(),
                properties.getProducerGroup(),
                properties.getTopic()
        );
    }

    @Override
    public void sendTimeoutMessage(Long orderId, LocalDateTime deadlineTime) {
        if (orderId == null || deadlineTime == null) {
            throw new IllegalArgumentException("orderId and deadlineTime must not be null");
        }
        try {
            Message message = new Message(
                    properties.getTopic(),
                    properties.getTag(),
                    buildMessageKey(orderId),
                    String.valueOf(orderId).getBytes(StandardCharsets.UTF_8)
            );
            long deliverTimeMillis = deadlineTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            message.putUserProperty(START_DELIVER_TIME_PROPERTY, String.valueOf(deliverTimeMillis));
            message.putUserProperty("orderId", String.valueOf(orderId));
            SendResult result = producer.send(message);
            log.info(
                    "RocketMQ timeout message sent. orderId={}, topic={}, tag={}, msgId={}, sendStatus={}",
                    orderId,
                    properties.getTopic(),
                    properties.getTag(),
                    result.getMsgId(),
                    result.getSendStatus()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send RocketMQ timeout message", ex);
        }
    }

    @Override
    public void destroy() {
        if (producer != null) {
            producer.shutdown();
            log.info("RocketMQ timeout producer shutdown.");
        }
    }

    private AclClientRPCHook createAclHook() {
        return new AclClientRPCHook(new SessionCredentials(properties.getAccessKey(), properties.getSecretKey()));
    }

    private String buildMessageKey(Long orderId) {
        return "group-order-timeout-" + orderId;
    }

    private void validateProperties() {
        requireText(properties.getNameServer(), "haofan.rocketmq.name-server");
        requireText(properties.getAccessKey(), "haofan.rocketmq.access-key");
        requireText(properties.getSecretKey(), "haofan.rocketmq.secret-key");
        requireText(properties.getProducerGroup(), "haofan.rocketmq.producer-group");
        requireText(properties.getTopic(), "haofan.rocketmq.topic");
        requireText(properties.getTag(), "haofan.rocketmq.tag");
    }

    private void requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(propertyName + " must be configured when RocketMQ is enabled");
        }
    }
}
