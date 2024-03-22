package com.mqexample.demo.config;

/**
 * @Author sgx
 * @Date 2024/3/1 16:58
 * @Description:
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 服务端配置类
 */
@Configuration
public class WebSocketServerConfig {

    /**
     * ServerEndpointExporter bean 注入
     * @return
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        ServerEndpointExporter serverEndpointExporter = new ServerEndpointExporter();
        return serverEndpointExporter;
    }

}

