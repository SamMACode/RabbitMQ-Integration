package com.micro.middleware.handler;


import com.micro.middleware.domain.TradeRequest;
import com.micro.middleware.domain.TradeResponse;
import com.micro.middleware.service.CreditCheckService;
import com.micro.middleware.service.ExecuteEvenuteService;
import com.micro.middleware.service.TradingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock Sample Configuration Class
 */
@Component
public class ServerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class);

    @Autowired
    private ExecuteEvenuteService executeService;

    @Autowired
    private CreditCheckService creditCheckService;

    @Autowired
    private TradingService tradingService;

    /**
     * handleMessage方法用于MessageListenerContainer中监听队列的信息,
     * */
    public TradeResponse handleMessage(TradeRequest request) {
        LOGGER.info("enter ServerHandler.handleMessage method, request: [{}]", request);
        TradeResponse response;
        List<Error> errors = new ArrayList<>();
        if(creditCheckService.canExecuteTrade(request, errors)) {
            response = executeService.executeTradeRequest(request);
        } else {
            response = new TradeResponse();
            response.setError(true);
            response.setErrorMessage(StringUtils.arrayToDelimitedString(errors.toArray(), ","));
        }
        tradingService.proceessTrade(request, response);
        LOGGER.info("complete ServerHandler.handleMessage method, response: [{}]", response);
        return response;
    }

}
