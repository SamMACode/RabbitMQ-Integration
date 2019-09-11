package com.micro.middleware.service.impl;

import com.micro.middleware.domain.TradeRequest;
import com.micro.middleware.domain.TradeResponse;
import com.micro.middleware.service.ExecuteEvenuteService;
import org.springframework.stereotype.Service;


/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock ExecuteEvenuteService Interface
 */
@Service
public class ExecuteEvenuteServiceImpl implements ExecuteEvenuteService {

    @Override
    public TradeResponse executeTradeRequest(TradeRequest request) {
        return null;
    }
}
