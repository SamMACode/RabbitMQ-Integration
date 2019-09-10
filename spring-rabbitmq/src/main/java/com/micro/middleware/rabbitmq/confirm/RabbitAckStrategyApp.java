package com.micro.middleware.rabbitmq.confirm;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ErrorHandler;

import javax.annotation.PostConstruct;

/**
 * @author Sam Ma
 * Use Jackson Util in Spring Boot RabbitMQ for message transfer
 */
@SpringBootApplication
public class RabbitAckStrategyApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitAckStrategyApp.class);

    public static void main(String[] args) {
        SpringApplication.run(RabbitAckStrategyApp.class, args);
    }

    private static final String ERROR_TEST_QUEUE = "error_test_queue";

    @PostConstruct
    private void initRabbitAckStrategy() throws InterruptedException {
        this.rabbitTemplate.convertAndSend(ERROR_TEST_QUEUE, new MessagePojo("messagePojo"));
        /**
         * Caused by: com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'some': was expecting ('true', 'false' or 'null')
         */
        this.rabbitTemplate.convertAndSend(ERROR_TEST_QUEUE, new MessagePojo("badMessage"), message ->
                // new Message("{\"some\": \"bad json\"}".getBytes(), m.getMessageProperties())
                // 使用错误的json格式,当对其按照json格式进行转换的时候,会抛出转换异常>
                new Message("some bad json".getBytes(), message.getMessageProperties())
        );
        Thread.sleep(5000);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * register error handler strategy and Jackson converter
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainer(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory containerFactory = new SimpleRabbitListenerContainerFactory();
        containerFactory.setConnectionFactory(connectionFactory);
        containerFactory.setMessageConverter(jsonConvert());
        containerFactory.setErrorHandler(errorHandler());
        return containerFactory;
    }

    @RabbitListener(queues = ERROR_TEST_QUEUE)
    public void handleMessage(MessagePojo messagePojo) {
        LOGGER.info("handleMessage method, receive message: [{}]", messagePojo);
    }

    @Bean
    public Queue ackStrategyQueue() {
        return new Queue(ERROR_TEST_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonConvert() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public ErrorHandler errorHandler() {
        return new ConditionalRejectingErrorHandler(new CustomFatalExceptionStrategy());
    }

    /**
     * define custom error handler strategy
     */
    public static class CustomFatalExceptionStrategy extends ConditionalRejectingErrorHandler.DefaultExceptionStrategy  {
        @Override
        public boolean isFatal(Throwable throwable) {
            if (throwable instanceof ListenerExecutionFailedException) {
                ListenerExecutionFailedException listenerException = (ListenerExecutionFailedException) throwable;
                LOGGER.error("failed to process inbound message from queue: [{}] failed message[{}]",
                        listenerException.getFailedMessage().getMessageProperties().getConsumerQueue(),
                        listenerException.getFailedMessage());
            }
            return super.isFatal(throwable);
        }
    }

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class MessagePojo {
    private String content;
}