#coding=utf-8
import pika
import sys
from pika import spec

credentials = pika.PlainCredentials("guest", "guest")
conn_params = pika.ConnectionParameters("localhost", credentials=credentials)
# 连接到代理服务器.
conn_broker = pika.BlockingConnection(conn_params)
# 获得进行消息会话的信道
channel = conn_broker.channel()


# 1.发送方确认模式处理器(将message消息消费改为确认模式).
def confirm_handler(frame):
    if type(frame.method) == spec.Confrim.SelectOK:
        print("Channel in 'confrim' mode/")
    elif type(frame.method) == spec.Basic.Nack:
        if frame.method.delivery_tag in msg_ids:
            print("Message lost!")
    elif type(frame.method) == spec.Basic.Ack:
        if frame.method.delivery_tag in msg_ids:
            print("confrim received!")
            msg_ids.remove(frame.method.delivery_tag)


channel.confirm_delivery()  # 将信道设置为confrim模式

# 声明交换器
# channel.exchange_declare(exchange="hello-exchange",
#                          exchange_type="direct",
#                          passive=False,
#                          durable=True,
#                          auto_delete=False)

# 从运行的命令行cmd中获取得到第一个输入的参数.
msg = sys.argv[1]
msg_props = pika.BasicProperties()
msg_props.content_type = "text/plain"
msg_ids = []
# 进行发布消息.
ack = channel.basic_publish(body=msg,
                      exchange="hello-exchange",
                      properties=msg_props,
                      routing_key="hola")
if ack:
    print("put message to rabbitMQ success")
else:
    print("put message to rabbitMQ failed")

msg_ids.append(len(msg_ids) + 1)
channel.close()