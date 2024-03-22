package com.mqexample.demo.controller;

import com.mqexample.demo.config.RabbitMQConfig;
import com.mqexample.demo.service.RabbitMQService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @Author sgx
 * @Date 2024/1/13 22:43
 * @Description:
 */
@RestController
@RequestMapping("/mall/rabbitmq")
public class RabbitMQController {
    @Resource
    private RabbitMQService rabbitMQService;




    private final RabbitListenerEndpointRegistry registry;

    public RabbitMQController(RabbitListenerEndpointRegistry registry) {

        this.registry = registry;
    }

    /**
     * 发送消息
     * @author sgx
     */
    @PostMapping("/sendMsg")
    public String sendMsg(@RequestParam(name = "msg") String msg) throws Exception {
        return rabbitMQService.sendMsg(msg);
    }


    @GetMapping("/singleThread")
    public void singleThread_consumer()throws Exception{
        rabbitMQService.singleThread_consumer();
    }

    @GetMapping("/singleThreadSsePush")
    public SseEmitter singleThread_Consumer_SsePushMsg()throws Exception{

        return rabbitMQService.SseSendMessage();

    }



    @GetMapping("/multiThread")
    public void multiThread_consumer()throws Exception{
        rabbitMQService.multiThread_consumer();
    }



    @GetMapping("/listen")
    @RabbitListener(id="sgx",queues = {RabbitMQConfig.RABBITMQ_DEMO_TOPIC},autoStartup = "false")
    @RabbitHandler(isDefault = true)
    public String monitor(Map map){

        MessageListenerContainer container = registry.getListenerContainer("sgx");
        //判断容器状态
        if(!container.isRunning()){
            //开启容器
            container.start();
            System.out.println("开启容器");
        }


        System.out.println("消费者从队列中消费消息："+map.toString());

        return map.toString();

    }
}