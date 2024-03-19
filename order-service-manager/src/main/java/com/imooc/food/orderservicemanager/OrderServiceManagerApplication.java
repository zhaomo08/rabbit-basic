package com.imooc.food.orderservicemanager;

import com.imooc.food.orderservicemanager.dao.OrderDetailDao;
import com.imooc.food.orderservicemanager.service.OrderMessageService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@SpringBootApplication
@MapperScan("com.imooc.food")
@ComponentScan("com.imooc.food")
@EnableAsync
public class OrderServiceManagerApplication {

    @Autowired
    OrderDetailDao orderDetailDao;
    @Autowired
    OrderMessageService orderMessageService;


    public static void main(String[] args) throws IOException, TimeoutException {
        SpringApplication.run(OrderServiceManagerApplication.class, args);

    }
}
