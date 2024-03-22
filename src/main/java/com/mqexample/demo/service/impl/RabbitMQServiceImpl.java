package com.mqexample.demo.service.impl;

import cn.hutool.http.ContentType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mqexample.demo.config.RabbitMQConfig;
import com.mqexample.demo.config.RabbitmqConfirmCallback;
import com.mqexample.demo.pojo.AtomicString;
import com.mqexample.demo.pojo.MessageA;
import com.mqexample.demo.service.CustomizedWebSocketServer;
import com.mqexample.demo.service.RabbitMQService;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author sgx
 * @Date 2024/1/13 22:26
 * @Description:
 */
@Slf4j
@Service
@Component
public class RabbitMQServiceImpl implements RabbitMQService {

    //日期格式化
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Resource
    private RabbitmqConfirmCallback rabbitmqConfirmCallback;

    @Resource
    private RabbitTemplate rabbitTemplate;


    @Resource
    private SimpleRabbitListenerContainerFactory factory;

    private final SseEmitter sseEmitter = new SseEmitter();


    @PostConstruct
    public void init() {
        //指定 ConfirmCallback
        rabbitTemplate.setConfirmCallback(rabbitmqConfirmCallback);
        //指定 ReturnCallback
        //rabbitTemplate.setReturnCallback(rabbitmqConfirmCallback);
    }


    //从rabbitmq消费来消息后使用SSE推送到APP
    @Override
    public SseEmitter SseSendMessage() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("127.0.0.1");
        connectionFactory.setPort(5672);
        try {
            //建立新连接
            Connection connection = connectionFactory.newConnection();

            Channel channel = connection.createChannel();

            channel.queueDeclare(RabbitMQConfig.RABBITMQ_DEMO_TOPIC, true, false, false, null);
            DefaultConsumer defaultConsumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {

                    super.handleDelivery(consumerTag, envelope, properties, body);

                    String message = new String(body, "UTF-8");
                    // 获取当前时间的毫秒级时间戳
                    long currentTimeMillis = System.currentTimeMillis();
                    // 将毫秒级时间戳转换为Date对象
                    Date currentDate = new Date(currentTimeMillis);
                    String sendTime = sdf.format(currentDate);
                    System.out.println("发送时间："+sendTime);

                    JSONObject json=JSONObject.parseObject(message);
                    json.put("sendTime",sendTime);

                    String jsonString = json.toString();

                    System.out.println("发送到客户端："+jsonString);

                    sseEmitter.send(jsonString);

                }
            };
            channel.basicConsume(RabbitMQConfig.RABBITMQ_DEMO_TOPIC, true, defaultConsumer);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sseEmitter;

    }


    private void establishChannel() {
        //通过连接工厂创建新的连接来和mq建立连接
        ConnectionFactory connectionFactory = new ConnectionFactory();
        //设置参数
        connectionFactory.setHost("127.0.0.1");//ip
        connectionFactory.setPort(5672);//端口
        try {
            //建立新连接
            Connection connection = connectionFactory.newConnection();
            //创建会话通道,生产者和mq服务所有通信都在channel中完成
            Channel channel = connection.createChannel();
            //监听队列
            //声明队列String queue, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments
            //参数名称:
            // 1.队列名称
            // 2.是否持久化mq重启后队列还在
            // 3.是否独占连接,队列只允许在该连接中访问,如果连接关闭后就会自动删除了,设置true可用于临时队列的创建
            // 4.自动删除,队列不在使用时就自动删除,如果将此参数和exclusive参数设置为true时,就可以实现临时队列
            // 5.参数,可以设置一个队列的扩展参数,比如可以设置队列存活时间
            channel.queueDeclare(RabbitMQConfig.RABBITMQ_DEMO_TOPIC, true, false, false, null);

            //实现消费方法
            DefaultConsumer defaultConsumer = new DefaultConsumer(channel) {
                /**
                 * 接受到消息后此方法将被调用
                 * @param consumerTag  消费者标签 用来标记消费者的,可以不设置,在监听队列的时候设置
                 * @param envelope  信封,通过envelope可以获取到交换机,获取用来标识消息的ID,可以用于确认消息已接收
                 * @param properties 消息属性,
                 * @param body 消息内容
                 * @throws IOException
                 */
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    super.handleDelivery(consumerTag, envelope, properties, body);
                    // 交换机
                    //String exchange = envelope.getExchange();
                    // 路由key
                    //String routingKey = envelope.getRoutingKey();
                    // 消息Id mq在channel中用来标识消息的id,可用于确认消息已接受
                    //long deliveryTag = envelope.getDeliveryTag();
                    // 消息内容
                    String message = new String(body, "UTF-8");


                    // 获取当前时间的毫秒级时间戳
                    long currentTimeMillis = System.currentTimeMillis();
                    // 将毫秒级时间戳转换为Date对象
                    Date currentDate = new Date(currentTimeMillis);
                    String sendTime = sdf.format(currentDate);
                    System.out.println("发送时间："+sendTime);

                    //将消息message转为json，插入时间戳，再转为String，发送
                    JSONObject json=JSONObject.parseObject(message);
                    json.put("sendTime",sendTime);

                    String jsonString = json.toString();

                    System.out.println("发送到客户端："+jsonString);


                    //websocket多线程推送
                    ExecutorService executor = Executors.newFixedThreadPool(5);

                    for (int i = 0; i < 5; i++) {
                        executor.execute(() -> {
                                    CustomizedWebSocketServer a = new CustomizedWebSocketServer();
                                    try {
                                        a.sendMessageAll(jsonString);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                        );
                    }


                    //将消费来的消息使用websocket推送到客户端
//                    CustomizedWebSocketServer a = new CustomizedWebSocketServer();
//                    a.sendMessageAll(jsonString);
                }
            };
            // 监听消息String queue, boolean autoAck, Map<String, Object> arguments, Consumer callback
            //参数名称:
            // 1.队列名称
            // 2.自动回复, 当消费者接受消息之后要告诉mq消息已经接受,如果将此参数设置为true表示会自动回复mq,如果设置为false要通过编程去实现了
            // 3.callback 消费方法,当消费者接受到消息之后执行的方法
            channel.basicConsume(RabbitMQConfig.RABBITMQ_DEMO_TOPIC, true, defaultConsumer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private void websocketPushMsg() throws IOException {
//        if (receivedBody != null) {
//            // 处理接收到的消息体
//            String message = new String(receivedBody, "UTF-8");
//
//            System.out.println(message);
//            // 获取当前时间的毫秒级时间戳
//            long currentTimeMillis = System.currentTimeMillis();
//            // 将毫秒级时间戳转换为Date对象
//            Date currentDate = new Date(currentTimeMillis);
//            String sendTime = sdf.format(currentDate);
//            System.out.println("发送时间：" + sendTime);
//
//            JSONObject json = JSONObject.parseObject(message);
//            json.put("sendTime", sendTime);
//
//            String jsonString = json.toString();
//
//            System.out.println("发送到客户端：" + jsonString);
//
//
//            //System.out.println("message from rabbitmq: " + message);
//
//            //将消费来的消息使用websocket推送到客户端
//            CustomizedWebSocketServer a = new CustomizedWebSocketServer();
//            a.sendMessageAll(jsonString);
//
//        }
//
//    }


    //发送消息到消息队列中
    @Override
    public String sendMsg(String msg) throws Exception {
        String msgId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        JSONObject json = new JSONObject();
        json.put("msg", msg);
        json.put("msgId", msgId);
        //json.put("sendTime", sendTime);
        //log.info("ss"+json);
        // 将JSONObject转为字符串
        String jsonString = json.toString();
        // 打印JSON字符串到控制台
        System.out.println(jsonString);

        try {
            //参数：交换机名字，交换机和队列绑定的匹配键，前端传来的msg，UUID
            rabbitTemplate.convertAndSend(RabbitMQConfig.RABBITMQ_DEMO_DIRECT_EXCHANGE,
                    RabbitMQConfig.RABBITMQ_DEMO_DIRECT_ROUTING,
                    jsonString,//String类型发送
                    new CorrelationData(msgId));
            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }


    @Override
    public void singleThread_consumer() throws IOException {

        RabbitMQServiceImpl rabbitMQService = new RabbitMQServiceImpl();
        rabbitMQService.establishChannel();

    }


    @Override
    public void multiThread_consumer() {

        ExecutorService executor = Executors.newFixedThreadPool(5);

        //提交五个任务
        for (int i = 0; i < 5; i++) {
            executor.execute(() -> {
                        RabbitMQServiceImpl rabbitMQService_1 = new RabbitMQServiceImpl();
                        rabbitMQService_1.establishChannel();
                    }
            );
        }

    }


//    private Map<String, Object> getMessage(String msg) {
//        String msgId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
//        CorrelationData correlationData = new CorrelationData(msgId);
//        String sendTime = sdf.format(new Date());
//        Map<String, Object> map = new HashMap<>();
//        map.put("msgId", msgId);
//        map.put("sendTime", sendTime);
//        map.put("msg", msg);
//        map.put("correlationData", correlationData);
//        return map;
//    }


}