"""
This will spawn a 0MQ server that publish states update via a PUB.
"""
import kivy
from kivy.app import App
from kivy.uix.floatlayout import FloatLayout
from kivy.properties import StringProperty

from kivy.lang import Builder


kv = '''
BoxLayout:
    orientation: 'vertical'

    BoxLayout:
        Button:
            text: 're-start'
            on_press: app.restart()

        BoxLayout:
            orientation: 'vertical'
            Label:
                text: 'host'

            BoxLayout:
                TextInput:
                    text: app.ip
                TextInput:
                    id: port
                    text: app.port
                    on_text: app.new_port()

    BoxLayout:
        Button:
            text: 'text to send'
            on_press: app.send()

        BoxLayout:
            orientation: 'vertical'
            TextInput:
                id: textinput_1
                text: 'red'
            TextInput:
                id: textinput_2
                text: 'green'

        BoxLayout:
            orientation: 'vertical'
            CheckBox:
                id: checkbox
                group: 'g'
                active: True

            CheckBox:
                group: 'g'
'''

import zmq

class SimplePublisher(object):
    """
    """
    def __init__(self, port):

        super(SimplePublisher, self).__init__()

        self.context = zmq.Context()
        self.socket = self.context.socket(zmq.PUB)
        self.socket.bind("tcp://*:%s" % port)

    def send_topic(self, topic, data):
        self.socket.send_string(topic+' '+data)

    def close(self):
        self.socket.close()
        self.context.term()


def get_own_ip():
    import socket
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8",80))
    ownIp = s.getsockname()[0]
    s.close()
    return ownIp


class TouchtracerApp(App):

    port = StringProperty()

    def build(self):
        self.ip = get_own_ip()
        self.port = '5556'
        self.pub = SimplePublisher(port = self.port)
        return Builder.load_string(kv)

    def send(self):
        if self.root.ids['checkbox'].active:
            strToSend = self.root.ids['textinput_1'].text
        else:
            strToSend = self.root.ids['textinput_2'].text

        print strToSend

        self.pub.send_topic('state', strToSend)

    def new_port(self):
        self.port = self.root.ids['port'].text

    def on_stop(self):
        self.pub.close()

    def restart(self):
        print self.port
        self.pub.close()
        self.pub = SimplePublisher(port = self.port)


if __name__ == '__main__':
    TouchtracerApp().run()
