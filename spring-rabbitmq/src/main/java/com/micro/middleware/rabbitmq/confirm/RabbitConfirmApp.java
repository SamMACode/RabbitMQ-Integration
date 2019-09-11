package com.micro.middleware.rabbitmq.confirm;


import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * @author Sam Ma
 * Spring RabbitMQ Confirm Strategy to Ensure Message dilivery Consistency
 */
@SpringBootApplication
public class RabbitConfirmApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitConfirmApp.class);

    private static final String CONFIRM_QUEUE = "spring.publisher.sample";

    /**
     * define some CountDown Latch strategy
     */
    private final CountDownLatch confirmLatch = new CountDownLatch(1);

    private final CountDownLatch returnLatch = new CountDownLatch(1);

    private final CountDownLatch listenerLatch = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext configContext = SpringApplication.run(RabbitConfirmApp.class, args);
        configContext.getBean(RabbitConfirmApp.class).rabbitConfirmApp();
        configContext.close();
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void rabbitConfirmApp() throws InterruptedException {
        setupRabbitCallback();

        this.rabbitTemplate.convertAndSend("", CONFIRM_QUEUE, "foo", new CorrelationData("Correlation for message 1"));
        Integer awaitTime = 10;

        if (this.confirmLatch.await(awaitTime, TimeUnit.SECONDS)) {
            LOGGER.info("[RabbitConfirmApp] confirmed received.");
        } else {
            LOGGER.info("[RabbitConfirmApp] confirmed not received.");
        }

        if (this.listenerLatch.await(awaitTime, TimeUnit.SECONDS)) {
            LOGGER.info("[RabbitConfirmApp] confirm returned by listener.");
        } else {
            LOGGER.info("[RabbitConfirmApp] message not received by listener.");
        }

        // Send a message to the default exchange to be routed to a noe-existent queue.
        this.rabbitTemplate.convertAndSend("", CONFIRM_QUEUE + CONFIRM_QUEUE, "bar", message -> {
            LOGGER.info("[noe-existent queue] message after conversion: [{}]", message);
            return message;
        });

        if (this.returnLatch.await(awaitTime, TimeUnit.SECONDS)) {
            LOGGER.info("[noe-existent queue] return received.");
        } else {
            LOGGER.info("[noe-existent queue] return not received.");
        }
    }

    private void setupRabbitCallback() {
        /**
         * confirms/returns enabled in application.properties - add the callback here.
         * */
        this.rabbitTemplate.setConfirmCallback((correlation, ack, reason) -> {
            if (correlation != null) {
                LOGGER.info("[Received]: " + (ack ? "ack" : "nack") + "for correlation: " + correlation);
            }
            this.confirmLatch.countDown();
        });

        this.rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            LOGGER.info("[Returned:] " + message + "\nreplyCode:" + replyCode
                    + "\nreplyText: " + replyText + "\nexchange/rk: " + exchange + "/" + routingKey);
            this.returnLatch.countDown();
        });

        /**
         * replace the correlation data with one containing the converted message in case
         * we want to resend it after a nack.
         * */
        this.rabbitTemplate.setCorrelationDataPostProcessor((message, correlationData) ->
                new CompleteMessageCorrelationData(correlationData != null ? correlationData.getId() : null, message)
        );

    }

    @RabbitListener(queues = CONFIRM_QUEUE)
    public void confirmQueueListener(String message) {
        LOGGER.info("received message [{}] from queue: [{}]", message, CONFIRM_QUEUE);
        this.listenerLatch.countDown();
    }

    @Bean
    public Queue confirmQueue() {
        return new Queue(CONFIRM_QUEUE, true);
    }

    /**
     * Base class for correlating publisher confirms to sent messages
     */
    @Getter
    class CompleteMessageCorrelationData extends CorrelationData {
        private final Message message;

        CompleteMessageCorrelationData(String id, Message message) {
            super(id);
            this.message = message;
        }

        @Override
        public String toString() {
            return "CompleteMessageCorrelationData [id=" + getId() + " , message=" + this.getMessage() + "]";
        }
    }

}
