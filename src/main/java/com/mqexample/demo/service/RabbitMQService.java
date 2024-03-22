package com.mqexample.demo.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * @Author sgx
 * @Date 2024/1/13 22:24
 * @Description:
 */
public interface RabbitMQService {
    String sendMsg(String msg) throws Exception;

     SseEmitter SseSendMessage() throws Exception;


     void singleThread_consumer() throws Exception;

    void multiThread_consumer() throws Exception;

}
