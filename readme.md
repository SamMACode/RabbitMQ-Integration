## `RabbitMQ`消息中间件

> `RabbitMQ`是一个开源的消息代理和队列服务器，用来通过普通协议在完全不同的应用之间共享数据，或者简单地将作业排队以便让分布式服务器进行处理。消息队列（`message queuing`）使用消息将应用程序连接起来，这些消息通过像`RabbitMQ`这样的消息代理服务器在应用之间进行路由，这就像在应用程序之间放了一个邮局。其底层使用`erlang`语言进行实现，`erlang`语言在通信上具有很高的效率。

使用`docker`在本地安装`rabbitmq`消息中间件：

```shell
# 从镜像仓库获取rabbitmq镜像
docker pull rabbitmq:management
# 使用docker run命令初始化rabbitmq镜像实例
docker run -d -p 5672:5672 -p 15672:15672 --name rabbitmq rabbitmq:management
~e194a2dbeb52f2296dfb6d1c527cf052d82be5ed9a4c974d70dcd6af3da3eb7e
```

从底部开始构造，队列（`queue`）：

从概念上来讲，`AMQP`消息路由必须有三部分：交换器、队列和绑定。生成者把消息发布到交换器上，消息最终到达队列，并被消费者接收，绑定决定了消息如何从路由器路由到特定的队列。可以通过`AMQP的basic.consume`命令订阅，这样做会将信道置为接收模式，直到取消对队列的订阅为止。如果只想从队列中获取单条消息而不是持续订阅，则可以使用`basic.get`命令从队列中获取消息。

当`RabbitMQ`消息队列拥有多个消费者的时候，队列收到的消息会以轮询`(round-robin)`的方式给消费者，每条消息只会发送给一个订阅的消费者。在消费者收到消息之后，其通过`AMQP`的`basic.ack`命令显示地向消息队列发送一个确认，当消息队列收到确认之后会将之前发送的消息从队列中移除。

联合起来，交换器（`exchange`）和绑定（`binding`）：

服务器会根据路由键将消息从交换器路由到队列，但它是如何处理投递到多个队列的情况？协议中定义的不同类型交换器发挥了作用，一共有四种类型：`direct`、`fanout`、`topic`和`headers`，每一种类型实现了不同的路由算法。`headers`交换器允许你匹配`AMQP`消息的`header`而非路由键。除此之外，`headers`交换器和`direct`交换器完全一致，但性能会差很多。

`direct`交换器非常简单，如果路由键匹配度的话，消息就被投递到对应的队列。服务器必须实现`direct`类型交换器，包含一个空白字符串名称的默认交换器。当声明一个队列时，它就自动绑定到默认交换器，并以队列名称作为路由键。第一个参数是消息体，第三个参数为路由键，这个路由键就是之前用来声明队列的名称。

```python
$channel->basic_push($msg, '', 'queue-name');
```

`fanout`交换器：这种类型的交换器会将收到的消息广播到绑定的队列上。消息通信的模式很简单，当发送一条消息到`fanout`交换器时，它会把消息投递给所有附加在此交换器的队列上。

`topic`交换器：`topic`交换器允许你实现有趣的消息通信场景，它使得来自不同源头的消息能够到达同一个队列。

```python
$channel->basic_publish($msg, 'logs-exchange', 'error.msg-inbox');
```

声明一个`msg-inbox-errors`队列，你可以将其绑定到交换器上来接收消息：

```python
$channel->queue_bind('msg-index-errors', 'logs-exchange', 'error.msg-inbox');
```

如果想要一个队列监听`msg-inbox`模块的所有`error`级别，可以通过将新的队列绑定到已有的同一个交换器来实现：

```python
$channel->queue_bind('msg-inbox-logs', 'logs-exchange', '*.msg-inbox');
```

`msg-inbox-logs`队列将会接收从`msg-inbox`模块发来的所有`error`、`warning`和`info`日志信息。那么如何接收所有的日志呢？单个`"."`把路由键分为了几部分，`"*"`匹配特定位置的任意文本。为了实现匹配所有规则，你可以使用`"#"`符号：

```python
$channel->queue_bind('all-logs', 'logs-exchange', '#');
```