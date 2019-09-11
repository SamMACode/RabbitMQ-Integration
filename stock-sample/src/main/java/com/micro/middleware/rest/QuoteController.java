package com.micro.middleware.rest;


import com.micro.middleware.domain.Quote;
import com.micro.middleware.domain.TradeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock QuoteController Logical
 */
@Controller
public class QuoteController {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteController.class);

    private ConcurrentMap<String, TradeResponse> responses = new ConcurrentHashMap<String, TradeResponse>();

    private static final long TIMEOUT = 30000;

    private Queue<Quote> quotes = new PriorityQueue<>(100,
            (quoteA, quoteB) -> new Long(quoteA.getTimestamp() - quoteB.getTimestamp()).intValue());

    /**
     * client订阅topic交换器,处理exchange中发送来数据
     */
    public void handleQuote(Quote quote) {
        LOGGER.info("handleQuote method client receive: [{}]", quote);
        long timestamp = System.currentTimeMillis() - TIMEOUT;
        quotes.stream().filter(quoteItem -> quoteItem.getTimestamp() < timestamp)
                .forEach(quoteItem -> quotes.remove(quoteItem));
        quotes.add(quote);
    }

    /**
     * 在向服务器端发送请求之后,处理server端响应的结果
     */
    public void handleTrade(TradeResponse response) {
        LOGGER.info("handleTrade method client received: [{}]", response);
        String key = response.getRequestId();
        responses.putIfAbsent(key, response);
        Collection<TradeResponse> queue = new ArrayList<>(responses.values());
        // 剔除过期的response数据.
        long timestamp = System.currentTimeMillis() - TIMEOUT;
        // 从queue中移除过期的response数据.
        queue.stream().filter(tradeResponse -> tradeResponse.getTimestamp() < timestamp)
                .forEach(tradeResponse -> responses.remove(tradeResponse));
    }

}
