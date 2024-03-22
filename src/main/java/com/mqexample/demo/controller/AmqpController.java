package com.mqexample.demo.controller;

/**
 * @Author sgx
 * @Date 2024/3/17 16:33
 * @Description:
 */

import com.mqexample.demo.service.impl.AmqpPushService;
import com.mqexample.demo.util.Runner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/amqp")
public class AmqpController {

    @GetMapping("/amqpServer")
    public void pushMessage()throws Exception{


        Runner.runExample(AmqpPushService.class);

    }
}
