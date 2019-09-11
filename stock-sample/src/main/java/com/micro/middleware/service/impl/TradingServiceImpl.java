package com.micro.middleware.service.impl;

import com.micro.middleware.domain.TradeRequest;
import com.micro.middleware.domain.TradeResponse;
import com.micro.middleware.service.TradingService;
import org.springframework.stereotype.Service;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock TradingService Interface
 */
@Service
public class TradingServiceImpl implements TradingService {

    @Override
    public void proceessTrade(TradeRequest request, TradeResponse response) {

    }
}
