#coding=utf-8
import json
import pika
import smtplib


# 定义回调函数
def send_mail(recipients, subject, message):
    """E-mail generator for received alerts."""
    headers = ("from: %s\r\nTo: \r\nDate: \r\n" + \
               "subject: %s\r\n\r\n") % ("alerts@ourcompany.com", subject)
    smtp_server = smtplib.SMTP()
    smtp_server.connect("mail.ourcompany.com", 25)
    smtp_server.sendmail("alerts@ourcompany.com",
                         recipients,
                         headers + str(message))
    smtp_server.close()


def critical_notify(channel, method, header, body):
    """sends critical alerts to administrators via e-mail."""
    email_recips = ["ops.team@ourcompany.com",]
    # 从json格式解码消息.
    message = json.loads(body)
    # send_mail(email_recips, "CRITICAL ALERT", message)
    print("send alert via e-mail! alert text: %s " + \
          "recipients: %s") % (str(message), str(email_recips))
    # 在消息已经被确认接收到的时候,向rabbit服务器发送ack指令,之后rabbit服务器才会向consumer发送新的消息.
    channel.basic_ack(delivery_tag=method.delivery_tag)


def rate_limit_notify(channel, method, header, body):
    """sends the message to the administrators via e-mail."""
    email_recips = ["api.team@ourcompany.com",]
    message = json.loads(body)
    # (f-asc_10) transmit e-mail to SMTP server
    send_mail(email_recips, "rate limit alert!", message)
    print("send alert via e-mail! alert text: %s " + \
          "recipients: %s") % (str(message), str(email_recips))
    channel.basic_ack(delivery_tag=method.delivery_tag)


if __name__ == "__main__":
    AMQP_SERVER = "localhost"

AMQP_USER = "alert_user"
AMQP_PASS = "alertme"
AMQP_VHOST = "/"
AMQP_EXCHANGE = "alerts"

# 建立到代理服务器的连接.
creds_broker = pika.PlainCredentials(AMQP_USER, AMQP_PASS)
conn_params = pika.ConnectionParameters(AMQP_SERVER,
                                        virtual_host=AMQP_VHOST,
                                        credentials=creds_broker)
conn_broker = pika.BlockingConnection(conn_params)
channel = conn_broker.channel()
# 声明交换器exchange,并且设置exchange交换器的类型为topic.当所有的消费者断开队列连接之后,交换器仍然存在.
channel.exchange_declare(exchange=AMQP_EXCHANGE,
                         type="topic",
                         auto_delete=False)
# 为告警topic声明并绑定队列和交换器<critical队列和critical.* topic绑定/rate_limit队列和*.rate_limit topic绑定>
channel.queue_declare(queue="critical", auto_delete=False)
channel.queue_bind(queue="critical",
                   exchange="alerts",
                   routing_key="critical.*")
channel.queue_declare(queue="rate_limit", auto_delete=False)
channel.queue_bind(queue="rate_limit",
                   exchange="alerts",
                   routing_key="*.rate_limit")
# 设置消费者订阅并启动监听程序<no_ack表示的是你想要手动的确认消息5>.
channel.basic_consume(critical_notify,
                      queue="critical",
                      no_ack=False,
                      consumer_tag="critical")
channel.basic_consume(rate_limit_notify,
                      queue="rate_limit",
                      no_ack=False,
                      consumer_tag="rate_limit")
print("ready for alerts!...")
channel.start_consuming()
