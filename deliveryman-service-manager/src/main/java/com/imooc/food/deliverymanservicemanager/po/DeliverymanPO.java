package com.imooc.food.deliverymanservicemanager.po;

import com.imooc.food.deliverymanservicemanager.enummeration.DeliverymanStatus;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
//@Data   不太推荐   也包含了 EqualsAndHashCode 这个注解
public class DeliverymanPO {
    private Integer id;
    private String name;
    private String district;
    private DeliverymanStatus status;
    private Date date;
}
