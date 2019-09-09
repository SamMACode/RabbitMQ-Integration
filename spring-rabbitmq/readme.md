## RabbitMQ消息队列

>`RabbitMQ`消息队列简要概述：消息队列的主要作用是将应用程序连接起来，这些消息通过像`RabbitMQ`这样的消息代理服务器在应用程序之间进行路由。这就像是在应用程序之间设置了一个邮局，各模块通过订阅消息队列对队列里的内容进行处理，`RabbitMQ`其底层是基于`erlang`语言编写。

`amqp`的核心元素：交换器、队列和绑定。生产者把消息发布到交换器上，消息最终到达队列并被消费者接收。绑定决定了消息是如何从路由器到达指定的队列。由于操作系统对`TCP`连接进行创建和销毁会付出非常昂贵的代价，因而引入了信道的概念。信道是建立在真实的`TCP`连接上的虚拟连接，`amqp`的命令都是通过信道发送出去的。

需要信道的原因在于：操作系统建立和销毁`TCP`会话是非常昂贵的开销，假设应用从队列消费消息并根据服务合理调度线程，那么只进行`TCP`连接进行连接到`RabbitMQ`时在高峰时期每秒都会有成千上万的`TCP`连接，这不仅会造成`TCP`连接的大量浪费，并且由于硬件资源的内容，该机器的性能很快就会到达瓶颈。

交换器的不同类型：`direct`交换器、`fanout`交换器、`topic`交换器、`headers`交换器。headers交换器允许你配置`amqp`消息的`header`而非路由键；`direct`交换器是非常简单的，如果路由键匹配的话，消息就可以被投递到对应的队列。`fanout`交换器有一些广播的意思，其会将消息投递给所有附加在此交换器上的队列；对于`topic`交换器，其允许你实现有趣的消息通信场景，它使得来自不同源头的消息能够到达同一个队列。

* `RabbitMQ`核心的`API`：
```java
com.rabbitmq.client.ConnectionFactory    //该类主要用来创建rabbit的连接对象
com.rabbitmq.client.Connection			 //通过制定用户名与密码创建连接对象   
com.rabbitmq.client.Channel	 			 //比较核心的操作类,可用于队列的创建、绑定以及消费. 
```
* `Spring-AMQP`对于`RabbitMQ`的支持
>`Spring`框架很好的支持了消息队列`RabbitMQ`，并且对原始底层的`api`也进行了封装，再结合`spring`的特性，极大的简化了使用方式。其官方文档地址为：[spring-amqp]:https://docs.spring.io/spring-amqp/docs/2.0.3.RELEASE/reference/html/_reference.html

1. 使用`spring`的将消息以`json`的形势进行传输，`Spring AMQP`的使用了不同的方式对`MessageConverter`基础类进行了实现，像`Jackson2JsonMessageConverter`就是将队列信息使用`jackson`进行转换。
``` java
template.setMessageConverter(jsonConverter());   //设置转换格式为使用json
// 将要传输的数据以byte字节的形式进行传输,同时设置message的属性为json的编码格式.
Message message =MessageBuilder.withBody(json.getBytes())
.andProperties(MessagePropertiesBuilder.newInstance().setContentType("application/json").build()).build();
// 在设置了使用json的方式进行传输数据之后,使用ParameterizedTypeReference<Foo>
// 将json数据接收到之后转换为object类型.
Foo foo =this.jsonRabbitTemplate
.receiveAndConvert(RECEIVE_AND_CONVERT_QUEUE, 10_000,
       new ParameterizedTypeReference<Foo>() {});
// 此外使用@RabbitListener注解可以对给定的queue进行监听.
@RabbitListener(queues = INFERRED_FOR_QUEUE)
public void listenForAFoo(Foo foo) {
  log.info("listener for expector, got a " + foo);
}
```
2. `spring amqp`中的callback回调方法的注册，在`consumer`端读取到消息`confrim`之后注册事件会进行触发调用。另一种情况是在`confrim`之前如果发生错误的话，也会触发到监听事件。
```java
// rabbitmq消息内容确认.
spring.rabbitmq.publisher-confirms=true
spring.rabbitmq.publisher-returns=true
spring.rabbitmq.template.mandatory=true
```
在启用对回调方法调用的时候需要在`SpringBoot`的`application.properties`文件中进行以上的配置。此外给回调返回的实现都可以使用`Java 8`的`lambda`表达式进行实现。在回调方法里都是函数式的接口`@FunctionalInterface`的修饰.
```java
// 对于消息被消费者消费后,进行confrim之后会触发该监听器事件.
this.rabbitTemplate.setConfirmCallback((correlation, ack, reason) -> {
  if(correlation != null) {
    System.out.println("[Received]: " + (ack ? "ack" : "nack") + "for correlation: " + correlation);}
    this.confirmLatch.countDown();
});
// 这种情况用于当进行消息投递的时候,投送过程中出现了错误或者其他原因的时候回进行返回，然后会触发该回调函数.
this.rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) 
-> {
  System.out.println("[Returned:] " + message + "\nreplyCode:" + replyCode+ "\nreplyText: " + replyText + "\nexchange/rk: " + exchange + "/" + routingKey);
  this.returnLatch.countDown();
});
// 该方法在向消息队列中发送消息之前进行触发.
this.rabbitTemplate.setCorrelationDataPostProcessor((message, correlationData) ->
  new CompleteMesageCorrelationData(correlationData != null ? correlationData.getId() : null, message));
```
3.`RabbitMQ`的全局错误处理机制，进行自定`errorHandler`需要继承。
`ConditionalRejectingErrorHandler.DefaultExceptionStrategy`这个基类，并重写其`isFatal()`方法，向全局的factory中注册`factory.setErrorHandler(errorHandler());`的错误处理机制，当在进行消息投送时候发生异常的情况下就会触发自定义的`Strategy`策略。在使用`json`格式的数据进行发送的时候由于`json`数据格式并不正确，因而会触发全局的错误处理机制。

```java
this.rabbitTemplate.convertAndSend(TEST_QUEUE, new RabbitApplication.Foo("bar"), m ->
  // new Message("{\"some\": \"bad json\"}".getBytes(), m.getMessageProperties())
  // 使用错误的json格式,当对其按照json格式进行转换的时候,会抛出转换异常>
  new Message("some bad json".getBytes(), m.getMessageProperties())
);
```
