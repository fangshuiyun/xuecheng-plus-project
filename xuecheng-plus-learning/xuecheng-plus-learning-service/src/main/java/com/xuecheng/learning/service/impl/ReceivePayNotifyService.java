package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Mr.M
 * @version 1.0
 * @description 接收支付结果
 * @date 2023/2/23 19:04
 */
@Slf4j
@Service
public class ReceivePayNotifyService {

    @Autowired
    MyCourseTablesService myCourseTablesService;

    //监听消息队列接收支付结果通知
    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message){
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //获取消息
        MqMessage mqMessage = JSON.parseObject(message.getBody(), MqMessage.class);
        log.debug("学习中心服务接收支付结果:{}", mqMessage);
        //消息类型
        String messageType = mqMessage.getMessageType();
        //选课记录id
        String choosecourseId = mqMessage.getBusinessKey1();
        //订单类型,60201表示购买课程
        String orderType = mqMessage.getBusinessKey2();
        if (PayNotifyConfig.MESSAGE_TYPE.equals(messageType) && "60201".equals(orderType)){
            boolean b = myCourseTablesService.saveChooseCourseSuccess(choosecourseId);
            if(!b){
                //添加选课失败，抛出异常，消息重回队列
                XueChengPlusException.cast("收到支付结果，添加选课失败");
            }
        }
    }
}
