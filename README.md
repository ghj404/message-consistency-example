# Message Consistency Example

消息一致性组件使用示例项目

## 项目说明

本项目演示如何使用 `message-consistency-spring-boot-starter` 组件实现分布式系统中的消息最终一致性。

## 技术栈

- Java 17
- Spring Boot 3.2.0
- MyBatis Plus 3.5.5
- RabbitMQ
- MySQL 8.0.33

## 快速开始

### 1. 环境准备

#### 1.1 安装MySQL
确保MySQL 8.0+已安装并运行。

#### 1.2 安装RabbitMQ
确保RabbitMQ已安装并运行。

```bash
# 使用Docker运行RabbitMQ（推荐）
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3-management
```

访问 http://localhost:15672 查看RabbitMQ管理界面（默认用户名/密码：guest/guest）

### 2. 数据库初始化

执行数据库初始化脚本：

```bash
mysql -u root -p < src/main/resources/init.sql
```

或者在MySQL客户端中执行 `src/main/resources/init.sql` 文件。

**注意**：消息一致性相关的表会由starter组件自动创建，无需手动创建。

### 3. 配置修改

根据实际环境修改 `src/main/resources/application.yml` 中的配置：

- 数据库连接信息
- RabbitMQ连接信息

### 4. 运行项目

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run
```

## 功能演示

### 1. 注解式消息一致性

使用 `@MessageConsistency` 注解自动保证消息的最终一致性。

**API接口**：
```
POST http://localhost:8080/api/orders/create/annotation
Content-Type: application/json

{
  "userId": 1001,
  "productName": "iPhone 15 Pro",
  "productPrice": 7999.00,
  "quantity": 1
}
```

**实现代码**：
```java
@MessageConsistency(
    exchange = "order.exchange",
    routingKey = "order.created",
    messageType = "ORDER_CREATED",
    businessKeyExpression = "#order.orderNo"
)
public OrderVO createOrderWithAnnotation(CreateOrderRequest request) {
    // 创建订单业务逻辑
    // 方法成功执行后，消息会自动保存到本地消息表并发送到RabbitMQ
}
```

### 2. 编程式消息一致性

手动调用API保存本地消息并发送。

**API接口**：
```
POST http://localhost:8080/api/orders/create/programmatic
Content-Type: application/json

{
  "userId": 1002,
  "productName": "MacBook Pro 16",
  "productPrice": 19999.00,
  "quantity": 1
}
```

**实现代码**：
```java
public OrderVO createOrderWithProgrammatic(CreateOrderRequest request) {
    // 1. 创建订单
    Order order = buildOrder(request);
    orderMapper.insert(order);
    
    // 2. 保存本地消息
    String messageId = localMessageService.saveRabbitMessage(
        ORDER_EXCHANGE,
        ORDER_CREATED_ROUTING_KEY,
        messageBody,
        "ORDER_CREATED",
        order.getOrderNo()
    );
    
    // 3. 发送消息
    rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_CREATED_ROUTING_KEY, messageBody);
}
```

### 3. 查询订单

```
GET http://localhost:8080/api/orders/{orderNo}
```

## 核心功能

### 1. 本地消息表

所有发送的消息都会先保存到本地消息表 `mc_local_message`，确保消息不丢失。

### 2. 消息补偿机制

定时任务会扫描待发送和发送失败的消息，自动重试发送（默认每分钟执行一次）。

### 3. 消息告警

- **重试次数告警**：当消息重试次数达到阈值时触发告警
- **长时间待发送告警**：当消息长时间未成功发送时触发告警

### 4. 消息归档

定时清理已成功发送和失败的历史消息，避免数据库表过大（默认每天凌晨2点执行）。

## 数据库表说明

### 业务表

- **biz_order**：订单表（业务数据）

### 消息一致性相关表（由starter自动创建）

- **mc_local_message**：本地消息表，记录所有发送的消息
- **mc_message_archive**：消息归档表，存储已归档的历史消息
- **mc_alert_log**：告警日志表，记录所有告警信息

## 配置说明

```yaml
message-consistency:
  enabled: true                          # 是否启用组件
  max-retry-times: 5                     # 最大重试次数
  timeout-minutes: 30                    # 消息超时时间（分钟）
  
  compensation:                          # 补偿任务配置
    enabled: true                        # 是否启用补偿任务
    cron: "0 */1 * * * ?"               # 执行频率（每分钟）
    batch-size: 100                      # 每次处理的消息数量
  
  alert:                                 # 告警配置
    enabled: true                        # 是否启用告警
    cron: "0 */5 * * * ?"               # 执行频率（每5分钟）
    channels: LOG                        # 告警渠道：LOG, DINGTALK, EMAIL
    retry-limit: 3                       # 重试次数告警阈值
    long-time-pending-hours: 24          # 长时间待发送告警阈值（小时）
  
  archive:                               # 归档配置
    enabled: true                        # 是否启用归档
    cron: "0 0 2 * * ?"                 # 执行频率（每天凌晨2点）
    sent-days-ago: 7                     # 归档已发送消息（天）
    failed-days-ago: 30                  # 归档失败消息（天）
```

## 测试用例

项目提供了两种创建订单的方式来演示消息一致性的使用：

1. **注解式**：使用 `@MessageConsistency` 注解，代码简洁，推荐使用
2. **编程式**：手动调用API，灵活性更高

可以通过以下方式验证消息一致性：

1. 创建订单后查看数据库 `mc_local_message` 表，确认消息已保存
2. 查看RabbitMQ管理界面，确认消息已发送到队列
3. 查看控制台日志，确认消费者成功消费消息
4. 故意断开RabbitMQ连接，观察补偿机制是否自动重试

## 监控和运维

- 查看本地消息表：`SELECT * FROM mc_local_message ORDER BY create_time DESC LIMIT 20;`
- 查看告警日志：`SELECT * FROM mc_alert_log ORDER BY create_time DESC LIMIT 20;`
- 查看归档消息：`SELECT * FROM mc_message_archive ORDER BY create_time DESC LIMIT 20;`

## 常见问题

### Q1: 消息发送失败怎么办？
A: 组件会自动重试，默认最多重试5次。可以通过 `max-retry-times` 配置调整重试次数。

### Q2: 如何查看消息发送状态？
A: 查询 `mc_local_message` 表，status字段表示消息状态：0-待发送，1-已发送，2-发送失败。

### Q3: 消息表会无限增长吗？
A: 不会。归档任务会定期清理历史消息，已发送的消息7天后归档，失败的消息30天后归档。

## 联系方式

如有问题，请联系：fanyu@example.com
