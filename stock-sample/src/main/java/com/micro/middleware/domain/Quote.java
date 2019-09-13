package com.micro.middleware.domain;

import lombok.Data;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock Quote entity
 */
@Data
public class Quote implements Serializable {

    public Quote(Stock stock, String price) {
        this.stock = stock;
        this.price = price;
    }

    public Quote(Stock stock, String price, long timestamp) {
        this.stock = stock;
        this.price = price;
        this.timestamp = timestamp;
    }

    private Stock stock;
    private String price;
    private long timestamp;

    private DateFormat format = DateFormat.getTimeInstance();

    public String getTimeString() {
        return format.format(new Date(timestamp));
    }

}
