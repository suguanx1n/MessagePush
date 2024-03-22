package com.mqexample.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @Author sgx
 * @Date 2024/3/1 16:59
 * @Description:  WebSocket服务端
 */
@Component
@ServerEndpoint("/webSocket/{clientId}")
public class CustomizedWebSocketServer {

    /**
     * 日志
     */
    private Logger logger = LoggerFactory.getLogger(CustomizedWebSocketServer.class);

    /**
     * 在线数
     */
    private static int onlineCount = 0;

    final Object lockA=new Object();

    /**
     * 线程安全的存储连接session的Map
     */
    private static Map<String, CustomizedWebSocketServer> clients = new ConcurrentHashMap<String, CustomizedWebSocketServer>();

    /**
     * session
     */
    private Session session;

    /**
     * 客户端端标识
     */
    private String clientId;

    /**
     * 客户端连接时方法
     * @param clientId
     * @param session
     * @throws IOException
     */
    @OnOpen
    public void onOpen(@PathParam("clientId") String clientId, Session session) throws IOException {
        logger.info("onOpen: has new client connect -"+clientId);
        //
        this.clientId = clientId;
        this.session = session;
        addOnlineCount();
        clients.put(clientId, this);
        logger.info("onOpen: 现在有 "+onlineCount+" 客户端在线");
    }

    /**
     * 客户端断开连接时方法
     * @throws IOException
     */
    @OnClose
    public void onClose() throws IOException {
        logger.info("onClose: 有新的客户端关闭了连接 -"+clientId);
        clients.remove(clientId);
        subOnlineCount();
        logger.info("onClose: 现在有 "+onlineCount+" 客户端在线");
    }

    /**
     * 收到消息时
     * @param message
     * @throws IOException
     */
    @OnMessage
    public void onMessage(String message) throws IOException {
//        logger.info("onMessage: [clientId: " + clientId + " ,message:" + message + "]");
        logger.info("onMessage: [message:" + message + "]");
    }

    /**
     * 发生error时
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        logger.info("onError: [clientId: " + clientId + " ,error:" + error.getCause() + "]");
    }

    /**
     * 指定端末发送消息
     * @param message
     * @param clientId
     * @throws IOException
     */
    public void sendMessageByClientId(String message, String clientId) throws IOException {
        for (CustomizedWebSocketServer item : clients.values()) {
            if (item.clientId.equals(clientId) ) {
                item.session.getAsyncRemote().sendText(message);
            }
        }
    }

    /**
     * 所有端末发送消息
     * @param message
     * @throws IOException
     */
    public void sendMessageAll(String message) throws IOException {
        synchronized (lockA) {
            for (CustomizedWebSocketServer item : clients.values()) {
                item.session.getAsyncRemote().sendText(message);
            }
        }

    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        CustomizedWebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        CustomizedWebSocketServer.onlineCount--;
    }

    public static synchronized Map<String, CustomizedWebSocketServer> getClients() {
        return clients;
    }
}
