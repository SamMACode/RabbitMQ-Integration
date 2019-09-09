#coding=utf-8
import pika

credentials = pika.PlainCredentials("guest", "guest")
conn_params = pika.ConnectionParameters("localhost", credentials=credentials)
# 建立到rabbit的连接并且创建channel连接信道.
conn_broker = pika.BlockingConnection(conn_params)
channel = conn_broker.channel()
# 声明交换器exchange和队列queue
channel.exchange_declare(exchange="log-exchange",
                         exchange_type="topic",
                         passive=False,
                         durable=True,
                         auto_delete=False)
channel.queue_declare(queue="msg-inbox-errors")
channel.queue_declare(queue="msg-inbox-logs")
channel.queue_declare(queue="all-logs")

# 将队列与消息交换器exchange进行绑定.
channel.queue_bind(queue="msg-inbox-errors",
                   exchange="log-exchange",
                   routing_key="error.msg-inbox")
channel.queue_bind(queue="msg-inbox-logs",
                   exchange="log-exchange",
                   routing_key="*.msg-inbox")
