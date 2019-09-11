package com.micro.middleware.rabbitmq.errorhandler;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
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
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ErrorHandler;

/**
 * @author Sam Ma
 * Use Jackson Util in Spring Boot RabbitMQ for message transfer
 */
@EnableRabbit
@SpringBootApplication
public class RabbitAckStrategyApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitAckStrategyApp.class);

    private static final String ERROR_TEST_QUEUE = "error_test_queue";

    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext configContext = SpringApplication.run(RabbitAckStrategyApp.class, args);
        configContext.getBean(RabbitAckStrategyApp.class).initRabbitAckStrategy();
        configContext.close();
    }

    private void initRabbitAckStrategy() throws InterruptedException {
        this.rabbitTemplate.convertAndSend(ERROR_TEST_QUEUE, new MessagePojo("messagePojo"));
        /**
         * Caused by: com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'some':
         * was expecting ('true', 'false' or 'null')
         */
        this.rabbitTemplate.convertAndSend(ERROR_TEST_QUEUE, new MessagePojo("badMessagePojo"), message ->
            // new Message("{\"some\": \"bad json\"}".getBytes(), m.getMessageProperties())
            // 使用错误的json格式,当对其按照json格式进行转换的时候,会抛出转换异常 MessageConversionException,该fatal异常级别较高不会进行默认处理.
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
    public SimpleRabbitListenerContainerFactory rabbitListenerContainer(ConnectionFactory connectionFactory,
                                                                        SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jsonConvert());
        factory.setErrorHandler(errorHandler());
        return factory;
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
     * define custom error handler strategy, override DefaultExceptionStrategy.isUserCauseFatal method
     * relevant see https://docs.spring.io/spring-amqp/docs/2.0.13.RELEASE/reference/html/_reference.html#exception-handling
     */
    public static class CustomFatalExceptionStrategy extends ConditionalRejectingErrorHandler.DefaultExceptionStrategy {

        /**
         * Since version 1.6.3 a convenient way to add user exceptions to the fatal list is to subclass ConditionalRejectingErrorHandler.DefaultExceptionStrategy
         * and override the method isUserCauseFatal(Throwable cause) to return true for fatal exceptions.
         */
        @Override
        public boolean isUserCauseFatal(Throwable throwable) {
            LOGGER.error("CustomFatalExceptionStrategy.isUserCauseFatal method, [{}]", throwable.getLocalizedMessage());
            if (throwable instanceof ListenerExecutionFailedException) {
                ListenerExecutionFailedException listenerException = (ListenerExecutionFailedException) throwable;
                LOGGER.error("failed to process inbound message from queue: [{}] failed message[{}]",
                        listenerException.getFailedMessage().getMessageProperties().getConsumerQueue(),
                        listenerException.getFailedMessage());
            }
            return true;
        }

        @Override
        public boolean isFatal(Throwable throwable) {
            LOGGER.info("CustomFatalExceptionStrategy.isFatal method, [{}]", throwable.getLocalizedMessage());
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