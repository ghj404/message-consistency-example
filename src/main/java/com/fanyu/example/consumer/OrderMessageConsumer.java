package com.fanyu.example.consumer;

import com.fanyu.example.event.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单消息消费者
 *
 * @author fanyu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageConsumer {

    private final ObjectMapper objectMapper;

    /**
     * 监听订单创建消息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "order.created.queue", durable = "true"),
            exchange = @Exchange(value = "order.exchange", type = "topic"),
            key = "order.created"
    ))
    public void handleOrderCreated(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            // 解析消息
            String messageBody = new String(message.getBody());
            OrderCreatedEvent event = objectMapper.readValue(messageBody, OrderCreatedEvent.class);

            log.info("接收到订单创建消息: orderNo={}, userId={}, productName={}, totalAmount={}",
                    event.getOrderNo(), event.getUserId(), event.getProductName(), event.getTotalAmount());

            // 处理订单创建事件（例如：发送通知、更新库存、积分奖励等）
            processOrderCreatedEvent(event);

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("订单创建消息处理成功，orderNo: {}", event.getOrderNo());

        } catch (Exception e) {
            log.error("订单创建消息处理失败，deliveryTag: {}", deliveryTag, e);

            try {
                // 消息处理失败，拒绝消息并重新入队
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                log.error("拒绝消息失败", ioException);
            }
        }
    }

    /**
     * 处理订单创建事件
     */
    private void processOrderCreatedEvent(OrderCreatedEvent event) {
        // 模拟业务处理
        log.info("处理订单创建事件：");
        log.info("  - 发送用户通知");
        log.info("  - 更新商品库存");
        log.info("  - 增加用户积分");
        log.info("  - 发送邮件/短信通知");

        // 实际业务逻辑在这里实现
        // 例如：
        // notificationService.sendOrderNotification(event);
        // inventoryService.decreaseStock(event.getProductName(), event.getQuantity());
        // pointService.addPoints(event.getUserId(), calculatePoints(event.getTotalAmount()));
    }
}
