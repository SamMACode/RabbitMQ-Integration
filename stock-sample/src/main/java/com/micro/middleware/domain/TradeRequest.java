package com.micro.middleware.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock TradeRequest entity
 */
@Data
public class TradeRequest {
    private String ticker;
    private long quantity;
    private BigDecimal price;
    private String orderType;
    private String accountName;
    private Boolean buyRequest;
    private String username;
    private String password;
    private String requestId;
    private String id = UUID.randomUUID().toString();

}
