package com.imooc.food.orderservicemanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.imooc.food.orderservicemanager.dao.OrderDetailDao;
import com.imooc.food.orderservicemanager.dto.OrderMessageDTO;
import com.imooc.food.orderservicemanager.enummeration.OrderStatus;
import com.imooc.food.orderservicemanager.po.OrderDetailPO;
import com.imooc.food.orderservicemanager.vo.OrderCreateVO;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private OrderDetailDao orderDetailDao;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    public String exchangeName;
    @Value("${rabbitmq.restaurant-routing-key}")
    public String restaurantRoutingKey;
    @Value("${rabbitmq.deliveryman-routing-key}")
    public String deliverymanRoutingKey;

    ObjectMapper objectMapper = new ObjectMapper();


    public void createOrder(OrderCreateVO orderCreateVO) throws IOException, TimeoutException, InterruptedException {
        log.info("createOrder:orderCreateVO:{}", orderCreateVO);
        OrderDetailPO orderPO = new OrderDetailPO();
        orderPO.setAddress(orderCreateVO.getAddress());
        orderPO.setAccountId(orderCreateVO.getAccountId());
        orderPO.setProductId(orderCreateVO.getProductId());
        orderPO.setStatus(OrderStatus.ORDER_CREATING);
        orderPO.setDate(new Date());
        orderDetailDao.insert(orderPO);

        OrderMessageDTO orderMessageDTO = new OrderMessageDTO();
        orderMessageDTO.setOrderId(orderPO.getId());
        orderMessageDTO.setProductId(orderPO.getProductId());
        orderMessageDTO.setAccountId(orderCreateVO.getAccountId());

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
            channel.confirmSelect();
            channel.addConfirmListener(new ConfirmListener() {
                public void handleAck(long deliveryTag, boolean multiple) throws IOException {
                    log.info("Ack, deliveryTag: {}, multiple: {}", deliveryTag, multiple);
                }

                public void handleNack(long deliveryTag, boolean multiple) throws IOException {
                    log.info("Nack, deliveryTag: {}, multiple: {}", deliveryTag, multiple);

                }
            });

            //设置单条消息TTL
//            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().expiration("15000").build();
            channel.basicPublish("exchange.order.restaurant", "key.restaurant", null, messageToSend.getBytes());

//            for (int i = 0; i < 50; i++) {
//                channel.basicPublish("exchange.order.restaurant", "key.restaurant", null, messageToSend.getBytes());
//                log.info("message sent  successfully");
//            }
            log.info("message sent successfully");
//            if (channel.waitForConfirms()) {
//                log.info("RabbitMQ confirm successfully");
//            } else {
//                log.info("RabbitMQ confirm fail");
//            }
            Thread.sleep(1000);

        }
    }

}
