package com.micro.middleware.service.impl;

import com.micro.middleware.domain.TradeRequest;
import com.micro.middleware.service.CreditCheckService;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock CreditCheckService Interface implementation
 */
@Service
public class CreditCheckServiceImpl implements CreditCheckService {

    @Override
    public boolean canExecuteTrade(TradeRequest request, List<Error> errors) {
        return true;
    }
}
