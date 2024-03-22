package com.imooc.food.orderservicemanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imooc.food.orderservicemanager.dao.OrderDetailDao;
import com.imooc.food.orderservicemanager.dto.OrderMessageDTO;
import com.imooc.food.orderservicemanager.enummeration.OrderStatus;
import com.imooc.food.orderservicemanager.po.OrderDetailPO;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OrderMessageService {

//    @Value("${rabbitmq.exchange}")
//    public String exchangeName;
//    @Value("${rabbitmq.deliveryman-routing-key}")
//    public String deliverymanRoutingKey;
//    @Value("${rabbitmq.settlement-routing-key}")
//    public String settlementRoutingKey;
//    @Value("${rabbitmq.reward-routing-key}")
//    public String rewardRoutingKey;


    @Autowired
    private OrderDetailDao orderDetailDao;
    ObjectMapper objectMapper = new ObjectMapper();


    public void handleMessage1(byte[] messageBody) throws Exception {
        log.info("handleOrderMessage:messageBody:{}", new String(messageBody));
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        try {
            // 将消息体反序列化为 DTO
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody,
                    OrderMessageDTO.class);

            // 数据库中读取订单 PO
            OrderDetailPO orderPO = orderDetailDao.selectOrder(orderMessageDTO.getOrderId());

            switch (orderPO.getStatus()) {

                case ORDER_CREATING:
                    if (orderMessageDTO.getConfirmed() && null != orderMessageDTO.getPrice()) {
                        orderPO.setStatus(OrderStatus.RESTAURANT_CONFIRMED);
                        orderPO.setPrice(orderMessageDTO.getPrice());
                        orderDetailDao.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.deliveryman", "key.deliveryman", null,
                                    messageToSend.getBytes());
                        }
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderPO);
                    }
                    break;
                case RESTAURANT_CONFIRMED:
                    if (null != orderMessageDTO.getDeliverymanId()) {
                        orderPO.setStatus(OrderStatus.DELIVERYMAN_CONFIRMED);
                        orderPO.setDeliverymanId(orderMessageDTO.getDeliverymanId());
                        orderDetailDao.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.settlement",
                                    "key.settlement",
                                    null,
                                    messageToSend.getBytes());
                        }
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderPO);
                    }
                    break;
                case DELIVERYMAN_CONFIRMED:
                    if (null != orderMessageDTO.getSettlementId()) {
                        orderPO.setStatus(OrderStatus.SETTLEMENT_CONFIRMED);
                        orderPO.setSettlementId(orderMessageDTO.getSettlementId());
                        orderDetailDao.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.reward", "key.reward", null, messageToSend.getBytes());
                        }
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderPO);
                    }
                    break;
                case SETTLEMENT_CONFIRMED:
                    if (null != orderMessageDTO.getRewardId()) {
                        orderPO.setStatus(OrderStatus.ORDER_CREATED);
                        orderPO.setRewardId(orderMessageDTO.getRewardId());
                        orderDetailDao.update(orderPO);
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderPO);
                    }
                    break;
            }

        } catch (JsonProcessingException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    ;
}
