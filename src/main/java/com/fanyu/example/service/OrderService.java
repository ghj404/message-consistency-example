package com.fanyu.example.service;

import com.fanyu.example.dto.CreateOrderRequest;
import com.fanyu.example.vo.OrderVO;

/**
 * 订单服务接口
 *
 * @author fanyu
 */
public interface OrderService {

    /**
     * 创建订单（使用注解式消息一致性）
     *
     * @param request 创建订单请求
     * @return 订单详情
     */
    OrderVO createOrderWithAnnotation(CreateOrderRequest request);

    /**
     * 创建订单（使用编程式消息一致性）
     *
     * @param request 创建订单请求
     * @return 订单详情
     */
    OrderVO createOrderWithProgrammatic(CreateOrderRequest request);

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    OrderVO getOrderByNo(String orderNo);
}
