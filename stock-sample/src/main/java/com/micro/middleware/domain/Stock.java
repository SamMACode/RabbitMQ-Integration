package com.micro.middleware.domain;

import lombok.Data;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock Stock entity
 */
@Data
public class Stock {

    private String ticker;
    private StockExchange exchange;

    public Stock(StockExchange exchange, String ticker) {
        this.ticker = ticker;
        this.exchange = exchange;
    }
}
