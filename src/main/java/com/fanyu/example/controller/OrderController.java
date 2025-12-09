package com.fanyu.example.controller;

import com.fanyu.example.dto.CreateOrderRequest;
import com.fanyu.example.service.OrderService;
import com.fanyu.example.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 *
 * @author fanyu
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单（注解式消息一致性）
     */
    @PostMapping("/create/annotation")
    public Result<OrderVO> createOrderWithAnnotation(@RequestBody CreateOrderRequest request) {
        log.info("接收到创建订单请求（注解式）: {}", request);
        OrderVO order = orderService.createOrderWithAnnotation(request);
        return Result.success(order);
    }

    /**
     * 创建订单（编程式消息一致性）
     */
    @PostMapping("/create/programmatic")
    public Result<OrderVO> createOrderWithProgrammatic(@RequestBody CreateOrderRequest request) {
        log.info("接收到创建订单请求（编程式）: {}", request);
        OrderVO order = orderService.createOrderWithProgrammatic(request);
        return Result.success(order);
    }

    /**
     * 查询订单
     */
    @GetMapping("/{orderNo}")
    public Result<OrderVO> getOrder(@PathVariable String orderNo) {
        log.info("查询订单，订单号: {}", orderNo);
        OrderVO order = orderService.getOrderByNo(orderNo);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 通用响应结果
     */
    public static class Result<T> {
        private int code;
        private String message;
        private T data;

        public static <T> Result<T> success(T data) {
            Result<T> result = new Result<>();
            result.code = 200;
            result.message = "success";
            result.data = data;
            return result;
        }

        public static <T> Result<T> error(String message) {
            Result<T> result = new Result<>();
            result.code = 500;
            result.message = message;
            return result;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}
