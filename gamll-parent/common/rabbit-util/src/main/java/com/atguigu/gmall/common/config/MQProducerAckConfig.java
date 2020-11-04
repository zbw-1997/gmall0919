package com.atguigu.gmall.common.config;

/**
 * @author Administrator
 * @create 2020-03-31 11:42
 */

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.entity.GmallCorrelationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @Description 消息发送确认
 * <p>
 * ConfirmCallback  只确认消息是否正确到达 Exchange 中
 * ReturnCallback   消息没有正确到达队列时触发回调，如果正确到达队列不执行
 * <p>
 * 1. 如果消息没有到exchange,则confirm回调,ack=false
 * 2. 如果消息到达exchange,则confirm回调,ack=true
 * 3. exchange到queue成功,则不回调return
 * 4. exchange到queue失败,则回调return
 *
 */
@Component
@Slf4j
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);            //指定 ConfirmCallback
        rabbitTemplate.setReturnCallback(this);             //指定 ReturnCallback
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            log.info("消息发送成功：" + JSON.toJSONString(correlationData));
        } else {
            log.info("消息发送失败：" + cause + " 数据：" + JSON.toJSONString(correlationData));
            this.addRetry(correlationData);
        }
    }

    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        // 反序列化对象输出
        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);
        System.out.println("CorrelationId  -> " + message.getMessageProperties().getHeaders().get("spring_returned_message_correlation"));

        String correlationId = (String)message.getMessageProperties().getHeaders().get("spring_returned_message_correlation");
        GmallCorrelationData correlationData = JSON.parseObject((String)redisTemplate.opsForValue().get(correlationId),GmallCorrelationData.class);

        this.addRetry(correlationData);
    }

    /**
     * 添加重试
     * @param correlationData
     */
    private void addRetry(CorrelationData correlationData) {
        GmallCorrelationData gmallCorrelationData = (GmallCorrelationData)correlationData;
        int retryCount = gmallCorrelationData.getRetryCount();
        if(retryCount >= MqConst.RETRY_COUNT) {
            log.error("消息重试失败：" + JSON.toJSONString(correlationData));
        } else {
            retryCount += 1;
            gmallCorrelationData.setRetryCount(retryCount);
            redisTemplate.opsForList().leftPush(MqConst.MQ_KEY_PREFIX, JSON.toJSONString(correlationData));
            //次数更新
            redisTemplate.opsForValue().set(gmallCorrelationData.getId(), JSON.toJSONString(correlationData));
        }
    }
}
