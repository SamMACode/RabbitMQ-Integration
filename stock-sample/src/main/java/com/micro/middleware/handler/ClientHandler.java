package com.micro.middleware.handler;


import com.micro.middleware.domain.Quote;
import com.micro.middleware.domain.Stock;
import com.micro.middleware.domain.TradeResponse;
import com.micro.middleware.rest.StockController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock Sample Configuration Class
 */
@Component
public class ClientHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);

    @Autowired
    private StockController stockController;

    public void handleMessage(Quote quote) {
        Stock stock = quote.getStock();
        LOGGER.info("received market data, ticker = " + stock.getTicker() + ",price = " + quote.getPrice());
        // 调用controller在界面上进行数据展示.
        stockController.displayQuote(quote);
    }

    public void handleMessage(TradeResponse response) {
        LOGGER.info("received trade response. [" + response + "]");
        // 在向server端发送请求之后,得到其反馈.
        stockController.updateTrade(response);
    }
}
