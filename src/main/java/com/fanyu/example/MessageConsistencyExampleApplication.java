package com.fanyu.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 消息一致性示例应用启动类
 *
 * @author fanyu
 */
@SpringBootApplication(scanBasePackages = {"com.fanyu"})
@MapperScan({"com.fanyu.example.mapper", "com.fanyu.core.mapper"})
public class MessageConsistencyExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageConsistencyExampleApplication.class, args);
    }
}
