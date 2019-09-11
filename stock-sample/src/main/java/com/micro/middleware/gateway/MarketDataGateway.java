package com.micro.middleware.gateway;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock MarketDataGateway Interface
 */
public interface MarketDataGateway {

    /**
     * RabbitServer端用于发送stock消息的gateway网关
     */
    void sendMarketData();

}
