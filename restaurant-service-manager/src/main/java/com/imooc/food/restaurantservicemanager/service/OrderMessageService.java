package com.imooc.food.restaurantservicemanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imooc.food.restaurantservicemanager.dao.ProductDao;
import com.imooc.food.restaurantservicemanager.dao.RestaurantDao;
import com.imooc.food.restaurantservicemanager.dto.OrderMessageDTO;
import com.imooc.food.restaurantservicemanager.enummeration.ProductStatus;
import com.imooc.food.restaurantservicemanager.enummeration.RestaurantStatus;
import com.imooc.food.restaurantservicemanager.po.ProductPO;
import com.imooc.food.restaurantservicemanager.po.RestaurantPO;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OrderMessageService {

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ProductDao productDao;
    @Autowired
    RestaurantDao restaurantDao;

    @Autowired
    Channel channel;

    DeliverCallback deliverCallback = (consumerTag, message) -> {
        String messageBody = new String(message.getBody());
        log.info("deliverCallback:messageBody:{}", messageBody);
        try {
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody,
                    OrderMessageDTO.class);

            ProductPO productPO = productDao.selsctProduct(orderMessageDTO.getProductId());
            log.info("onMessage:productPO:{}", productPO);
            RestaurantPO restaurantPO = restaurantDao.selsctRestaurant(productPO.getRestaurantId());
            log.info("onMessage:restaurantPO:{}", restaurantPO);
            if (ProductStatus.AVALIABLE == productPO.getStatus() && RestaurantStatus.OPEN == restaurantPO.getStatus()) {
                orderMessageDTO.setConfirmed(true);
                orderMessageDTO.setPrice(productPO.getPrice());
            } else {
                orderMessageDTO.setConfirmed(false);
            }
            log.info("sendMessage:restaurantOrderMessageDTO:{}", orderMessageDTO);

//            try (Connection connection = connectionFactory.newConnection();
//                 Channel channel = connection.createChannel()) {
//                channel.addReturnListener(new ReturnListener() {
//                    @Override
//                    public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) throws IOException {
//                        // 也可以在此做业务操作
//                        log.info("Message Return: replyCode:{}, replyText:{}," +
//                                        " exchange:{}, routingKey:{}, " +
//                                        "properties:{}, " + "body:{}",
//                                replyCode, replyText, exchange, routingKey, properties, body);
//
//                    }
//                });
            channel.addReturnListener(new ReturnCallback() {
                @Override
                public void handle(Return returnMessage) {
                    log.info("Message Return: returnMessage:{}", returnMessage);
                }
            });
//            if (message.getEnvelope().getDeliveryTag() % 2 == 0) {
//                channel.basicAck(message.getEnvelope().getDeliveryTag(), true);
//            }
            Thread.sleep(3000);
            channel.basicNack(message.getEnvelope().getDeliveryTag(), false, false);

//            channel.basicAck(message.getEnvelope().getDeliveryTag(), true);  //  没有手动 ack  在管控台  queue Unacked 在微服务关闭后 改为 ready 状态

//                channel.basicNack(message.getEnvelope().getDeliveryTag(), false,true);  // 手动拒收 重回队列
            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
            channel.basicPublish("exchange.order.restaurant",
                    "key.order",
                    true,
                    null,
                    messageToSend.getBytes());

            Thread.sleep(1000);  // channel 不会被关闭  可以添加的 listener 代码可以执行
//            }
        } catch (JsonProcessingException | InterruptedException e) {
            e.printStackTrace();
        }
    };

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        log.info("start linstening message");
        // DLX
        channel.exchangeDeclare(
                "exchange.dlx",
                BuiltinExchangeType.TOPIC,
                true,
                false,
                null
        );

        channel.queueDeclare(
                "queue.dlx",
                true,
                false,
                false,
                null
        );

        channel.queueBind(
                "queue.dlx",
                "exchange.dlx",
                "#"
        );


        channel.exchangeDeclare(
                "exchange.order.restaurant",
                BuiltinExchangeType.DIRECT,
                true,
                false,
                null);

        //设置队列TTL
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-message-ttl", 150000);
        args.put("x-max-length", 5);
        args.put("x-dead-letter-exchange", "exchange.dlx");

        channel.queueDeclare(
                "queue.restaurant",
                true,
                false,
                false,
                args);

        channel.queueBind(
                "queue.restaurant",
                "exchange.order.restaurant",
                "key.restaurant");

        channel.basicQos(2);

        channel.basicConsume("queue.restaurant", false, deliverCallback, consumerTag -> {
        });
        while (true) {
            Thread.sleep(6000);
        }
    }
}

