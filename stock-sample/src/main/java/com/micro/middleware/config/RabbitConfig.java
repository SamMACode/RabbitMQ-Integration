package com.micro.middleware.config;

import com.micro.middleware.gateway.MarketDataGateway;
import com.micro.middleware.gateway.StockServiceGateway;
import com.micro.middleware.gateway.impl.MarketDataGatewayImpl;
import com.micro.middleware.gateway.impl.StockServiceGatewayImpl;
import com.micro.middleware.handler.ClientHandler;
import com.micro.middleware.handler.ServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;

/**
 * @author Sam Ma
 * RabbitMQ Message Middleware Stock Sample Configuration Class
 */
@Configuration
public class RabbitConfig {

    /**
     * 定义交换器信息,server端将stock股票消息发送到exchange交换器内
     */
    private static final String MARKET_DATA_EXCHANGE_NAME = "app.stock.marketdata";

    /**
     * 定义消息队列queue,客户端将请求的消息发送到queue中(server端定义用来接收请求)
     */
    private static final String STOCK_REQUEST_QUEUE_NAME = "app.stock.request";

    /**
     * clients客户端会将用户的请求发送到<"config.stock.request>队列,并使用默认的路由routing key
     */
    private static final String STOCK_REQUEST_ROUTING_KEY = STOCK_REQUEST_QUEUE_NAME;

    /**
     * Server Side Profile Configuration
     */
    @Profile("server")
    @Configuration
    public static class ServerConfiguration {

        private static final Logger LOGGER = LoggerFactory.getLogger(ServerConfiguration.class);

        @Autowired
        private MessageConverter jsonConverter;

        @Autowired
        private ServerHandler serverHandler;

        @Autowired
        private ConnectionFactory connectionFactory;

        @Autowired
        private RabbitTemplate rabbitTemplate;

        @Autowired
        private MarketDataGateway marketDataGateway;

        @PostConstruct
        protected void configureRabbitTemplate() {
            rabbitTemplate.setExchange(MARKET_DATA_EXCHANGE_NAME);
        }

        /**
         * 该队列queue用于存放交易请求,该队列会绑定到默认的direct交换器上.
         */
        @Bean
        public Queue stockRequestQueue() {
            return new Queue(STOCK_REQUEST_QUEUE_NAME);
        }

        @Bean
        public MessageListenerAdapter messageListenerAdapter() {
            return new MessageListenerAdapter(serverHandler, jsonConverter);
        }

        /**
         * 对server端的接手请求的queue进行监听,并设置其severHandler处理机制:
         * 1.handleMessage(TradeRequest request)用于处理前段向后台发送tradeRequest请求信息.
         * 监听队列<stockRequestQueue()>
         */
        @Bean
        public SimpleMessageListenerContainer messageListenerContainer() {
            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
            // 设置进行监听的消息队列<stockRequestQueue>.
            container.setQueues(stockRequestQueue());
            container.setMessageListener(messageListenerAdapter());
            return container;
        }

        /**
         * 定义MarketDataGateway实体类,会周期的fixedRate向消息队列发送消息.
         * @param connectionFactory
         * @return
         */
        @Bean
        public MarketDataGateway marketDataGateway(ConnectionFactory connectionFactory) {
            MarketDataGatewayImpl marketDataGateway = new MarketDataGatewayImpl();
            marketDataGateway.setConnectionFactory(connectionFactory);
            return marketDataGateway;
        }

        @Scheduled(cron = "0/5 * * * * ?")
        public void scheduledMarketData() {
            marketDataGateway.sendMarketData();
        }

    }

    /**
     * Client Side Profile Configuration
     */
    @Profile("client")
    @Configuration
    public static class ClientConfiguration {

        @Autowired
        private ClientHandler clientHandler;

        @Autowired
        private MessageConverter jsonConverter;

        @Value("${stocks.quote.pattern}")
        private String marketRoutingKey;

        @Autowired
        private ConnectionFactory connectionFactory;

        @Qualifier("marketDataExchange")
        @Autowired
        private TopicExchange marketDataExchange;

        @Autowired
        private RabbitTemplate rabbitTemplate;

        @PostConstruct
        @Autowired
        protected void configureRabbitTemplate() {
            rabbitTemplate.setRoutingKey(STOCK_REQUEST_ROUTING_KEY);
        }

        /**
         * 定义队列消息监听container:
         * 1.handleMessage(Quote quote)处理的是stock交易市场传输过来的stock股票信息.监听队列<marketDataQueue>
         * 2.handleMessage(TradeResponse response) 处理的是发送交易请求transRequest之后,处理server响应的TradeResponse.监听队列<traderJoeQueue>
         */
        @Bean
        public SimpleMessageListenerContainer messageListenerContainer() {
            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
            // 设置对给定的消息队列进行监听.
            container.setQueues(marketDataQueue(), traderJoeQueue());
            container.setMessageListener(messageListenerAdapter());
            // 设置消息的回复类型为Auto回复.
            container.setAcknowledgeMode(AcknowledgeMode.AUTO);
            return container;
        }

        @Bean
        public MessageListenerAdapter messageListenerAdapter() {
            return new MessageListenerAdapter(clientHandler, jsonConverter);
        }

        /**
         * 对于给定的marketDataQueue()队列进行绑定的设置Binding,指定其exchange交换器以及路由marketRoutingKey.
         */
        @Bean
        public Queue marketDataQueue() {
            return new AnonymousQueue();
        }

        /**
         * 声明匿名消息队列,其绑定到默认的交换器上.
         */
        @Bean
        public Queue traderJoeQueue() {
            return new AnonymousQueue();
        }

        @Bean
        public Binding marketDataBinding() {
            return BindingBuilder.bind(marketDataQueue()).to(marketDataExchange).with(marketRoutingKey);
        }

        @Bean
        public StockServiceGateway stockServiceGateway(RabbitTemplate rabbitTemplate) {
            StockServiceGatewayImpl gateway = new StockServiceGatewayImpl();
            gateway.setDefaultReplyTo(traderJoeQueue().getName());
            gateway.setRabbitOperations(rabbitTemplate);
            return gateway;
        }

    }

    /**
     * 定义topic交换器:server端将stock信息发送到交换器上,客户端client订阅消息队列queue内容.
     */
    @Bean(name = "marketDataExchange")
    public TopicExchange marketDataExchange() {
        return new TopicExchange(MARKET_DATA_EXCHANGE_NAME);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
