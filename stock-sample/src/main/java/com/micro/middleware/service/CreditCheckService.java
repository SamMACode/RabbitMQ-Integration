package com.micro.middleware.service;


import com.micro.middleware.domain.TradeRequest;

import java.util.List;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock MarketDataGateway Interface
 */
public interface CreditCheckService {

    /**
     * 对证券交易的客户身份信息进行校验
     * @param request
     * @param errors
     * @return
     */
    boolean canExecuteTrade(TradeRequest request, List<Error> errors);
}
