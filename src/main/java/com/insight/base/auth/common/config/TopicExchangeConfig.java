package com.insight.base.auth.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 宣炳刚
 * @date 2019-09-03
 * @remark Topic交换机配置
 */
@Configuration
public class TopicExchangeConfig {

    /**
     * Topic交换机
     *
     * @return TopicExchange
     */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("amq.topic");
    }

    /**
     * 新增用户队列
     *
     * @return Queue
     */
    @Bean
    public Queue userQueue() {
        return new Queue("insight.user");
    }

    /**
     * 默认用户绑定
     * @return Binding
     */
    @Bean
    public Binding userBinding(){
        return BindingBuilder.bind(userQueue()).to(exchange()).with("auth.addUser");
    }
}
