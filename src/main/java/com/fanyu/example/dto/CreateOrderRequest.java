package com.fanyu.example.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单请求DTO
 *
 * @author fanyu
 */
@Data
public class CreateOrderRequest {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品价格
     */
    private BigDecimal productPrice;

    /**
     * 购买数量
     */
    private Integer quantity;
}
