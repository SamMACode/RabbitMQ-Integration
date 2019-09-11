package com.micro.middleware.rest;


import com.micro.middleware.domain.Quote;
import com.micro.middleware.domain.TradeRequest;
import com.micro.middleware.domain.TradeResponse;
import com.micro.middleware.gateway.StockServiceGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock StockController Logical
 */
@Controller
public class StockController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StockController.class);

    @Autowired
    private StockServiceGateway serviceGateway;

    public void displayQuote(Quote quote) {
        LOGGER.info(" stock Controller receive market message: [{}]", quote);
    }

    public void updateTrade(TradeResponse response) {
        LOGGER.info(" stock Controller receive trans message: [{}]", response);
    }

    public void sendTradeRequest(String message) {
        String[] tokens = message.split("\\s");
        String quantityString = tokens[0];
        String ticker = tokens[1];
        int quantity = Integer.parseInt(quantityString);
        TradeRequest tr = new TradeRequest();
        tr.setAccountName("ACCT-123");
        tr.setBuyRequest(true);
        tr.setOrderType("MARKET");
        tr.setTicker(ticker);
        tr.setQuantity(quantity);
        tr.setRequestId("REQ-1");
        tr.setUsername("Joe Trader");
        serviceGateway.send(tr);
    }

}
