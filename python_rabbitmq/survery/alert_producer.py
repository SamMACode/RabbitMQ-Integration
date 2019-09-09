#coding=utf-8
import json
import pika
from optparse import OptionParser

opt_parser = OptionParser()
opt_parser.add_option("-r",
                      "--routing-key",
                      dest="routing_key",
                      help="routing key for message " + \
                      " (e.g. myalert.im)")
opt_parser.add_option("-m",
                      "--message",
                      dest="message",
                      help="message text for alert.")
args = opt_parser.parse_args()[0]
# 建立到服务器的连接.
creds_broker = pika.PlainCredentials("alert_user", "alertme")
conn_params = pika.ConnectionParameters("localhost",
                                        virtual_host="/",
                                        credentials=creds_broker)
conn_broker = pika.BlockingConnection(conn_params)
channel = conn_broker.channel()

# 将告警信息发送给服务器.
msg = json.dumps(args.message)
msg_props = pika.BasicProperties()
msg_props.content_type = "application/json"
msg_props.durable = False

channel.basic_publish(body=msg,
                      exchange="alerts",
                      properties=msg_props,
                      routing_key='critical.*')
print("send message %s tagged with routing key '%s' to " + \
      "exchange '/'.") % (json.dumps(args.message), 'critical.*')
