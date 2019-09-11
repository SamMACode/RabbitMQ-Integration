package com.micro.middleware.gateway.impl;

import com.micro.middleware.domain.TradeRequest;
import lombok.Data;
import org.springframework.amqp.rabbit.core.RabbitGatewaySupport;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author Sam Ma
 * Used for send trades request to an external process
 */
@Data
public class StockServiceGatewayImpl extends RabbitGatewaySupport implements com.micro.middleware.gateway.StockServiceGateway {

    private String defaultReplyTo;

    @Override
    public void send(TradeRequest request) {
        getRabbitOperations().convertAndSend(request, (message) -> {
            // 在MessagePostProcessor中通过MessageProperties设置消息处理的结果发送到那个队列<defaultReplyTo>.
            message.getMessageProperties().setReplyTo(defaultReplyTo);
            // 为每个消息设置唯一的id标识UUID.randomUUID().toString().
            message.getMessageProperties().setCorrelationId(UUID.randomUUID().toString());
            return message;
        });
    }
}
