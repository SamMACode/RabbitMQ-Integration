## stock股票实时展示(使用RabbitMQ作为消息中间件Middleware)
>项目使用`RabbitMQ`作为消息中间件进行消息的发布与订阅，主要的业务逻辑为：server端实时每隔`0.5s`向客户端推送stock股票的实时价格，客户端（`web`或者`swing`）接收服务端推送的股票信息然后在控制面板上进行展示。此外客户端可以向服务端`server`发起购买股票(`stock`)的请求，`server`端收到请求之后对用户的购买请求进行处理，并将处理结果进行返回。客户端收到处理的结果之后，将结果呈现给用户；

使用到`RabbitMQ`的部分: 
* `topic`交换器(`app.stock.marketdata`)：该交换器用于服务器端server推送股票的实时价格。client的队列`(marketDataQueue)`使用特定的路由键`(marketRoutingKey)`与该交换器进行绑定用于接收符合路由规则的stock股票实时数据，多个不同的队列可以与同一个`topic`交换器进行绑定，根据路由规则的不同在不同的队列接收交换器中的不同`stock`实时数据；
* `RabbitMQ`的消息监听机制：用于监听消息队列内容的`serverHandler`在接收到client端的交易数据后，使用配置中对该队列的监听器处理方法`(method)`对新来临的数据进行处理。在server端进行处理之后并将结果`tradeResponse`发布到执行的队列;
* 在`containerListener`中对于指定的消息队列(`queue`)指定监听处理方法：也即当在消息队列`queue`中有新的消息发送来的时候，使用容器监听中的配置对来到的消息进行处理。这增加了对消息内容处理的灵活性；通常对于处理之后的数据是需要展示到用户界面上，通过队列监听方法当侦测到消息队列有新的消息到来之后，就会触发监听方法的调用。为了保证多线程访问的安全，可以将队列中的数据保存在一个临时的`ConcurrentHashMap`，前台页面可以通过`Http`请求获取得到消息队列里面的内容；
* 关于`server`端通过exchange(topic)交换器实时推送股票stock的信息，不同的客户端可以使用消息队列queue与不同的路由规则(`routingkey`)与交换器进行绑定，按照需求进行实时接收股票数据。swing端使用`routing{app.stock.quotes.nasdaq.*}`与exchange绑定接收`nasdaq`类型的stock信息；web端使用`routing{app.stock.quotes.nyse.*}`与exchange绑定用于接收`nyse`类型的stock信息;

应用服务端的相关内容配置：

```java
protected static String MARKET_DATA_EXCHANGE_NAME = "app.stock.marketdata";
@Bean  //声明数据数据marketData的交换器(topic类型).
public TopicExchange marketDataExchange() {
    return new TopicExchange(MARKET_DATA_EXCHANGE_NAME);
}
@Override 
protected void configureRabbitTemplate(RabbitTemplate rabbitTemplate) {
    // 在server端channel设置交换器名称,这样在每次推送stock实时数据的时候不用显示的声明交换器的名称.
    rabbitTemplate.setExchange(MARKET_DATA_EXCHANGE_NAME);
}
// 在sendMarketData方法中使用spring的scheduletask机制，每隔0.5s向交换器推送不同的stock股票信息.
String routingKey = "app.stock.quotes." + stock.getExchange() + "." + stock.getTicker();
getRabbitOperations().convertAndSend(routingKey, quote);
```
客户端的`queue`对消息队列的订阅(`swing`端以及`web`端)：
```java
/* handleMessage(Quote quote) 处理的是stock交易市场传输过来的stock股票信息. 监听队列<marketDataQueue>
*  handleMessage(TradeResponse response) 处理的是发送交易请求transRequest之
*  后,处理server响应的TradeResponse. 监听队列<traderJoeQueue>
*/
@Bean
public SimpleMessageListenerContainer messageListenerContainer() {
  SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory());
  // 设置对给定的消息队列进行监听.
  container.setQueues(marketDataQueue(), traderJoeQueue());
  container.setMessageListener(messageListenerAdapter());
  // 设置消息的回复类型为Auto回复.
  container.setAcknowledgeMode(AcknowledgeMode.AUTO);
  return container;
}
// swing端将接受stock股票的queue与交换器exchange通过路由key[marketRoutingKey]进行绑定
BindingBuilder.bind(marketDataQueue()).to(marketDataExchange()).with(marketRoutingKey)
```
`web`端`xml`配置接收消息队列`queue`中实时的`stock`股票信息.

```xml
<rabbit:queue id="marketDataQueue"></rabbit:queue>
<!-- 将rabbitMQ的返回绑定到<broadcast.responses>交换器,该交换器会使用fanout广播的形式向所有的队列发布该消息. -->
<rabbit:fanout-exchange name="broadcast.responses" xmlns="http://www.springframework.org/schema/rabbit">
  <bindings>
  <!-- 将<tradeQueue>绑定到<broadcast.responses>的交换器上(exchange). -->
    <binding queue="tradeQueue"></binding>
  </bindings>
</rabbit:fanout-exchange>
<!-- 监听marketDataQueue队列,当队列中有新的元素进入使用quote-controller的handleQuote对元素进行处理. -->
<rabbit:listener ref="quote-controller" method="handleQuote" queues="marketDataQueue"></rabbit:listener>
```
`server`端对于交易请求的处理，通过接收`TradeRequest`请求返回得到`TradeResponse`结果，并将返回结果发送到指定的交换器`exchange`或者`queue`中。在一般的`ServerHandler`中`handlerMessage`中可以进行请求处理，但是其结果返回一般多为`void`。由于客户端在将请求上送来的时候对`messageProperties`指定了`replyTo`的值，服务端处理完成之后会将处理的结果发送到指定的交换器或者队列中。在客户端通过对`replyTo`队列或者`exchange`交换器进行监听，当队列中有新的`response`返回的时候客户端立刻对其进行处理。

```java
// 在发送请求的send方法中设置message的属性值setReplyTo(defaultReplyTo)队列.
@Override
public void send(TradeRequest request) {
  getRabbitOperations().convertAndSend(request, (message) -> {
  // 在MessagePostProcessor中通过MessageProperties设置消息处理的结果发送到那个队列<defaultReplyTo>.
  message.getMessageProperties().setReplyTo(defaultReplyTo);
  // 为每个消息设置唯一的id标识UUID.randomUUID().toString().
  message.getMessageProperties().setCorrelationId(UUID.randomUUID().toString());
  return message;
  });
}
/**
  * desc:对server端的接手请求的queue进行监听,并设置其severHandler处理机制.
  *  handleMessage(TradeRequest request)用于处理前段向后台发送tradeRequest请求信息.
  *  监听队列<stockRequestQueue()>
  **/
@Bean
public SimpleMessageListenerContainer messageListenerContainer() {
  SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory());
  // 设置进行监听的消息队列<stockRequestQueue>.
  container.setQueues(stockRequestQueue());
  container.setMessageListener(messageListenerAdapter());
  return container;
}
// swing端接收TradeResponse返回信息.
public void handleMessage(TradeResponse response) {
  log.info("received trade response. [" + response + "]");
  // 在向server端发送请求之后,得到其反馈.
  stockController.updateTrade(response);
}
```
`web`端的配置，发送请求到`queue`并且指定`replyTo`的队列信息.`replyTo`指定的交换器为`fanout`交换器，这样绑定该交换器的所有队列都能够获取得到交易处理的结果信息.

```xml
<rabbit:queue id="tradeQueue"></rabbit:queue>
<!-- 将rabbitMQ的返回绑定到<broadcast.responses>交换器,该交换器会使用fanout广播的形式向所有的队列发布该消息. -->
<rabbit:fanout-exchange name="broadcast.responses" xmlns="http://www.springframework.org/schema/rabbit">
  <bindings>
  <!-- 将<tradeQueue>绑定到<broadcast.responses>的交换器上(exchange). -->
    <binding queue="tradeQueue"></binding>
  </bindings>
</rabbit:fanout-exchange>
<!-- 使用监听器listener监听队列tradeQueue,并使用handleTrade方法进行处理. -->
<rabbit:listener ref="quote-controller" method="handleTrade" queues="tradeQueue"></rabbit:listener>
```