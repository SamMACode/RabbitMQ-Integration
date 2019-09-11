package com.micro.middleware.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock TradeResponse entity
 */
@Data
public class TradeResponse {

    private String ticker;
    private long quantity;
    private BigDecimal price;
    private String orderType;
    private String confirmationNumber;
    private boolean error;
    private String errorMessage;
    private String accountName;
    private long timestamp = System.currentTimeMillis();
    private String requestId;

}
