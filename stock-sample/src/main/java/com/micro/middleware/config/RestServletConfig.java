package com.micro.middleware.config;


import com.micro.middleware.gateway.StockServiceGateway;
import com.micro.middleware.gateway.impl.StockServiceGatewayImpl;
import com.micro.middleware.rest.QuoteController;
import com.micro.middleware.rest.StockController;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author Sam Ma
 * RabbitMQ Message Middleware RestServletConfig Configuration Class
 */
@Configuration
public class RestServletConfig {

    private static final String TRADE_QUEUE = "tradeQueue";

    private static final String MARKET_DATA_QUEUE = "marketDataQueue";

    @Value("${web.stocks.quote.pattern}")
    private String webStockPattern;

    @Autowired
    private MessageConverter messageConverter;

    @Autowired
    private QuoteController quoteController;

    @Autowired
    private StockController stockController;

    @Bean
    public StockServiceGateway stockServiceGateway(ConnectionFactory connectionFactory) {
        StockServiceGatewayImpl stockGateway = new StockServiceGatewayImpl();

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setRoutingKey("app.stock.request");
        rabbitTemplate.setMessageConverter(messageConverter);
        stockGateway.setRabbitOperations(rabbitTemplate);

        stockGateway.setDefaultReplyTo("fanout://broadcast.responses/");
        return stockGateway;
    }

    @Bean
    public SimpleMessageListenerContainer quoteListenerContainer(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);

        MessageListenerAdapter quoteListenerAdapter = new MessageListenerAdapter(quoteController, "handleQuote");
        quoteListenerAdapter.setMessageConverter(this.messageConverter);
        container.setMessageListener(quoteListenerAdapter);
        container.setQueueNames(MARKET_DATA_QUEUE);

        return container;
    }

    @Bean
    public SimpleMessageListenerContainer stockListenerContainer(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);

        MessageListenerAdapter quoteListenerAdapter = new MessageListenerAdapter(stockController, "handleTrade");
        quoteListenerAdapter.setMessageConverter(this.messageConverter);
        container.setMessageListener(quoteListenerAdapter);
        container.setQueueNames(TRADE_QUEUE);

        return container;
    }


    /**
     * define binding with fanout Exchange
     */
    @Bean
    public Binding tradeFanoutQueueBinding() {
        return BindingBuilder.bind(marketDataQueue()).to(fanoutExchange());
    }

    @Bean
    public Binding appStockMarketDataTopicBinding() {
        return BindingBuilder.bind(marketDataQueue()).to(topicExchange()).with(webStockPattern);
    }

    /**
     * define fanout Exchange and Topic Exchange
     */
    @Bean(name = "broadcast.responses")
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange("broadcastExchange");
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange("topicExchange");
    }

    /**
     * define tradeQueue queue and marketDataQueue queue
     */
    @Bean
    public Queue tradeQueue() {
        return new Queue(TRADE_QUEUE, true);
    }

    @Bean
    public Queue marketDataQueue() {
        return new Queue(MARKET_DATA_QUEUE, true);
    }

}
