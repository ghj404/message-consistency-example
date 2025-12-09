package com.fanyu.example.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单创建事件消息
 *
 * @author fanyu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 事件时间戳
     */
    private Long timestamp;
}
