package com.micro.middleware.service;

import com.micro.middleware.domain.TradeRequest;
import com.micro.middleware.domain.TradeResponse;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock TradingService Interface
 */
public interface TradingService {

    /**
     * 进行交易处理TradingService
     * @param request
     * @param response
     */
    void proceessTrade(TradeRequest request, TradeResponse response);
}
