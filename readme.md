## `RabbitMQ`消息中间件

> `RabbitMQ`是一个开源的消息代理和队列服务器，用来通过普通协议在完全不同的应用之间共享数据，或者简单地将作业排队以便让分布式服务器进行处理。

使用`docker`在本地安装`rabbitmq`消息中间件：

```shell
# 从镜像仓库获取rabbitmq镜像
docker pull rabbitmq:management
# 使用docker run命令初始化rabbitmq镜像实例
docker run -d -p 5672:5672 -p 15672:15672 --name rabbitmq rabbitmq:management
~e194a2dbeb52f2296dfb6d1c527cf052d82be5ed9a4c974d70dcd6af3da3eb7e
```

