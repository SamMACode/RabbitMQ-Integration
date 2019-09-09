package com.micro.middleware.rabbitmq.json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Sam Ma
 * Use Jackson Util in Spring Boot RabbitMQ for message transfer
 */
@SpringBootApplication
public class RabbitSerializeApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitSerializeApp.class);

	public static void main(String[] args) {
		SpringApplication.run(RabbitSerializeApp.class, args);
	}

    /**
     * define some rabbitmq message queue
     */
    private static final String INFERRED_FOO_QUEUE = "sample.inferred.foo";
    private static final String INFERRED_BAR_QUEUE = "sample.inferred.bar";
    private static final String RECEIVE_AND_CONVERT_QUEUE = "sample.receive.and.convert";
    private static final String MAPPED_QUEUE = "sample.mapped";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Qualifier("jsonRabbitTemplate")
    @Autowired
    private RabbitTemplate jsonRabbitTemplate;

    private volatile CountDownLatch latchDown = new CountDownLatch(2);

    @PostConstruct
    public void runJsonTransfer() throws InterruptedException {
        String json = "{\"foo\" : \"value\"}";
        Message message = MessageBuilder.withBody(json.getBytes())
                .andProperties(MessagePropertiesBuilder.newInstance().setContentType("application/json").build())
                .build();
        // inferred type, convert object data to json
        this.rabbitTemplate.send(INFERRED_FOO_QUEUE, message);
        this.rabbitTemplate.send(INFERRED_BAR_QUEUE, message);
        this.latchDown.await(10, TimeUnit.SECONDS);
        // convertAndReceive with type.
        this.rabbitTemplate.send(RECEIVE_AND_CONVERT_QUEUE, message);
        this.rabbitTemplate.send(RECEIVE_AND_CONVERT_QUEUE, message);

        FooPojo foo = this.jsonRabbitTemplate.receiveAndConvert(RECEIVE_AND_CONVERT_QUEUE, 10_000,
                new ParameterizedTypeReference<FooPojo>(){});
        LOGGER.info("[RECEIVE_AND_CONVERT_QUEUE] Excepted a Foo, got a [{}] data [{}]", foo.getClass().toString(), foo);
        BarPojo bar = this.jsonRabbitTemplate.receiveAndConvert(RECEIVE_AND_CONVERT_QUEUE, 10_000,
                new ParameterizedTypeReference<BarPojo>(){});
        LOGGER.info("[RECEIVE_AND_CONVERT_QUEUE] Excepted a Bar, get a [{}] data [{}]", bar.getClass().toString(), bar);

        // mapped type information with legacy POJO listener.
        this.latchDown = new CountDownLatch(2);
        message.getMessageProperties().setHeader("__TypeId__", "foo");
        this.rabbitTemplate.send(MAPPED_QUEUE, message);
        message.getMessageProperties().setHeader("__TypeId__", "bar");
        this.rabbitTemplate.send(MAPPED_QUEUE, message);
        // this.latch.await方法会阻塞主线程10s的时间,让其他线程完成对队列中消息的处理,之后删除rabbitQueue队列.
        this.latchDown.await(10, TimeUnit.SECONDS);
        this.amqpAdmin.deleteQueue(RECEIVE_AND_CONVERT_QUEUE);
    }

    /**
     * define message middleware queue listener
     */
    @RabbitListener(queues = INFERRED_FOO_QUEUE)
    public void listenForAFoo(FooPojo foo) {
        LOGGER.info("listenForAFoo method listener Expected a Foo, got a [{}]", foo);
        this.latchDown.countDown();
    }

    @RabbitListener(queues = INFERRED_BAR_QUEUE)
    public void listenForAFoo(BarPojo bar) {
        LOGGER.info("listenForAFoo method listener Expected a Bar, get a [{}]", bar);
        this.latchDown.countDown();
    }

    /**
     * define message listener container for MAPPED_QUEUE queue
     */
    @Bean
    public SimpleMessageListenerContainer legacyPojoListener(ConnectionFactory factory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(factory);
        container.setQueueNames(MAPPED_QUEUE);
        MessageListenerAdapter messageListener = new MessageListenerAdapter(new Object() {
            @SuppressWarnings("unused")
            public void handleMessage(Object object) {
                LOGGER.info("legacyPojoListener method [SimpleMessageListenerContainer] Got a [" + object.getClass() + "]" + object);
                RabbitSerializeApp.this.latchDown.countDown();
            }
        });
        Jackson2JsonMessageConverter jsonConverter = new Jackson2JsonMessageConverter();
        jsonConverter.setClassMapper(classMapper());
        messageListener.setMessageConverter(jsonConverter);
        container.setMessageListener(messageListener);
        return container;
    }

    @Bean
    public DefaultClassMapper classMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>(2);
        idClassMapping.put("foo", FooPojo.class);
        idClassMapping.put("bar", BarPojo.class);
        classMapper.setIdClassMapping(idClassMapping);
        return classMapper;
    }

    @Bean
    public MessageConverter jsonConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean(name = "jsonRabbitTemplate")
    public RabbitTemplate jsonRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonConverter());
        return template;
    }

    @Bean
    public Queue inferredFoo() {
        return new Queue(INFERRED_FOO_QUEUE, true);
    }

    @Bean
    public Queue inferredBar() {
        return new Queue(INFERRED_BAR_QUEUE, true);
    }

    @Bean
    public Queue convertAndReceiveQueue() {
        return new Queue(RECEIVE_AND_CONVERT_QUEUE, true);
    }

    @Bean
    public Queue mappedQueue() {
        return new Queue(MAPPED_QUEUE, true);
    }

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class FooPojo {
    private String foo;
}

class BarPojo extends FooPojo {
    public BarPojo() {
        super();
    }

    private BarPojo(String foo) {
        super(foo);
    }

}
