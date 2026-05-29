package com.campus.pinhaofan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "haofan.rocketmq")
public class RocketMqProperties {

    private boolean enabled;

    private String nameServer;

    private String accessKey;

    private String secretKey;

    private String producerGroup;

    private String consumerGroup;

    private String topic;

    private String tag;
}
