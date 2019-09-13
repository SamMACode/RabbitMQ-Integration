package com.micro.middleware.gateway.impl;

import com.micro.middleware.domain.Quote;
import com.micro.middleware.domain.Stock;
import com.micro.middleware.domain.StockExchange;
import com.micro.middleware.gateway.MarketDataGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitGatewaySupport;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Sam Ma
 * Used for Send marketing stock relevant information
 */
public class MarketDataGatewayImpl extends RabbitGatewaySupport implements MarketDataGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketDataGatewayImpl.class);

    private static final Random RANDOM = new Random();

    private final List<MockStock> stocks = new ArrayList<>();

    public MarketDataGatewayImpl() {
        this.stocks.add(new MockStock("AAPL", StockExchange.nasdaq, 255));
        this.stocks.add(new MockStock("CSCO", StockExchange.nasdaq, 22));
        this.stocks.add(new MockStock("DELL", StockExchange.nasdaq, 15));
        this.stocks.add(new MockStock("GOOG", StockExchange.nasdaq, 500));
        this.stocks.add(new MockStock("INTC", StockExchange.nasdaq, 22));
        this.stocks.add(new MockStock("MSFT", StockExchange.nasdaq, 29));
        this.stocks.add(new MockStock("ORCL", StockExchange.nasdaq, 24));
        this.stocks.add(new MockStock("CAJ", StockExchange.nyse, 43));
        this.stocks.add(new MockStock("F", StockExchange.nyse, 12));
        this.stocks.add(new MockStock("GE", StockExchange.nyse, 18));
        this.stocks.add(new MockStock("HMC", StockExchange.nyse, 32));
        this.stocks.add(new MockStock("HPQ", StockExchange.nyse, 48));
        this.stocks.add(new MockStock("IBM", StockExchange.nyse, 130));
        this.stocks.add(new MockStock("TM", StockExchange.nyse, 76));
    }

    @Override
    public void sendMarketData() {
        Quote quote = generateFakeQuote();
        Stock stock = quote.getStock();
        LOGGER.info("Sending market data for [{}] security ticket [{}] exchange", stock.getTicker(), stock.getExchange());
        String routingKey = "config.stock.quotes." + stock.getExchange() + "." + stock.getTicker();
        getRabbitOperations().convertAndSend(routingKey, quote);
    }

    private Quote generateFakeQuote() {
        MockStock stock = this.stocks.get(RANDOM.nextInt(this.stocks.size()));
        String price = stock.randomPrice();
        return new Quote(stock, price);
    }

    /**
     * Defined Security Domain Object
     */
    private static class MockStock extends Stock {
        private final int basePrice;
        private final DecimalFormat twoPlacesFormat = new DecimalFormat("0.00");

        private MockStock(String ticker, StockExchange stockExchange, int basePrice) {
            super(stockExchange, ticker);
            this.basePrice = basePrice;
        }

        private String randomPrice() {
            return this.twoPlacesFormat.format(this.basePrice + Math.abs(RANDOM.nextGaussian()));
        }
    }

}
