package com.insight.base.auth.common.client;

import com.insight.utils.Json;
import com.insight.utils.common.ApplicationContextHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * @author 宣炳刚
 * @date 2019-09-03
 * @remark RabbitMQ客户端
 */
public class RabbitClient {
    private static final RabbitTemplate TEMPLATE = ApplicationContextHolder.getContext().getBean(RabbitTemplate.class);

    /**
     * 发送用户数据到队列
     *
     * @param data 用户DTO
     */
    public static void sendTopic(Object data) {
        Object object = Json.clone(data, Object.class);
        TEMPLATE.convertAndSend("amq.topic", "auth.addUser", object);
    }
}
