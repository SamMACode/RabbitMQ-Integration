#coding=utf-8
import pika
credentials = pika.PlainCredentials("guest", "guest")
conn_params = pika.ConnectionParameters("localhost",
                                        credentials=credentials)
# 建立到代理服务器的连接
conn_broker = pika.BlockingConnection(conn_params)
# 获得连接的信道
channel = conn_broker.channel()
# 声明交换器
channel.exchange_declare(exchange="hello-exchange",
                         exchange_type="direct",
                         passive=False,
                         durable=True,
                         auto_delete=False)
# 声明队列
channel.queue_declare(queue="hello-queue")
# 通过"hola"将队列与交换器绑定起来.
channel.queue_bind(queue="hello-queue",
                   exchange="hello-exchange",
                   routing_key="hola")

# 用于处理传入消息的函数.
def msg_consumer(channel, method, header, body) :
    channel.basic_ack(delivery_tag=method.delivery_tag)
    if body == "quit":
        channel.basic_cancel(consumer_tag="hello-consumer")
        channel.stop_consuming()
    else:
        print(body)     #在python2.7中是表达式,但是在python3中成为了函数.
    return

# 订阅消费者.
channel.basic_consume(msg_consumer,
                      queue="hello-queue",
                      consumer_tag="hello-consumer")
channel.start_consuming()