package com.mqexample.demo.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.mqexample.demo.config.RabbitMQConfig;
import com.rabbitmq.client.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonServer;
import lombok.SneakyThrows;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.amqp.transport.AmqpError;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static io.vertx.proton.ProtonHelper.message;

/**
 * @Author sgx
 * @Date 2024/3/17 16:37
 * @Description:
 */
@Component
public class AmqpPushService extends AbstractVerticle {

    private static final int PORT = 5673;

    //日期格式化
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


    @Override
    public void start() throws Exception {
        // 创建 Proton 服务器实例
        ProtonServer server = ProtonServer.create(vertx);

        // 配置新连接的处理方式
        server.connectHandler((connection) -> {
            initConnection(vertx, connection); // 初始化连接
        });

        // 监听指定端口，处理连接请求
        server.listen(PORT, (res) -> {
            if (res.succeeded()) {
                System.out.println("Listening on port " + res.result().actualPort());
            } else {
                System.out.println("Failed to start listening on port " + PORT + ":");
                res.cause().printStackTrace();
            }
        });
    }

    // 初始化新连接
    private static void initConnection(Vertx vertx, ProtonConnection connection) {
        // 处理连接打开事件
        connection.openHandler(res -> {
            System.out.println("Client connection opened, container-id: " + connection.getRemoteContainer());
            connection.open();
        });

        // 处理连接关闭事件
        connection.closeHandler(c -> {
            System.out.println("Client closing connection, container-id: " + connection.getRemoteContainer());
            connection.close();
            connection.disconnect();
        });

        // 处理连接断开事件
        connection.disconnectHandler(c -> {
            System.out.println("Client socket disconnected, container-id: " + connection.getRemoteContainer());
            connection.disconnect();
        });

        // 处理会话打开事件
        connection.sessionOpenHandler(session -> {
            session.closeHandler(x -> {
                session.close();
                session.free();
            });
            session.open();
        });

        // 处理发送者打开事件
        connection.senderOpenHandler(sender -> {
            initSender(vertx, connection, sender); // 初始化发送者
        });

        // 处理接收者打开事件
        connection.receiverOpenHandler(receiver -> {
            try {
                initReceiver(receiver); // 初始化接收者
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    // 初始化发送者
    private static void initSender(Vertx vertx, ProtonConnection connection, ProtonSender sender) {
        // 获取远程源
        org.apache.qpid.proton.amqp.messaging.Source remoteSource = (org.apache.qpid.proton.amqp.messaging.Source) sender.getRemoteSource();
        if (remoteSource == null) {
            // 如果远程源为空，则关闭发送者并设置错误条件
            sender.setTarget(null);
            sender.setCondition(new ErrorCondition(AmqpError.INVALID_FIELD, "No source terminus specified"));
            sender.open();
            sender.close();
            return;
        }

        // 如果远程源是动态的，则为其设置动态地址
        if (remoteSource.getDynamic()) {
            String dynamicAddress = UUID.randomUUID().toString();
            remoteSource.setAddress(dynamicAddress);
        }

        // 配置发送者的本地源和目标
        sender.setSource(remoteSource);
        sender.setTarget(sender.getRemoteTarget());


        //消费消息队列里面的消息，然后推送给客户端=================================================================
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("127.0.0.1");
        connectionFactory.setPort(5672);
        try {
            //建立与rabbitmq的连接
            Connection connectionRabbitMq = connectionFactory.newConnection();

            Channel channel = connectionRabbitMq.createChannel();

            channel.queueDeclare(RabbitMQConfig.RABBITMQ_DEMO_TOPIC, true, false, false, null);
            DefaultConsumer defaultConsumer = new DefaultConsumer(channel) {
                @SneakyThrows
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {

                    super.handleDelivery(consumerTag, envelope, properties, body);

                    String message = new String(body, "UTF-8");
                    // 获取当前时间的毫秒级时间戳
                    long currentTimeMillis = System.currentTimeMillis();
                    // 将毫秒级时间戳转换为Date对象
                    Date currentDate = new Date(currentTimeMillis);
                    String sendTime = sdf.format(currentDate);
                    System.out.println("发送时间：" + sendTime);

                    JSONObject json = JSONObject.parseObject(message);
                    json.put("sendTime", sendTime);
                    //System.out.println(json);

                    String jsonString = json.toString();
                    System.out.println("发送到客户端：" + jsonString);


                    Message m = message(jsonString);

                    Thread.sleep(500);

                    sender.send(m);


                }
            };
            channel.basicConsume(RabbitMQConfig.RABBITMQ_DEMO_TOPIC, true, defaultConsumer);

        } catch (Exception e) {
            e.printStackTrace();
        }


        //========================================================================================================


        // 打开发送者
        sender.open();
    }

    // 初始化接收者
    private static void initReceiver(ProtonReceiver receiver) throws InterruptedException {

        //CountDownLatch latch = new CountDownLatch(1); // 创建一个CountDownLatch
        // 获取远程目标
        org.apache.qpid.proton.amqp.messaging.Target remoteTarget = (org.apache.qpid.proton.amqp.messaging.Target) receiver.getRemoteTarget();
        if (remoteTarget == null) {
            // 如果远程目标为空，则关闭接收者并设置错误条件
            receiver.setTarget(null);
            receiver.setCondition(new ErrorCondition(AmqpError.INVALID_FIELD, "No target terminus specified"));
            receiver.open();
            receiver.close();
            return;
        }

        // 如果远程目标是动态的，则为其设置动态地址
        if (remoteTarget.getDynamic()) {
            String dynamicAddress = UUID.randomUUID().toString();
            remoteTarget.setAddress(dynamicAddress);
        }

        // 配置接收者的本地源和目标
        receiver.setTarget(remoteTarget);
        receiver.setSource(receiver.getRemoteSource());

        // 处理到达消息的事件
        receiver.handler((delivery, msg) -> {
            String address = remoteTarget.getAddress();
            if (address == null) {
                address = msg.getAddress();
            }

            Section body = msg.getBody();
            if (body instanceof AmqpValue) {
                String content = (String) ((AmqpValue) body).getValue();
                System.out.println("Received message for address: " + address + ", body: " + content);
            }
        });
        //latch.await(); // 等待CountDownLatch的计数为0

        // 处理接收者分离事件
        receiver.detachHandler(x -> {
            receiver.detach();
            receiver.free();
        });

        // 处理接收者关闭事件
        receiver.closeHandler(x -> {
            receiver.close();
            receiver.free();
        });

        // 打开接收者
        receiver.open();
    }
}
