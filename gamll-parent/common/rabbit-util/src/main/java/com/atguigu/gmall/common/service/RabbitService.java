package com.atguigu.gmall.common.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.entity.GmallCorrelationData;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 * @create 2020-03-31 11:43
 */
@Service
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate redisTemplate;

    //过期时间：分钟
    public static final int OBJECT_TIMEOUT = 10;

    /**
     *  发送消息
     * @param exchange 交换机
     * @param routingKey 路由键
     * @param message 消息
     */
    public boolean sendMessage(String exchange, String routingKey, Object message) {
        GmallCorrelationData correlationData = new GmallCorrelationData();
        String correlationId = UUID.randomUUID().toString();
        correlationData.setId(correlationId);
        correlationData.setMessage(message);
        correlationData.setExchange(exchange);
        correlationData.setRoutingKey(routingKey);

        redisTemplate.opsForValue().set(correlationId, JSON.toJSONString(correlationData), OBJECT_TIMEOUT, TimeUnit.MINUTES);
        rabbitTemplate.convertAndSend(exchange, routingKey, message,correlationData);
        return true;
    }
    /**
     * 发送延迟消息
     * @param exchange 交换机
     * @param routingKey 路由键
     * @param message 消息
     * @param delayTime 单位：秒
     */
    public boolean sendDelayMessage(String exchange, String routingKey, Object message, int delayTime) {
        GmallCorrelationData correlationData = new GmallCorrelationData();
        String correlationId = UUID.randomUUID().toString();
        correlationData.setId(correlationId);
        correlationData.setMessage(message);
        correlationData.setExchange(exchange);
        correlationData.setRoutingKey(routingKey);
        correlationData.setDelay(true);
        correlationData.setDelayTime(delayTime);

        redisTemplate.opsForValue().set(correlationId, JSON.toJSONString(correlationData), OBJECT_TIMEOUT, TimeUnit.MINUTES);
        this.rabbitTemplate.convertAndSend(exchange, routingKey, message, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setDelay(delayTime*1000);
                return message;
            }
        },correlationData);
        return true;
    }

}