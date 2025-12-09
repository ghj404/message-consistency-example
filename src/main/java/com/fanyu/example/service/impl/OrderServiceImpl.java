package com.fanyu.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanyu.core.annotation.MessageConsistency;
import com.fanyu.core.service.LocalMessageService;
import com.fanyu.example.dto.CreateOrderRequest;
import com.fanyu.example.entity.Order;
import com.fanyu.example.event.OrderCreatedEvent;
import com.fanyu.example.mapper.OrderMapper;
import com.fanyu.example.service.OrderService;
import com.fanyu.example.vo.OrderVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 订单服务实现类
 *
 * @author fanyu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final LocalMessageService localMessageService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * RabbitMQ交换机名称
     */
    private static final String ORDER_EXCHANGE = "order.exchange";

    /**
     * RabbitMQ路由键
     */
    private static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    @Override
    @Transactional(rollbackFor = Exception.class)
    @MessageConsistency(
            destination = ORDER_EXCHANGE,
            routingKey = ORDER_CREATED_ROUTING_KEY,
            messageType = "ORDER_CREATED",
            businessKey = "#order.orderNo"
    )
    public OrderVO createOrderWithAnnotation(CreateOrderRequest request) {
        log.info("开始创建订单（注解式），用户ID: {}, 商品: {}", request.getUserId(), request.getProductName());

        // 1. 创建订单
        Order order = buildOrder(request);
        orderMapper.insert(order);
        log.info("订单创建成功，订单号: {}", order.getOrderNo());

        // 2. 构建订单创建事件
        OrderCreatedEvent event = buildOrderCreatedEvent(order);

        // 3. 注解会自动保存本地消息表并发送消息
        // 方法返回值会作为消息体，通过AOP拦截处理
        return convertToVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrderWithProgrammatic(CreateOrderRequest request) {
        log.info("开始创建订单（编程式），用户ID: {}, 商品: {}", request.getUserId(), request.getProductName());

        // 1. 创建订单
        Order order = buildOrder(request);
        orderMapper.insert(order);
        log.info("订单创建成功，订单号: {}", order.getOrderNo());

        // 2. 构建订单创建事件
        OrderCreatedEvent event = buildOrderCreatedEvent(order);

        // 3. 编程式保存本地消息并发送
        try {
            String messageBody = objectMapper.writeValueAsString(event);

            // 3.1 保存本地消息表
            String messageId = localMessageService.saveRabbitMessage(
                    ORDER_EXCHANGE,
                    ORDER_CREATED_ROUTING_KEY,
                    messageBody,
                    "ORDER_CREATED",
                    order.getOrderNo()
            );

            // 3.2 发送消息到RabbitMQ
            rabbitTemplate.convertAndSend(
                    ORDER_EXCHANGE,
                    ORDER_CREATED_ROUTING_KEY,
                    messageBody,
                    message -> {
                        message.getMessageProperties().setMessageId(messageId);
                        message.getMessageProperties().setCorrelationId(messageId);
                        return message;
                    }
            );

            log.info("订单消息发送成功，messageId: {}, orderNo: {}", messageId, order.getOrderNo());
        } catch (JsonProcessingException e) {
            log.error("订单消息序列化失败", e);
            throw new RuntimeException("订单消息序列化失败", e);
        }

        return convertToVO(order);
    }

    @Override
    public OrderVO getOrderByNo(String orderNo) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderMapper.selectOne(queryWrapper);

        if (order == null) {
            return null;
        }

        return convertToVO(order);
    }

    /**
     * 构建订单对象
     */
    private Order buildOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(request.getUserId());
        order.setProductName(request.getProductName());
        order.setProductPrice(request.getProductPrice());
        order.setQuantity(request.getQuantity());

        // 计算总金额
        BigDecimal totalAmount = request.getProductPrice()
                .multiply(new BigDecimal(request.getQuantity()));
        order.setTotalAmount(totalAmount);

        // 初始状态为待支付
        order.setStatus(0);

        return order;
    }

    /**
     * 构建订单创建事件
     */
    private OrderCreatedEvent buildOrderCreatedEvent(Order order) {
        return new OrderCreatedEvent(
                order.getOrderNo(),
                order.getUserId(),
                order.getProductName(),
                order.getTotalAmount(),
                System.currentTimeMillis()
        );
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 转换为VO
     */
    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);

        // 设置状态描述
        switch (order.getStatus()) {
            case 0:
                vo.setStatusDesc("待支付");
                break;
            case 1:
                vo.setStatusDesc("已支付");
                break;
            case 2:
                vo.setStatusDesc("已取消");
                break;
            default:
                vo.setStatusDesc("未知");
        }

        return vo;
    }
}
