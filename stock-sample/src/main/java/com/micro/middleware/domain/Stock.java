package com.micro.middleware.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock Stock entity
 */
@Data
public class Stock implements Serializable {

    private String ticker;
    private StockExchange exchange;

    public Stock(StockExchange exchange, String ticker) {
        this.ticker = ticker;
        this.exchange = exchange;
    }
}
