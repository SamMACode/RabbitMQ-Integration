#coding=utf-8
import pika

credentials = pika.PlainCredentials("guest", "guest")
conn_params = pika.ConnectionParameters("localhost", credentials=credentials)
# 建立到rabbit的连接并且创建channel连接信道.
conn_broker = pika.BlockingConnection(conn_params)
channel = conn_broker.channel()
# 声明三个监听队列.
channel.queue_declare(queue="errors_queue")
channel.queue_declare(queue="warnings_queue")
channel.queue_declare(queue="info_queue")
# 声明交换器
exchange = 'rabbitmq.log'
# 声明交换器
channel.exchange_declare(exchange=exchange,
                         exchange_type="topic",
                         passive=False,
                         durable=True,
                         auto_delete=False)

# 将队列绑定到日志交换器上.
channel.queue_bind(queue="errors_queue",
                   exchange=exchange,
                   routing_key="error")
channel.queue_bind(queue="warnings_queue",
                   exchange=exchange,
                   routing_key="warning")
channel.queue_bind(queue="info_queue",
                   exchange=exchange,
                   routing_key="info")


# 定义回调函数function.
def error_callback(msg):
    print('error: ' + msg.body)


def warning_callback(msg):
    print('warning: ' + msg.body)


def info_callback(msg):
    print('info: ' + msg.body)


# 对给定的队列进行订阅consumer.
channel.basic_consume(error_callback, queue="errors_queue")
channel.start_consuming()
channel.basic_consume(warning_callback, queue="warnings_queue")
channel.start_consuming()
channel.basic_consume(info_callback, queue="info_queue")

