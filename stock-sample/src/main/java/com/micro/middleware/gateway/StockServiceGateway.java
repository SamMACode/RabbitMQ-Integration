package com.micro.middleware.gateway;

import com.micro.middleware.domain.TradeRequest;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock StockServiceGateway Interface
 */
public interface StockServiceGateway {

    /**
     * 用于向服务端发送请求
     */
    void send(TradeRequest request);

}
