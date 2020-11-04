package com.atguigu.gmall.task.scheduled;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Administrator
 * @create 2020-03-31 12:09
 */
@Component
@EnableScheduling
@Slf4j
public class ScheduledTask {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitService rabbitService;

//    @Scheduled(cron = "0/30 * *  * * ?")
////    public void task() {
////        log.info("每30秒执行一次");
////        String msg = (String) redisTemplate.opsForList().rightPop(MqConst.MQ_KEY_PREFIX);
////        if(StringUtils.isEmpty(msg)) return;
////
////        // 再次发送消息
////        GmallCorrelationData gmallCorrelationData = JSON.parseObject(msg, GmallCorrelationData.class);
////        if (gmallCorrelationData.isDelay()){
////            rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(),gmallCorrelationData.getRoutingKey(),gmallCorrelationData.getMessage(),message ->{
////                message.getMessageProperties().setDelay(gmallCorrelationData.getDelayTime()*1000);
////                return message;
////            },gmallCorrelationData);
////        }else {
////            rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(),gmallCorrelationData.getRoutingKey(),gmallCorrelationData.getMessage(),gmallCorrelationData);
////        }
////    }

    @Scheduled(cron = "0/30 * * * * ?")
    public void task1(){
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_1,"");
    }
    //发送清空缓存的消息
    //@Scheduled(cron = "0/35 * * * * ?")
    @Scheduled(cron = "0 0 18 * * ?")
    public void task2() {
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_18, "");
    }
}

